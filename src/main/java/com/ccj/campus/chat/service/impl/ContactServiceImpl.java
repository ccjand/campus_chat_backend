package com.ccj.campus.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ccj.campus.chat.entity.ChatRoom;
import com.ccj.campus.chat.entity.Contact;
import com.ccj.campus.chat.mapper.ChatRoomMapper;
import com.ccj.campus.chat.mapper.ContactMapper;
import com.ccj.campus.chat.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ChatRoomMapper roomMapper;
    private final ContactMapper contactMapper;

    @Override
    @Transactional
    public ChatRoom getOrCreateSingleRoom(Long userId, Long friendId) {
        // 用 ext_info 存两个 uid 的排序 key 来做幂等（小id_大id）
        long small = Math.min(userId, friendId);
        long big   = Math.max(userId, friendId);
        String pairKey = small + "_" + big;

        // 先查是否已存在
        QueryWrapper<ChatRoom> qw = new QueryWrapper<>();
        qw.eq("type", ChatRoom.TYPE_SINGLE)
          .apply("ext_info ->> 'pairKey' = {0}", pairKey);
        ChatRoom existing = roomMapper.selectOne(qw);
        if (existing != null) return existing;

        // 创建房间
        ChatRoom room = new ChatRoom();
        room.setType(ChatRoom.TYPE_SINGLE);
        HashMap<String, Object> ext = new HashMap<>();
        ext.put("pairKey", pairKey);
        ext.put("uid1", small);
        ext.put("uid2", big);
        room.setExtInfo(ext);
        room.setCreateTime(LocalDateTime.now());
        roomMapper.insert(room);

        // 双方各建一条会话
        ensureContact(userId, room.getId());
        ensureContact(friendId, room.getId());

        return room;
    }

    @Override
    @Transactional
    public ChatRoom createGroupRoom(String name) {
        ChatRoom room = new ChatRoom();
        room.setType(ChatRoom.TYPE_GROUP);
        room.setName(name);
        room.setExtInfo(new HashMap<>());
        room.setCreateTime(LocalDateTime.now());
        roomMapper.insert(room);
        return room;
    }

    @Override
    public void ensureContact(Long userId, Long roomId) {
        Contact c = new Contact();
        c.setUserId(userId);
        c.setRoomId(roomId);
        c.setTop(false);
        c.setMute(false);
        c.setActiveTime(LocalDateTime.now());
        try {
            contactMapper.insert(c);
        } catch (DuplicateKeyException ignore) {
            // 已存在
        }
    }

    @Override
    public List<Contact> listContacts(Long userId) {
        QueryWrapper<Contact> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).orderByDesc("top").orderByDesc("active_time");
        return contactMapper.selectList(qw);
    }

    @Override
    public void setTop(Long userId, Long roomId, boolean top) {
        UpdateWrapper<Contact> w = new UpdateWrapper<>();
        w.eq("user_id", userId).eq("room_id", roomId).set("top", top);
        contactMapper.update(null, w);
    }

    @Override
    public void setMute(Long userId, Long roomId, boolean mute) {
        UpdateWrapper<Contact> w = new UpdateWrapper<>();
        w.eq("user_id", userId).eq("room_id", roomId).set("mute", mute);
        contactMapper.update(null, w);
    }
}