package com.ccj.campus.chat.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
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
 * 消息实体。对齐论文 4.2：
 *  - JSONB 扩展字段（ext_info）
 *  - 客户端序列号幂等（from_uid + room_id + client_seq 唯一约束）
 *  - is_visible 为 DB GENERATED 计算列，只读
 */
@Data
@TableName(value = "chat_message", autoResultMap = true)
public class Message implements Serializable {

    /** 消息类型枚举（对齐论文"整数枚举标识文本、撤回、图片、文件等 12 种类型"） */
    public static final int TYPE_TEXT         = 1;
    public static final int TYPE_IMAGE        = 2;
    public static final int TYPE_FILE         = 3;
    public static final int TYPE_VOICE        = 4;
    public static final int TYPE_EMOJI        = 5;
    public static final int TYPE_RECALL       = 6;
    public static final int TYPE_LEAVE_CARD   = 7;
    public static final int TYPE_CHECKIN_CARD = 8;
    public static final int TYPE_NOTICE_CARD  = 9;

    public static final int STATUS_NORMAL   = 0;
    public static final int STATUS_RECALLED = 1;
    public static final int STATUS_DELETED  = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roomId;
    private Long fromUid;
    private Integer type;
    private String content;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extInfo;

    private String clientSeq;
    private Integer status;
    private LocalDateTime createTime;

    /** DB 生成的计算列，只读 */
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Boolean isVisible;
}