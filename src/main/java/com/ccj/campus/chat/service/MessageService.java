package com.ccj.campus.chat.service;

import com.ccj.campus.chat.dto.ChatMessageDTO;
import com.ccj.campus.chat.entity.Message;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息业务接口。对齐论文 3.2 / 5.2：
 *  - 异步持久化（消费者回调入口）
 *  - 撤回（2 分钟时间窗）
 *  - 已读回执（基于数据库时间戳）
 *  - 历史消息分页、离线消息补推
 */
public interface MessageService {

    /** 预分配消息 id，用于 STOMP 推送前回填 */
    Long prepareMessageId();

    /** 异步持久化（MQ 消费者回调） */
    void persist(ChatMessageDTO dto);

    /** 更新会话最后一条消息 */
    void updateLastMsg(Long roomId, Long msgId);

    /** 撤回 */
    void recall(Long msgId, Long uid);

    /** 已读回执 */
    void markRead(Long roomId, Long msgId, Long readerId);

    /** 分页历史消息 */
    List<Message> pullHistory(Long roomId, LocalDateTime cursor, int size);

    /** 上线后拉取离线消息 */
    List<ChatMessageDTO> pullOffline(Long uid, int max);
}