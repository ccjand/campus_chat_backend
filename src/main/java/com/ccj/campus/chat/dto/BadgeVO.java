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
}