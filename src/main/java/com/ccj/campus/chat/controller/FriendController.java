package com.ccj.campus.chat.controller;

import com.ccj.campus.chat.common.R;
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

    /** 添加好友 */
    @PostMapping("/add")
    public R<Void> add(@RequestBody @Valid TargetReq req) {
        friendService.addFriend(LoginUser.currentUid(), req.getTargetId());
        return R.ok();
    }

    /** 删除好友 */
    @PostMapping("/remove")
    public R<Void> remove(@RequestBody @Valid TargetReq req) {
        friendService.removeFriend(LoginUser.currentUid(), req.getTargetId());
        return R.ok();
    }

    /** 好友列表 */
    @GetMapping("/list")
    public R<List<UserFriend>> list() {
        return R.ok(friendService.listFriends(LoginUser.currentUid()));
    }

    /** 拉黑 */
    @PostMapping("/block")
    public R<Void> block(@RequestBody @Valid TargetReq req) {
        friendService.block(LoginUser.currentUid(), req.getTargetId());
        return R.ok();
    }

    /** 取消拉黑 */
    @PostMapping("/unblock")
    public R<Void> unblock(@RequestBody @Valid TargetReq req) {
        friendService.unblock(LoginUser.currentUid(), req.getTargetId());
        return R.ok();
    }

    /** 黑名单列表 */
    @GetMapping("/blacklist")
    public R<List<UserBlacklist>> blacklist() {
        return R.ok(friendService.listBlocked(LoginUser.currentUid()));
    }

    @Data
    static class TargetReq {
        @NotNull private Long targetId;
    }
}