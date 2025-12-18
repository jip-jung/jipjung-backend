package com.jipjung.project.controller;

import com.jipjung.project.controller.dto.response.CollectionResponse;
import com.jipjung.project.controller.dto.response.JourneyResponse;
import com.jipjung.project.global.response.ApiResponse;
import com.jipjung.project.service.CollectionService;
import com.jipjung.project.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 컬렉션 컨트롤러
 * <p>
 * 완성된 집 컬렉션 관리 API를 제공합니다.
 * PRD: COLLECTION_FEATURE_PRD.md
 */
@Tag(name = "컬렉션", description = "완성된 집 컬렉션 관리 API")
@RestController
@RequestMapping({"/api/collection", "/api/collections"})
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    // =========================================================================
    // 컬렉션 목록 조회
    // =========================================================================

    @Operation(
            summary = "완성된 집 목록 조회",
            description = """
                    사용자가 완성한 집 목록을 조회합니다.
                    
                    **포함 정보:**
                    - 테마 정보 (themeCode, themeName)
                    - 매물 정보 (propertyName, location)
                    - 저축 정보 (totalSaved, savingPeriodDays)
                    - 대표 전시 여부 (isMainDisplay)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<CollectionResponse>> getCollections(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CollectionResponse response = collectionService.getCollections(userDetails.getId());
        return ApiResponse.success(response);
    }

    // =========================================================================
    // 저축 여정 조회
    // =========================================================================

    @Operation(
            summary = "저축 여정 상세 조회",
            description = """
                    완성된 집의 저축 여정을 상세 조회합니다.
                    
                    **11단계 Phase 구성:**
                    - Phase 1-6: 집 짓기 (터파기 → 완공)
                    - Phase 7-11: 가구 배치 (바닥 정돈 → 인테리어 완성)
                    
                    **Phase별 정보:**
                    - 해당 Phase의 저축 이벤트 목록
                    - 누적 저축 금액
                    - Phase 도달 시각
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "컬렉션을 찾을 수 없음")
    })
    @GetMapping("/{collectionId}/journey")
    public ResponseEntity<ApiResponse<JourneyResponse>> getJourney(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long collectionId
    ) {
        JourneyResponse response = collectionService.getJourney(userDetails.getId(), collectionId);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "진행 중인 드림홈 여정 조회",
            description = """
                    현재 진행 중인 드림홈의 저축 여정을 조회합니다.
                    
                    **특징:**
                    - 완성된 컬렉션이 아닌 ACTIVE 상태의 드림홈 여정
                    - 미도달 Phase는 reachedAt/cumulativeAmount/events가 비어있을 수 있음
                    - 응답 형식은 완성된 여정과 동일
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "진행 중인 드림홈이 없음")
    })
    @GetMapping("/in-progress/journey")
    public ResponseEntity<ApiResponse<JourneyResponse>> getInProgressJourney(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        JourneyResponse response = collectionService.getInProgressJourney(userDetails.getId());
        return ApiResponse.success(response);
    }

    // =========================================================================
    // 대표 컬렉션 설정
    // =========================================================================

    @Operation(
            summary = "대표 컬렉션 설정",
            description = """
                    해당 컬렉션을 대표 전시로 설정합니다.
                    
                    **규칙:**
                    - 기존 대표 컬렉션은 자동으로 해제됩니다.
                    - 사용자당 하나의 대표 컬렉션만 설정 가능합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "설정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "컬렉션을 찾을 수 없음")
    })
    @PutMapping("/{collectionId}/main-display")
    public ResponseEntity<ApiResponse<MainDisplayResponse>> setMainDisplay(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long collectionId
    ) {
        collectionService.setMainDisplay(userDetails.getId(), collectionId);
        return ApiResponse.success(new MainDisplayResponse(true, collectionId, true));
    }

    /**
     * 대표 컬렉션 설정 응답
     */
    public record MainDisplayResponse(
            boolean success,
            Long collectionId,
            boolean isMainDisplay
    ) {}
}
