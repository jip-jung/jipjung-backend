package com.jipjung.project.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 로그아웃 응답 DTO
 */
@Schema(description = "로그아웃 응답")
public record LogoutResponse(
        @Schema(description = "결과 메시지", example = "로그아웃 성공")
        String message
) {}
