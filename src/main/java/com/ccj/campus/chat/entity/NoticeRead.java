package com.ccj.campus.chat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知已读记录（联合主键 notice_id + user_id）
 */
@Data
@TableName("notice_read")
public class NoticeRead implements Serializable {
    private Long noticeId;
    private Long userId;
    private LocalDateTime readTime;
}