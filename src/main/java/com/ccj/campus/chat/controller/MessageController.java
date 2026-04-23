package com.ccj.campus.chat.controller;

import com.ccj.campus.chat.common.R;
import com.ccj.campus.chat.dto.ChatMessageDTO;
import com.ccj.campus.chat.entity.Message;
import com.ccj.campus.chat.security.LoginUser;
import com.ccj.campus.chat.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息模块 HTTP 接口。
 * WebSocket 侧消息收发在 ChatMessageController，这里负责历史查询、离线拉取。
 */
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /** 进入房间时调用，标记整个房间已读 */
    @PostMapping("/read/room")
    public R<Void> markRoomRead(@RequestParam Long roomId) {
        messageService.markRoomRead(roomId, LoginUser.currentUid());
        return R.ok();
    }

    /** GET /message/history?roomId=&cursor=&size= */
    @GetMapping("/history")
    public R<List<Message>> history(@RequestParam Long roomId,
                                    @RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursor,
                                    @RequestParam(defaultValue = "30") int size) {
        return R.ok(messageService.pullHistory(roomId, cursor, size));
    }

    /** GET /message/offline */
    @GetMapping("/offline")
    public R<List<ChatMessageDTO>> offline(@RequestParam(defaultValue = "200") int max) {
        return R.ok(messageService.pullOffline(LoginUser.currentUid(), max));
    }

    /**
     * GET /message/since?roomId=&sinceId=&limit=
     * 增量拉取：拉取 id 严格大于 sinceId 的可见消息。
     * 前端用法：断线重连或从后台恢复时，以本地最后一条真实消息的 id 作为 sinceId，
     * 调用本接口补齐掉线期间漏收的消息。
     */
    @GetMapping("/since")
    public R<List<Message>> since(@RequestParam Long roomId,
                                  @RequestParam Long sinceId,
                                  @RequestParam(defaultValue = "200") int limit) {
        return R.ok(messageService.listSince(roomId, sinceId, limit));
    }
}