package com.ccj.campus.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserLoginResp {
    private Long uid;
    private String name;
    private String avatar;
    private Integer role;
    private String token;
}