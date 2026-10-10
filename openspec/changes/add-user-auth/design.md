# Design

## Context

- 包名为 `com.example.demo`，已有 Sa-Token 过滤器与拦截器（`SaTokenConfig`），但白名单只放行 `/user/doLogin`，该接口实际不存在。
- `UserController`、`UserService`/`UserServiceImpl` 均为空壳；`User` 实体只有 `id/name/age/email/createTime/updateTime`，无登录所需字段。
- `Result<T>` 已存在（成功码 `200`）；`GlobalExceptionHandle` 已处理 `NotLoginException→401`、`NotPermissionException→403`、参数校验异常→400、`RuntimeException→500`。
- `RedisUtils` 已有 `increment`、`setWithExpire`、`get`、`delete`、`hasKey`，但**缺少给已有 key 单独设过期时间的 `expire`**。
- `sa-token-redis-template` 已引入，会话自动落 Redis；`application-dev.yaml` 中 `sa-token.timeout=2592000`（30 天）、`token-name=Authorization`、`context-path=/api`。
- 动机见 proposal.md。

## Goals / Non-Goals

**Goals:**

- 建立最小可用的注册 / 登录 / 登出 / 当前用户闭环，登录态用 Sa-Token + Redis。
- 密码 BCrypt 加密，登录接口具备 IP 限流与账号锁定两道防爆破。
- token 2 小时绝对过期。

**Non-Goals:**

- 图形验证码、短信验证码（留 V2）。
- 邮箱验证、找回密码、多角色权限体系（`StpInterfaceImpl` 仍返回 mock 权限）。
- 包名 `com.example.demo` → `com.example.travel` 的重构（独立变更）。

## Decisions

### 1. 登录标识：手机号或邮箱，注册必填手机号
- 登录入参为单个 `account` 字符串，按格式判定类型：匹配 `^1[3-9]\d{9}$` 视为手机号，否则按邮箱处理。
- 注册入参 `phone` 必填、`email`/`name` 选填。
- **理由**：前端只需一个输入框，同时保留手机号为主标识。备选方案（只手机号 / 只邮箱）被否决，因为用户明确要"手机号或邮箱"。

### 2. 密码加密：`spring-security-crypto` 的 BCrypt
- 单独引入 `spring-security-crypto`，只取 `BCryptPasswordEncoder`，注册为 Bean。
- **理由**：用户指定；避免引入整套 Spring Security。备选（Hutool 自带 `BCrypt`）也可用，但 `BCryptPasswordEncoder` 是更标准的加密组件，且 CLAUDE.md 要求 BCrypt。

### 3. 防爆破：Redis 计数，限流 + 锁定分层
- **IP 限流**：key `rate:login:{ip}`，TTL 1 分钟，登录入口 `INCR`，超过 10 次返回 429。
- **账号锁定**：key `login:fail:{account}`，TTL 15 分钟。登录失败 `INCR`；达到 5 次后，在密码校验前直接拒绝（返回 423）；登录成功 `DELETE` 清零。
- **首次数计数时设置 TTL**：`INCR` 后若返回 1，调 `RedisUtils.expire(key, ttl)`。为此给 `RedisUtils` 补一个 `expire(String key, long timeout, TimeUnit unit)` 方法。
- **失败统一返回"账号或密码错误"**：不区分账号不存在还是密码错，防止账号枚举。
- **理由**：两道防线互补——账号锁定防单账号爆破，IP 限流防分布式批量尝试。备选（只用其中一道）被否决，因单靠账号锁定可被多账号轮询绕过，单靠 IP 限流可被换 IP 绕过。

### 4. token 2 小时绝对过期
- `sa-token.timeout` 由 `2592000` 改为 `7200`，`active-timeout` 保持 `-1`。
- **理由**：`active-timeout=-1` 表示不做"按活跃顺延"，实现用户要求的"登录后 2 小时过期、需重登"。

### 5. 注册即自动登录
- 注册成功后直接 `StpUtil.login(userId)` 并返回 token。
- **理由**：用户指定。注册接口因此也需在 Sa-Token 白名单中。

### 6. 端点命名与白名单
- 统一为 `/user/register`、`/user/login`、`/user/logout`、`/user/me`。
- 更新 `SaTokenConfig` 放行 `/user/login`、`/user/register`；`logout`、`me` 走登录态（`@SaCheckLogin`）。
- **理由**：干净命名，替换掉不存在的 `/user/doLogin`。

### 7. DTO / VO 分层
- 新增 `RegisterDTO`（`@NotBlank phone`、`@NotBlank password`、可选 `email`/`name`）、`LoginDTO`（`@NotBlank account`、`@NotBlank password`）、`UserVO`（脱敏，不含 `password`）。
- 引入 `spring-boot-starter-validation`（Spring Boot 4 的 web starter 已不再内置校验）。
- **理由**：符合 CLAUDE.md 不直接暴露 Entity 的约定；`me` 接口必须排除密码。

### 8. 数据模型
- `User` 新增 `phone`（唯一）、`password`、`status`（1 正常 / 0 禁用）。
- 提供 `ALTER TABLE` SQL：加列 + `phone` 唯一索引。
- 登录时校验 `status=1`，否则拒绝。

## Risks / Trade-offs

- **[锁定判定泄漏账号存在性]** 锁定提示会暴露某账号是否正在被攻击或是否存在 → 可接受（业务常见），如需隐藏可对锁定与普通失败返回同一文案。
- **[IP 限流在反向代理后失效]** 若经 Nginx 等代理，`request.getRemoteAddr()` 取到代理 IP → 从 `X-Forwarded-For` 取真实 IP（实现时预留）。
- **[Redis 计数与 TTL 非原子]** `INCR` 与首次设 TTL 分两步，极端情况下进程在两步之间崩溃会留下永久 key → 用 `INCR` 返回值判断首次，或后续引入 Lua 脚本；V1 接受该小概率风险。
- **[`StringRedisSerializer` 存值]** 当前 `RedisTemplate` 值序列化为字符串，`increment` 可正常作用，但 `get` 返回字符串；业务仅做计数，无影响。

## Migration Plan

1. 执行 `ALTER TABLE` SQL 为 `user` 表加列与唯一索引。
2. 更新 `application-dev.yaml`（`sa-token.timeout: 7200`）与 `SaTokenConfig` 白名单。
3. 新增依赖（`spring-security-crypto`、`spring-boot-starter-validation`）并实现代码。
4. 部署后验证：注册 → 自动登录 → `me` → `logout` → 未登录访问被 401 拦截。

回滚：配置与 SQL 均为可逆（删除列前需确认无数据依赖），代码回退即可。
