package com.ccj.campus.chat.service.impl;

import com.ccj.campus.chat.common.BusinessException;
import com.ccj.campus.chat.common.ResultCode;
import com.ccj.campus.chat.entity.ChatGroup;
import com.ccj.campus.chat.entity.ChatGroupMember;
import com.ccj.campus.chat.entity.ChatRoom;
import com.ccj.campus.chat.mapper.ChatGroupMapper;
import com.ccj.campus.chat.mapper.ChatGroupMemberMapper;
import com.ccj.campus.chat.service.ContactService;
import com.ccj.campus.chat.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final ChatGroupMapper groupMapper;
    private final ChatGroupMemberMapper memberMapper;
    private final ContactService contactService;

    @Override
    @Transactional
    public ChatGroup create(Long ownerId, String name, int type, Long classId) {
        // 先创建 chat_room
        ChatRoom room = contactService.createGroupRoom(name);

        ChatGroup g = new ChatGroup();
        g.setRoomId(room.getId());
        g.setName(name);
        g.setOwnerId(ownerId);
        g.setType(type);
        g.setClassId(classId);
        g.setDeleted(false);
        g.setCreateTime(LocalDateTime.now());
        groupMapper.insert(g);

        // 群主自动成为成员
        ChatGroupMember owner = new ChatGroupMember();
        owner.setGroupId(g.getId());
        owner.setUserId(ownerId);
        owner.setRole(ChatGroupMember.ROLE_OWNER);
        owner.setJoinTime(LocalDateTime.now());
        memberMapper.insert(owner);

        // 群主自动创建会话
        contactService.ensureContact(ownerId, room.getId());

        return g;
    }

    @Override
    @Transactional
    public void addMember(Long operatorId, Long groupId, Long userId) {
        checkAdmin(operatorId, groupId);
        ChatGroupMember m = new ChatGroupMember();
        m.setGroupId(groupId);
        m.setUserId(userId);
        m.setRole(ChatGroupMember.ROLE_MEMBER);
        m.setJoinTime(LocalDateTime.now());
        memberMapper.insert(m);

        // 新成员自动创建会话
        ChatGroup g = groupMapper.selectById(groupId);
        if (g != null) {
            contactService.ensureContact(userId, g.getRoomId());
        }
    }

    @Override
    @Transactional
    public void removeMember(Long operatorId, Long groupId, Long userId) {
        checkAdmin(operatorId, groupId);
        // 不能踢群主
        ChatGroupMember target = memberMapper.selectByGroupAndUser(groupId, userId);
        BusinessException.check(target != null, ResultCode.GROUP_NOT_MEMBER);
        BusinessException.check(target.getRole() != ChatGroupMember.ROLE_OWNER, ResultCode.GROUP_NOT_OWNER);
        memberMapper.deleteById(target.getId());
    }

    @Override
    @Transactional
    public void setAdmin(Long ownerId, Long groupId, Long userId, boolean isAdmin) {
        // 对齐论文 3.2："群主可将管理权限委托给指定成员"
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

        // 原群主降级为普通成员
        ChatGroupMember old = memberMapper.selectByGroupAndUser(groupId, ownerId);
        old.setRole(ChatGroupMember.ROLE_MEMBER);
        memberMapper.updateById(old);

        // 新群主升级
        newOwner.setRole(ChatGroupMember.ROLE_OWNER);
        memberMapper.updateById(newOwner);

        // 更新 chat_group 的 owner_id
        ChatGroup g = groupMapper.selectById(groupId);
        g.setOwnerId(newOwnerId);
        groupMapper.updateById(g);
    }

    @Override
    @Transactional
    public void dissolve(Long ownerId, Long groupId) {
        // 对齐论文 3.2："群主也可解散群组"
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
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatGroupMember>()
                        .eq("group_id", groupId));
    }

    // ==================== 权限校验 ====================

    private void checkOwner(Long userId, Long groupId) {
        ChatGroupMember m = memberMapper.selectByGroupAndUser(groupId, userId);
        if (m == null || m.getRole() != ChatGroupMember.ROLE_OWNER) {
            throw new BusinessException(ResultCode.GROUP_NOT_OWNER);
        }
    }

    /** 对齐论文 3.2："不同角色在群成员管理、群公告发布等操作上的权限有所差异" */
    private void checkAdmin(Long userId, Long groupId) {
        ChatGroupMember m = memberMapper.selectByGroupAndUser(groupId, userId);
        if (m == null) throw new BusinessException(ResultCode.GROUP_NOT_MEMBER);
        if (m.getRole() != ChatGroupMember.ROLE_OWNER && m.getRole() != ChatGroupMember.ROLE_ADMIN) {
            throw new BusinessException(ResultCode.GROUP_NOT_ADMIN);
        }
    }
}