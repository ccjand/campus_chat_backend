package com.ccj.campus.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ccj.campus.chat.dto.BadgeVO;
import com.ccj.campus.chat.entity.FriendRequest;
import com.ccj.campus.chat.entity.LeaveApplication;
import com.ccj.campus.chat.entity.SysUser;
import com.ccj.campus.chat.mapper.ContactMapper;
import com.ccj.campus.chat.mapper.FriendRequestMapper;
import com.ccj.campus.chat.mapper.LeaveApplicationMapper;
import com.ccj.campus.chat.mapper.SysUserMapper;
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
    private final SysUserMapper sysUserMapper;

    @Override
    public BadgeVO getBadge(Long uid) {
        // 角色从数据库 sys_user.role 字段读取，不依赖外部传参
        SysUser user = sysUserMapper.selectById(uid);
        Integer role = (user != null) ? user.getRole() : null;

        int unreadMsgCount = contactMapper.countTotalUnread(uid);

        QueryWrapper<FriendRequest> frQw = new QueryWrapper<>();
        frQw.eq("to_id", uid).eq("status", 0);
        boolean contactDot = friendRequestMapper.selectCount(frQw) > 0;

        // 只有辅导员(3)和院长(4)才有请假审批权限，才需要显示工作台红点
        boolean workbenchDot = false;
        if (role != null && (role == SysUser.ROLE_COUNSELOR || role == SysUser.ROLE_STAFF)) {
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
    public void pushBadgeIfOnline(Long uid) {
        if (onlineUserService.isOnline(uid)) {
            BadgeVO badge = getBadge(uid);
            onlineUserService.push(uid, "/queue/badge", badge);
        }
    }
}