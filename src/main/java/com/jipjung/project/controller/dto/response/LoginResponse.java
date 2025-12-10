package com.jipjung.project.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 로그인 응답 DTO
 * 
 * accessToken은 Authorization 헤더로 전달됨
 * body에는 user 정보만 포함
 */
@Schema(description = "로그인 응답")
public record LoginResponse(
        @Schema(description = "사용자 정보")
        UserInfo user
) {
    
    @Schema(description = "사용자 정보")
    public record UserInfo(
            @Schema(description = "사용자 ID", example = "1")
            Long id,
            
            @Schema(description = "이메일", example = "user@example.com")
            String email,
            
            @Schema(description = "닉네임", example = "건축왕")
            String nickname,
            
            @Schema(description = "온보딩 완료 여부", example = "true")
            Boolean onboardingCompleted,

            @Schema(description = "선호 지역 목록 (구/군명)", example = "[\"강남구\", \"서초구\"]")
            List<String> preferredAreas
    ) {}
}
