package com.jipjung.project.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 스트릭 정보 DTO
 * <p>
 * 저축 응답에 포함되는 스트릭 정보를 담습니다.
 */
@Schema(description = "스트릭 정보")
public record StreakInfo(

        @Schema(description = "현재 연속일수", example = "5")
        int currentStreak,

        @Schema(description = "최대 연속일수", example = "14")
        int maxStreak,

        @Schema(description = "스트릭 획득 경험치", example = "50")
        int expEarned
) {
}
