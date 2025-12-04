package com.jipjung.project.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 아파트 목록 조회 응답 (페이징 메타 포함)
 */
@Schema(description = "아파트 목록 조회 응답 (페이징)")
public record ApartmentListPageResponse(
        @Schema(description = "현재 페이지의 아파트 목록")
        List<ApartmentListResponse> apartments,

        @Schema(description = "검색 조건에 맞는 전체 아파트 개수", example = "1523")
        int totalCount,

        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
        int page,

        @Schema(description = "페이지 크기", example = "10")
        int size,

        @Schema(description = "전체 페이지 수", example = "153")
        int totalPages
) {
    public static ApartmentListPageResponse of(List<ApartmentListResponse> apartments,
                                               int totalCount,
                                               int page,
                                               int size) {
        int totalPages = (int) Math.ceil((double) totalCount / size);
        return new ApartmentListPageResponse(apartments, totalCount, page, size, totalPages);
    }
}
