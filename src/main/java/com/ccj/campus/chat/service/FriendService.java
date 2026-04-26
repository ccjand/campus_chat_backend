package com.ccj.campus.chat.service;

import com.ccj.campus.chat.dto.FriendRequestVO;
import com.ccj.campus.chat.dto.FriendVO;
import com.ccj.campus.chat.entity.UserBlacklist;
import com.ccj.campus.chat.entity.UserFriend;

import java.util.List;

/**
 * 好友与黑名单业务。对齐论文 3.2：
 * "用户可在系统内搜索、添加或删除好友，
 * 支持将指定用户加入黑名单以屏蔽其消息。"
 */
public interface FriendService {
    // ==================== 好友申请 ====================

    /**
     * 发送好友申请
     */
    void sendRequest(Long fromId, Long toId, String reason);

    /**
     * 同意好友申请
     */
    void acceptRequest(Long currentUid, Long requestId);

    /**
     * 拒绝好友申请
     */
    void rejectRequest(Long currentUid, Long requestId);

    /**
     * 我收到的好友申请列表（待处理）
     */
    List<FriendRequestVO> listReceivedRequests(Long userId);

    /**
     * 我发出的好友申请列表
     */
    List<FriendRequestVO> listSentRequests(Long userId);

    /**
     * 添加好友（双向写入）
     */
    void addFriend(Long userId, Long friendId);

    /**
     * 删除好友（双向软删除）
     */
    void removeFriend(Long userId, Long friendId);

    /**
     * 判断是否为好友
     */
    boolean isFriend(Long userId, Long friendId);

    /**
     * 好友列表
     */
    List<FriendVO> listFriends(Long userId);

    /**
     * 加入黑名单
     */
    void block(Long userId, Long targetId);

    /**
     * 移出黑名单
     */
    void unblock(Long userId, Long targetId);

    /**
     * 黑名单列表
     */
    List<UserBlacklist> listBlocked(Long userId);

    /**
     * 判断是否被对方拉黑
     */
    boolean isBlocked(Long userId, Long targetId);
}