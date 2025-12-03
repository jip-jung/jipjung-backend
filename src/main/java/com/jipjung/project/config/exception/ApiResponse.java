package com.jipjung.project.config.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Schema(description = "API 공통 응답 래퍼")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        @Schema(description = "HTTP 상태 코드", example = "200")
        int code,

        @Schema(description = "HTTP 상태명", example = "OK")
        String status,

        @Schema(description = "응답 메시지", example = "성공")
        String message,

        @Schema(description = "응답 데이터", nullable = true)
        T data
) {
    /**
     * 본문만 필요한 경우 사용 (필터, 핸들러 등에서 직접 쓰기 위함)
     */
    public static <T> ApiResponse<T> successBody(T data) {
        return body(HttpStatus.OK, "성공", data);
    }

    public static ApiResponse<Void> successBody() {
        return successBody(null);
    }

    public static <T> ApiResponse<T> errorBody(HttpStatus status, String message) {
        return body(status, message, null);
    }

    public static <T> ApiResponse<T> errorBody(HttpStatus status, String message, T data) {
        return body(status, message, data);
    }

    public static <T> ApiResponse<T> errorBody(ErrorCode errorCode) {
        HttpStatus status = HttpStatus.valueOf(errorCode.getStatus());
        return body(status, errorCode.getMessage(), null);
    }

    /**
     * 컨트롤러/예외 핸들러에서 사용: HTTP 상태까지 함께 설정
     */
    public static <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(successBody(data));
    }

    public static ResponseEntity<ApiResponse<Void>> success() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(successBody());
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(body(HttpStatus.CREATED, "성공", data));
    }

    public static ResponseEntity<ApiResponse<Void>> created() {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(body(HttpStatus.CREATED, "성공", null));
    }

    public static <T> ResponseEntity<ApiResponse<T>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(errorBody(status, message));
    }

    public static <T> ResponseEntity<ApiResponse<T>> error(HttpStatus status, String message, T data) {
        return ResponseEntity.status(status)
                .body(errorBody(status, message, data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> error(int code, String message) {
        HttpStatus status = HttpStatus.valueOf(code);
        return error(status, message);
    }

    public static <T> ResponseEntity<ApiResponse<T>> error(int code, String message, T data) {
        HttpStatus status = HttpStatus.valueOf(code);
        return error(status, message, data);
    }

    public static ResponseEntity<ApiResponse<Void>> error(ErrorCode errorCode) {
        HttpStatus status = HttpStatus.valueOf(errorCode.getStatus());
        return error(status, errorCode.getMessage());
    }

    private static <T> ApiResponse<T> body(HttpStatus status, String message, T data) {
        return new ApiResponse<>(
                status.value(),
                status.name(),
                message,
                data
        );
    }
}
