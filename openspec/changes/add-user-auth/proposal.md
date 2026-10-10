# Proposal

## Why

当前服务没有任何用户注册 / 登录能力：`UserController` 是空壳，`User` 实体缺少登录所需字段，Sa-Token 虽已接入但白名单只放行了不存在的 `/user/doLogin`。前端无法获得登录态，后续行程、偏好等所有需要用户身份的业务都无法开展。本次补齐最小可用的注册 + 登录闭环，并附带基础的防爆破能力。

## What Changes

- 新增用户注册接口：手机号必填 + 密码，邮箱与昵称可选，注册成功即自动登录并返回 token。
- 新增用户登录接口：支持手机号或邮箱 + 密码登录。
- 新增登出接口与"获取当前用户"接口。
- 密码使用 BCrypt（`spring-security-crypto`）加密存储，不存明文。
- 登录安全加固：IP 限流（10 次/分钟）与账号失败锁定（5 次失败锁 15 分钟），基于 Redis 计数实现。
- 会话交由 Sa-Token 管理并落 Redis（`sa-token-redis-template` 已接入），token 有效期调整为 2 小时绝对过期。
- `User` 实体新增 `phone`（唯一）、`password`、`status` 字段，并提供 ALTER TABLE 脚本。
- 统一鉴权白名单，从 `/user/doLogin` 调整为 `/user/login`、`/user/register`。

## Capabilities

### New Capabilities

- `user-auth`: 用户注册、登录、登出、获取当前用户，以及登录会话管理与防爆破（限流、锁定）。

### Modified Capabilities

<!-- 无现有 spec，本次不修改任何已有能力。 -->

## Impact

- **代码**：`User` 实体、`UserService`/`UserServiceImpl`、`UserController`、`SaTokenConfig`、`RedisUtils`（新增 `expire`）、`GlobalExceptionHandle`（可能新增锁定/限流异常映射）。
- **新增文件**：`RegisterDTO`、`LoginDTO`、`UserVO`（`dto`/`vo` 包）。
- **依赖**：新增 `spring-security-crypto`（仅 BCrypt）、`spring-boot-starter-validation`。
- **配置**：`application-dev.yaml` 中 `sa-token.timeout` 由 `2592000` 改为 `7200`。
- **数据库**：`user` 表新增 `phone`、`password`、`status` 列（附 ALTER TABLE SQL）。
- **API**：新增 `/api/user/register`、`/api/user/login`、`/api/user/logout`、`/api/user/me`；移除 `/user/doLogin` 放行。
