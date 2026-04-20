package com.ccj.campus.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FriendRequestVO {
    private Long requestId;
    private Long fromId;          // 申请人 id
    private Long toId;            // 被申请人 id

    /**
     * 对方的信息（收到的申请 = 申请人信息，发出的申请 = 被申请人信息）
     */
    private Long targetId;
    private String targetName;
    private String targetAvatar;
    private String targetAccountNumber;
    private Integer targetRole;

    private String reason;
    private Integer status;       // 0=待处理 1=已同意 2=已拒绝
    private LocalDateTime createTime;
}