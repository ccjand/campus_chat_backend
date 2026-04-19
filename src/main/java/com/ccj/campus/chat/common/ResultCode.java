package com.ccj.campus.chat.common;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(0, "success"),

    // 认证鉴权
    UNAUTHORIZED(401, "未登录或令牌失效"),
    FORBIDDEN(403, "无权访问"),
    TOKEN_EXPIRED(40101, "令牌已过期"),
    TOKEN_INVALID(40102, "令牌无效"),
    ACCOUNT_LOCKED(40103, "账号已锁定，请稍后重试"),

    // 通用
    BAD_REQUEST(400, "参数错误"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 消息
    MSG_RECALL_TIMEOUT(50001, "超过 2 分钟无法撤回"),
    MSG_NOT_OWNER(50002, "不能操作他人的消息"),

    // 签到
    CHECKIN_NOT_IN_RANGE(60001, "不在签到范围内"),
    CHECKIN_SESSION_ENDED(60002, "签到已结束"),
    CHECKIN_DUPLICATE(60003, "已签到，请勿重复提交"),
    CHECKIN_CODE_WRONG(60004, "签到码错误"),

    // 请假
    LEAVE_STATE_INVALID(70001, "当前状态不允许该操作"),
    LEAVE_NOT_APPROVER(70002, "你不是审批人"),

    // 群组
    GROUP_NOT_OWNER(80001, "仅群主可执行该操作"),
    GROUP_NOT_ADMIN(80002, "仅群主或管理员可执行该操作"),
    GROUP_NOT_MEMBER(80003, "你不是群成员");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}