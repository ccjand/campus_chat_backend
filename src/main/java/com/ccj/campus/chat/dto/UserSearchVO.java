package com.ccj.campus.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSearchVO {
    private Long uid;
    private String accountNumber;  // 学号/工号
    private String name;
    private Integer role;          // 1=学生 2=教师 3=管理员
    private String avatar;
    private String departmentName; // 院系名称（非 id）
    private Boolean isFriend;      // 当前用户是否已添加为好友
    private Boolean isBlocked;     // 当前用户是否已拉黑该用户
}