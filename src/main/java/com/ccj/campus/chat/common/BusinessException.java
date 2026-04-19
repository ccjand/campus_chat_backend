package com.ccj.campus.chat.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ResultCode rc) {
        super(rc.getMsg());
        this.code = rc.getCode();
    }

    public BusinessException(ResultCode rc, String msg) {
        super(msg);
        this.code = rc.getCode();
    }

    public static void check(boolean condition, ResultCode rc) {
        if (!condition) {
            throw new BusinessException(rc);
        }
    }
}