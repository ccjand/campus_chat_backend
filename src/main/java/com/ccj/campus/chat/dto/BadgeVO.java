package com.ccj.campus.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BadgeVO {
    private int unreadMsgCount;
    private boolean contactDot;
    private boolean workbenchDot;
    private boolean mineDot;

    private boolean leaveDot;       // 有待审批请假
    private boolean leaveResultDot;
    private boolean supplementDot;  // 有待审批补签
    private boolean noticeDot;      // 有未读通知
}