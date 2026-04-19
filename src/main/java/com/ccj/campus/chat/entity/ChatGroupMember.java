package com.ccj.campus.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 群成员。对齐论文 3.2 三级角色制度：
 *   1=群主  2=管理员  3=普通成员
 */
@Data
@TableName("chat_group_member")
public class ChatGroupMember implements Serializable {

    public static final int ROLE_OWNER  = 1;
    public static final int ROLE_ADMIN  = 2;
    public static final int ROLE_MEMBER = 3;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long userId;
    private Integer role;
    private String nickname;
    private LocalDateTime joinTime;
}