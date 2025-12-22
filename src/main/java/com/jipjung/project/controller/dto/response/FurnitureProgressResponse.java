package com.jipjung.project.controller.dto.response;

/**
 * 인테리어 진행 상태 응답 DTO
 * <p>
 * 서버에서 클램핑/저장된 실제 값을 클라이언트에 반환합니다.
 * 클라이언트는 이 값으로 로컬 상태를 동기화해야 합니다.
 *
 * @param buildTrack     저장된 트랙 (house 또는 furniture)
 * @param furnitureStage 저장된 인테리어 단계 (0-5)
 * @param furnitureExp   저장된 경험치
 */
public record FurnitureProgressResponse(
        String buildTrack,
        int furnitureStage,
        int furnitureExp
) {}
