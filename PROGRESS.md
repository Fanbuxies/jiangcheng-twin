# 数字孪生合并进度

更新时间：2026-08-20 01:36（Asia/Shanghai）

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

- 状态：Gate 2 失败，按硬要求停止；未提交
- Gate 2：失败
  - 探针：`ruoyi-twin/src/test/java/com/ruoyi/twin/gate/BuildingPaginationGate.java`
  - 目标：验证第 1 页、第 2 页、总数、页间 ID 不重复及顺序
  - 实际错误：`java.lang.VerifyError: Bad return type`
  - 失败位置：`com.github.pagehelper.parser.defaults.DefaultCountSqlParser.sqlToCount`
  - 依赖证据：
    - MyBatis-Plus 3.5.5 引入 `com.github.jsqlparser:jsqlparser:4.6`
    - PageHelper 6.1.1 需要 `com.github.jsqlparser:jsqlparser:4.7`
    - Maven 最终选择 4.6，导致 PageHelper 字节码验证失败
- 当前未提交文件：
  - `ruoyi-twin/src/main/java/com/ruoyi/twin/building/mapper/BuildingMapper.java`
  - `ruoyi-twin/src/main/java/com/ruoyi/twin/building/service/impl/BuildingServiceImpl.java`
  - `ruoyi-twin/src/main/resources/mapper/BuildingMapper.xml`
  - `ruoyi-twin/src/test/java/com/ruoyi/twin/gate/BuildingPaginationGate.java`
- 遗留问题：
  - Gate 2 未通过，尚未批量改造其余 Mapper/Service
  - 阶段 4 至阶段 7 尚未开始

## 验证清单总览

1. 系统管理回归：未验证（缺少有效登录态，未猜测密码）
2. 系统监控回归：未验证
3. 系统工具代码生成回归：未验证
4. 宿主分页总数和翻页：未验证
5. 构建与包名/依赖清零：部分通过（构建通过，清零未完成）
6. 应用启动与映射解析：未通过（阶段 3 前的依赖冲突）
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
