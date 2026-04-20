package com.ccj.campus.chat.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class FriendRequestVO {
    private Long requestId;
    private Long fromId;
    private String fromName;
    private String fromAvatar;
    private String fromAccountNumber;
    private Integer fromRole;
    private String reason;
    private Integer status;
    private LocalDateTime createTime;
}