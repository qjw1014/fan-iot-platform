# 后端工程

本目录为 Spring Boot 3 + Java 17 + Maven 后端工程。

## 已实现

- Spring Boot 3 基础工程
- PostgreSQL 数据源配置
- Flyway 数据库迁移
- Redis 连接配置
- JWT 登录认证
- Spring Security 无状态认证
- Swagger/OpenAPI
- 统一返回结构
- 统一异常处理
- 用户管理基础接口

## 本地启动

先准备 PostgreSQL 和 Redis，然后在本目录执行：

```powershell
mvn spring-boot:run
```

Swagger 地址：

```text
http://localhost:8080/swagger-ui.html
```

健康检查：

```text
http://localhost:8080/actuator/health
```

## 管理员初始化

通过环境变量创建初始管理员账号：

- `ADMIN_USERNAME`
- `ADMIN_PASSWORD`
- `ADMIN_REAL_NAME`

如果未配置用户名或密码，系统会跳过管理员初始化。
