# travel-client

AI 智能旅行助手后端服务（业务中台）。当前已实现用户注册 / 登录 / 登出 / 获取当前用户等认证能力。

## 技术栈

Spring Boot 4.1.1 · MyBatis-Plus 3.5.17 · Redis · Sa-Token 1.46.0 · MySQL 8.x · Lombok · Swagger(OpenAPI)

> 注意：项目使用 Spring Boot 4，依赖需用 `mybatis-plus-spring-boot4-starter`、`sa-token-spring-boot4-starter` 等 `-spring-boot4-` 版本。

## 项目结构

```
src/main/java/com/example/demo/
├── DemoApplication.java          # 启动类（含 @MapperScan、/index 示例路由）
├── Person.java                   # 遗留的示例 POJO（未被业务使用）
├── common/                       # 公共组件
│   ├── Result.java               # 统一返回封装 Result<T>（code/message/data）
│   └── BizException.java         # 业务异常，携带错误码，由全局异常处理器统一转 Result
├── config/                       # 配置类
│   ├── AuthConfig.java           # 注册 BCryptPasswordEncoder Bean（密码加密）
│   ├── CosConfig.java            # CORS 跨域处理策略（Sa-Token 的 SaCorsHandleFunction）
│   ├── MyBatisPlusConfig.java    # Mapper 扫描配置（@MapperScan）
│   ├── OpenApiConfig.java        # Swagger/OpenAPI 文档配置（含 Authorization 安全方案）
│   ├── RedisConfig.java          # RedisTemplate 序列化配置（String 序列化器）
│   └── SaTokenConfig.java        # Sa-Token 全局过滤器 + 拦截器 + 白名单
├── controller/                   # 控制层：接收 HTTP 请求，调用 Service
│   └── UserController.java       # 用户认证接口（注册/登录/登出/当前用户）
├── dto/                          # 入参对象
│   ├── LoginDTO.java             # 登录入参（账号 + 密码，含参数校验）
│   └── RegisterDTO.java          # 注册入参（手机号/密码/邮箱/昵称，含参数校验）
├── enticy/                       # 实体类（注：包名为 entity 的拼写误写）
│   └── User.java                 # 用户实体，映射表 `user`
├── handler/                      # 全局处理器
│   ├── GlobalExceptionHandle.java# 全局异常处理（@RestControllerAdvice）
│   └── MyMetaObjectHandler.java  # MyBatis-Plus 字段自动填充（createTime/updateTime）
├── mapper/                       # 数据访问层（DAO）
│   └── UserMapper.java           # User 的 Mapper（继承 BaseMapper<User>）
├── service/                      # 业务逻辑层
│   ├── UserService.java          # 用户服务接口（继承 IService<User>）
│   └── impl/
│       ├── StpInterfaceImpl.java # Sa-Token 权限/角色加载实现（@Component）
│       └── UserServiceImpl.java  # 用户服务实现（注册/登录/登出/当前用户）
├── utils/                        # 工具类
│   └── RedisUtils.java           # Redis 操作封装（get/set/delete/expire/increment 等）
└── vo/                           # 出参对象（不暴露 Entity）
    └── UserVO.java               # 用户返回对象（不含密码字段）

src/main/resources/
├── application.yaml              # 主配置（激活 dev 环境）
├── application-dev.yaml          # 开发环境配置（端口 9091、MySQL、Redis、Sa-Token）
├── application-prod.yaml         # 生产环境配置（端口 9090）
├── banner.txt                    # 启动时打印的 banner
├── mapper/                       # MyBatis XML 目录（当前为空，占位）
└── db/migration/
    └── V2__add_user_auth_columns.sql  # 用户表认证字段迁移脚本（phone/password/status）

src/test/
├── java/com/example/demo/
│   └── DemoApplicationTests.java # 集成测试（@SpringBootTest，H2 内存库）
├── resources/
│   ├── application-test.yaml     # 测试环境配置（H2）
│   └── data.sql                  # 测试数据（创建 user 表并插入 5 条数据）
└── error/
    └── demoerror.txt             # 遗留的错误日志样例文件
```

## 各文件作用说明

### 启动与示例

| 文件 | 作用 |
|---|---|
| `DemoApplication.java` | 项目启动类。`@SpringBootApplication` + `@MapperScan("com.example.demo.mapper")`；同时是 `@RestController`，映射 `/index` 作为示例路由，启动后打印 Sa-Token 配置。 |
| `Person.java` | 简单的 POJO（name/gender/age），属于早期演示遗留代码，当前未被任何业务引用。 |

### 公共组件 common/

| 文件 | 作用 |
|---|---|
| `Result.java` | 统一返回结构 `Result<T>{ code, message, data }`，提供 `success` / `error` 静态方法。成功码为 200。 |
| `BizException.java` | 运行时业务异常，额外携带 `code` 错误码，由 `GlobalExceptionHandle` 统一捕获并转为 `Result`。 |

### 配置 config/

| 文件 | 作用 |
|---|---|
| `AuthConfig.java` | 注册 `BCryptPasswordEncoder` Bean，供密码加密与校验使用。 |
| `CosConfig.java` | 配置 Sa-Token 的 CORS 跨域策略（`SaCorsHandleFunction`），放行 OPTIONS 预检请求。 |
| `MyBatisPlusConfig.java` | 通过 `@MapperScan` 扫描 mapper 接口。 |
| `OpenApiConfig.java` | 配置 Swagger/OpenAPI 文档标题与 `Authorization` Header 安全方案，供接口调试。 |
| `RedisConfig.java` | 自定义 `RedisTemplate<String, Object>`，统一使用 String 序列化器。 |
| `SaTokenConfig.java` | 注册 Sa-Token 全局过滤器与拦截器；放行 `/user/login`、`/user/register` 及 Swagger 文档，其余路由校验登录。 |

### 控制层 controller/

| 文件 | 作用 |
|---|---|
| `UserController.java` | 认证相关接口：`/user/register` 注册、`/user/login` 登录、`/user/logout` 登出（需登录）、`/user/me` 获取当前用户（需登录）。 |

### 入参 dto/

| 文件 | 作用 |
|---|---|
| `LoginDTO.java` | 登录入参，含 `account`（手机号或邮箱）、`password`，带 `@NotBlank` 校验。 |
| `RegisterDTO.java` | 注册入参，含 `phone`、`password`、`email`（可选，`@Email`）、`name`（可选），带校验。 |

### 实体 enticy/

| 文件 | 作用 |
|---|---|
| `User.java` | 用户实体，`@TableName("user")`；主键自增；`createTime`/`updateTime` 由 `MyMetaObjectHandler` 自动填充。 |

### 处理器 handler/

| 文件 | 作用 |
|---|---|
| `GlobalExceptionHandle.java` | `@RestControllerAdvice` 全局异常处理：Sa-Token 未登录(401)/无权限(403)、参数校验(400)、`BizException`、运行时异常(500) 等。 |
| `MyMetaObjectHandler.java` | MyBatis-Plus 字段自动填充器，插入时填 `createTime`，更新时填 `updateTime`。 |

### 数据访问 mapper/

| 文件 | 作用 |
|---|---|
| `UserMapper.java` | 继承 `BaseMapper<User>`，提供 User 表的基础 CRUD 能力。 |

### 业务 service/

| 文件 | 作用 |
|---|---|
| `UserService.java` | 用户服务接口，继承 `IService<User>`，定义注册/登录/登出/当前用户方法。 |
| `UserServiceImpl.java` | 用户服务实现。登录含 IP 限流与失败锁定（Redis）；密码 BCrypt 校验；登录成功用 `StpUtil.login()` 签发 token。 |
| `StpInterfaceImpl.java` | Sa-Token 权限/角色加载实现，返回模拟的权限码与角色集合。 |

### 工具 utils/

| 文件 | 作用 |
|---|---|
| `RedisUtils.java` | Redis 操作封装：`get/set/delete/expire/increment` 及 Hash 操作，供登录限流等场景使用。 |

### 出参 vo/

| 文件 | 作用 |
|---|---|
| `UserVO.java` | 用户返回对象，仅含 id/name/age/email/phone/status，不含密码，避免直接暴露 Entity。 |

### 资源 resources/

| 文件 | 作用 |
|---|---|
| `application.yaml` | 主配置，通过 `spring.profiles.active: dev` 激活开发环境。 |
| `application-dev.yaml` | 开发环境：`context-path: /api`、端口 9091、MySQL、Redis、Sa-Token 等配置。 |
| `application-prod.yaml` | 生产环境：`context-path: /api`、端口 9090。 |
| `banner.txt` | 应用启动时打印的 ASCII banner。 |
| `db/migration/V2__add_user_auth_columns.sql` | 给 `user` 表新增 `phone`/`password`/`status` 字段及手机号唯一索引。 |

### 测试 test/

| 文件 | 作用 |
|---|---|
| `DemoApplicationTests.java` | `@SpringBootTest` 集成测试，使用 H2 内存库，验证 `selectList` 返回 5 条数据。 |
| `application-test.yaml` | 测试环境配置，使用 H2 数据源。 |
| `data.sql` | 测试初始化数据，建 `user` 表并插入 5 条用户数据。 |

## 备注

- `enticy` 包名是 `entity` 的拼写误写，后续可重命名以保持规范。
- `Person.java`、`src/test/error/demoerror.txt` 为早期演示遗留文件，暂未被业务使用。
