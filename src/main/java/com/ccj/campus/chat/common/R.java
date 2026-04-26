package com.ccj.campus.chat.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回对象。
 * 对齐论文 4.3：所有 RESTful 接口统一封装 code / msg / data 三段结构。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> implements Serializable {

    private int code;
    private String msg;
    private T data;

    public static <T> R<T> ok() {
        return build(ResultCode.SUCCESS, null);
    }

    public static <T> R<T> ok(T data) {
        return build(ResultCode.SUCCESS, data);
    }

    public static <T> R<T> fail(ResultCode code) {
        return build(code, null);
    }

    public static <T> R<T> fail(ResultCode code, String msg) {
        R<T> r = new R<>();
        r.setCode(code.getCode());
        r.setMsg(msg);
        return r;
    }

    public static <T> R<T> fail(String msg) {
        R<T> r = new R<>();
        r.setCode(200);
        r.setMsg(msg);
        return r;
    }

    private static <T> R<T> build(ResultCode code, T data) {
        R<T> r = new R<>();
        r.setCode(code.getCode());
        r.setMsg(code.getMsg());
        r.setData(data);
        return r;
    }
}