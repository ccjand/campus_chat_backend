package com.ccj.campus.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ccj.campus.chat.dto.ContactVO;
import com.ccj.campus.chat.entity.ChatRoom;
import com.ccj.campus.chat.entity.Contact;
import com.ccj.campus.chat.entity.Message;
import com.ccj.campus.chat.mapper.ChatRoomMapper;
import com.ccj.campus.chat.mapper.ContactMapper;
import com.ccj.campus.chat.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        long big = Math.max(userId, friendId);
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

    /**
     * 会话列表。改造后一次 SQL 拼齐所有字段，前端无需再 N+1：
     * - 对方昵称 / 头像 由后端按 type 自动判别
     * - summary 由后端根据消息 type / status 翻译为文字占位符
     * - unread_count 在数据库层面算好
     * - 按 top DESC, 最新时间 DESC 排序
     */
    @Override
    public List<ContactVO> listContacts(Long userId) {
        List<Map<String, Object>> rows = contactMapper.listWithUnread(userId);
        return rows.stream().map(this::toVO).collect(Collectors.toList());
    }

    private ContactVO toVO(Map<String, Object> row) {
        Integer lastType = toInt(row.get("last_type"));
        Integer lastStatus = toInt(row.get("last_status"));
        String lastContent = (String) row.get("last_content");
        String summary = buildSummary(lastType, lastStatus, lastContent);

        LocalDateTime lastTime = toLocalDateTime(row.get("last_time"));
        LocalDateTime activeTime = toLocalDateTime(row.get("active_time"));
        LocalDateTime timestamp = lastTime != null ? lastTime : activeTime;

        return ContactVO.builder()
                .id(toLong(row.get("id")))
                .roomId(toLong(row.get("room_id")))
                .type(toInt(row.get("type")))
                .name((String) row.get("name"))
                .avatar((String) row.get("avatar"))
                .summary(summary)
                .unreadCount(toInt(row.get("unread_count")))
                .timestamp(timestamp)
                .top(Boolean.TRUE.equals(row.get("top")))
                .mute(Boolean.TRUE.equals(row.get("mute")))
                .build();
    }

    /**
     * 把消息的 type / status / content 翻译成前端可直接展示的摘要文本。
     * 对齐 Message 实体里的 12 种 type 枚举与 STATUS_RECALLED 状态。
     */
    private String buildSummary(Integer type, Integer status, String content) {
        if (type == null) {
            // 该会话从未有过消息
            return "";
        }
        // 撤回态（状态位优先于类型位）
        if (status != null && status == Message.STATUS_RECALLED) {
            return "消息已撤回";
        }
        if (type == Message.TYPE_RECALL) {
            return "消息已撤回";
        }
        switch (type) {
            case Message.TYPE_TEXT:
                return content == null ? "" : content;
            case Message.TYPE_IMAGE:
                return "[图片]";
            case Message.TYPE_FILE:
                return "[文件]";
            case Message.TYPE_VOICE:
                return "[语音]";
            case Message.TYPE_EMOJI:
                return "[表情]";
            case Message.TYPE_LEAVE_CARD:
                return "[请假卡片]";
            case Message.TYPE_CHECKIN_CARD:
                return "[签到卡片]";
            case Message.TYPE_NOTICE_CARD:
                return "[通知卡片]";
            default:
                return content == null ? "" : content;
        }
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Long) return (Long) o;
        if (o instanceof Number) return ((Number) o).longValue();
        return Long.valueOf(o.toString());
    }

    private static Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Integer) return (Integer) o;
        if (o instanceof Number) return ((Number) o).intValue();
        return Integer.valueOf(o.toString());
    }

    /**
     * PostgreSQL 的 timestamp 列经 JDBC 回到 Map 里是 java.sql.Timestamp；
     * 如果驱动/配置不同也可能是 LocalDateTime / OffsetDateTime。这里做统一兜底转换。
     */
    private static LocalDateTime toLocalDateTime(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDateTime) return (LocalDateTime) o;
        if (o instanceof Timestamp) return ((Timestamp) o).toLocalDateTime();
        if (o instanceof OffsetDateTime) return ((OffsetDateTime) o).toLocalDateTime();
        if (o instanceof java.util.Date) {
            return LocalDateTime.ofInstant(((java.util.Date) o).toInstant(), ZoneId.systemDefault());
        }
        return null;
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

    @Override
    public void markRoomRead(Long roomId, Long userId) {
        contactMapper.markRoomRead(roomId, userId);
    }
}