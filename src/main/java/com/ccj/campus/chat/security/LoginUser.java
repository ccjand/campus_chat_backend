package com.ccj.campus.chat.security;

import com.ccj.campus.chat.common.BusinessException;
import com.ccj.campus.chat.common.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.Serializable;

/**
 * 登录用户信息。用 Spring Security 的 SecurityContextHolder 取，
 * 替换论文旧版的自定义 RequestHolder —— 统一通过 Spring Security 生态取用户。
 */
@Data
@AllArgsConstructor
public class LoginUser implements Serializable {
    private Long uid;
    private Integer role;      // 1=学生 2=教师 3=管理员

    /**
     * 取当前登录用户 id
     */
    public static Long currentUid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return ((LoginUser) auth.getPrincipal()).getUid();
    }

    public static LoginUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return (LoginUser) auth.getPrincipal();
    }

    public boolean isTeacher() {
        return role != null && role == 2;
    }

    public boolean isStudent() {
        return role != null && role == 1;
    }

    public boolean isAdmin() {
        return role != null && role == 3;
    }
}