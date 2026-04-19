package com.ccj.campus.chat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * WebSocket 消息 DTO。对齐论文 5.2 Listing 1 的 ChatMessage 数据结构。
 * 客户端 send 到 /app/chat/send 时使用。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageDTO implements Serializable {

    /** 服务端生成的消息 id（持久化后回填） */
    private Long id;

    /** 发送方 uid - 客户端不填，服务端从 Principal 拿 */
    private Long fromUid;

    /** 接收方 uid（单聊）或 群 id（群聊） */
    private Long receiverId;

    private Long roomId;

    /** 1文本 2图片 3文件 4语音 5表情 6撤回 7请假卡片 8签到卡片 ... */
    private Integer type;

    private String content;

    private Map<String, Object> extInfo;

    /** 客户端序列号，幂等去重用 */
    private String clientSeq;

    private LocalDateTime createTime;
}