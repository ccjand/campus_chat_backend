package com.ccj.campus.chat.controller;

import com.ccj.campus.chat.common.R;
import com.ccj.campus.chat.dto.UserLoginResp;
import com.ccj.campus.chat.entity.SysUser;
import com.ccj.campus.chat.security.LoginUser;
import com.ccj.campus.chat.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

/**
 * 对齐论文 4.3 认证组接口：登录与登出。
 * 路径：/auth/**  —— 在 SecurityConfig 中放行。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public R<UserLoginResp> login(@RequestBody @Valid LoginReq req) {
        return R.ok(userService.login(req.getAccountNumber(), req.getPassword()));
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ")
                ? header.substring(7) : "";
        userService.logout(LoginUser.currentUid(), token);
        return R.ok();
    }

    @GetMapping("/me")
    public R<SysUser> me() {
        return R.ok(userService.getById(LoginUser.currentUid()));
    }

    @Data
    static class LoginReq {
        @NotBlank(message = "账号不能为空")
        private String accountNumber;
        @NotBlank(message = "密码不能为空")
        private String password;
    }
}