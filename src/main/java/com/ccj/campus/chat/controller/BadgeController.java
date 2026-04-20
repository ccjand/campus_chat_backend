package com.ccj.campus.chat.controller;

import com.ccj.campus.chat.common.R;
import com.ccj.campus.chat.dto.BadgeVO;
import com.ccj.campus.chat.security.LoginUser;
import com.ccj.campus.chat.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/badge")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    @GetMapping
    public R<BadgeVO> badge() {
        LoginUser lu = LoginUser.current();
        return R.ok(badgeService.getBadge(lu.getUid(), lu.getRole()));
    }
}