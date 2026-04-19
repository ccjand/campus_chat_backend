package com.ccj.campus.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 群组。对齐论文 3.2：
 *   - 班级群(type=1) + 兴趣群(type=2)
 *   - 群内三级角色在 ChatGroupMember 中定义
 */
@Data
@TableName("chat_group")
public class ChatGroup implements Serializable {

    public static final int TYPE_CLASS    = 1;
    public static final int TYPE_INTEREST = 2;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private String name;
    private String avatar;
    private Long ownerId;
    private String announcement;
    private Integer type;
    private Long classId;
    private Boolean deleted;
    private LocalDateTime createTime;
}