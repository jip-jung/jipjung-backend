package com.jipjung.project.controller.dto.response;

/**
 * 지역 좌표 응답 DTO
 * 
 * 지역명과 해당 지역의 중심 좌표를 반환합니다.
 */
public record RegionCoordinatesResponse(
    String region,
    Double latitude,
    Double longitude
) {
    /**
     * 기본 좌표 (서울시청)
     */
    public static RegionCoordinatesResponse defaultCoordinates(String region) {
        return new RegionCoordinatesResponse(region, 37.5665, 126.9780);
    }
}
