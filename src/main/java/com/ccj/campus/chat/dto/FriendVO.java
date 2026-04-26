package com.ccj.campus.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FriendVO {
    private Long uid;            // 好友的用户 ID
    private Long roomId;         // 你们之间的单聊房间 ID
    private String fullName;     // 姓名
    private String accountNumber;// 账号
    private String avatar;       // 头像
    private Integer role;        // 角色
    private String remark;       // 备注
    private String department;   // 学院/部门
    private Boolean isBlocked;   // 是否已拉黑
}