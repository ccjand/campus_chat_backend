package com.ccj.campus.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ccj.campus.chat.common.BusinessException;
import com.ccj.campus.chat.common.ResultCode;
import com.ccj.campus.chat.dto.FriendRequestVO;
import com.ccj.campus.chat.entity.FriendRequest;
import com.ccj.campus.chat.entity.SysUser;
import com.ccj.campus.chat.entity.UserBlacklist;
import com.ccj.campus.chat.entity.UserFriend;
import com.ccj.campus.chat.mapper.FriendRequestMapper;
import com.ccj.campus.chat.mapper.SysUserMapper;
import com.ccj.campus.chat.mapper.UserBlacklistMapper;
import com.ccj.campus.chat.mapper.UserFriendMapper;
import com.ccj.campus.chat.service.FriendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final UserFriendMapper friendMapper;
    private final UserBlacklistMapper blacklistMapper;
    private final FriendRequestMapper requestMapper;
    private final SysUserMapper userMapper;  // 如果还没注入的话加上

    @Override
    public void sendRequest(Long fromId, Long toId, String reason) {
        // 不能加自己
        BusinessException.check(!fromId.equals(toId), ResultCode.BAD_REQUEST);
        // 已经是好友
        BusinessException.check(!isFriend(fromId, toId), ResultCode.BAD_REQUEST, "已经是好友");
        // 被对方拉黑
        BusinessException.check(!isBlocked(toId, fromId), ResultCode.BAD_REQUEST, "对方已将你拉黑");

        // 检查是否已有待处理的申请
        QueryWrapper<FriendRequest> qw = new QueryWrapper<>();
        qw.eq("from_id", fromId).eq("to_id", toId).eq("status", 0);
        FriendRequest existing = requestMapper.selectOne(qw);
        if (existing != null) {
            // 已有待处理申请，更新理由和时间即可
            existing.setReason(reason);
            existing.setUpdateTime(LocalDateTime.now());
            requestMapper.updateById(existing);
            return;
        }

        // 检查是否有旧的（已同意/已拒绝）记录，有则覆盖
        QueryWrapper<FriendRequest> qw2 = new QueryWrapper<>();
        qw2.eq("from_id", fromId).eq("to_id", toId);
        FriendRequest old = requestMapper.selectOne(qw2);
        if (old != null) {
            old.setStatus(0);
            old.setReason(reason);
            old.setUpdateTime(LocalDateTime.now());
            requestMapper.updateById(old);
            return;
        }

        FriendRequest req = new FriendRequest();
        req.setFromId(fromId);
        req.setToId(toId);
        req.setReason(reason);
        req.setStatus(0);
        req.setCreateTime(LocalDateTime.now());
        req.setUpdateTime(LocalDateTime.now());
        requestMapper.insert(req);
    }

    @Override
    @Transactional
    public void acceptRequest(Long currentUid, Long requestId) {
        FriendRequest req = requestMapper.selectById(requestId);
        BusinessException.check(req != null, ResultCode.BAD_REQUEST, "申请不存在");
        BusinessException.check(req.getToId().equals(currentUid), ResultCode.FORBIDDEN, "无权操作");
        BusinessException.check(req.getStatus() == 0, ResultCode.BAD_REQUEST, "该申请已处理");

        // 更新申请状态
        req.setStatus(1);
        req.setUpdateTime(LocalDateTime.now());
        requestMapper.updateById(req);

        // 双向写入好友关系（复用已有逻辑）
        addFriend(req.getFromId(), req.getToId());
    }

    @Override
    public void rejectRequest(Long currentUid, Long requestId) {
        FriendRequest req = requestMapper.selectById(requestId);
        BusinessException.check(req != null, ResultCode.BAD_REQUEST, "申请不存在");
        BusinessException.check(req.getToId().equals(currentUid), ResultCode.FORBIDDEN, "无权操作");
        BusinessException.check(req.getStatus() == 0, ResultCode.BAD_REQUEST, "该申请已处理");

        req.setStatus(2);
        req.setUpdateTime(LocalDateTime.now());
        requestMapper.updateById(req);
    }

    @Override
    public List<FriendRequestVO> listReceivedRequests(Long userId) {
        QueryWrapper<FriendRequest> qw = new QueryWrapper<>();
        qw.eq("to_id", userId).eq("status", 0).orderByDesc("create_time");
        List<FriendRequest> list = requestMapper.selectList(qw);
        return list.stream().map(r -> buildVO(r, r.getFromId())).collect(Collectors.toList());
    }

    @Override
    public List<FriendRequestVO> listSentRequests(Long userId) {
        QueryWrapper<FriendRequest> qw = new QueryWrapper<>();
        qw.eq("from_id", userId).orderByDesc("create_time");
        List<FriendRequest> list = requestMapper.selectList(qw);
        return list.stream().map(r -> buildVO(r, r.getToId())).collect(Collectors.toList());
    }

    /**
     * 组装 VO，查询对方的基本信息
     */
    private FriendRequestVO buildVO(FriendRequest r, Long targetUid) {
        SysUser u = userMapper.selectById(targetUid);
        return FriendRequestVO.builder()
                .requestId(r.getId())
                .fromId(r.getFromId())
                .fromName(u != null ? u.getName() : "未知用户")
                .fromAvatar(u != null ? u.getAvatar() : null)
                .fromAccountNumber(u != null ? u.getAccountNumber() : null)
                .fromRole(u != null ? u.getRole() : null)
                .reason(r.getReason())
                .status(r.getStatus())
                .createTime(r.getCreateTime())
                .build();
    }

    @Override
    @Transactional
    public void addFriend(Long userId, Long friendId) {
        BusinessException.check(!userId.equals(friendId), ResultCode.BAD_REQUEST);
        // 检查是否被对方拉黑
        BusinessException.check(!isBlocked(friendId, userId), ResultCode.BAD_REQUEST);

        // 双向写入
        insertFriend(userId, friendId);
        insertFriend(friendId, userId);
    }

    private void insertFriend(Long userId, Long friendId) {
        // 先查是否存在已删除的记录，有则恢复
        UserFriend existing = friendMapper.selectOne(
                new QueryWrapper<UserFriend>()
                        .eq("user_id", userId)
                        .eq("friend_id", friendId));
        if (existing != null) {
            if (existing.getDeleted()) {
                existing.setDeleted(false);
                existing.setCreateTime(LocalDateTime.now());
                friendMapper.updateById(existing);
            }
            // 已存在且未删除，忽略
            return;
        }
        UserFriend f = new UserFriend();
        f.setUserId(userId);
        f.setFriendId(friendId);
        f.setDeleted(false);
        f.setCreateTime(LocalDateTime.now());
        try {
            friendMapper.insert(f);
        } catch (DuplicateKeyException ignore) {
        }
    }

    @Override
    @Transactional
    public void removeFriend(Long userId, Long friendId) {
        // 双向软删除
        UpdateWrapper<UserFriend> w1 = new UpdateWrapper<>();
        w1.eq("user_id", userId).eq("friend_id", friendId).set("deleted", true);
        friendMapper.update(null, w1);

        UpdateWrapper<UserFriend> w2 = new UpdateWrapper<>();
        w2.eq("user_id", friendId).eq("friend_id", userId).set("deleted", true);
        friendMapper.update(null, w2);
    }

    @Override
    public List<UserFriend> listFriends(Long userId) {
        return friendMapper.listByUser(userId);
    }

    @Override
    public boolean isFriend(Long userId, Long friendId) {
        return friendMapper.selectRelation(userId, friendId) != null;
    }

    // ==================== 黑名单 ====================

    @Override
    @Transactional
    public void block(Long userId, Long targetId) {
        BusinessException.check(!userId.equals(targetId), ResultCode.BAD_REQUEST);
        UserBlacklist b = new UserBlacklist();
        b.setUserId(userId);
        b.setTargetId(targetId);
        b.setCreateTime(LocalDateTime.now());
        try {
            blacklistMapper.insert(b);
        } catch (DuplicateKeyException ignore) {
        }
        // 拉黑同时删除好友关系
        removeFriend(userId, targetId);
    }

    @Override
    @Transactional
    public void unblock(Long userId, Long targetId) {
        QueryWrapper<UserBlacklist> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).eq("target_id", targetId);
        blacklistMapper.delete(qw);
    }

    @Override
    public List<UserBlacklist> listBlocked(Long userId) {
        return blacklistMapper.listByUser(userId);
    }

    @Override
    public boolean isBlocked(Long userId, Long targetId) {
        return blacklistMapper.selectByUserAndTarget(userId, targetId) != null;
    }
}