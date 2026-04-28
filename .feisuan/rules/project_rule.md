
# 开发规范指南

为保证代码质量、可维护性、安全性与可扩展性，开发人员 ccj 请在开发过程中严格遵循以下规范。

## 一、项目基础信息

- **项目名称**：campus-chat-platform (校园即时通讯系统)
- **工作目录**：`/Users/ccj/space/idea_java_projects/campuschat`
- **作者**：ccj
- **Java 版本**：JDK 11 (注：POM中配置为11，IDE环境为17，请以项目配置为准)
- **构建工具**：Maven
- **API 前缀**：`/capi` (配置于 `server.servlet.context-path`)

## 二、技术栈与依赖版本

- **主框架**：Spring Boot 2.7.18
- **核心依赖**：
  - `spring-boot-starter-web` / `spring-boot-starter-security`
  - `spring-boot-starter-websocket` (STOMP)
  - `spring-boot-starter-data-redis`
  - `mybatis-plus-boot-starter` (3.5.5)
  - `postgresql` (数据库)
  - `lombok` (实体类简化)
  - `jjwt` (0.11.5) (身份认证)
  - `rocketmq-spring-boot-starter` (2.2.3) (消息队列)
  - `minio` (8.5.7) (文件存储)
  - `hutool-all` (工具类库)

## 三、目录结构规范

项目采用标准的 Spring Boot 包结构，具体目录树如下：

```text
campuschat/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── ccj/
│   │   │           └── campus/
│   │   │               └── chat/
│   │   │                   ├── common/         # 公共类（常量、异常等）
│   │   │                   ├── config/         # Spring 配置类（Redis、Websocket等）
│   │   │                   ├── controller/     # 控制层（API 接口）
│   │   │                   ├── dto/            # 数据传输对象
│   │   │                   ├── entity/         # 数据库实体
│   │   │                   ├── mapper/         # MyBatis-Plus Mapper 接口
│   │   │                   ├── mq/             # 消息队列生产者/消费者
│   │   │                   ├── security/       # 安全相关配置与工具
│   │   │                   ├── service/        # 业务逻辑接口
│   │   │                   │   └── impl/       # 业务逻辑实现类
│   │   │                   ├── util/           # 工具类
│   │   │                   └── websocket/      # WebSocket 处理逻辑
│   │   └── resources/
│   │       ├── db/          # SQL 脚本（如有）
│   │       ├── mapper/      # MyBatis XML 映射文件
│   │       └── application.yaml # 配置文件
│   └── test/
│       └── java/
│           └── com/
│               └── ccj/
│                   └── campus/
│                       └── chat/              # 测试代码
```

## 四、分层架构规范

| 层级        | 职责说明                         | 开发约束与注意事项                                               |
|-------------|----------------------------------|----------------------------------------------------------------|
| **Controller** | 处理 HTTP/WebSocket 请求与响应，定义 API 接口 | 访问路径需包含 `/capi` 前缀；返回 JSON 结果；`@Valid` 校验输入      |
| **Service**    | 实现业务逻辑、事务管理与数据校验   | 必须通过 Mapper 层访问数据库；返回 DTO 而非 Entity（除非必要）      |
| **Mapper**     | 数据库访问与持久化操作             | 继承 `BaseMapper`；XML 映射文件位于 `resources/mapper`             |
| **Entity**     | 映射数据库表结构                   | 使用 MyBatis-Plus 注解（如 `@TableName`）；包名统一为 `entity`     |

### 接口与实现分离

- 所有业务逻辑通过接口定义（如 `UserService`），具体实现放在 `service/impl` 子包中。

## 五、安全与性能规范

### 输入校验

- 使用 `@Valid` 与 JSR-303 校验注解（如 `@NotBlank`, `@Size` 等）。
- **重要**：Spring Boot 2.x 中校验依赖通常为 `spring-boot-starter-validation`，请确保注入 `Validator`。

### 事务管理

- `@Transactional` 注解仅用于 **Service 层**方法。
- **数据库连接池配置**：`hikari.maximum-pool-size: 100`，请避免在大并发下创建过多连接。

### 数据库配置

- **数据库类型**：PostgreSQL
- **驱动**：`org.postgresql.Driver`
- **连接串**：`jdbc:postgresql://localhost:15432/campus_chat`

## 六、代码风格规范

### 命名规范

| 类型       | 命名方式             | 示例                  |
|------------|----------------------|-----------------------|
| 类名       | UpperCamelCase       | `UserServiceImpl`     |
| 方法/变量  | lowerCamelCase       | `saveUser()`          |
| 常量       | UPPER_SNAKE_CASE     | `MAX_LOGIN_ATTEMPTS`  |

### 注释规范

- 所有类、方法、字段需添加 **Javadoc** 注释。
- **语言**：中文注释。

### 类型命名规范（阿里巴巴风格）

| 后缀 | 用途说明                     | 示例         |
|------|------------------------------|--------------|
| DTO  | 数据传输对象                 | `UserDTO`    |
| DO   | 数据库实体对象               | `UserDO`     |
| VO   | 视图展示对象                 | `UserVO`     |
| Query| 查询参数封装对象             | `UserQuery`  |

### 实体类简化工具

- 使用 Lombok 注解而非手动编写 getter/setter：
  - `@Data`
  - `@NoArgsConstructor`
  - `@AllArgsConstructor`

## 七、扩展性与日志规范

### 接口优先原则

- 所有业务逻辑通过接口定义（如 `UserService`），具体实现放在 `impl` 包中。

### 日志记录

- 使用 `@Slf4j` 注解代替 `System.out.println`。
- **日志级别**：
  - `com.ccj.campus.chat`: `info`
  - `com.ccj.campus.chat.mapper`: `error`

### 消息队列 (RocketMQ)

- 使用 `rocketmq-spring-boot-starter` 进行异步消息处理。
- 生产者组名称：`campus_chat_producer`

### 文件存储

- 使用 **MinIO** 进行文件上传与管理。
- **Bucket 名称**：`campus-chat`

### WebSocket (STOMP)

- 最大会话数配置：`2000`。
- 消息撤回窗口期：`120` 秒。

## 八、编码原则总结

| 原则       | 说明                                       |
|------------|--------------------------------------------|
| **SOLID**  | 高内聚、低耦合，增强可维护性与可扩展性     |
| **DRY**    | 避免重复代码，提高复用性                   |
| **KISS**   | 保持代码简洁易懂                           |
| **YAGNI**  | 不实现当前不需要的功能                     |
| **OWASP**  | 防范常见安全漏洞，如 SQL 注入、XSS 等      |
