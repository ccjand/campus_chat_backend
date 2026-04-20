package com.ccj.campus.chat.service;

import com.ccj.campus.chat.entity.ChatRoom;
import com.ccj.campus.chat.entity.Contact;

import java.util.List;

/**
 * 会话与房间管理。对齐论文 3.2 / 4.3：
 *   - 创建单聊/群聊房间
 *   - 管理会话列表（置顶、免打扰）
 */
public interface ContactService {

    /** 创建单聊房间（若已存在则直接返回） */
    ChatRoom getOrCreateSingleRoom(Long userId, Long friendId);

    /** 创建群聊房间 */
    ChatRoom createGroupRoom(String name);

    /** 为用户创建一条会话记录 */
    void ensureContact(Long userId, Long roomId);

    /** 获取用户的会话列表 */
    List<Contact> listContacts(Long userId);

    /** 置顶/取消 */
    void setTop(Long userId, Long roomId, boolean top);

    /** 免打扰/取消 */
    void setMute(Long userId, Long roomId, boolean mute);

    /** 进入房间时标记整个房间已读 */
    void markRoomRead(Long roomId, Long userId);
}