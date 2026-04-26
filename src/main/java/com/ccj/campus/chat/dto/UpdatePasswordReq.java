package com.ccj.campus.chat.dto;

import lombok.Data; // 如果你用了 Lombok，可以直接用 @Data

@Data
public class UpdatePasswordReq {
    private String accountNumber; // 账号/学号
    private String oldPassword;   // 原密码（新增）
    private String password;      // 新密码
}