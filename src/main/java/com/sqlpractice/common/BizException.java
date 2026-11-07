package com.sqlpractice.common;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final int status;
    private final int code;

    public BizException(String message) {
        this(400, 400, message);
    }

    public BizException(int status, String message) {
        this(status, status, message);
    }

    public BizException(int status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static BizException badRequest(String message) {
        return new BizException(400, 400, message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(401, 401, message);
    }

    public static BizException forbidden(String message) {
        return new BizException(403, 403, message);
    }

    public static BizException notFound(String message) {
        return new BizException(404, 404, message);
    }
}
