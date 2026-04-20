package com.ccj.campus.chat.service;

import com.ccj.campus.chat.dto.BadgeVO;

public interface BadgeService {

    /**
     * 查询用户的 tab 徽章数据
     */
    BadgeVO getBadge(Long uid, Integer role);

    /**
     * 通过 WebSocket 推送最新徽章给指定用户（如果在线）
     */
    void pushBadgeIfOnline(Long uid, Integer role);

}