package com.ccj.campus.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ccj.campus.chat.dto.BadgeVO;
import com.ccj.campus.chat.entity.FriendRequest;
import com.ccj.campus.chat.entity.LeaveApplication;
import com.ccj.campus.chat.mapper.ContactMapper;
import com.ccj.campus.chat.mapper.FriendRequestMapper;
import com.ccj.campus.chat.mapper.LeaveApplicationMapper;
import com.ccj.campus.chat.service.BadgeService;
import com.ccj.campus.chat.websocket.OnlineUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BadgeServiceImpl implements BadgeService {

    private final ContactMapper contactMapper;
    private final FriendRequestMapper friendRequestMapper;
    private final LeaveApplicationMapper leaveMapper;
    private final OnlineUserService onlineUserService;

    @Override
    public BadgeVO getBadge(Long uid, Integer role) {
        int unreadMsgCount = contactMapper.countTotalUnread(uid);

        QueryWrapper<FriendRequest> frQw = new QueryWrapper<>();
        frQw.eq("to_id", uid).eq("status", 0);
        boolean contactDot = friendRequestMapper.selectCount(frQw) > 0;

        boolean workbenchDot = false;
        if (role != null && (role == 2 || role == 3)) {
            QueryWrapper<LeaveApplication> leaveQw = new QueryWrapper<>();
            leaveQw.eq("approver_id", uid).eq("status", 0);
            workbenchDot = leaveMapper.selectCount(leaveQw) > 0;
        }

        boolean mineDot = false;

        return BadgeVO.builder()
                .unreadMsgCount(unreadMsgCount)
                .contactDot(contactDot)
                .workbenchDot(workbenchDot)
                .mineDot(mineDot)
                .build();
    }

    @Override
    public void pushBadgeIfOnline(Long uid, Integer role) {
        if (onlineUserService.isOnline(uid)) {
            BadgeVO badge = getBadge(uid, role);
            onlineUserService.push(uid, "/queue/badge", badge);
        }
    }
}