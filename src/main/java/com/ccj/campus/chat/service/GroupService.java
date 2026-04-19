package com.ccj.campus.chat.service;

import com.ccj.campus.chat.entity.ChatGroup;
import com.ccj.campus.chat.entity.ChatGroupMember;

import java.util.List;

/**
 * 群组业务接口。对齐论文 3.2：
 *   - 班级群 + 兴趣群
 *   - 三级角色：群主 / 管理员 / 普通成员
 *   - 群主可转让管理权限、解散群组
 */
public interface GroupService {

    ChatGroup create(Long ownerId, String name, int type, Long classId);

    void addMember(Long operatorId, Long groupId, Long userId);

    void removeMember(Long operatorId, Long groupId, Long userId);

    void setAdmin(Long ownerId, Long groupId, Long userId, boolean isAdmin);

    void transferOwner(Long ownerId, Long groupId, Long newOwnerId);

    void dissolve(Long ownerId, Long groupId);

    void updateAnnouncement(Long operatorId, Long groupId, String announcement);

    List<ChatGroupMember> listMembers(Long groupId);
}