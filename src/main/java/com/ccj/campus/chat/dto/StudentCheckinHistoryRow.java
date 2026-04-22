package com.ccj.campus.chat.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学生签到历史查询的扁平行映射。
 * Mapper 层先把 session + course + record 关联查平，由 Service 再按 courseId 分组。
 */
@Data
public class StudentCheckinHistoryRow implements Serializable {
    private Long sessionId;
    private Long courseId;
    private String courseName;
    private String sessionTitle;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /** 该学生此次签到的记录状态（无记录则为 null） */
    private Integer recStatus;

    /** 该学生的签到时间（无记录则为 null） */
    private LocalDateTime checkInTime;
}