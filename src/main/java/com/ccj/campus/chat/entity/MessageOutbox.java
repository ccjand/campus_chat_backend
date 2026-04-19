package com.ccj.campus.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 本地消息表（Outbox Pattern）。
 *
 * 对齐论文 1.3：
 * "本地消息表与异步重试机制的结合，为网络抖动等异常场景下的消息可靠性提供了制度性保障"
 *
 * 写入时机：业务事务内同库同事务写入，保证"业务与 outbox 要么都成功、要么都失败"。
 * 发送时机：独立定时任务扫描 status=0/2 的记录，RocketMQ 投递，成功后 status=1。
 */
@Data
@TableName("mq_outbox")
public class MessageOutbox implements Serializable {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_SENT    = 1;
    public static final int STATUS_FAILED  = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String topic;
    private String tag;

    /** JSONB 字段内容 */
    private String payload;

    private Integer status;
    private Integer retryCount;

    private LocalDateTime nextRetryTime;
    private LocalDateTime createTime;
    private LocalDateTime sendTime;
}