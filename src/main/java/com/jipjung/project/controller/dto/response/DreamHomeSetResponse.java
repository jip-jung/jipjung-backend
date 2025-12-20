package com.jipjung.project.controller.dto.response;

import com.jipjung.project.domain.Apartment;
import com.jipjung.project.domain.DreamHome;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 드림홈 설정 응답 DTO
 */
@Schema(description = "드림홈 설정 응답")
public record DreamHomeSetResponse(

        @Schema(description = "드림홈 정보")
        DreamHomeInfo dreamHome
) {

    /**
     * 드림홈 상세 정보
     */
    @Schema(description = "드림홈 상세 정보")
    public record DreamHomeInfo(
            @Schema(description = "드림홈 ID", example = "10")
            Long dreamHomeId,

            @Schema(description = "아파트 코드", example = "11410-61")
            String aptSeq,

            @Schema(description = "표시명 (houseName 우선, 없으면 아파트명)", example = "우리 가족의 첣 집")
            String propertyName,

            @Schema(description = "사용자 정의 집 이름 (nullable)", example = "우리 가족의 첣 집")
            String houseName,

            @Schema(description = "위치", example = "서울 서대문구 홍제동")
            String location,

            @Schema(description = "최신 실거래가 (원)", example = "850000000")
            Long price,

            @Schema(description = "목표 금액 (원)", example = "300000000")
            Long targetAmount,

            @Schema(description = "월 목표 저축액 (원)", example = "2500000")
            Long monthlyGoal,

            @Schema(description = "D-Day (남은 일수)", example = "1095")
            int dDay,

            @Schema(description = "달성률 (%)", example = "0.0")
            double achievementRate
    ) {
    }

    /**
     * 팩토리 메서드: DreamHome + Apartment + latestDealPrice -> Response
     */
    public static DreamHomeSetResponse from(DreamHome dreamHome, Apartment apartment, Long latestDealPrice) {
        int dDay = calculateDDay(dreamHome.getTargetDate());
        String location = buildLocation(apartment);
        
        // houseName이 있으면 그것을 사용, 없으면 aptNm
        String aptNm = apartment != null ? apartment.getAptNm() : null;
        String houseName = dreamHome.getHouseName();
        String displayName = (houseName != null && !houseName.isBlank()) ? houseName : aptNm;

        return new DreamHomeSetResponse(
                new DreamHomeInfo(
                        dreamHome.getDreamHomeId(),
                        dreamHome.getAptSeq(),
                        displayName,
                        houseName,
                        location,
                        latestDealPrice,
                        dreamHome.getTargetAmount(),
                        dreamHome.getMonthlyGoal(),
                        dDay,
                        dreamHome.getAchievementRate()
                )
        );
    }

    /**
     * D-Day 계산 (오늘부터 목표일까지 남은 일수)
     */
    private static int calculateDDay(LocalDate targetDate) {
        if (targetDate == null) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), targetDate);
    }

    /**
     * 위치 문자열 생성
     */
    private static String buildLocation(Apartment apartment) {
        if (apartment == null) {
            return null;
        }
        return apartment.getUmdNm();
    }
}
