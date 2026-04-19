package com.ccj.campus.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 黑名单。对齐论文 3.2：
 * "支持将指定用户加入黑名单以屏蔽其消息"
 */
@Data
@TableName("user_blacklist")
public class UserBlacklist implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long targetId;
    private LocalDateTime createTime;
}