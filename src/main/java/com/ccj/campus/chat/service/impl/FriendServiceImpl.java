package com.ccj.campus.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ccj.campus.chat.common.BusinessException;
import com.ccj.campus.chat.common.ResultCode;
import com.ccj.campus.chat.dto.ChatMessageDTO;
import com.ccj.campus.chat.dto.FriendRequestVO;
import com.ccj.campus.chat.dto.FriendVO;
import com.ccj.campus.chat.entity.*;
import com.ccj.campus.chat.mapper.*;
import com.ccj.campus.chat.mq.MQTopic;
import com.ccj.campus.chat.service.*;
import com.ccj.campus.chat.websocket.OnlineUserService;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final UserFriendMapper friendMapper;
    private final UserBlacklistMapper blacklistMapper;
    private final FriendRequestMapper requestMapper;
    private final SysUserMapper userMapper;
    private final ContactService contactService;
    private final MessageService messageService;
    private final OutboxService outboxService;
    private final OnlineUserService onlineUserService;
    private final BadgeService badgeService;
    private final SysDepartmentMapper sysDepartmentMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private final Cache<String, Boolean> friendCache;
    private final Cache<String, Boolean> blockCache;


    @Override
    public List<FriendVO> listFriends(Long userId) {
        // 这里的 listByUser sql 里已经有 AND deleted = FALSE
        // 所以被你删除的好友天然就不会查出来，更不会展示！
        List<UserFriend> friends = friendMapper.listByUser(userId);

        return friends.stream().map(f -> {
            Long friendId = f.getFriendId();
            // 关联获取用户最新信息
            SysUser u = userMapper.selectById(friendId);

            // 获取部门名称
            String departmentName = "";
            if (u != null && u.getDepartmentId() != null) {
                SysDepartment dept = sysDepartmentMapper.selectById(u.getDepartmentId());
                if (dept != null) {
                    departmentName = dept.getName();
                }
            }

            // 【核心】调用现成的方法，判断该好友是否被当前用户拉黑
            boolean blocked = isBlocked(userId, friendId);

            // 获取或创建房间ID，保证前端能拿到 roomId 进行聊天跳转
            ChatRoom room = contactService.getOrCreateSingleRoom(userId, friendId);

            return FriendVO.builder()
                    .uid(friendId)
                    .roomId(room.getId())
                    .fullName(u != null ? u.getName() : "未知用户")
                    .accountNumber(u != null ? u.getAccountNumber() : "")
                    .avatar(u != null ? u.getAvatar() : "")
                    .role(u != null && u.getRole() != null ? u.getRole() : 1)
                    .remark(f.getRemark())
                    .department(departmentName)
                    .isBlocked(blocked) // 塞入拉黑状态给前端
                    .build();
        }).collect(Collectors.toList());
    }


    @Override
    public void sendRequest(Long fromId, Long toId, String reason) {
        BusinessException.check(!fromId.equals(toId), ResultCode.BAD_REQUEST);
        BusinessException.check(!isFriend(fromId, toId), ResultCode.BAD_REQUEST, "已经是好友");
        BusinessException.check(!isBlocked(fromId, toId), ResultCode.BAD_REQUEST, "请先将对方移出黑名单");
        BusinessException.check(!isBlocked(toId, fromId), ResultCode.BAD_REQUEST, "对方已将你拉黑");

        QueryWrapper<FriendRequest> qw = new QueryWrapper<>();
        qw.eq("from_id", fromId).eq("to_id", toId).eq("status", 0);
        FriendRequest existing = requestMapper.selectOne(qw);
        if (existing != null) {
            existing.setReason(reason);
            existing.setUpdateTime(LocalDateTime.now());
            requestMapper.updateById(existing);
            // 刷新被申请方通讯录红点
            badgeService.pushBadgeIfOnline(toId, null);
            return;
        }

        QueryWrapper<FriendRequest> qw2 = new QueryWrapper<>();
        qw2.eq("from_id", fromId).eq("to_id", toId);
        FriendRequest old = requestMapper.selectOne(qw2);
        if (old != null) {
            old.setStatus(0);
            old.setReason(reason);
            old.setUpdateTime(LocalDateTime.now());
            requestMapper.updateById(old);
            badgeService.pushBadgeIfOnline(toId, null);
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

        // 通知被申请方通讯录 tab 出现红点
        badgeService.pushBadgeIfOnline(toId, null);
    }

    @Override
    @Transactional
    public void acceptRequest(Long currentUid, Long requestId) {
        FriendRequest req = requestMapper.selectById(requestId);
        BusinessException.check(req != null, ResultCode.BAD_REQUEST, "申请不存在");
        BusinessException.check(req.getToId().equals(currentUid), ResultCode.FORBIDDEN, "无权操作");
        BusinessException.check(req.getStatus() == 0, ResultCode.BAD_REQUEST, "该申请已处理");

        req.setStatus(1);
        req.setUpdateTime(LocalDateTime.now());
        requestMapper.updateById(req);

        // 双向写入好友关系
        addFriend(req.getFromId(), req.getToId());

        // ===== 新增：创建房间 + 发送打招呼消息 =====
        ChatRoom room = contactService.getOrCreateSingleRoom(req.getFromId(), req.getToId());

        // 以申请者身份发送"我们成为好友啦"
        ChatMessageDTO greet = new ChatMessageDTO();
        greet.setFromUid(req.getFromId());
        greet.setReceiverId(req.getToId());
        greet.setRoomId(room.getId());
        greet.setType(1); // 文本
        greet.setContent("我们成为好友啦，开始聊天吧！");
        greet.setClientSeq("friend-greet-" + req.getId() + "-" + req.getFromId() + "-" + req.getToId());
        greet.setCreateTime(LocalDateTime.now());

        Long msgId = messageService.prepareMessageId();
        greet.setId(msgId);

        // 同步持久化消息（命中幂等键时 inserted=false）
        boolean inserted = messageService.persist(greet);

        // 没插入成功（DO NOTHING）就不更新会话、不重复推送
        if (!inserted) {
            return;
        }

        // 更新会话最新消息
        messageService.updateLastMsg(room.getId(), msgId);

        // 在线推送到个人队列，不用广播
        if (onlineUserService.isOnline(req.getFromId())) {
            onlineUserService.push(req.getFromId(), "/queue/messages", greet);
        }
        if (onlineUserService.isOnline(req.getToId())) {
            onlineUserService.push(req.getToId(), "/queue/messages", greet);
        }
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

        // 通知申请者：你的好友申请已被拒绝
        pushRequestResult(req, 2);

        badgeService.pushBadgeIfOnline(currentUid, null);
    }

    /**
     * 通知申请者好友申请的处理结果
     */
    private void pushRequestResult(FriendRequest req, int status) {
        SysUser handler = userMapper.selectById(req.getToId());
        Map<String, Object> evt = new HashMap<>();
        evt.put("event", "friend_request_result");
        evt.put("requestId", req.getId());
        evt.put("status", status);  // 1=已同意 2=已拒绝
        evt.put("handlerId", req.getToId());
        evt.put("handlerName", handler != null ? handler.getName() : "未知用户");
        onlineUserService.push(req.getFromId(), "/queue/messages", evt);
    }

    @Override
    public List<FriendRequestVO> listReceivedRequests(Long userId) {
        QueryWrapper<FriendRequest> qw = new QueryWrapper<>();
        qw.eq("to_id", userId).orderByDesc("create_time");
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

    private FriendRequestVO buildVO(FriendRequest r, Long targetUid) {
        SysUser u = userMapper.selectById(targetUid);
        return FriendRequestVO.builder()
                .requestId(r.getId())
                .fromId(r.getFromId())
                .toId(r.getToId())
                .targetId(targetUid)
                .targetName(u != null ? u.getName() : "未知用户")
                .targetAvatar(u != null ? u.getAvatar() : null)
                .targetAccountNumber(u != null ? u.getAccountNumber() : null)
                .targetRole(u != null ? u.getRole() : null)
                .reason(r.getReason())
                .status(r.getStatus())
                .createTime(r.getCreateTime())
                .build();
    }

    // ==================== 好友关系 ====================

    @Override
    @Transactional
    public void addFriend(Long userId, Long friendId) {
        BusinessException.check(!userId.equals(friendId), ResultCode.BAD_REQUEST);
        BusinessException.check(!isBlocked(friendId, userId), ResultCode.BAD_REQUEST);
        insertFriend(userId, friendId);
        insertFriend(friendId, userId);

        // 一致性：DB 写完后，立刻清 L1 + L2
        evictFriendCache(userId, friendId);
    }

    private void evictFriendCache(Long a, Long b) {
        String key = Math.min(a, b) + ":" + Math.max(a, b);
        friendCache.invalidate(key);
        stringRedisTemplate.delete("cache:friend:" + key);
    }

    private void evictBlockCache(Long userId, Long targetId) {
        String key = userId + ":" + targetId;
        blockCache.invalidate(key);
        stringRedisTemplate.delete("cache:block:" + key);
        // 反向也清一下，因为 handleSend 会查双向
        String reverseKey = targetId + ":" + userId;
        blockCache.invalidate(reverseKey);
        stringRedisTemplate.delete("cache:block:" + reverseKey);
    }

    private void insertFriend(Long userId, Long friendId) {
        friendMapper.upsertRelation(userId, friendId);
    }

    @Override
    @Transactional
    public void removeFriend(Long userId, Long friendId) {
        UpdateWrapper<UserFriend> w1 = new UpdateWrapper<>();
        w1.eq("user_id", userId).eq("friend_id", friendId).set("deleted", true);
        friendMapper.update(null, w1);

        UpdateWrapper<UserFriend> w2 = new UpdateWrapper<>();
        w2.eq("user_id", friendId).eq("friend_id", userId).set("deleted", true);
        friendMapper.update(null, w2);

        evictFriendCache(userId, friendId);
    }

    @Override
    public boolean isFriend(Long userId, Long friendId) {
        String key = Math.min(userId, friendId) + ":" + Math.max(userId, friendId);

        // L1: Caffeine（纳秒级）
        Boolean l1 = friendCache.getIfPresent(key);
        if (l1 != null) return l1;

        // L2: Redis（毫秒级）
        String redisKey = "cache:friend:" + key;
        String l2 = stringRedisTemplate.opsForValue().get(redisKey);
        if (l2 != null) {
            boolean result = "1".equals(l2);
            friendCache.put(key, result);  // 回填 L1
            return result;
        }

        // L3: DB
        boolean result = friendMapper.selectRelation(userId, friendId) != null;
        friendCache.put(key, result);                                               // 写 L1
        stringRedisTemplate.opsForValue().set(redisKey, result ? "1" : "0",
                java.time.Duration.ofMinutes(10));                                  // 写 L2
        return result;
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

        evictBlockCache(userId, targetId);
        // 拉黑后好友关系也可能变化，顺便清掉
        evictFriendCache(userId, targetId);
    }

    @Override
    @Transactional
    public void unblock(Long userId, Long targetId) {
        QueryWrapper<UserBlacklist> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).eq("target_id", targetId);
        blacklistMapper.delete(qw);

        evictBlockCache(userId, targetId);
    }

    @Override
    public List<UserBlacklist> listBlocked(Long userId) {
        return blacklistMapper.listByUser(userId);
    }

    @Override
    public boolean isBlocked(Long userId, Long targetId) {
        String key = userId + ":" + targetId;  // 有方向，不归一化

        // L1
        Boolean l1 = blockCache.getIfPresent(key);
        if (l1 != null) return l1;

        // L2
        String redisKey = "cache:block:" + key;
        String l2 = stringRedisTemplate.opsForValue().get(redisKey);
        if (l2 != null) {
            boolean result = "1".equals(l2);
            blockCache.put(key, result);
            return result;
        }

        // L3: DB
        boolean result = blacklistMapper.selectByUserAndTarget(userId, targetId) != null;
        blockCache.put(key, result);
        stringRedisTemplate.opsForValue().set(redisKey, result ? "1" : "0",
                java.time.Duration.ofMinutes(10));
        return result;
    }
}