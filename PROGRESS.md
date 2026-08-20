# 数字孪生合并进度

更新时间：2026-08-20 15:26（Asia/Shanghai）

## 阶段 1：搬迁源码，编译通过

- 状态：已提交
- 提交：`edcaef43 feat: 搬迁数字孪生模块源码`
- Gate 3：通过
  - 命令：`mvn clean package -DskipTests`
  - 结果：8 个 Reactor 模块全部 `SUCCESS`，`BUILD SUCCESS`
- 改动文件：
  - `pom.xml`
  - `ruoyi-admin/pom.xml`
  - `ruoyi-admin/src/main/resources/application.yml`
  - `ruoyi-twin/**`（完整文件清单用 `git show --name-only edcaef43` 查看）
  - `data-prep/**`
  - `docs/twin/**`
  - `sql/twin/schema.sql`
- 验证清单：
  - 第 5 项部分通过：完整 Maven 构建通过；最终的 `com.baomidou`、`com.wuhan` 清零尚未完成
  - 第 6 项未通过：去 MyBatis-Plus 前，应用运行时存在 PageHelper/MyBatis-Plus 依赖冲突
- 已处理的实测差异：
  - twin 与宿主异常处理器默认 Bean 同名，已为 twin advice 指定独立 Bean 名并限定业务包
  - Spring Boot 4.1 使用 Jackson 3，twin 的 JSON 类型已从 Jackson 2 迁移至 Jackson 3

## 阶段 2：数据库合并与 Druid 验证

- 状态：已提交
- 提交：`48d09de1 chore: 切换数据脚本到若依数据库`
- Gate 1：通过
  - `ry` 已启用 PostGIS 3.4
  - Druid 1.2.28 的 `stat + wall` 成功执行完整 `selectGeoJson` SQL
  - 未关闭 wall，未修改 Druid filter 配置
- 数据导入结果：
  - `t_building`：29,818 行
  - `t_device`：2,000 行
  - `t_device_telemetry`：17 个子分区
  - `t_partition_probe`：8 个子分区
  - 原 `twin` 库保留，未删除
- 改动文件：
  - `data-prep/scripts/load/load_districts.py`
  - `data-prep/scripts/load/load_facilities.py`
  - `data-prep/scripts/load/load_to_pg.py`
  - `data-prep/scripts/ops/run_partition_maintenance.ps1`
  - `data-prep/scripts/seed/seed_devices.py`
- 验证清单：
  - 6 个 Python 文件通过 AST 解析
  - PowerShell 维护脚本通过语法解析，UTF-8 BOM 保留
  - Windows 计划任务 `WuhanTwin-PartitionMaintenance` 存在且为 Ready

## 阶段 3：去 MyBatis-Plus 化

- 状态：已提交
- 提交：`3747822d refactor: 移除数字孪生 MyBatis-Plus`
- Gate 2：通过
  - 探针：`ruoyi-twin/src/test/java/com/ruoyi/twin/gate/BuildingPaginationGate.java`
  - 结果：`GATE2_PASS total=29818 page1=20 page2=20 firstPageLastId=3536 secondPageFirstId=3537`
  - 验证内容：总数、前两页各 20 条、页间 ID 不重复且顺序连续
  - 历史失败根因：MyBatis-Plus 3.5.5 传递 JSqlParser 4.6，覆盖了 PageHelper 6.1.1 所需的 4.7；阶段 3 删除 MyBatis-Plus 后冲突彻底消失
- 完成内容：
  - 5 个 entity 删除 MyBatis-Plus 注解
  - 5 个 Mapper 删除 `BaseMapper`，继承方法改为显式 SQL
  - 建筑、设备、设施分页统一改为 PageHelper
  - Mapper XML 移入 `resources/mapper/twin/`
  - 删除 MyBatis-Plus 依赖、配置类与 yml 配置段
  - 3 个分页 DTO 均保留 `@Max(500)`，对应入口均有 `@Valid`
- 阶段验证：
  - `mvn clean package -DskipTests`：8 个 Reactor 模块全部 `SUCCESS`
  - `ruoyi-twin` 搜索 `com.baomidou` / `BaseMapper` / `IPage` / `Wrappers`：0 结果
  - 源码与资源搜索 `com.wuhan`：0 结果；方案与进度文档保留历史说明
  - 91 端口启动出现 `Started RuoYiApplication`，Druid 正常初始化，无 MyBatis 映射解析错误，`/login` 返回 200
  - 宿主回归：系统管理 7 页、系统监控 4 页及代码生成列表均正常加载；用户条件查询返回 admin 单条；参数页第 2 页为第 11/11 条，操作日志第 2 页为第 11–16/16 条

## 阶段 4：异常处理隔离

- 状态：已提交
- 提交：`f33a94cf test: 验证数字孪生异常隔离`
- 生产代码：阶段 1 解决 Bean 冲突时已提前完成，本阶段未再改生产逻辑
  - Bean 名：`twinGlobalExceptionHandler`
  - 优先级：`@Order(1)`
  - 包范围：building / device / facility / alarm / stat
  - `com.ruoyi.twin.web` 明确不在 twin advice 范围内
- 验证探针：`ruoyi-twin/src/test/java/com/ruoyi/twin/gate/ExceptionIsolationGate.java`
  - 结果：`EXCEPTION_ISOLATION_PASS twinCode=405 hostCode=500 packages=5 order=1`
  - 运行时触发 `GET /api/building/0`，日志确认由 twin `handleConstraintViolationException` 处理
  - 完整构建通过；宿主 12 个回归页面再次正常加载，参数与操作日志第二页结果保持正确
  - 管理页 Controller 尚未进入阶段 5，实际管理接口的 `AjaxResult` HTTP 响应留待阶段 5 联调

## 阶段 5：若依管理页

- 状态：已提交
- 提交：`041b9cea feat: 新增数字孪生管理页`
- 新增管理入口：
  - `BuildingManageController`：建筑列表、新增、编辑、删除
  - `DeviceManageController`：设备列表、新增、编辑、删除
  - `AlarmManageController`：告警列表、新增、编辑、删除
- 新增模板：`templates/twin/{building,device,alarm}/` 下列表、add、edit 共 9 个页面
- 建筑几何处理：
  - 未使用代码生成器，Controller 复用 `BuildingService` 与现有 Mapper
  - 表单仅提交 lon/lat 数字输入，不提交 footprint / center
  - footprint 与 center 仍由 `BuildingMapper.xml` 中的 PostGIS 函数构造
- 管理端分页：VO 转换后保留 PageHelper 的 total、页码和页大小元数据
- 阶段验证：
  - `mvn -pl ruoyi-twin -am test-compile -DskipTests`：通过
  - `ManagementControllerGate`：`MANAGEMENT_CONTROLLER_PASS controllers=3 routes=21 templates=9 total=5 rows=2`
  - Gate 覆盖 Controller 继承关系、路由、权限、写操作日志、返回类型、模板、禁止 Mapper 注入、分页 total 回归与建筑表单字段
  - `mvn clean package -DskipTests`：8 个 Reactor 模块全部 `SUCCESS`，`BUILD SUCCESS`

## 阶段 6：三维场景嵌入

- 状态：已提交
- 提交：`2c6e75f7 feat: 嵌入数字孪生三维场景`
- 前端按 `npm run build -- --base=/twin/` 构建，类型检查与 Vite 构建通过
- `assets` 与 `cesium` 产物进入 `static/twin/`，共 13,188,536 字节
- `tiles` 未进入静态资源和 Git，111 个文件、65,727,963 字节落在 `D:/ruoyi/twinTiles/`
- `ResourcesConfig` 新增 `/twin/tiles/**` 磁盘映射，继续受 Shiro `/** = user` 规则保护，未增加 anon
- 新增 `/twin/index` 场景入口及 `templates/twin/index.html` 壳页
- 删除 twin 跨域配置；同源接口继续使用 `/api` 与 `/ws/realtime`
- SPA 已识别登录页 HTML 与 401/403，提示会话失效并跳出 iframe 到 `/login`
- `SceneEmbeddingGate`：`SCENE_EMBEDDING_PASS`
- `mvn clean package -DskipTests`：8 个 Reactor 模块全部 `SUCCESS`，`BUILD SUCCESS`

## 阶段 7：菜单与收尾

- 状态：已提交
- 提交：`b921ac02 feat: 完成数字孪生菜单与联调收尾`
- 菜单：`sql/twin/menu.sql` 已写入并执行，`sys_menu` 的 2000–2004 共 5 行存在，菜单序列已推进到 2004
- 日志：`com.ruoyi.twin: info` 与 `com.ruoyi.twin.simulator: warn` 生效，模拟器运行时不再输出 twin Mapper SQL
- 模拟器：`app.simulator.enabled: true`，默认启动后实时表按 3 秒周期刷新
- 会话失效：Axios 通过最终响应 URL 识别 Shiro 跳转到 `/login`，提示会话失效并跳出 SPA；前端重新按 `--base=/twin/` 构建
- 临时验证数据：建筑、设备、告警、代码生成候选表均已通过 Controller 清理，数据库复核为 0

## 验证清单总览

1. 系统管理回归：通过（用户/角色/菜单/部门/岗位/字典/参数均返回 200；用户条件查询为 admin 单条；参数第 2 页 total=11、rows=1）
2. 系统监控回归：通过（在线用户、定时任务、登录日志、操作日志均返回 200；操作日志第 2 页 total=29、rows=10）
3. 系统工具代码生成回归：通过（安全表 `sys_config` 导入成功，预览生成 10 个文件，随后通过 Controller 删除且主表/列明细均为 0）
4. 宿主分页总数和翻页：通过（参数与操作日志的 total、第二页条数正确；用户条件查询 total=1）
5. 构建与包名/依赖清零：通过（完整 Maven 构建通过；代码与资源中的 `com.baomidou`、`com.wuhan` 均清零，文档保留历史文本）
6. 应用启动与映射解析：通过（出现 `Started RuoYiApplication`，Druid 初始化正常，无 MyBatis 映射解析错误）
7. 数字孪生菜单：通过（左侧目录及三维底座、建筑管理、设备管理、告警记录四个子菜单可见可打开）
8. Cesium 场景：通过（Cesium、哈希资源、`/twin/tiles/tileset.json` 与 glb 均为 200；29,818 栋白模可见；场景点击建筑再次请求详情并弹出属性面板）
9. 三个管理页 CRUD：通过（列表分页、条件查询、新增/编辑/删除均成功；新增建筑 footprint `ST_IsValid=true`、非空，中心点及多边形由 PostGIS 正确构造）
10. 操作日志：通过（建筑、设备、告警各自 INSERT/UPDATE/DELETE 共 9 条，admin，status=0）
11. 模拟器与 WebSocket：通过（默认开关已打开；实时表时间戳按 3 秒推进；`/ws/realtime` 握手为 OPEN；概率 1.0 验证产生 PENDING 告警后已清理临时设备）
12. 未登录和会话过期：通过（匿名访问 `/twin/index` 跳 `/login`；登录后清除会话再触发 SPA 请求，800ms 内顶层跳 `/login`，提示文案为“登录状态已失效，请重新登录”）
13. 日志增长速率：通过（模拟器运行观察窗口内无 `com.ruoyi.twin.*Mapper` DEBUG 输出，未刷屏）

## 约束检查

- `ruoyi-framework`：仅按阶段 6 方案修改 `ResourcesConfig`，新增 `/twin/tiles/**` 磁盘映射
- `ruoyi-common`：未修改
- `ruoyi-system`：未修改
- 未 push
- 工作区 `.trash/` 保存搬迁时排除的数据副本和 `TwinApplication.java`，未提交
