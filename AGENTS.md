# 项目开发约束

若依（RuoYi）4.8.3 单体版管理系统，已从 MySQL 迁移到 PostgreSQL，正在合并武汉数字孪生项目。

本文件是本仓库的通用开发约束，面向所有协作 agent。`CLAUDE.md` 引用本文件，内容以本文件为准。

## 技术栈

| 层 | 选型 | 版本 |
|---|---|---|
| 运行时 | Java | 17（Temurin） |
| 框架 | Spring Boot | 4.1.0 |
| 构建 | Maven | 3.9.x |
| ORM | MyBatis + PageHelper | 4.1.0 / 4.1.0 |
| 连接池 | Druid | 1.2.28（含 wall / stat filter） |
| 安全 | Apache Shiro | 3.0.0 |
| 视图 | Thymeleaf + jQuery + Bootstrap | — |
| 调度 | Quartz | 2.5.2 |
| 数据库 | PostgreSQL / PostGIS | 16-3.4（容器 `twin-pg`） |
| 接口文档 | springdoc-openapi | 3.1.0 |

**注意：不是 MyBatis-Plus。** 分页一律用 PageHelper（`startPage()` + `PageInfo`/`getDataTable()`）。

## 模块结构

```
ruoyi-admin       启动模块，Controller / 静态资源 / Thymeleaf 模板 / 配置文件
ruoyi-framework   框架核心：Shiro、数据源、MyBatis、拦截器、AOP
ruoyi-system      系统业务：用户 / 角色 / 菜单 / 部门 / 字典 / 参数
ruoyi-quartz      定时任务
ruoyi-generator   代码生成器
ruoyi-common      通用工具、注解、枚举、统一返回
```

主类：`ruoyi-admin/src/main/java/com/ruoyi/RuoYiApplication.java`
配置：`ruoyi-admin/src/main/resources/application.yml`、`application-druid.yml`

## 环境与启动

```bash
mvn clean package -DskipTests
java -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai -jar ruoyi-admin/target/ruoyi-admin.jar
```

- 应用端口 **90**，访问 http://localhost:90
- Druid 监控 http://localhost:90/druid
- 不要用根目录的 `ry.bat` / `ry.sh`，那是交互式菜单脚本，假定 jar 与脚本同目录
- 启动前确认 90 端口没有残留进程，否则重新打包时 jar 被占用

**登录账号**：`admin`。密码**不是**若依默认的 `admin123`，种子数据被改过；实际值查 `sys_user` 表或问项目负责人。连续错 5 次锁定 10 分钟，不要盲试。

## 数据库

- PostGIS 容器 `twin-pg`，宿主端口 **5434**（避开 5432 本机 PG 和 5433 其他实例）
- 业务库 `ry`，用户 `twin`
- 连接串与口令见 `ruoyi-admin/src/main/resources/application-druid.yml`，**禁止把口令写进代码、日志、文档或提交**
- 建表脚本：`sql/postgresql/ry_pg.sql`、`sql/postgresql/quartz_pg.sql`（MySQL 原脚本 `sql/ry_20260319.sql` 保留不动，仅供对照）
- 分页方言已配置为 `postgresql`（`application.yml` 的 `pagehelper.helperDialect`）

命令行连库：

```bash
docker exec twin-pg psql -U twin -d ry -c "select 1"
```

## 通用规则

- 修改文件前必须先读取当前内容，禁止凭记忆重写整个文件
- 每次任务只创建/修改明确指定的文件，禁止重构无关代码
- 禁止自行新增或升级依赖，需要时先说明理由并等待确认
- 所有代码注释使用中文
- 保持文件原有缩进、换行符和编码 —— **本仓库 mapper XML 多为 CRLF**，不要整体重排
- 不删除他人代码或注释，包括看起来无用的
- 不主动 commit / push，除非明确要求

## Java 与后端约束

- 遵循《阿里巴巴 Java 开发手册》黄山版【强制】级条款
- 分层严格：controller → service → mapper，**Controller 禁止注入 Mapper**
- entity 与 vo/dto 分离，entity 不出 service 层
- 若依体系的 Controller 继承 `BaseController`，列表返回 `TableDataInfo`（`startPage()` + `getDataTable()`），增删改返回 `AjaxResult`（`toAjax()`）
- 写操作加 `@Log(title = "...", businessType = BusinessType.XXX)`，纳入操作日志
- 权限注解 `@RequiresPermissions("模块:业务:动作")`，与 `sys_menu.perms` 对齐
- **`startPage()` 与紧随其后的查询之间不得插入任何其他语句**，否则 ThreadLocal 分页参数会串到别的查询上

## SQL 与 MyBatis 约束

- 数据库是 PostgreSQL，**不要写 MySQL 方言**：无 `ifnull`（用 `coalesce`）、无 `find_in_set`、无反引号、无 `date_format`、无 `limit ?,?`
- DELETE / UPDATE 必须带 WHERE，执行前先用 SELECT 确认影响行数
- 禁止 DROP TABLE / DROP DATABASE / TRUNCATE
- mapper XML 放 `src/main/resources/mapper/<模块>/`，文件名必须以 `Mapper.xml` 结尾（扫描规则 `classpath*:mapper/**/*Mapper.xml`）
- **空间字段（geometry）一律用 PostGIS 函数处理，禁止在 Java 侧做几何运算**
- **含 geometry 列的表禁止用代码生成器生成 CRUD** —— 若依 `GenUtils` 的类型映射表没有 geometry，会 fallback 成 String，生成的表单会出现填几何体的输入框，INSERT 撞 NOT NULL 约束必挂。这类表手写 Controller 并复用已处理几何构造的 Service
- 查询含 geometry 的表时**禁止 `SELECT *`**，几何列用 `ST_X(center) AS lon` 这类投影显式取值

## 前端约束

### 若依原生页面（Thymeleaf）

- 模板放 `ruoyi-admin/src/main/resources/templates/<模块>/`
- 表格用 bootstrap-table，遵循既有页面的 `$.table.init` 写法
- 参照同目录既有页面，不要引入新的前端框架或组件库

### Cesium 三维场景（Vue3 SPA）

- 一律 `<script setup lang="ts">`
- Cesium Viewer 单例管理，禁止在组件内直接 `new Viewer`
- 设备点位渲染必须用 Primitive API（`BillboardCollection` / `LabelCollection`），**禁止 Entity 循环 add** —— 设备数超 200 时 Entity 会导致逐帧重建卡顿
- 建筑点选用 `scene.pick` 获取 `Cesium3DTileFeature`，通过 `setColor` 高亮，**禁止销毁重建 tileset**
- 全局只注册一个 `ScreenSpaceEventHandler`，按 picked 对象类型分发
- API 请求统一走 `src/api/request.ts` 封装

## 分支与协作

- `master`：稳定分支
- `develop`：开发分支，日常开发在此进行
- 禁止对 master 执行 `push --force`；禁止 `reset --hard`、`checkout .` 等丢弃未提交改动的操作
- 禁止用 `--no-verify` 跳过钩子；禁止 rebase 已推送的分支

## 当前进行中的工作

### 1. PostgreSQL 迁移（已完成，待提交）

工作区有一批未提交改动：数据源配置、24 个 mapper XML 的方言改写、代码生成器的元数据查询改走 `information_schema`。应用已验证可正常启动与运行。

### 2. 合并 wuhan-digital-twin（计划中）

把 `D:\Code\wuhan-digital-twin`（武汉七区建筑白模三维底座 + 楼宇物联网监测）合并为本项目的 `ruoyi-twin` 模块，左侧栏新增「数字孪生」栏目。

**完整方案见 [docs/twin-merge-plan.md](docs/twin-merge-plan.md)，动手前必须先读。** 其中包含三个开工前必须完成的验证 Gate（Druid × PostGIS、PageHelper 分页、Boot 3.2→4.1 编译），跳过会导致大范围返工。

## 本机环境坑位

- **Git Bash 跑 docker 必须禁用 MSYS 路径转换**：`-v 宿主路径:/app/output` 里的容器路径会被 Git Bash 翻译成 Git 安装目录，产物写到 `D:/Soft/Git/app/output/`。加 `MSYS_NO_PATHCONV=1`，或容器路径写成 `//app/output`。用 PowerShell 无此问题
- 本机同时装了 PowerShell 与 Git Bash，两者语法不通用，写脚本前先确认在哪个 shell 下跑
