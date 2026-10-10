# Tasks

## 1. Dependencies & configuration

- [x] 1.1 Add `spring-security-crypto` and `spring-boot-starter-validation` to `pom.xml`; verify `mvn -q -DskipTests compile` succeeds and resolves both artifacts
- [x] 1.2 Change `sa-token.timeout` to `7200` in `application-dev.yaml`; verify the value is applied (e.g. via startup log or config inspection)

## 2. Data model

- [x] 2.1 Add `phone`, `password`, `status` fields to the `User` entity (`phone` unique, `status` default 1); verify the project compiles
- [x] 2.2 Provide an `ALTER TABLE` SQL for the `user` table (add `phone`/`password`/`status` columns + unique index on `phone`); verify it runs without error against the dev database

## 3. Support types & utilities

- [x] 3.1 Add `RegisterDTO`, `LoginDTO`, `UserVO` (VO excludes `password`) with validation annotations; verify the project compiles
- [x] 3.2 Add `expire(String key, long timeout, TimeUnit unit)` to `RedisUtils`; verify the method compiles and sets a TTL on an existing key

## 4. Auth service

- [x] 4.1 Define a `BCryptPasswordEncoder` bean (under `auth/config`); verify the bean is present in the Spring context
- [x] 4.2 Implement `register`: check phone/email uniqueness, hash password, insert user, auto-login; verify a new registration returns a token and the user persists with a hashed password
- [x] 4.3 Implement `login`: IP rate limit, account lock check, account lookup by phone or email, BCrypt verify, status check, `StpUtil.login`; verify success, wrong-password, unknown-account, rate-limit, and lockout paths
- [x] 4.4 Implement `logout` and `getCurrentUser` (returns `UserVO` without password); verify logout invalidates the session and current-user returns the expected fields

## 5. Controller & wiring

- [x] 5.1 Implement `/user/register`, `/user/login`, `/user/logout`, `/user/me` in `UserController` returning `Result<T>`; verify each endpoint responds correctly
- [x] 5.2 Update `SaTokenConfig` whitelist to `/user/login` and `/user/register`; verify an unauthenticated request to `/user/me` returns 401

## 6. Integration verification

- [x] 6.1 Run an end-to-end flow: register → auto-login → `me` → `logout` → unauthenticated `me` returns 401; verify token expiry after 2 hours and lock/rate-limit behavior via repeated requests

## Workflow follow-up

- Archive the change after the project's review and verification requirements are satisfied.
- Verify the archived spec lands under `openspec/specs/user-auth/`.
