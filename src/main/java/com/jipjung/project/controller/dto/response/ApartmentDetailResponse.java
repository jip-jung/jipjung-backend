package com.jipjung.project.controller.dto.response;

import com.jipjung.project.domain.Apartment;
import com.jipjung.project.domain.ApartmentDeal;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * 아파트 상세 조회 응답 DTO
 * 아파트 기본정보 + 모든 실거래 이력
 */
@Schema(description = "아파트 상세 조회 응답 (기본정보 + 모든 실거래 이력)")
public record ApartmentDetailResponse(
        // 아파트 기본정보
        @Schema(description = "아파트 코드", example = "11410-61")
        String aptSeq,

        @Schema(description = "아파트명", example = "금천현대")
        String aptNm,

        @Schema(description = "읍면동명", example = "홍제동")
        String umdNm,

        @Schema(description = "지번 주소", example = "서대문구 홍제동 123-45")
        String jibun,

        @Schema(description = "도로명", example = "연희로")
        String roadNm,

        @Schema(description = "도로명번호 (본번)", example = "10")
        String roadNmBonbun,

        @Schema(description = "도로명번호 (부번)", example = "5")
        String roadNmBubun,

        @Schema(description = "건축년도", example = "2015")
        Integer buildYear,

        @Schema(description = "위도", example = "37.5689043200000")
        BigDecimal latitude,

        @Schema(description = "경도", example = "126.9341234000000")
        BigDecimal longitude,

        // 실거래 이력 목록
        @Schema(description = "실거래 이력 목록 (시간순 내림차순)")
        List<DealInfo> deals
) {
    @Schema(description = "아파트 실거래 정보")
    public record DealInfo(
            @Schema(description = "거래번호", example = "12345")
            Long dealNo,

            @Schema(description = "동", example = "A")
            String aptDong,

            @Schema(description = "층", example = "5")
            String floor,

            @Schema(description = "거래일자 (YYYY-MM-DD)", example = "2024-11-15")
            java.time.LocalDate dealDate,

            @Schema(description = "거래금액 (만원)", example = "450000")
            Long dealAmount,

            @Schema(description = "전용면적 (㎡)", example = "84.5")
            BigDecimal exclusiveArea
    ) {
        public static DealInfo from(ApartmentDeal deal) {
            return new DealInfo(
                    deal.getDealNo(),
                    deal.getAptDong(),
                    deal.getFloor(),
                    deal.getDealDate(),
                    deal.getDealAmountNum(),
                    deal.getExcluUseAr()
            );
        }
    }

    public static ApartmentDetailResponse from(Apartment apartment, List<ApartmentDeal> deals) {
        List<DealInfo> dealInfos = deals != null
                ? deals.stream().map(DealInfo::from).toList()
                : List.of();

        return new ApartmentDetailResponse(
                apartment.getAptSeq(),
                apartment.getAptNm(),
                apartment.getUmdNm(),
                apartment.getJibun(),
                apartment.getRoadNm(),
                apartment.getRoadNmBonbun(),
                apartment.getRoadNmBubun(),
                apartment.getBuildYear(),
                apartment.getLatitude(),
                apartment.getLongitude(),
                dealInfos
        );
    }
}
