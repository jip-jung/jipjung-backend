package com.jipjung.project.controller.dto.response;

import com.jipjung.project.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "온보딩 정보 저장 응답")
public record OnboardingResponse(
        @Schema(description = "사용자 정보") UserInfo user,
        @Schema(description = "DSR 분석 결과") DsrResult dsrResult
) {
    /**
     * 팩토리 메서드
     */
    public static OnboardingResponse from(User user, DsrResult dsrResult, List<String> preferredAreas) {
        return new OnboardingResponse(
                UserInfo.from(user, preferredAreas),
                dsrResult
        );
    }

    // ============================
    // Nested Records
    // ============================

    @Schema(description = "사용자 정보")
    public record UserInfo(
            @Schema(description = "사용자 ID") Long id,
            @Schema(description = "닉네임") String nickname,
            @Schema(description = "온보딩 완료 여부") boolean onboardingCompleted,
            @Schema(description = "선호 지역 목록 (구/군명)") List<String> preferredAreas
    ) {
        public static UserInfo from(User user, List<String> preferredAreas) {
            return new UserInfo(
                    user.getId(),
                    user.getNickname(),
                    true,  // 온보딩 완료 후 항상 true
                    preferredAreas
            );
        }
    }

    @Schema(description = "DSR 분석 결과")
    public record DsrResult(
        @Schema(description = "DSR 비율 (%)", example = "12.0") double dsrRatio,
        @Schema(description = "등급 (SAFE, CAUTION, DANGER)", example = "SAFE") String grade,
        @Schema(description = "예상 최대 대출 가능액 (원)", example = "400000000") long maxLoanAmount
    ) { }
}
