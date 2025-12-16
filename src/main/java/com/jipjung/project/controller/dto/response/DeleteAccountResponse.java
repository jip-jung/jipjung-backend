package com.jipjung.project.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 회원탈퇴 응답 DTO
 */
@Schema(description = "회원탈퇴 응답")
public record DeleteAccountResponse(
        @Schema(description = "결과 메시지", example = "회원탈퇴가 완료되었습니다")
        String message
) {}
