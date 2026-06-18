package com.warehouse.common;

import lombok.Data;

@Data
public class Result<T> {

    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    private Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = ResultCode.SUCCESS.getCode();
        result.message = ResultCode.SUCCESS.getMessage();
        result.data = data;
        return result;
    }

    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.code = ResultCode.SUCCESS.getCode();
        result.message = ResultCode.SUCCESS.getMessage();
        return result;
    }

    public static <T> Result<T> error(int code, String msg) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = msg;
        return result;
    }

    public static <T> Result<T> fail(String msg) {
        return error(400, msg);
    }

    public static <T> Result<T> unauthorized() {
        return error(401, "未登录或登录已过期");
    }
}
