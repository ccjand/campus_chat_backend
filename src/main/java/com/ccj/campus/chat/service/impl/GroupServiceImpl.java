package com.ccj.campus.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ccj.campus.chat.common.BusinessException;
import com.ccj.campus.chat.common.ResultCode;
import com.ccj.campus.chat.dto.FriendVO;
import com.ccj.campus.chat.entity.*;
import com.ccj.campus.chat.mapper.*;
import com.ccj.campus.chat.service.ContactService;
import com.ccj.campus.chat.service.FriendService;
import com.ccj.campus.chat.service.GroupService;
import com.ccj.campus.chat.websocket.OnlineUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final ChatGroupMapper groupMapper;
    private final ChatGroupMemberMapper memberMapper;
    private final ContactService contactService;

    private final SysUserMapper userMapper;
    private final FriendService friendService;
    private final SysClassMapper sysClassMapper;
    private final SysUserClassRelMapper sysUserClassRelMapper;
    private final CourseMapper courseMapper;
    private final CourseClassRelMapper courseClassRelMapper;
    private final OnlineUserService onlineUserService;

    private enum CreateScope {
        FRIEND,             // 学生
        CLASS_SELF_MANAGED, // 教师/辅导员/院长（非管理员）
        CLASS_ALL           // 管理员
    }

    private CreateScope resolveCreateScope(Integer role) {
        if (role == null) throw new BusinessException(ResultCode.FORBIDDEN);
        if (role.equals(SysUser.ROLE_STUDENT)) return CreateScope.FRIEND;
        if (role.equals(SysUser.ROLE_ADMIN)) return CreateScope.CLASS_ALL;
        return CreateScope.CLASS_SELF_MANAGED;
    }

    @Override
    @Transactional
    public ChatGroup create(Long ownerId, String name, int type, Long classId, List<Long> memberIds) {
        SysUser ownerUser = userMapper.selectById(ownerId);
        BusinessException.check(ownerUser != null, ResultCode.USER_NOT_FOUND);

        CreateScope scope = resolveCreateScope(ownerUser.getRole());

        Set<Long> allowedMemberIds = new HashSet<>();
        Long normalizedClassId = null;
        int normalizedType;

        if (scope == CreateScope.FRIEND) {
            normalizedType = ChatGroup.TYPE_INTEREST;
            List<FriendVO> friends = friendService.listFriends(ownerId);
            allowedMemberIds.addAll(friends.stream().map(FriendVO::getUid).filter(Objects::nonNull).collect(Collectors.toSet()));
        } else {
            normalizedType = ChatGroup.TYPE_CLASS;
            BusinessException.check(classId != null, ResultCode.BAD_REQUEST);

            Set<Long> allowedClassIds = scope == CreateScope.CLASS_ALL
                    ? new HashSet<>(listAllClassIds())
                    : new HashSet<>(listManagedClassIds(ownerId));

            BusinessException.check(allowedClassIds.contains(classId), ResultCode.FORBIDDEN);
            normalizedClassId = classId;

            List<Long> classUsers = Optional.ofNullable(sysUserClassRelMapper.listUserIdsByClass(classId))
                    .orElse(Collections.emptyList());
            allowedMemberIds.addAll(classUsers.stream().filter(Objects::nonNull).collect(Collectors.toSet()));
            allowedMemberIds.remove(ownerId);
        }

        Set<Long> selectedMemberIds = new LinkedHashSet<>();
        if (memberIds != null) {
            for (Long uid : memberIds) {
                if (uid == null) continue;
                if (uid.equals(ownerId)) continue;
                BusinessException.check(allowedMemberIds.contains(uid), ResultCode.FORBIDDEN);
                selectedMemberIds.add(uid);
            }
        }

        ChatRoom room = contactService.createGroupRoom(name);

        ChatGroup g = new ChatGroup();
        g.setRoomId(room.getId());
        g.setName(name);
        g.setOwnerId(ownerId);
        g.setType(normalizedType);
        g.setClassId(normalizedClassId);
        g.setDeleted(false);
        g.setCreateTime(LocalDateTime.now());
        groupMapper.insert(g);

        ChatGroupMember owner = new ChatGroupMember();
        owner.setGroupId(g.getId());
        owner.setUserId(ownerId);
        owner.setRole(ChatGroupMember.ROLE_OWNER);
        owner.setJoinTime(LocalDateTime.now());
        memberMapper.insert(owner);

        contactService.ensureContact(ownerId, room.getId());

        for (Long uid : selectedMemberIds) {
            ChatGroupMember m = new ChatGroupMember();
            m.setGroupId(g.getId());
            m.setUserId(uid);
            m.setRole(ChatGroupMember.ROLE_MEMBER);
            m.setJoinTime(LocalDateTime.now());
            memberMapper.insert(m);

            // 离线可见：持久化会话联系人
            contactService.ensureContact(uid, room.getId());

            // 在线实时：通知客户端立即刷新会话列表
            pushRecentRefreshEvent(uid, room.getId(), g.getName(), ownerId);
        }

        return g;
    }

    @Override
    public Map<String, Object> getCreateCandidates(Long currentUid) {
        SysUser me = userMapper.selectById(currentUid);
        BusinessException.check(me != null, ResultCode.USER_NOT_FOUND);

        CreateScope scope = resolveCreateScope(me.getRole());
        Map<String, Object> res = new HashMap<>();

        if (scope == CreateScope.FRIEND) {
            List<Map<String, Object>> friends = friendService.listFriends(currentUid).stream()
                    .map(this::toFriendCandidate)
                    .collect(Collectors.toList());
            res.put("mode", "friend");
            res.put("friends", friends);
            res.put("classes", Collections.emptyList());
            return res;
        }

        List<Long> classIds = scope == CreateScope.CLASS_ALL ? listAllClassIds() : listManagedClassIds(currentUid);
        List<Map<String, Object>> classes = buildClassCandidates(classIds, currentUid);

        res.put("mode", "class");
        res.put("friends", Collections.emptyList());
        res.put("classes", classes);
        return res;
    }

    private Map<String, Object> toFriendCandidate(FriendVO f) {
        Map<String, Object> m = new HashMap<>();
        m.put("uid", f.getUid());
        m.put("name", f.getFullName());
        m.put("avatar", f.getAvatar());
        m.put("accountNumber", f.getAccountNumber());
        return m;
    }

    private List<Long> listAllClassIds() {
        return sysClassMapper.selectList(new QueryWrapper<SysClass>().orderByDesc("create_time"))
                .stream()
                .map(SysClass::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<Long> listManagedClassIds(Long uid) {
        Set<Long> ids = new LinkedHashSet<>();

        // 1) 教师关联课程 -> 课程关联班级
        List<Course> courses = Optional.ofNullable(courseMapper.listByTeacher(uid)).orElse(Collections.emptyList());
        for (Course c : courses) {
            if (c == null || c.getId() == null) continue;
            ids.addAll(Optional.ofNullable(courseClassRelMapper.listClassIdsByCourse(c.getId()))
                    .orElse(Collections.emptyList()));
        }

        // 2) 用户直接绑定班级（辅导员/院长可用）
        ids.addAll(Optional.ofNullable(sysUserClassRelMapper.listClassIdsByUser(uid))
                .orElse(Collections.emptyList()));

        ids.remove(null);
        return new ArrayList<>(ids);
    }

    private List<Map<String, Object>> buildClassCandidates(List<Long> classIds, Long currentUid) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (classIds == null || classIds.isEmpty()) return out;

        Set<Long> dedupClassIds = new LinkedHashSet<>(classIds);
        for (Long classId : dedupClassIds) {
            if (classId == null) continue;

            SysClass cls = sysClassMapper.selectById(classId);
            if (cls == null) continue;

            List<Long> userIds = Optional.ofNullable(sysUserClassRelMapper.listUserIdsByClass(classId))
                    .orElse(Collections.emptyList());

            List<Map<String, Object>> users = new ArrayList<>();
            Set<Long> dedupUsers = new LinkedHashSet<>(userIds);
            for (Long uid : dedupUsers) {
                if (uid == null || uid.equals(currentUid)) continue;
                SysUser u = userMapper.selectById(uid);
                if (u == null || Boolean.FALSE.equals(u.getEnabled())) continue;

                Map<String, Object> item = new HashMap<>();
                item.put("uid", u.getId());
                item.put("name", u.getName());
                item.put("avatar", u.getAvatar());
                item.put("accountNumber", u.getAccountNumber());
                users.add(item);
            }

            Map<String, Object> classMap = new HashMap<>();
            classMap.put("classId", cls.getId());
            classMap.put("className", cls.getName());
            classMap.put("users", users);
            out.add(classMap);
        }

        return out;
    }

    @Override
    @Transactional
    public void addMember(Long operatorId, Long groupId, Long userId) {
        checkAdmin(operatorId, groupId);
        ChatGroupMember m = memberMapper.selectByGroupAndUser(groupId, userId);
        if (m != null) return;

        ChatGroupMember nm = new ChatGroupMember();
        nm.setGroupId(groupId);
        nm.setUserId(userId);
        nm.setRole(ChatGroupMember.ROLE_MEMBER);
        nm.setJoinTime(LocalDateTime.now());
        memberMapper.insert(nm);

        ChatGroup g2 = groupMapper.selectById(groupId);
        if (g2 != null) {
            contactService.ensureContact(userId, g2.getRoomId());
            pushRecentRefreshEvent(userId, g2.getRoomId(), g2.getName(), operatorId);
        }
    }

    private void pushRecentRefreshEvent(Long targetUid, Long roomId, String groupName, Long operatorId) {
        if (targetUid == null || roomId == null) return;
        if (!onlineUserService.isOnline(targetUid)) return;

        Map<String, Object> data = new HashMap<>();
        data.put("roomId", roomId);
        data.put("content", "你已加入群聊：" + (groupName == null ? "" : groupName));
        data.put("fromUid", operatorId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", 4); // 前端 index 的 handleWsPayload 会识别 type=4
        payload.put("data", data);

        onlineUserService.push(targetUid, "/queue/messages", payload);
    }

    @Override
    @Transactional
    public void removeMember(Long operatorId, Long groupId, Long userId) {
        checkAdmin(operatorId, groupId);
        ChatGroupMember target = memberMapper.selectByGroupAndUser(groupId, userId);
        BusinessException.check(target != null, ResultCode.GROUP_NOT_MEMBER);
        BusinessException.check(target.getRole() != ChatGroupMember.ROLE_OWNER, ResultCode.GROUP_NOT_OWNER);
        memberMapper.deleteById(target.getId());
    }

    @Override
    @Transactional
    public void setAdmin(Long ownerId, Long groupId, Long userId, boolean isAdmin) {
        checkOwner(ownerId, groupId);
        ChatGroupMember m = memberMapper.selectByGroupAndUser(groupId, userId);
        BusinessException.check(m != null, ResultCode.GROUP_NOT_MEMBER);
        m.setRole(isAdmin ? ChatGroupMember.ROLE_ADMIN : ChatGroupMember.ROLE_MEMBER);
        memberMapper.updateById(m);
    }

    @Override
    @Transactional
    public void transferOwner(Long ownerId, Long groupId, Long newOwnerId) {
        checkOwner(ownerId, groupId);
        ChatGroupMember newOwner = memberMapper.selectByGroupAndUser(groupId, newOwnerId);
        BusinessException.check(newOwner != null, ResultCode.GROUP_NOT_MEMBER);

        ChatGroupMember old = memberMapper.selectByGroupAndUser(groupId, ownerId);
        old.setRole(ChatGroupMember.ROLE_MEMBER);
        memberMapper.updateById(old);

        newOwner.setRole(ChatGroupMember.ROLE_OWNER);
        memberMapper.updateById(newOwner);

        ChatGroup g = groupMapper.selectById(groupId);
        g.setOwnerId(newOwnerId);
        groupMapper.updateById(g);
    }

    @Override
    @Transactional
    public void dissolve(Long ownerId, Long groupId) {
        checkOwner(ownerId, groupId);
        ChatGroup g = groupMapper.selectById(groupId);
        g.setDeleted(true);
        groupMapper.updateById(g);
    }

    @Override
    @Transactional
    public void updateAnnouncement(Long operatorId, Long groupId, String announcement) {
        checkAdmin(operatorId, groupId);
        ChatGroup g = groupMapper.selectById(groupId);
        g.setAnnouncement(announcement);
        groupMapper.updateById(g);
    }

    @Override
    public List<ChatGroupMember> listMembers(Long groupId) {
        return memberMapper.selectList(
                new QueryWrapper<ChatGroupMember>().eq("group_id", groupId)
        );
    }

    private void checkOwner(Long userId, Long groupId) {
        ChatGroupMember m = memberMapper.selectByGroupAndUser(groupId, userId);
        if (m == null || m.getRole() != ChatGroupMember.ROLE_OWNER) {
            throw new BusinessException(ResultCode.GROUP_NOT_OWNER);
        }
    }

    private void checkAdmin(Long userId, Long groupId) {
        ChatGroupMember m = memberMapper.selectByGroupAndUser(groupId, userId);
        if (m == null) throw new BusinessException(ResultCode.GROUP_NOT_MEMBER);
        if (m.getRole() != ChatGroupMember.ROLE_OWNER && m.getRole() != ChatGroupMember.ROLE_ADMIN) {
            throw new BusinessException(ResultCode.GROUP_NOT_ADMIN);
        }
    }
}