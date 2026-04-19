package com.ccj.campus.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ccj.campus.chat.common.BusinessException;
import com.ccj.campus.chat.common.ResultCode;
import com.ccj.campus.chat.entity.UserBlacklist;
import com.ccj.campus.chat.entity.UserFriend;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final UserFriendMapper friendMapper;
    private final UserBlacklistMapper blacklistMapper;

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