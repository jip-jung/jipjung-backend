package com.jipjung.project.controller.dto.response;

import com.jipjung.project.domain.FavoriteApartment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 관심 아파트 응답 DTO
 */
@Schema(description = "관심 아파트 응답")
public record FavoriteResponse(
        @Schema(description = "관심 아파트 ID", example = "42")
        Long id,

        @Schema(description = "아파트 코드", example = "11410-61")
        String aptSeq,

        @Schema(description = "아파트명", example = "금천현대")
        String aptNm,

        @Schema(description = "읍면동명", example = "홍제동")
        String umdNm,

        @Schema(description = "도로명", example = "연희로")
        String roadNm,

        @Schema(description = "건축년도", example = "2015")
        Integer buildYear,

        @Schema(description = "위도", example = "37.5689043200000")
        BigDecimal latitude,

        @Schema(description = "경도", example = "126.9341234000000")
        BigDecimal longitude,

        @Schema(description = "등록일시 (ISO 8601)", example = "2024-11-15T14:30:25")
        LocalDateTime createdAt
) {
    public static FavoriteResponse from(FavoriteApartment favorite) {
        return new FavoriteResponse(
                favorite.getId(),
                favorite.getAptSeq(),
                favorite.getApartment() != null ? favorite.getApartment().getAptNm() : null,
                favorite.getApartment() != null ? favorite.getApartment().getUmdNm() : null,
                favorite.getApartment() != null ? favorite.getApartment().getRoadNm() : null,
                favorite.getApartment() != null ? favorite.getApartment().getBuildYear() : null,
                favorite.getApartment() != null ? favorite.getApartment().getLatitude() : null,
                favorite.getApartment() != null ? favorite.getApartment().getLongitude() : null,
                favorite.getCreatedAt()
        );
    }
}
