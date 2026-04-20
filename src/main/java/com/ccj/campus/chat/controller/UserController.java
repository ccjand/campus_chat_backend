package com.ccj.campus.chat.controller;

import com.ccj.campus.chat.common.R;
import com.ccj.campus.chat.dto.UserSearchVO;
import com.ccj.campus.chat.security.LoginUser;
import com.ccj.campus.chat.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 搜索用户 — 对齐论文 3.2："用户可在系统内搜索、添加或删除好友" */
    @GetMapping("/search")
    public R<List<UserSearchVO>> search(@RequestParam String keyword) {
        return R.ok(userService.searchUsers(keyword, LoginUser.currentUid()));
    }
}