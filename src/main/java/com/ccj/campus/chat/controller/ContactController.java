package com.ccj.campus.chat.controller;

import com.ccj.campus.chat.common.R;
import com.ccj.campus.chat.entity.ChatRoom;
import com.ccj.campus.chat.entity.Contact;
import com.ccj.campus.chat.security.LoginUser;
import com.ccj.campus.chat.service.ContactService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 会话管理接口。对齐论文 4.3：会话与消息组。
 */
@RestController
@RequestMapping("/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    /** 获取或创建单聊房间（传入好友 uid） */
    @PostMapping("/room/single")
    public R<ChatRoom> getOrCreateSingle(@RequestBody @Valid FriendRoomReq req) {
        return R.ok(contactService.getOrCreateSingleRoom(LoginUser.currentUid(), req.getFriendId()));
    }

    /** 会话列表 */
    @GetMapping("/list")
    public R<List<Contact>> list() {
        return R.ok(contactService.listContacts(LoginUser.currentUid()));
    }

    /** 进入房间时调用，标记整个房间已读 */
    @PostMapping("/room/read")
    public R<Void> markRoomRead(@RequestBody @Valid RoomIdReq req) {
        contactService.markRoomRead(req.getRoomId(), LoginUser.currentUid());
        return R.ok();
    }

    /** 置顶 / 取消 */
    @PostMapping("/top")
    public R<Void> top(@RequestBody @Valid TopMuteReq req) {
        contactService.setTop(LoginUser.currentUid(), req.getRoomId(), req.getValue());
        return R.ok();
    }

    /** 免打扰 / 取消 */
    @PostMapping("/mute")
    public R<Void> mute(@RequestBody @Valid TopMuteReq req) {
        contactService.setMute(LoginUser.currentUid(), req.getRoomId(), req.getValue());
        return R.ok();
    }

    @Data
    static class FriendRoomReq {
        @NotNull private Long friendId;
    }

    @Data
    static class TopMuteReq {
        @NotNull private Long roomId;
        @NotNull private Boolean value;
    }

    @Data
    static class RoomIdReq {
        @NotNull private Long roomId;
    }
}
