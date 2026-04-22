package com.ccj.campus.chat.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学生端「我的签到记录」内层单条记录。
 * checkedIn 为 false 时 status 与 checkInTime 均为 null。
 */
@Data
public class StudentHistoryRecordVO implements Serializable {
    private Long sessionId;
    private String sessionTitle;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /** 是否已签 */
    private Boolean checkedIn;

    /**
     * 签到状态。未签到为 null。
     * 1=正常 2=迟到 3=超出范围 4=异常 5=补签
     */
    private Integer status;

    /** 实际签到时间，未签到为 null */
    private LocalDateTime checkInTime;
}