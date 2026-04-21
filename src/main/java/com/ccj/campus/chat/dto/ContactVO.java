package com.ccj.campus.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话列表返回给前端的 VO。
 *
 * 对齐 /capi/contact/list 接口重构需求：后端一次性把首页渲染需要的
 * 全部字段拼好，消除前端对 /friend/list + N 次 /message/history 的依赖。
 */
@Data
@Builder
public class ContactVO implements Serializable {

    /** 会话记录主键 id (chat_contact.id) */
    private Long id;

    /** 聊天室 id */
    private Long roomId;

    /** 会话类型 1=单聊 2=群聊 */
    private Integer type;

    /** 展示名称：单聊=对方昵称，群聊=群名 */
    private String name;

    /** 展示头像：单聊=对方头像，群聊=群头像 */
    private String avatar;

    /** 最新一条消息的摘要（图片/文件/语音等已在后端做过占位符替换） */
    private String summary;

    /** 未读数（room 内 m.id > last_read_id 且 from_uid != self） */
    private Integer unreadCount;

    /** 会话最后活跃时间（最新一条消息的 createTime，若无消息则回退 active_time） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /** 是否置顶 */
    private Boolean top;

    /** 是否免打扰 */
    private Boolean mute;
}