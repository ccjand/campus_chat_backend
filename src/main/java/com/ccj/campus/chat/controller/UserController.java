package com.ccj.campus.chat.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccj.campus.chat.common.R;
import com.ccj.campus.chat.common.ResultCode;
import com.ccj.campus.chat.dto.UpdatePasswordReq;
import com.ccj.campus.chat.dto.UserSearchVO;
import com.ccj.campus.chat.entity.SysUser;
import com.ccj.campus.chat.security.LoginUser;
import com.ccj.campus.chat.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 搜索用户 — 对齐论文 3.2："用户可在系统内搜索、添加或删除好友"
     */
    @GetMapping("/search")
    public R<List<UserSearchVO>> search(@RequestParam String keyword) {
        return R.ok(userService.searchUsers(keyword, LoginUser.currentUid()));
    }

    /**
     * 获取所有可选审批人（教师或辅导员）
     */
    @GetMapping("/approvers")
    public R<List<UserSearchVO>> getApprovers() {
        return R.ok(userService.getApprovers());
    }

    /**
     * 更新个人信息（只允许修改姓名、头像）
     */
    @PostMapping("/update")
    public R<Void> updateProfile(@RequestBody UpdateProfileReq req) {
        SysUser user = userService.getById(LoginUser.currentUid());
        if (user == null) {
            return R.fail(ResultCode.USER_NOT_FOUND);
        }

        // 仅允许更新姓名和头像
        if (req.getName() != null && !req.getName().trim().isEmpty()) {
            user.setName(req.getName().trim());
        }
        if (req.getAvatar() != null) {
            user.setAvatar(req.getAvatar().trim());
        }

        user.setUpdateTime(LocalDateTime.now());
        userService.updateUserInfo(user);
        return R.ok();
    }

    @Data
    public static class UpdateProfileReq {
        private String name;
        private String avatar;
    }

    @PostMapping("/updatePassword")
    public R<Void> updatePassword(@RequestBody UpdatePasswordReq req) {
        // 尝试获取当前登录用户
        Long currentUserId = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser) {
            currentUserId = ((LoginUser) auth.getPrincipal()).getUid();
        }

        if (currentUserId != null) {
            // ==================== 情况 1：已登录 ====================
            // 已登录，直接根据 Token 中的用户 ID 修改密码
            userService.updatePasswordById(currentUserId, req.getPassword());
            return R.ok(null);
        } else {
            // ==================== 情况 2：未登录 (忘记密码) ====================
            String accountNumber = req.getAccountNumber();
            String oldPassword = req.getOldPassword();
            String newPassword = req.getPassword();

            if (accountNumber == null || accountNumber.trim().isEmpty()) {
                return R.fail("账号不能为空");
            }
            if (oldPassword == null || oldPassword.trim().isEmpty()) {
                return R.fail("原密码不能为空");
            }

            // 1. 去数据库查这个账号对应的真实信息
            SysUser user = userService.getByAccountNumber(accountNumber);
            if (user == null) {
                return R.fail("账号不存在");
            }

            // 2. 校验旧密码是否正确（使用 Spring Security 的 PasswordEncoder）
            if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
                return R.fail("原密码错误");
            }

            // 3. 校验通过，根据账号修改为新密码
            userService.updatePasswordByAccount(accountNumber, newPassword);
            return R.ok(null);
        }
    }
}