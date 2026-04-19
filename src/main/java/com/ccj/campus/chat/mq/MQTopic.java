package com.ccj.campus.chat.mq;

/**
 * RocketMQ 主题/标签常量。对齐论文 5.2 / 1.3：
 *  - 离线消息补推
 *  - 消息异步持久化
 *  - 通知推送（全量/定向）
 */
public final class MQTopic {
    private MQTopic() {}

    /** 消息异步持久化 */
    public static final String MSG_PERSIST_TOPIC = "msg-persist-topic";

    /** 离线消息补推 */
    public static final String OFFLINE_MSG_TOPIC = "offline-msg-topic";

    /** 通知广播（全量/院系/班级） */
    public static final String NOTICE_BROADCAST_TOPIC = "notice-broadcast-topic";

    /** 请假状态变更 */
    public static final String LEAVE_EVENT_TOPIC = "leave-event-topic";

    /** 签到会话开启广播（教师发起签到时通知所有在课学生） */
    public static final String CHECKIN_START_TOPIC = "checkin-start-topic";
}