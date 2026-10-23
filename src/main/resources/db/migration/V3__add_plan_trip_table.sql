-- 旅行计划保存表：按 planTaskId 幂等存储 AI 生成的行程 JSON
CREATE TABLE t_plan_trip (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL COMMENT '用户ID',
    plan_task_id VARCHAR(64)  NOT NULL COMMENT '规划任务ID（幂等键）',
    title        VARCHAR(255) NULL COMMENT '行程标题',
    destination  VARCHAR(64)  NULL COMMENT '目的地',
    start_date   VARCHAR(16)  NULL COMMENT '开始日期',
    end_date     VARCHAR(16)  NULL COMMENT '结束日期',
    trip_json    LONGTEXT     NOT NULL COMMENT '完整行程 JSON',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_plan_task (user_id, plan_task_id)
) COMMENT '旅行计划保存表';