-- 用户认证字段：手机号(唯一)、密码(BCrypt)、状态(1正常 0禁用)
ALTER TABLE `user`
    ADD COLUMN phone    VARCHAR(20)  NULL COMMENT '手机号',
    ADD COLUMN password VARCHAR(128) NULL COMMENT '密码(BCrypt)',
    ADD COLUMN status   TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0禁用';

-- 手机号唯一索引（已存在 phone 数据且可能重复时需先处理重复值再执行）
ALTER TABLE `user`
    ADD UNIQUE INDEX uk_user_phone (phone);
