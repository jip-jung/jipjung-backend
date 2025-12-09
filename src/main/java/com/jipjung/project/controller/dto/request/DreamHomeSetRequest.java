package com.jipjung.project.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 드림홈 설정 요청 DTO
 */
@Schema(description = "드림홈 설정 요청")
public record DreamHomeSetRequest(

        @Schema(description = "아파트 고유 ID", example = "11410-61", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "아파트 코드는 필수입니다")
        String aptSeq,

        @Schema(description = "목표 금액 (원 단위)", example = "300000000", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "목표 금액은 필수입니다")
        @Min(value = 1, message = "목표 금액은 1원 이상이어야 합니다")
        Long targetAmount,

        @Schema(description = "목표 달성일 (YYYY-MM-DD)", example = "2028-12-31", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "목표 달성일은 필수입니다")
        @Future(message = "목표 달성일은 미래 날짜여야 합니다")
        LocalDate targetDate,

        @Schema(description = "월 목표 저축액 (원 단위, 선택)", example = "2500000")
        @Min(value = 0, message = "월 목표 저축액은 0 이상이어야 합니다")
        Long monthlyGoal
) {
}
