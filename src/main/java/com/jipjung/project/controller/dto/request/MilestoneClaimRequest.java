package com.jipjung.project.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 마일스톤 보상 수령 요청 DTO
 */
@Schema(description = "마일스톤 보상 수령 요청")
public record MilestoneClaimRequest(

        @Schema(description = "마일스톤 일수 (7, 14, 21, 28)", example = "7")
        @NotNull(message = "마일스톤 일수는 필수입니다")
        Integer milestoneDays
) {
}
