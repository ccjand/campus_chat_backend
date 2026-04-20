package com.ccj.campus.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("friend_request")
public class FriendRequest implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fromId;
    private Long toId;
    private String reason;
    private Integer status;       // 0=待处理 1=已同意 2=已拒绝
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}