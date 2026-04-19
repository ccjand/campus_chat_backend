package com.ccj.campus.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 聊天房间。对齐论文 4.2：
 *   type: 1=单聊  2=群聊
 *   ext_info: JSONB 扩展字段
 */
@Data
@TableName(value = "chat_room", autoResultMap = true)
public class ChatRoom implements Serializable {

    public static final int TYPE_SINGLE = 1;
    public static final int TYPE_GROUP  = 2;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer type;
    private String name;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extInfo;

    private LocalDateTime createTime;
}