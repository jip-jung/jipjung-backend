package com.jipjung.project.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 회원탈퇴 요청 DTO
 */
@Schema(description = "회원탈퇴 요청")
public record DeleteAccountRequest(
        @Schema(description = "현재 비밀번호", example = "Test1234!@")
        @NotBlank(message = "비밀번호를 입력해주세요")
        String password
) {}
