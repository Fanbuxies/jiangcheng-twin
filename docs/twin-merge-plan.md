# 把 wuhan-digital-twin 合并进 RuoYi，新增「数字孪生」栏目

> 交由另一个 agent（codex）独立执行，故写成自包含形式：所有路径、SQL、命令均可直接使用，不依赖会话上下文。
> 文档中每一条技术断言都经过实际验证，验证方式随条目注明。未经验证的推测一律标注 **[待验证]**。

## Context

`D:\Code\RouYi`（若依 4.8.3 单体版，已完成 MySQL→PostgreSQL 迁移，跑在 90 端口）要吸收独立项目 `D:\Code\wuhan-digital-twin`（武汉七区建筑白模三维底座 + 楼宇物联网监测）。

用户已拍板的四条，不可改：

1. **完全合并为单进程** —— twin 后端变成 RuoYi 的 Maven 模块，最终一个 jar、一个端口（90）
2. **左侧栏新增顶级栏目「数字孪生」**
3. **源码拷贝进 RouYi 仓库**（不用 submodule，拷贝后两边分叉）
4. **管理页用若依原生重写** —— 三维场景保留 Cesium SPA，建筑/设备/告警的增删改查改用若依 Thymeleaf 页面，纳入若依的权限、日志、导出体系

完成态：`http://localhost:90` 登录后，左侧出现「数字孪生」栏目，「三维底座」进 Cesium 场景，另三项是若依原生管理页，全程一套登录态、一套权限。

## 现状对比

| | RuoYi（宿主） | wuhan-digital-twin |
|---|---|---|
| Spring Boot | **4.1.0** | **3.2.12** |
| ORM | MyBatis 原生 + **PageHelper** | **MyBatis-Plus 3.5.5** |
| 连接池 | **Druid**（含 wall/stat filter） | HikariCP（Boot 默认） |
| 前端 | Thymeleaf + jQuery + Bootstrap | Vue3 + Vite5 + Cesium 1.115 |
| 安全 | Shiro 3.0 | 无 |
| 数据库 | `ry` @ localhost:5434 | `twin` @ localhost:5434（**同一 `twin-pg` 容器**） |
| Java | 17 | 17 |
| 体量 | — | 68 个 Java 文件 / 29 个前端源文件 |

同容器、同 Java 版本，是本次合并最大的两个便利条件。

---

## 一、可行性结论

**整体可行**，技术主线（去 MP 化 + 单进程 + iframe 嵌三维场景 + 管理页原生化）成立。以下为逐项验证结果。

### 已验证成立的判断

| 判断 | 验证方式与结果 |
|---|---|
| Spring Boot 3.2→4.1 迁移面小 | 枚举了 twin 全部 Spring/Jakarta import：`BeanUtils`、`ConfigurationProperties`、`@Transactional`、validation 与 web 注解等**全是长期稳定 API**，无冷门或已废弃调用 |
| 去 MP 化成本可控 | `extends ServiceImpl` **0 处**；BaseMapper 继承方法**仅 7 处调用**（`selectById`×3、`selectCount`×2、`deleteById`×2）；`Wrappers` **仅 2 处**且都是单条件 eq；业务查询全在 XML |
| 删 `TwinApplication` 不影响调度 | `@EnableScheduling` 挂在 `simulator/config/SimulatorConfig.java:23`，不在主类 |
| twin 的 mapper 能被若依扫到 | 文件名均为 `XxxMapper.xml`，匹配若依的 `classpath*:mapper/**/*Mapper.xml` |
| 接口路径无冲突 | twin 全在 `/api/**`，若依用 `/system|/monitor|/tool` |

### 风险清单

| 级别 | 风险 | 说明 |
|---|---|---|
| **P0** | **geometry 列不能走代码生成器** | `t_building.footprint geometry(Polygon,4326) NOT NULL`、`center geometry(Point,4326)`。若依 `GenUtils` 的类型映射表无 geometry，会 fallback 成 String，生成的表单出现填几何体的输入框，INSERT 撞 NOT NULL 约束必挂。**管理页必须复用 twin 现有 Service** |
| **P0** | **前端无 vue-router** | `src/router/` 不存在，`main.ts` 未 `.use(router)`，views 下只有 `TwinView.vue`。管理功能原本全在 `AdminDrawer.vue` 抽屉里。已由决策 4 解决（管理页原生重写），但要求 SPA 侧只保留三维场景 |
| **P1** | **Druid wall/stat filter × PostGIS SQL** | twin 原走 Hikari 无 SQL 解析，合并后走 Druid。twin SQL 含 `jsonb_build_object`/`jsonb_agg`/`::jsonb`/`::text`/`ST_AsGeoJSON` 嵌套子查询，Druid 的 PG 方言解析器覆盖有限，wall filter 解析失败会**拒绝执行**。**[待验证]** 必须在 Gate 1 实测 |
| **P1** | **两个 `@RestControllerAdvice` 互抢** | 若依 `GlobalExceptionHandler` 与 twin 的同名类**都无 `@Order`、都无 `basePackages` 限定**，且都处理 `BindException` 等相同异常。twin 的异常可能被若依处理成 `AjaxResult`(code=500)，而 twin 前端 `request.ts` 只认 `code===200`，会把所有业务错误显示成"网络请求失败" |
| **P2** | PageHelper 对 PostGIS 分页 SQL 的 count 改写 | 原方案称去 MP 化"零回归风险"**不准确**：对若依是零风险，但对 twin 是**新引入**的未验证组合。已查分页 SQL（`selectBuildingPage`）结构简单（无 group by/distinct/union，仅含 `ST_X`/`ST_Y` 投影列），PageHelper 应能正确生成 count，但需实测 |
| **P2** | 模拟器默认开启 | `SimulatorConfig` 的 `@ConditionalOnProperty(matchIfMissing = true)` 意味着**不配置就跑**。合并后若忘记显式关闭，两个任务（3s/6s）会立刻开始刷日志 |
| **P2** | session 过期后 SPA 的表现 | 过期后 `/api/**` 被 Shiro 拦截，twin 前端收到若依的响应格式或登录页 HTML，只会提示"网络请求失败"，用户不知道该重新登录 |
| **P3** | 演示模式 | 若依 `demoEnabled: true`，`DemoModeException` 可能拦截 twin 的写操作，联调期建议置 false |
| **P3** | iframe 中的 Cesium 生命周期 | 若依 tab 切换不销毁 iframe，多开 tab 会有多个 WebGL context 常驻显存；tab 关闭时 viewer 是否正确 destroy 需观察 |

---

## 二、开工前的三个 Gate

这三项都可能推翻后续大量工作，**必须在写业务代码前先做**，每项半小时内出结论。任一 Gate 失败，先解决再往下走。

### Gate 1：Druid 能否放行 PostGIS SQL（P1）

在 `ry` 库装好 PostGIS 后，用若依进程的 Druid 数据源执行一条 twin 的真实复杂 SQL（取 `BuildingMapper.xml` 的 `selectGeoJson`，含 `jsonb_build_object` + `::jsonb` + 嵌套子查询）。

- **通过** → 继续
- **失败**（Druid 报 SQL 解析错误或 wall 拒绝）→ 在 `application-druid.yml` 的 `filter` 段调整：优先尝试关掉 `wall`，其次给 `stat` 关掉 `merge-sql`。改动记录在案，因为这会削弱若依原有的 SQL 防护，需要在文档里写明取舍

### Gate 2：PageHelper 能否正确分页 twin 的查询（P2）

改造一个 Mapper（建议 `BuildingMapper.selectBuildingPage`）为 PageHelper 形式，跑通分页 + 总数正确 + 翻到第二页数据正确。

- **通过** → 按同一模式改其余 6 个
- **失败** → 退回方案 B：twin 侧自己在 SQL 里写 `LIMIT/OFFSET`，Service 层手工查 count，不依赖任何分页插件

### Gate 3：编译能否通过（Boot 3.2 → 4.1）

只做阶段 1 的搬迁与 pom 改写，跑 `mvn clean package -DskipTests`。静态分析显示 API 面干净，预期顺利，但这是唯一能证明的方式。重点关注 `spring-boot-starter-websocket` 与 springdoc 2.3.0→3.1.0。

---

## 三、目标结构

```
D:\Code\RouYi\
├─ ruoyi-twin/                        新 Maven 模块（源自 wuhan-digital-twin/backend）
│  ├─ pom.xml                            parent 改 ruoyi，删 spring-boot-starter-parent
│  └─ src/main/
│     ├─ java/com/ruoyi/twin/            包名 com.wuhan.twin → com.ruoyi.twin
│     │  ├─ building/ device/ facility/ alarm/ stat/    业务模块，Service 层双向复用
│     │  ├─ simulator/ ws/               模拟器与实时推送
│     │  ├─ web/                         【新增】若依风格管理页 Controller
│     │  └─ common/                      R/PageResult/BizException，异常处理需限定包
│     └─ resources/mapper/twin/*.xml
├─ ruoyi-admin/src/main/resources/
│  ├─ static/twin/                       Cesium SPA 产物（assets + cesium，约 13MB）
│  └─ templates/twin/                    壳页 + 三个管理页（building.html 等）
├─ data-prep/                            Python 数据准备脚本
├─ docs/twin/                            architecture / requirements / runbook
└─ sql/twin/                             schema.sql + menu.sql
```

**包名必须改为 `com.ruoyi.twin`**：若依主类 `com.ruoyi.RuoYiApplication` 的组件扫描够不到 `com.wuhan`，`typeAliasesPackage: com.ruoyi.**.domain` 也扫不到。改包名比打 `@ComponentScan` 补丁干净。

---

## 四、三个关键技术决策

### 决策 1：ORM 走「twin 去 MyBatis-Plus 化」

两条路：让 RuoYi 换 `mybatis-plus-spring-boot4-starter`（保 twin 的 MP 代码），或让 twin 去 MP 统一到若依的 MyBatis+PageHelper。

**选后者。** 依据是 MP 依赖极浅（见上表验证数据），去 MP 化只动 twin 侧约 15 个文件，**若依一行不改**。反之要改 [MyBatisConfig.java:117](ruoyi-framework/src/main/java/com/ruoyi/framework/config/MyBatisConfig.java:117) 手工构造的 `SqlSessionFactory`，并赌 PageHelper 与 MP 分页拦截器在 Boot 4 下共存——PageHelper 是若依全部列表分页的基础，冲突即全局故障，且该组合无成熟先例。用宿主的全局风险换被合并方 15 个文件，不划算。

**表述修正**：这不是"零风险"，而是**把风险从宿主转移到被合并方**。twin 侧的分页由 Gate 2 兜底。

### 决策 2：管理页复用 twin 的 Service，只新增 Controller 与页面

这是 P0 约束的直接结果。`t_building` 的 `footprint`/`center` 是 geometry 列，代码生成器不认识，而 twin 的 `BuildingServiceImpl` 已经处理了几何构造（`insertBuilding` 按中心点 + `FOOTPRINT_HALF_EXTENT` 构造矩形 footprint）。

**正确做法：**

- **Service / Mapper**：完全复用 twin 现有的，**不用代码生成器生成**
- **Controller**：手写在 `com.ruoyi.twin.web` 下，`extends BaseController`，注入 twin 的 Service，用 `startPage()` + `getDataTable()` 返回 `TableDataInfo`。模板参照 [SysPostController.java](ruoyi-admin/src/main/java/com/ruoyi/web/controller/system/SysPostController.java)
- **页面**：可用代码生成器生成 `.html` + `.js` 骨架，但**必须手工删掉 footprint/center 的表单项，换成 lon/lat 两个数字输入框**（与 `BuildingSaveDTO` 对齐）

**顺带的架构收益**：去 MP 化要求 Mapper 从 `IPage` 改成返回 `List`，这正好让 Service 层统一暴露 List 返回、分页交给调用方用 PageHelper 控制。SPA 的 `/api` Controller 与管理页 Controller 复用同一个 Service 方法，只是各自包装成 `PageResult` 或 `TableDataInfo`。**两处需求合并改造，一次到位。**

### 决策 3：两套响应体系并存，用包路径隔离

twin 的 `R<T>`（成功码 200）与若依的 `AjaxResult`（成功码 0）不强行统一——SPA 走前者，管理页走后者。但必须解决异常处理器互抢：

- twin 的 `GlobalExceptionHandler` 改为 `@RestControllerAdvice(basePackages = "com.ruoyi.twin")` 并加 `@Order(1)`（优先级高于若依的）
- **注意**：新增的管理页 Controller 也在 `com.ruoyi.twin.web` 下，会被 twin 的 handler 捕获，但它需要的是若依的 `AjaxResult` 格式。因此 twin 的 handler 应限定到具体业务包（`com.ruoyi.twin.building`、`.device`、`.facility`、`.alarm`、`.stat`），**把 `com.ruoyi.twin.web` 留给若依的 handler**

---

## 五、执行步骤

### 阶段 1：搬迁源码，编译通过（Gate 3）

1. 原仓库 `D:\Code\wuhan-digital-twin` 打 tag 留锚点（如 `pre-merge-20260820`）
2. 拷 `backend/` → `ruoyi-twin/`，**排除 `target/`、`logs/`**
3. 拷 `data-prep/` → `data-prep/`（排除 `output/`、`logs/`、`__pycache__/`）；`docs/*.md` → `docs/twin/`；`db/schema.sql` 另存一份到 `sql/twin/`
4. **绝对不要拷 `pgdata/`** —— 那是 `twin-pg` 容器正在挂载的数据卷，拷贝无意义且可能损坏数据
5. 全量替换包名 `com.wuhan.twin` → `com.ruoyi.twin`（Java 的 package/import、mapper XML 的 namespace 与 resultType、yml 中的类引用），目录同步迁移
6. 改写 `ruoyi-twin/pom.xml`：
   - parent 改 `com.ruoyi:ruoyi:4.8.3`，删 `spring-boot-starter-parent` 与自身 `<version>`
   - 删 `spring-boot-maven-plugin`（打包由 ruoyi-admin 负责）、`postgresql`（父 pom 已管理）、**`mybatis-plus-spring-boot3-starter`**
   - 保留 `spring-boot-starter-validation`、`spring-boot-starter-websocket`、`net.postgis:postgis-jdbc`、`lombok`
   - springdoc 去掉自己的 2.3.0 版本号，继承父 pom 的 3.1.0
   - 新增对 `ruoyi-common` 的依赖（要用 `BaseController`、`TableDataInfo`、PageHelper 封装）
7. 父 pom `<modules>` 加 `<module>ruoyi-twin</module>`；`ruoyi-admin/pom.xml` 加 `ruoyi-twin` 依赖（参照已有的 `ruoyi-generator` 依赖块）
8. twin `application.yml` 的 `app.*` 配置段整段并入 `ruoyi-admin/src/main/resources/application.yml`。**tileset 相机那组数字带大段实测标定注释，必须连注释原样搬** —— 那是踩坑标定出来的，动了会导致初始视角看不到白模
9. 删 `TwinApplication.java`（`@EnableScheduling` 在 `SimulatorConfig` 上，不受影响）
10. **立刻把 `app.simulator.enabled` 显式设为 `false`** —— 该配置 `matchIfMissing = true`，不写就是开启

**Gate 3**：`mvn clean package -DskipTests` 通过。

### 阶段 2：数据库合并 + Gate 1

```bash
docker exec twin-pg psql -U twin -d ry -c "CREATE EXTENSION IF NOT EXISTS postgis"
docker exec twin-pg pg_dump -U twin -d twin --schema=public --no-owner -T spatial_ref_sys > twin_dump.sql
docker exec -i twin-pg psql -U twin -d ry < twin_dump.sql
```

- `t_device_telemetry`、`t_partition_probe` 是**按天分区表**（当前已建到 2026-09-02），确认子分区定义完整导入
- `spatial_ref_sys` 由扩展自带，必须排除，否则冲突
- pg_dump 可能带出 `CREATE EXTENSION` 语句，导入前检查是否与上一步重复
- **`twin` 库在全部验证通过前不要删**，留作回滚
- 分区维护脚本 `data-prep/scripts/ops/run_partition_maintenance.ps1` 及 `data-prep/` 下所有 Python 脚本的目标库从 `twin` 改为 `ry`（Windows 计划任务 `WuhanTwin-PartitionMaintenance` 每日 03:17 调用）

**Gate 1**：跑通 `selectGeoJson` 那条复杂 SQL，确认 Druid 不拦截。

### 阶段 3：去 MyBatis-Plus 化 + Gate 2

改动**只在 `ruoyi-twin` 内**，不得修改 ruoyi-framework / ruoyi-common / ruoyi-system 任何文件。

1. **5 个 entity 删 MP 注解**（building/device/deviceRealtime/facility/alarm 的 `XxxDO.java`）：
   - `@TableName` / `@TableId(type = IdType.AUTO)` → 删，主键回填改由 XML 的 `useGeneratedKeys="true" keyProperty="id"` 承担（`insertBuilding` 已是此写法）
   - `@TableField(exist = false)` → 删。这些是 `lon`/`lat`/`geojson` 等 SQL 计算列，现有 XML 用显式 `resultMap` 映射，删注解不影响
2. **5 个 Mapper 去掉 `extends BaseMapper<XxxDO>`**，补齐 7 处继承方法：
   - `BuildingMapper`：`selectById`、`deleteById`
   - `DeviceMapper`：`selectById`、`deleteById`，以及把 `Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getBuildingId, id)` 改写成 `countByBuildingId(@Param("buildingId") Long)`
   - 调用点：`building/service/impl/BuildingServiceImpl.java:163,168,175`、`device/service/impl/DeviceServiceImpl.java:128,148,157,166`
   - **新写的 `selectById` 严禁用 `SELECT *`** —— 会带出 `footprint`/`center` 两个 geometry 列，映射到 DO 必然失败。照现有 `selectDetailById` 的写法，显式列出字段，几何列用 `ST_X(center) AS lon` 这类投影
3. **分页改 PageHelper**（7 个文件）：
   - 原：`Page<T> page = new Page<>(current, size); IPage<T> r = mapper.selectXxxPage(page, ...)`
   - 新：`PageHelper.startPage(current, size);` 紧接 `List<T> list = mapper.selectXxxPage(...);` 再 `new PageInfo<>(list)`
   - Mapper 签名去掉 `IPage` 首参，返回改 `List<T>`；XML 不动（PageHelper 自动改写）
   - **`startPage()` 与查询之间不得插入任何其他语句**，否则 ThreadLocal 分页参数会串到别的查询上
4. `common/result/PageResult.java` 的 `of(IPage)` 改为接收 `PageInfo`（`getTotal()`/`getPageNum()`/`getPageSize()`），保留 `of(List, total, current, size)` 重载
5. **删 `common/config/MybatisPlusConfig.java`**。注意其中 `MAX_LIMIT = 500` 的单页上限保护随之失效，**需在 Service 层参数校验里补回**（PageHelper 无等价配置，缺了这个就是个恶意大分页入口）
6. 删 yml 里的 `mybatis-plus:` 段；mapper XML 移到 `resources/mapper/twin/` 子目录避免与若依的混放

**Gate 2**：先只改 `selectBuildingPage` 验证分页正确，再批量改其余。
**完成标志**：全局搜 `com.baomidou` 结果为 0。

### 阶段 4：异常处理隔离

按决策 3 改造：

- twin 的 `common/exception/GlobalExceptionHandler.java` 改为 `@RestControllerAdvice(basePackages = {"com.ruoyi.twin.building", "com.ruoyi.twin.device", "com.ruoyi.twin.facility", "com.ruoyi.twin.alarm", "com.ruoyi.twin.stat"})`，加 `@Order(1)`
- `com.ruoyi.twin.web`（管理页 Controller）**不列入**，让它落到若依的 handler，返回 `AjaxResult`
- 验证：故意触发一次参数校验失败，确认 `/api/**` 返回 twin 的 `R` 格式、管理页接口返回若依的 `AjaxResult` 格式

### 阶段 5：管理页（若依原生）

对 building / device / alarm 三个模块，各做一套：

1. Controller 放 `com.ruoyi.twin.web`，`extends BaseController`，注入 twin 现有 Service。方法签名照 [SysPostController.java](ruoyi-admin/src/main/java/com/ruoyi/web/controller/system/SysPostController.java)：`@GetMapping()` 返回页面、`@PostMapping("/list")` + `startPage()` + `getDataTable()`、add/edit/remove 走 `toAjax()`
2. 权限注解 `@RequiresPermissions("twin:building:view|list|add|edit|remove")`，与菜单 `perms` 对齐
3. 页面放 `templates/twin/`，可用代码生成器出骨架，但**必须手工处理几何字段**：删掉 footprint/center 表单项，换成 lon/lat 数字输入（对齐 `BuildingSaveDTO`）
4. 操作日志：写操作加 `@Log(title = "建筑管理", businessType = BusinessType.INSERT)` 等，这是纳入若依体系的主要收益之一
5. Excel 导出按需，若做则在 DO/VO 字段上加 `@Excel` 注解

### 阶段 6：三维场景嵌入

1. 前端以子路径重新构建（Vite 默认 `base=/`，不改则产物里的绝对路径在若依下全 404）：
   ```bash
   npm run build -- --base=/twin/
   ```
2. 产物落位：
   - `dist/assets/` + `dist/cesium/` → `ruoyi-admin/src/main/resources/static/twin/`（约 13MB，可进 jar）
   - **`dist/tiles/`（63MB）不进 jar 也不进 git** → 放 `D:/ruoyi/twinTiles/`，照 [ResourcesConfig.java:45](ruoyi-framework/src/main/java/com/ruoyi/framework/config/ResourcesConfig.java:45) 现有 `/profile/**` 那条，新增一条把 `/twin/tiles/**` 映射过去
3. 壳页 `templates/twin/index.html`：参照原 `frontend/dist/index.html`，把 `/assets`、`/cesium` 前缀改为 `/twin/...`。配一个 Controller 方法映射 `/twin/index` 到该模板
4. 前端接口层无需改动：`baseURL: '/api'` 与 `/ws/realtime` 合并后天然同源，Vite proxy 在生产构建中本就不生效
5. 删 twin 的 `common/config/WebMvcConfig.java` 的 CORS 放行 —— 同源后不需要，留着白白扩大攻击面
6. **Shiro 放行：先不加任何 anon 规则，直接实测。** 静态资源与切片是同源请求，浏览器会自动带 `JSESSIONID`，Shiro 的 `user` 规则应当放行已登录会话。只有实测发现 Cesium 加载失败时，才按最小范围补 anon。原方案里"直接放行 `/twin/tiles/**`"会让未登录用户能拖走整份城市建筑数据，是不必要的暴露
7. **SPA 的 session 过期处理**：在 `frontend/src/api/request.ts` 的响应拦截器里补一个分支——识别出未登录响应（HTTP 302 / 返回 HTML / 若依的未登录 code），提示用户并 `top.location.href = '/login'` 跳出 iframe 到登录页

### 阶段 7：菜单与收尾

菜单 SQL（`sql/twin/menu.sql`），`menu_type`：`M`=目录、`C`=菜单：

```sql
insert into sys_menu(menu_id, menu_name, parent_id, order_num, url, target, menu_type, visible, perms, icon, create_by, create_time, remark)
values
 (2000, '数字孪生', 0,    5, '#',              '',         'M', '0', null,                'fa fa-cube',      'admin', now(), '数字孪生目录'),
 (2001, '三维底座', 2000, 1, '/twin/index',    'menuItem', 'C', '0', 'twin:scene:view',   'fa fa-globe',     'admin', now(), ''),
 (2002, '建筑管理', 2000, 2, '/twin/building', 'menuItem', 'C', '0', 'twin:building:view','fa fa-building',  'admin', now(), ''),
 (2003, '设备管理', 2000, 3, '/twin/device',   'menuItem', 'C', '0', 'twin:device:view',  'fa fa-microchip', 'admin', now(), ''),
 (2004, '告警记录', 2000, 4, '/twin/alarm',    'menuItem', 'C', '0', 'twin:alarm:view',   'fa fa-bell',      'admin', now(), '');

select setval('sys_menu_menu_id_seq', (select max(menu_id) from sys_menu));
```

用 2000+ 的 ID 段避开若依自身菜单。**末尾的 `setval` 必须执行**，否则以后在页面上新增菜单会主键冲突。按钮级权限（add/edit/remove）如需精细控制，再往下挂 `menu_type='F'` 的子项。

收尾项：

1. **日志降噪**：yml 里 `com.ruoyi` 是 `debug`，包名改成 `com.ruoyi.twin` 后模拟器（3s/6s 各一个）的 SQL 日志会淹没日志文件。**必须加 `com.ruoyi.twin.simulator: warn`**
2. 全部功能验证通过后，再把 `app.simulator.enabled` 打开
3. `.gitignore` 补：`ruoyi-admin/src/main/resources/static/twin/tiles/`、`data-prep/output/`、`data-prep/logs/`、`__pycache__/`
4. twin 原 `.env` 的 `POSTGRES_PASSWORD` **不要拷进 RouYi 仓库**，口令沿用若依 `application-druid.yml` 既有配置
5. 原 twin `CLAUDE.md` 的项目约束（Controller 统一返回、分层严格、几何运算交给 PostGIS、Cesium 单例与 Primitive API）并入 RouYi 的 CLAUDE.md，否则这些约定会随合并丢失

---

## 六、验证清单

### 回归验证（证明"没碰宿主"这一设计前提成立）

**这一组不可跳过。** 整个方案的立足点就是"若依一行不改"，这是唯一能证明它的检查。

1. 系统管理 → 用户 / 角色 / 菜单 / 部门 / 岗位 / 字典 / 参数：列表、分页、翻页、条件查询
2. 系统监控 → 在线用户 / 定时任务 / 登录日志 / 操作日志
3. 系统工具 → 代码生成（表导入、预览）
4. 重点确认**分页总数与翻页结果正确**（PageHelper 是本次唯一与宿主共享的组件）

### 新功能验证

5. `mvn clean package -DskipTests` 通过；全局搜 `com.baomidou`、`com.wuhan` 均为 0
6. 启动后日志出现 `Started RuoYiApplication`，无 Druid 连接异常、无 MyBatis 映射解析错误
7. 左侧出现「数字孪生」，四个子菜单可点开
8. 三维底座：Cesium 场景加载、白模可见（白屏先查 `/twin/tiles/tileset.json` 是否 200）、点选建筑弹属性面板
9. 三个管理页：列表分页、条件查询、新增/编辑/删除。**重点验证新增建筑**——填 lon/lat 后应由 PostGIS 正确构造 footprint 几何，去库里 `SELECT ST_AsText(footprint)` 确认不是空或非法值
10. 操作日志里能看到管理页的增删改记录（验证已纳入若依体系）
11. 打开 `app.simulator.enabled=true`：实时状态 3 秒刷新、WebSocket `/ws/realtime` 握手成功、告警列表有新增
12. 未登录直接访问 `http://localhost:90/twin/index` 应跳登录页；登录后在 SPA 里等到 session 过期，确认提示合理而非"网络请求失败"
13. 观察日志增长速率，确认模拟器没刷屏

---

## 七、执行约束

- 遵守 RouYi 根目录 CLAUDE.md 与用户全局规约：不做未要求的重构/格式化/重命名，不新增未确认的依赖，保持文件原有缩进与换行符（本仓库 XML 多为 CRLF），注释用中文
- Java/SQL/MyBatis 遵循阿里巴巴 Java 开发手册【强制】级条款（原 twin 项目有 `.claude/rules/java-alibaba.md`，可一并搬入）
- **每个阶段结束提交一次**。阶段 1、3 改动量大且机械，分开提交便于二分定位
- 数据库写操作前先 `SELECT` 确认影响行数；DDL 先输出 SQL 确认
- 三个 Gate 的结论要记录下来（尤其 Gate 1 若改了 Druid filter 配置，属于对宿主的改动，必须显式说明取舍）

## 八、已知的坑

- **Git Bash 跑 docker 必须禁用 MSYS 路径转换**：`-v 宿主路径:/app/output` 的容器路径会被翻译成 Git 安装目录，产物写到 `D:/Soft/Git/app/output/`。加 `MSYS_NO_PATHCONV=1`，或容器路径写成 `//app/output`。PowerShell 无此问题
- 3D Tiles 由 `geodan/pg2b3dm` 镜像生成（本机无 dotnet），容器内连库用主机名 `twin-pg:5432`，需 `--network wuhan-digital-twin_default`。**合并后若 compose 项目名变了，网络名要跟着改**
- 8080 / 5173 可能仍被原 twin 进程占用；合并后这两个端口不再需要，排查时注意别连错后端
- 若依 admin 账号密码是 `admin`（**不是**默认的 `admin123`），种子数据被改过
- 开工前先停掉占用 90 端口的若依进程，否则重新打包时 jar 被占用
