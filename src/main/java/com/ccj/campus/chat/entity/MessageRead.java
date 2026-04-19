package com.ccj.campus.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对齐论文 3.2：基于数据库时间戳的消息已读回执机制
 */
@Data
@TableName("chat_message_read")
public class MessageRead implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long msgId;
    private Long roomId;
    private Long readerId;
    private LocalDateTime readTime;
}