-- =============================================================
-- 校园即时通讯系统 PostgreSQL Schema
-- 对齐论文 4.2：六大业务域 24 张核心表
--   - JSONB 存储扩展字段（消息扩展、请假附件、通知目标等）
--   - GENERATED ALWAYS AS 计算列（签到结束时间、消息软删除标记等）
--   - 部分索引（partial index）减小活跃数据查询开销
-- =============================================================

-- ==================== 业务域 1: 用户 ====================
CREATE TABLE IF NOT EXISTS sys_department (
                                              id         BIGSERIAL PRIMARY KEY,
                                              name       VARCHAR(64) NOT NULL UNIQUE,
    code       VARCHAR(32) NOT NULL UNIQUE,
    create_time TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS sys_class (
                                         id            BIGSERIAL PRIMARY KEY,
                                         department_id BIGINT NOT NULL REFERENCES sys_department(id),
    name          VARCHAR(64) NOT NULL,
    grade         SMALLINT NOT NULL,
    create_time   TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS sys_user (
                                        id              BIGSERIAL PRIMARY KEY,
                                        account_number  VARCHAR(32) NOT NULL UNIQUE,          -- 学号 / 工号
    password_hash   VARCHAR(128) NOT NULL,                -- BCrypt 哈希
    name            VARCHAR(32) NOT NULL,
    role            SMALLINT NOT NULL,                    -- 1=学生 2=教师 3=管理员
    department_id   BIGINT,                               -- 逻辑外键（论文：避免物理外键的批量写入性能问题）
    avatar          VARCHAR(255),
    phone           VARCHAR(32),                          -- 入库前脱敏（138****1234）
    email           VARCHAR(128),
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,        -- 封禁控制
    create_time     TIMESTAMPTZ NOT NULL DEFAULT now(),
    update_time     TIMESTAMPTZ NOT NULL DEFAULT now()
    );
-- 部分索引：只为启用的用户建索引，封禁用户不参与常规查询
CREATE INDEX IF NOT EXISTS idx_user_enabled_role
    ON sys_user(role) WHERE enabled = TRUE;

CREATE TABLE IF NOT EXISTS sys_user_class_rel (
                                                  user_id  BIGINT NOT NULL REFERENCES sys_user(id),
    class_id BIGINT NOT NULL REFERENCES sys_class(id),
    PRIMARY KEY (user_id, class_id)
    );

CREATE TABLE IF NOT EXISTS user_friend (
                                           id          BIGSERIAL PRIMARY KEY,
                                           user_id     BIGINT NOT NULL,
                                           friend_id   BIGINT NOT NULL,
                                           remark      VARCHAR(32),
    deleted     BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_user_friend UNIQUE (user_id, friend_id)
    );

CREATE TABLE IF NOT EXISTS user_blacklist (
                                              id          BIGSERIAL PRIMARY KEY,
                                              user_id     BIGINT NOT NULL,
                                              target_id   BIGINT NOT NULL,
                                              create_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_user_blacklist UNIQUE (user_id, target_id)
    );

-- ==================== 业务域 2: 通讯 ====================
CREATE TABLE IF NOT EXISTS chat_room (
                                         id           BIGSERIAL PRIMARY KEY,
                                         type         SMALLINT NOT NULL,                        -- 1=单聊 2=群聊
                                         name         VARCHAR(64),
    ext_info     JSONB NOT NULL DEFAULT CAST('{}' AS jsonb),      -- 论文：JSONB 扩展
    create_time  TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS chat_contact (
                                            id              BIGSERIAL PRIMARY KEY,
                                            user_id         BIGINT NOT NULL,
                                            room_id         BIGINT NOT NULL,
                                            last_msg_id     BIGINT,
                                            last_read_id    BIGINT,                               -- 已读回执的数据库时间戳锚点
                                            top             BOOLEAN NOT NULL DEFAULT FALSE,       -- 置顶
                                            mute            BOOLEAN NOT NULL DEFAULT FALSE,       -- 免打扰
                                            active_time     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_contact UNIQUE (user_id, room_id)
    );
CREATE INDEX IF NOT EXISTS idx_contact_user_active
    ON chat_contact(user_id, active_time DESC);

CREATE TABLE IF NOT EXISTS chat_message (
                                            id          BIGSERIAL PRIMARY KEY,
                                            room_id     BIGINT NOT NULL,
                                            from_uid    BIGINT NOT NULL,
                                            type        SMALLINT NOT NULL,                        -- 1=文本 2=图片 3=文件 4=语音 5=表情 6=撤回 7=请假卡片 8=签到卡片 ...
                                            content     TEXT,
                                            ext_info    JSONB NOT NULL DEFAULT CAST('{}' AS jsonb),      -- 论文：JSONB 存消息附加元数据
    client_seq  VARCHAR(64) NOT NULL,                     -- 客户端序列号 - 幂等去重
    status      SMALLINT NOT NULL DEFAULT 0,              -- 0=正常 1=已撤回 2=已删除（软删除）
    create_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- 论文：软删除标记为 GENERATED ALWAYS AS 计算列，查询无需显式判断
    is_visible  BOOLEAN GENERATED ALWAYS AS (status = 0) STORED,
    CONSTRAINT uk_msg_idem UNIQUE (from_uid, room_id, client_seq)
    );
-- 部分索引：只索引可见消息，减小活跃数据查询开销（论文原话）
CREATE INDEX IF NOT EXISTS idx_msg_room_time_visible
    ON chat_message(room_id, create_time DESC) WHERE is_visible = TRUE;

CREATE TABLE IF NOT EXISTS chat_message_read (
                                                 id          BIGSERIAL PRIMARY KEY,
                                                 msg_id      BIGINT NOT NULL,
                                                 room_id     BIGINT NOT NULL,
                                                 reader_id   BIGINT NOT NULL,
                                                 read_time   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_msg_read UNIQUE (msg_id, reader_id)
    );

-- 论文 1.3: 本地消息表 (outbox pattern) —— 与业务库在同一事务
CREATE TABLE IF NOT EXISTS mq_outbox (
                                         id              BIGSERIAL PRIMARY KEY,
                                         topic           VARCHAR(64) NOT NULL,
    tag             VARCHAR(32),
    payload         JSONB NOT NULL,
    status          SMALLINT NOT NULL DEFAULT 0,          -- 0=待发 1=已发 2=发送失败
    retry_count     SMALLINT NOT NULL DEFAULT 0,
    next_retry_time TIMESTAMPTZ,
    create_time     TIMESTAMPTZ NOT NULL DEFAULT now(),
    send_time       TIMESTAMPTZ
    );
CREATE INDEX IF NOT EXISTS idx_outbox_status_retry
    ON mq_outbox(status, next_retry_time) WHERE status IN (0, 2);

-- ==================== 业务域 3: 群组 ====================
CREATE TABLE IF NOT EXISTS chat_group (
                                          id          BIGSERIAL PRIMARY KEY,
                                          room_id     BIGINT NOT NULL UNIQUE,                   -- 与 chat_room 一一对应
                                          name        VARCHAR(64) NOT NULL,
    avatar      VARCHAR(255),
    owner_id    BIGINT NOT NULL,
    announcement TEXT,
    type        SMALLINT NOT NULL,                        -- 1=班级群 2=兴趣群
    class_id    BIGINT,                                   -- 班级群关联
    deleted     BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS chat_group_member (
                                                 id          BIGSERIAL PRIMARY KEY,
                                                 group_id    BIGINT NOT NULL,
                                                 user_id     BIGINT NOT NULL,
                                                 role        SMALLINT NOT NULL DEFAULT 3,              -- 论文三级角色：1=群主 2=管理员 3=普通成员
                                                 nickname    VARCHAR(32),
    join_time   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_group_member UNIQUE (group_id, user_id)
    );

-- ==================== 业务域 4: 课堂签到 ====================
CREATE TABLE IF NOT EXISTS course (
                                      id           BIGSERIAL PRIMARY KEY,
                                      name         VARCHAR(64) NOT NULL,
    teacher_id   BIGINT NOT NULL,
    create_time  TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS course_class_rel (
                                                course_id BIGINT NOT NULL,
                                                class_id  BIGINT NOT NULL,
                                                PRIMARY KEY (course_id, class_id)
    );

-- 论文 4.2: 签到会话表，结束时间由 GENERATED 列自动维护
CREATE TABLE IF NOT EXISTS checkin_session (
                                               id                BIGSERIAL PRIMARY KEY,
                                               course_id         BIGINT NOT NULL,
                                               creator_id        BIGINT NOT NULL,                     -- 教师 id
                                               title             VARCHAR(64),
    center_latitude   NUMERIC(10, 7) NOT NULL,             -- 论文：高精度数值类型
    center_longitude  NUMERIC(10, 7) NOT NULL,
    radius_meters     INTEGER NOT NULL,
    duration_minutes  INTEGER NOT NULL,
    start_time        TIMESTAMP NOT NULL DEFAULT now(),
    -- 论文 4.2 原话：结束时间由数据库 GENERATED ALWAYS AS 计算列自动维护，消除时区偏差风险
    -- 注：使用 TIMESTAMP（不带时区），使 timestamp + interval 运算为 IMMUTABLE
    end_time          TIMESTAMP GENERATED ALWAYS AS (start_time + duration_minutes * INTERVAL '1 minute') STORED,
    code              VARCHAR(8),                          -- 签到码（可选）
    status            SMALLINT NOT NULL DEFAULT 1,         -- 1=进行中 2=已结束
    create_time       TIMESTAMPTZ NOT NULL DEFAULT now()
    );
CREATE INDEX IF NOT EXISTS idx_checkin_session_course_active
    ON checkin_session(course_id, start_time DESC) WHERE status = 1;

CREATE TABLE IF NOT EXISTS checkin_session_class (
                                                     session_id BIGINT NOT NULL,
                                                     class_id   BIGINT NOT NULL,
                                                     PRIMARY KEY (session_id, class_id)
    );

-- 论文：签到会话 ID + 学生 ID 联合唯一约束，杜绝重复提交
CREATE TABLE IF NOT EXISTS checkin_record (
                                              id          BIGSERIAL PRIMARY KEY,
                                              session_id  BIGINT NOT NULL,
                                              student_id  BIGINT NOT NULL,
                                              latitude    NUMERIC(10, 7),
    longitude   NUMERIC(10, 7),
    distance_m  INTEGER,                                  -- 距围栏中心的球面距离
    status      SMALLINT NOT NULL,                        -- 1=正常 2=迟到 3=范围外 4=异常 5=补签通过
    checkin_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_checkin_record UNIQUE (session_id, student_id)
    );

CREATE TABLE IF NOT EXISTS checkin_supplement (
                                                  id           BIGSERIAL PRIMARY KEY,
                                                  session_id   BIGINT NOT NULL,
                                                  student_id   BIGINT NOT NULL,
                                                  reason       TEXT NOT NULL,
                                                  attachment   JSONB,                                   -- 补签证明材料 JSONB
                                                  status       SMALLINT NOT NULL DEFAULT 0,             -- 0=待审核 1=通过 2=驳回
                                                  approver_id  BIGINT,
                                                  approve_time TIMESTAMPTZ,
                                                  approve_note VARCHAR(255),
    create_time  TIMESTAMPTZ NOT NULL DEFAULT now()
    );

-- ==================== 业务域 5: 请假 ====================
-- 论文 5.4: 状态机 - 0 待审批 / 1 已通过 / 2 已驳回 / 3 已撤销
CREATE TABLE IF NOT EXISTS leave_application (
                                                 id              BIGSERIAL PRIMARY KEY,
                                                 applicant_id    BIGINT NOT NULL,
                                                 approver_id     BIGINT NOT NULL,                      -- 教师 / 辅导员
                                                 type            SMALLINT NOT NULL,                    -- 1=病假 2=事假 3=其他
                                                 reason          TEXT NOT NULL,
                                                 start_time      TIMESTAMPTZ NOT NULL,
                                                 end_time        TIMESTAMPTZ NOT NULL,
                                                 attachments     JSONB NOT NULL DEFAULT CAST('[]' AS jsonb),  -- 证明材料
    status          SMALLINT NOT NULL DEFAULT 0,
    approve_note    VARCHAR(255),
    approve_time    TIMESTAMPTZ,
    card_msg_id     BIGINT,                               -- 关联聊天中内嵌的请假卡片消息
    create_time     TIMESTAMPTZ NOT NULL DEFAULT now(),
    update_time     TIMESTAMPTZ NOT NULL DEFAULT now()
    );
CREATE INDEX IF NOT EXISTS idx_leave_approver_pending
    ON leave_application(approver_id, create_time DESC) WHERE status = 0;

-- 状态流转日志
CREATE TABLE IF NOT EXISTS leave_log (
                                         id          BIGSERIAL PRIMARY KEY,
                                         leave_id    BIGINT NOT NULL,
                                         from_status SMALLINT,
                                         to_status   SMALLINT NOT NULL,
                                         operator_id BIGINT NOT NULL,
                                         remark      VARCHAR(255),
    create_time TIMESTAMPTZ NOT NULL DEFAULT now()
    );

-- ==================== 业务域 6: 通知与考试 ====================
CREATE TABLE IF NOT EXISTS notice (
                                      id          BIGSERIAL PRIMARY KEY,
                                      title       VARCHAR(128) NOT NULL,
    content     TEXT NOT NULL,
    publisher_id BIGINT NOT NULL,
    scope_type  SMALLINT NOT NULL,                         -- 1=全量 2=按院系 3=按班级 4=按个人
    scope_data  JSONB NOT NULL DEFAULT CAST('{}' AS jsonb),       -- 论文：JSONB 存定向推送目标
    create_time TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS notice_read (
                                           notice_id BIGINT NOT NULL,
                                           user_id   BIGINT NOT NULL,
                                           read_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (notice_id, user_id)
    );

CREATE TABLE IF NOT EXISTS exam (
                                    id          BIGSERIAL PRIMARY KEY,
                                    course_id   BIGINT NOT NULL,
                                    name        VARCHAR(128) NOT NULL,
    exam_time   TIMESTAMPTZ NOT NULL,
    duration_minutes INTEGER NOT NULL,
    location    VARCHAR(128) NOT NULL,
    create_time TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS exam_student_rel (
                                                exam_id    BIGINT NOT NULL,
                                                student_id BIGINT NOT NULL,
                                                seat_no    VARCHAR(32),
    PRIMARY KEY (exam_id, student_id)
    );
CREATE INDEX IF NOT EXISTS idx_exam_student_time
    ON exam_student_rel(student_id);