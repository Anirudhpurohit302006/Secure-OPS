package com.secureops.exception;

import org.springframework.http.HttpStatus;

/** Expected, user-facing failure. Message is safe to show to the user. */
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }

    public static ApiException notFound(String message) { return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message); }
    public static ApiException badRequest(String message) { return new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message); }
}
