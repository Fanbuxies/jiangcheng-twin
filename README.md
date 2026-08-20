<h1 align="center">江城数字孪生运管平台</h1>

<h4 align="center">城市三维底座 + 楼宇物联网监测 · 一屏观全城，一网管到底</h4>

<p align="center">
	<img src="https://img.shields.io/badge/Java-17-orange.svg">
	<img src="https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen.svg">
	<img src="https://img.shields.io/badge/PostGIS-3.4-blue.svg">
	<img src="https://img.shields.io/badge/Cesium-1.115-lightgrey.svg">
	<img src="https://img.shields.io/badge/license-MIT-green.svg">
</p>

## 项目简介

江城数字孪生运管平台把**武汉中心城区七区的建筑三维底座**与**楼宇物联网设备监测**整合进一套完整的后台管理系统。

三维场景里可以看到 29,818 栋建筑白模、2,000 台在线监测设备与市政设施的实时状态；点选任意建筑或设备即可查看属性详情。同时它又是一个完备的管理后台——建筑、设备、告警的增删改查全部纳入统一的权限、操作日志与数据字典体系，与用户、角色、菜单、定时任务等系统功能共用同一套账号。

整个平台**单进程部署**，一个 jar、一个端口，不依赖任何注册中心或消息中间件。

## 核心功能

### 数字孪生

- **三维底座**：七区 29,818 栋建筑白模，3D Tiles 1.1 隐式分块（implicit tiling）加载，按用途分色、按高度分明度
- **立面增强**：CustomShader 程序化绘制楼层横带、窗格与屋顶，按建筑用途区分立面样式
- **设备监测**：2,000 台设备（烟感／水浸／温湿度／电气／摄像头），实时状态经 WebSocket 推送，3 秒一拍
- **市政设施**：充电桩、路灯、井盖、公交站独立图层与告警通道
- **点选交互**：建筑、设备、设施均可点选高亮并弹出属性面板
- **告警**：设备与设施告警实时产生、分级展示

### 管理后台

- **业务管理**：建筑／设备／告警列表、条件查询、分页、增删改查，写操作全部记录操作日志
- **系统功能**：用户、角色、菜单、部门、岗位、字典、参数、通知公告
- **系统监控**：在线用户、定时任务、登录日志、操作日志、数据监控、服务监控
- **系统工具**：表单构建、代码生成、接口文档

## 技术栈

| 层 | 选型 |
|---|---|
| 运行时 | Java 17 |
| 框架 | Spring Boot 4.1.0 |
| ORM | MyBatis + PageHelper |
| 连接池 | Druid（含 stat / wall filter） |
| 安全 | Apache Shiro 3.0 |
| 服务端视图 | Thymeleaf + Bootstrap + jQuery |
| 三维前端 | Vue 3 + TypeScript + Vite 5 + Pinia + Element Plus + Cesium 1.115 |
| 数据库 | PostgreSQL 16 + PostGIS 3.4 |
| 调度 | Quartz |
| 数据准备 | Python 3.10 + psycopg2 |

## 模块结构

```
jiangcheng-twin
├── ruoyi-admin        启动模块：Controller、静态资源、Thymeleaf 模板、配置
├── ruoyi-framework    框架核心：Shiro、数据源、MyBatis、拦截器、AOP
├── ruoyi-system       系统业务：用户/角色/菜单/部门/字典/参数
├── ruoyi-quartz       定时任务
├── ruoyi-generator    代码生成器
├── ruoyi-common       通用工具、注解、枚举、统一返回
├── ruoyi-twin         数字孪生业务：建筑/设备/设施/告警/统计/模拟器/WebSocket
├── data-prep          数据准备脚本：OSM 拉取、入库、设备播种、分区维护
├── docs               架构、接口清单、运维手册
└── sql                建表脚本与菜单数据
```

模块名沿用 `ruoyi-` 前缀、包名沿用 `com.ruoyi`，是为了保持与上游的合并能力（见文末说明）。

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.9+
- Docker（用于 PostGIS 容器）
- Node.js 18+（仅在需要重新构建三维前端时）

### 1. 启动数据库

```bash
docker run -d --name twin-pg -p 5434:5432 \
  -e POSTGRES_DB=ry -e POSTGRES_USER=twin -e POSTGRES_PASSWORD=<你的口令> \
  postgis/postgis:16-3.4
```

数据库连接配置见 `ruoyi-admin/src/main/resources/application-druid.yml`。

### 2. 初始化表结构

```bash
docker exec twin-pg psql -U twin -d ry -c "CREATE EXTENSION IF NOT EXISTS postgis"
docker exec -i twin-pg psql -U twin -d ry < sql/postgresql/ry_pg.sql
docker exec -i twin-pg psql -U twin -d ry < sql/postgresql/quartz_pg.sql
docker exec -i twin-pg psql -U twin -d ry < sql/twin/schema.sql
docker exec -i twin-pg psql -U twin -d ry < sql/twin/menu.sql
```

### 3. 放置三维切片

3D Tiles 切片体积较大（约 63 MB），未纳入版本库。生成方式见 `docs/twin/runbook.md`，生成后放到：

```
D:/ruoyi/twinTiles/       # 可通过 app.tileset.location 配置
├── tileset.json
├── content/*.glb
└── subtrees/*.subtree
```

### 4. 构建与启动

```bash
mvn clean package -DskipTests
java -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai -jar ruoyi-admin/target/ruoyi-admin.jar
```

访问 http://localhost:90 ，登录后从左侧「数字孪生」进入。

接口文档：http://localhost:90/swagger-ui.html

## 目录约定

- 三维前端源码位于独立工程，构建产物（`assets` / `cesium`）落在 `ruoyi-admin/src/main/resources/static/twin/`，随 jar 一同分发
- 切片走磁盘目录映射而非打包进 jar，映射规则见 `ResourcesConfig`
- 空间字段一律由 PostGIS 函数处理，Java 侧不做几何运算
- 含 geometry 列的表不使用代码生成器生成 CRUD，详见 `AGENTS.md`

更多开发约束见 [AGENTS.md](AGENTS.md)。

## 关于本项目

本项目基于 [RuoYi](https://gitee.com/y_project/RuoYi) v4.8.3 二次开发，在其之上完成了 PostgreSQL 迁移与数字孪生模块的整合。

框架层（`ruoyi-common` / `ruoyi-framework` / `ruoyi-system` / `ruoyi-quartz` / `ruoyi-generator`）保留原有的模块命名与 `com.ruoyi` 包名，以便持续合并上游的安全更新与版本升级。数字孪生相关代码位于 `ruoyi-twin` 模块。

感谢若依团队提供的优秀开源框架。

## 许可证

[MIT License](LICENSE) © 2018 RuoYi
