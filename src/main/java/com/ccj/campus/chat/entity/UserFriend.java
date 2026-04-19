package com.ccj.campus.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 好友关系。对齐论文 3.2：
 * "用户可在系统内搜索、添加或删除好友"
 */
@Data
@TableName("user_friend")
public class UserFriend implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long friendId;
    private String remark;

    @TableLogic
    private Boolean deleted;

    private LocalDateTime createTime;
}