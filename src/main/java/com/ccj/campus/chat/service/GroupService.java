// GroupService.java
package com.ccj.campus.chat.service;

import com.ccj.campus.chat.entity.ChatGroup;
import com.ccj.campus.chat.entity.ChatGroupMember;

import java.util.List;
import java.util.Map;

/**
 * 群组业务接口
 */
public interface GroupService {

    ChatGroup create(Long ownerId, String name, int type, Long classId, List<Long> memberIds);

    Map<String, Object> getCreateCandidates(Long currentUid);

    void addMember(Long operatorId, Long groupId, Long userId);

    void removeMember(Long operatorId, Long groupId, Long userId);

    void setAdmin(Long ownerId, Long groupId, Long userId, boolean isAdmin);

    void transferOwner(Long ownerId, Long groupId, Long newOwnerId);

    void dissolve(Long ownerId, Long groupId);

    void updateAnnouncement(Long operatorId, Long groupId, String announcement);

    List<ChatGroupMember> listMembers(Long groupId);
}