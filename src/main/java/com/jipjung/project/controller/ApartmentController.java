package com.jipjung.project.controller;

import com.jipjung.project.global.response.ApiResponse;
import com.jipjung.project.controller.dto.request.ApartmentSearchRequest;
import com.jipjung.project.controller.dto.request.FavoriteRequest;
import com.jipjung.project.controller.dto.response.ApartmentDetailResponse;
import com.jipjung.project.controller.dto.response.ApartmentListPageResponse;
import com.jipjung.project.controller.dto.response.FavoriteResponse;
import com.jipjung.project.controller.dto.response.RegionCoordinatesResponse;
import com.jipjung.project.service.ApartmentService;
import com.jipjung.project.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "아파트", description = "아파트 실거래가 조회 및 관심 아파트 관리 API")
@RestController
@RequestMapping("/api/apartments")
@RequiredArgsConstructor
public class ApartmentController {

    private final ApartmentService apartmentService;

    @Operation(
            summary = "아파트 목록 조회",
            description = "검색 조건에 따라 아파트 목록을 조회합니다. 각 아파트의 최신 실거래가 1건이 포함됩니다.\n\n" +
                    "**검색 조건**: 아파트명, 읍면동명, 거래일, 거래금액 등으로 필터링 가능\n\n" +
                    "**페이징**: page와 size 파라미터로 페이징 처리"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ApartmentListPageResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<ApartmentListPageResponse>> searchApartments(
            @Parameter(description = "검색 조건") ApartmentSearchRequest request) {
        ApartmentListPageResponse result = apartmentService.searchApartments(request);
        return ApiResponse.success(result);
    }

    @Operation(
            summary = "아파트 상세 조회",
            description = "특정 아파트의 상세 정보와 모든 실거래 이력을 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ApartmentDetailResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "아파트를 찾을 수 없음"
            )
    })
    @GetMapping("/{aptSeq}")
    public ResponseEntity<ApiResponse<ApartmentDetailResponse>> getApartmentDetail(
            @Parameter(description = "아파트 코드", example = "11410-61") @PathVariable String aptSeq) {
        ApartmentDetailResponse response = apartmentService.getApartmentDetail(aptSeq);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "관심 아파트 등록",
            description = "아파트를 관심 목록에 추가합니다.\n\n" +
                    "**인증 필요**: JWT 토큰 필요 (Authorization 헤더에 Bearer 토큰)\n\n" +
                    "**아파트 단위 저장**: apt_seq 기준으로 아파트 자체를 즐겨찾기",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "등록 성공",
                    content = @Content(schema = @Schema(implementation = FavoriteResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "이미 등록된 관심 아파트 또는 유효하지 않은 요청"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            )
    })
    @PostMapping("/favorites")
    public ResponseEntity<ApiResponse<FavoriteResponse>> addFavorite(
            @Valid @RequestBody FavoriteRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        FavoriteResponse response = apartmentService.addFavorite(userId, request);
        return ApiResponse.created(response);
    }

    @Operation(
            summary = "내 관심 아파트 목록 조회",
            description = "현재 로그인한 사용자의 관심 아파트 목록을 조회합니다.\n\n" +
                    "**인증 필요**: JWT 토큰 필요",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = List.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            )
    })
    @GetMapping("/favorites")
    public ResponseEntity<ApiResponse<List<FavoriteResponse>>> getMyFavorites(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        List<FavoriteResponse> favorites = apartmentService.getMyFavorites(userId);
        return ApiResponse.success(favorites);
    }

    @Operation(
            summary = "관심 아파트 삭제",
            description = "관심 아파트 목록에서 삭제합니다.\n\n" +
                    "**인증 필요**: JWT 토큰 필요\n\n" +
                    "**권한**: 본인의 관심 아파트만 삭제 가능",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "본인의 관심 아파트가 아님"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "관심 아파트를 찾을 수 없음"
            )
    })
    @DeleteMapping("/favorites/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFavorite(
            @Parameter(description = "관심 아파트 ID") @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        apartmentService.deleteFavorite(userId, id);
        return ApiResponse.success();
    }

    @Operation(
            summary = "지역 좌표 조회",
            description = "지역명으로 해당 지역의 중심 좌표를 조회합니다.\n\n" +
                    "**좌표 계산**: 해당 지역 아파트들의 평균 위도/경도\n\n" +
                    "**기본값**: 해당 지역에 아파트가 없으면 서울시청 좌표 반환"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = RegionCoordinatesResponse.class))
            )
    })
    @GetMapping("/regions/{regionName}/coordinates")
    public ResponseEntity<ApiResponse<RegionCoordinatesResponse>> getRegionCoordinates(
            @Parameter(description = "지역명 (예: 강남구, 서초구)", example = "강남구") 
            @PathVariable String regionName) {
        RegionCoordinatesResponse response = apartmentService.getRegionCoordinates(regionName);
        return ApiResponse.success(response);
    }
}
