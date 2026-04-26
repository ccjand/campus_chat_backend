package com.ccj.campus.chat.controller;

import com.ccj.campus.chat.common.R;
import com.ccj.campus.chat.dto.FriendRequestVO;
import com.ccj.campus.chat.dto.FriendVO;
import com.ccj.campus.chat.entity.UserBlacklist;
import com.ccj.campus.chat.entity.UserFriend;
import com.ccj.campus.chat.security.LoginUser;
import com.ccj.campus.chat.service.FriendService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 好友与黑名单接口。对齐论文 3.2 / 4.3 用户组。
 */
@RestController
@RequestMapping("/friend")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    /**
     * 发送好友申请
     */
    @PostMapping("/request/send")
    public R<Void> sendRequest(@RequestBody @Valid SendRequestReq req) {
        friendService.sendRequest(LoginUser.currentUid(), req.getTargetId(), req.getReason());
        return R.ok();
    }

    /**
     * 同意好友申请
     */
    @PostMapping("/request/accept")
    public R<Void> acceptRequest(@RequestBody @Valid RequestIdReq req) {
        friendService.acceptRequest(LoginUser.currentUid(), req.getRequestId());
        return R.ok();
    }

    /**
     * 拒绝好友申请
     */
    @PostMapping("/request/reject")
    public R<Void> rejectRequest(@RequestBody @Valid RequestIdReq req) {
        friendService.rejectRequest(LoginUser.currentUid(), req.getRequestId());
        return R.ok();
    }

    /**
     * 收到的好友申请列表（待处理）
     */
    @GetMapping("/request/received")
    public R<List<FriendRequestVO>> receivedRequests() {
        return R.ok(friendService.listReceivedRequests(LoginUser.currentUid()));
    }

    /**
     * 我发出的好友申请列表
     */
    @GetMapping("/request/sent")
    public R<List<FriendRequestVO>> sentRequests() {
        return R.ok(friendService.listSentRequests(LoginUser.currentUid()));
    }

    @Data
    static class SendRequestReq {
        @NotNull
        private Long targetId;
        private String reason;  // 可选的申请理由
    }

    @Data
    static class RequestIdReq {
        @NotNull
        private Long requestId;
    }

    /**
     * 添加好友
     */
    @PostMapping("/add")
    public R<Void> add(@RequestBody @Valid TargetReq req) {
        friendService.addFriend(LoginUser.currentUid(), req.getTargetId());
        return R.ok();
    }

    /**
     * 删除好友
     */
    @PostMapping("/remove")
    public R<Void> remove(@RequestBody @Valid TargetReq req) {
        friendService.removeFriend(LoginUser.currentUid(), req.getTargetId());
        return R.ok();
    }

    /**
     * 好友列表
     */
    @GetMapping("/list")
    public R<List<FriendVO>> list() {
        return R.ok(friendService.listFriends(LoginUser.currentUid()));
    }

    /**
     * 拉黑
     */
    @PostMapping("/block")
    public R<Void> block(@RequestBody @Valid TargetReq req) {
        friendService.block(LoginUser.currentUid(), req.getTargetId());
        return R.ok();
    }

    /**
     * 取消拉黑
     */
    @PostMapping("/unblock")
    public R<Void> unblock(@RequestBody @Valid TargetReq req) {
        friendService.unblock(LoginUser.currentUid(), req.getTargetId());
        return R.ok();
    }

    /**
     * 黑名单列表
     */
    @GetMapping("/blacklist")
    public R<List<UserBlacklist>> blacklist() {
        return R.ok(friendService.listBlocked(LoginUser.currentUid()));
    }

    @Data
    static class TargetReq {
        @NotNull
        private Long targetId;
    }
}