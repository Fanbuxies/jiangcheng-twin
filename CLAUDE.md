# CLAUDE.md

项目开发约束见 @AGENTS.md —— 技术栈、模块结构、启动命令、数据库、Java/SQL/前端规约全在其中，以那份为准。本文件只补充速查信息，避免两份文档不同步。

## 关键路径速查

| 用途 | 路径 |
|---|---|
| 启动类 | `ruoyi-admin/src/main/java/com/ruoyi/RuoYiApplication.java` |
| 主配置 / 数据源 | `ruoyi-admin/src/main/resources/application.yml` / `application-druid.yml` |
| 左侧菜单模板 | `ruoyi-admin/src/main/resources/templates/index.html`（菜单项来自 `sys_menu` 表，改菜单是插数据不是改模板） |
| Shiro 过滤链 | `ruoyi-framework/src/main/java/com/ruoyi/framework/config/ShiroConfig.java`（`filterChainDefinitionMap`，末条 `/**` 需登录） |
| MyBatis 装配 | `ruoyi-framework/src/main/java/com/ruoyi/framework/config/MyBatisConfig.java`（手工构造 `SqlSessionFactory`，非自动配置） |
| 多数据源 | `ruoyi-framework/src/main/java/com/ruoyi/framework/config/DruidConfig.java` + `datasource/DynamicDataSource.java`（`@DataSource` 注解切换，枚举仅 MASTER/SLAVE） |
| 静态资源映射 | `ruoyi-framework/src/main/java/com/ruoyi/framework/config/ResourcesConfig.java`（`/profile/**` → 本地磁盘目录，大文件走这里而非打进 jar） |
| 全局异常处理 | `ruoyi-framework/src/main/java/com/ruoyi/framework/web/exception/GlobalExceptionHandler.java` |
| CRUD Controller 范例 | `ruoyi-admin/src/main/java/com/ruoyi/web/controller/system/SysPostController.java` |
| 合并方案 | `docs/twin-merge-plan.md` |

## 常用命令

```bash
mvn clean package -DskipTests
```

```bash
java -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai -jar ruoyi-admin/target/ruoyi-admin.jar
```

```bash
docker exec twin-pg psql -U twin -d ry -c "select tablename from pg_tables where schemaname='public' order by 1"
```

## 排查提示

- 应用日志级别 `com.ruoyi` 为 debug，会打完整 SQL 与参数，SQL 报错时日志里能直接看到成品语句
- 启动失败先看是不是 90 端口被上一次的进程占着
- 页面报错但日志无异常时，查 Shiro 过滤链是不是把请求拦在了登录页
- 列表分页数据不对，先确认 `startPage()` 与查询之间没有插入别的语句
