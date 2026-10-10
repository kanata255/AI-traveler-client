package com.example.demo.common;

/**
 * 业务异常，携带错误码，由 GlobalExceptionHandle 统一转为 Result
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
