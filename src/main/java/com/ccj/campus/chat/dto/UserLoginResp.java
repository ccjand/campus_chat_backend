package com.ccj.campus.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserLoginResp {
    private Long uid;

    /** 真实姓名，如"张三" */
    private String name;

    /** 学号 / 工号，如"2020110411" */
    private String accountNumber;

    /** 所属学院 / 部门名称，如"计算机科学与技术学院" */
    private String departmentName;

    private String avatar;

    /** 1=学生 2=教师 3=管理员 */
    private Integer role;

    private String token;

}