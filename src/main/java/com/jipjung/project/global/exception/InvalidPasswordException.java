package com.jipjung.project.global.exception;

/**
 * 비밀번호 불일치 시 사용
 */
public class InvalidPasswordException extends RuntimeException {
    private final ErrorCode errorCode;

    public InvalidPasswordException() {
        super(ErrorCode.INVALID_PASSWORD.getMessage());
        this.errorCode = ErrorCode.INVALID_PASSWORD;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
