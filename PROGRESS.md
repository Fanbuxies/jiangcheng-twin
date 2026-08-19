# 数字孪生合并进度

更新时间：2026-08-20 02:27（Asia/Shanghai）

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

- 状态：已完成，待本阶段提交
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

- 状态：已完成，待本阶段提交
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

## 验证清单总览

1. 系统管理回归：通过（用户/角色/菜单/部门/岗位/字典/参数列表；用户条件查询；参数翻页）
2. 系统监控回归：通过（在线用户/定时任务/登录日志/操作日志列表；操作日志翻页）
3. 系统工具代码生成回归：部分通过（列表加载正常；表导入与预览留待有安全候选表时验证）
4. 宿主分页总数和翻页：通过（参数 11 条、操作日志 16 条，第二页条数与区间正确）
5. 构建与包名/依赖清零：通过（代码与资源清零；文档保留历史文本）
6. 应用启动与映射解析：通过
7. 数字孪生菜单：未实现
8. Cesium 场景：未实现
9. 三个管理页 CRUD：未实现
10. 操作日志：未实现
11. 模拟器与 WebSocket：未验证
12. 未登录和会话过期：未验证
13. 日志增长速率：未验证

## 约束检查

- `ruoyi-framework`：未修改
- `ruoyi-common`：未修改
- `ruoyi-system`：未修改
- 未 push
- 工作区 `.trash/` 保存搬迁时排除的数据副本和 `TwinApplication.java`，未提交
