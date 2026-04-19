package com.ccj.campus.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话列表。对齐论文 3.2：支持置顶与免打扰设置
 */
@Data
@TableName("chat_contact")
public class Contact implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long roomId;
    private Long lastMsgId;
    private Long lastReadId;
    private Boolean top;
    private Boolean mute;
    private LocalDateTime activeTime;
}