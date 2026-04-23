package com.ccj.campus.chat.dto;

import lombok.Data;

@Data
public class StudentCheckinStatusVO {
    private Long studentId;
    private String studentName;
    private String accountNumber;
    private Integer status; // 0=未签到, 1=已签到, 3=已请假
}