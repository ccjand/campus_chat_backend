package com.ccj.campus.chat.service;

/**
 * 本地消息表（Outbox Pattern）服务接口。
 * 对齐论文 1.3："本地消息表与异步重试机制的结合，为网络抖动等异常场景下
 *              的消息可靠性提供了制度性保障"。
 */
public interface OutboxService {

    /** 入队：业务事务内同库写入 mq_outbox */
    void enqueue(String topic, String tag, Object payload);
}