package com.jipjung.project.controller.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 인테리어 진행 상태 업데이트 요청 DTO
 * <p>
 * 클라이언트에서 계산된 인테리어 진행 상태를 서버에 동기화합니다.
 * 서버는 값의 유효성을 검증하고 필요 시 클램핑하여 저장합니다.
 *
 * @param buildTrack     현재 트랙 (house 또는 furniture)
 * @param furnitureStage 인테리어 단계 (0: 미시작, 1-5: 진행 중)
 * @param furnitureExp   현재 단계 내 경험치 (0 이상)
 */
public record FurnitureProgressRequest(
        @NotNull(message = "buildTrack은 필수입니다")
        @Pattern(regexp = "^(house|furniture)$", message = "buildTrack은 house 또는 furniture여야 합니다")
        String buildTrack,

        @Min(value = 0, message = "furnitureStage는 0 이상이어야 합니다")
        @Max(value = 5, message = "furnitureStage는 5 이하여야 합니다")
        int furnitureStage,

        @Min(value = 0, message = "furnitureExp는 0 이상이어야 합니다")
        int furnitureExp
) {
    /**
     * 서버 측 클램핑이 적용된 안전한 값 생성
     */
    public FurnitureProgressRequest clamp() {
        String safeTrack = "house".equals(buildTrack) || "furniture".equals(buildTrack) 
                ? buildTrack : "house";
        int safeStage = Math.min(5, Math.max(0, furnitureStage));
        int safeExp = Math.max(0, furnitureExp);
        return new FurnitureProgressRequest(safeTrack, safeStage, safeExp);
    }
}
