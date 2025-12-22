package com.jipjung.project.controller;

import com.jipjung.project.controller.dto.request.DeleteAccountRequest;
import com.jipjung.project.controller.dto.request.FurnitureProgressRequest;
import com.jipjung.project.controller.dto.request.OnboardingRequest;
import com.jipjung.project.controller.dto.request.ProfileUpdateRequest;
import com.jipjung.project.controller.dto.response.DeleteAccountResponse;
import com.jipjung.project.controller.dto.response.FurnitureProgressResponse;
import com.jipjung.project.controller.dto.response.OnboardingResponse;
import com.jipjung.project.controller.dto.response.ProfileUpdateResponse;
import com.jipjung.project.global.response.ApiResponse;
import com.jipjung.project.service.CustomUserDetails;
import com.jipjung.project.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 컨트롤러
 * - 온보딩 및 프로필 관리 API
 */
@Tag(name = "사용자", description = "온보딩, 프로필 관리 및 계정 삭제 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "온보딩 정보 저장",
            description = """
                    사용자의 온보딩 정보를 저장합니다.
                    
                    **저장 정보:**
                    - 출생년도
                    - 연소득 (원 단위)
                    - 월 기존 대출 상환액 (원 단위)
                    - 선호 지역 (배열, 최대 10개, 각 50자 이내)
                    
                    **응답 정보:**
                    - 업데이트된 사용자 정보
                    - DSR 분석 결과 (SAFE/CAUTION/DANGER 3단계)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "온보딩 정보 저장 완료",
                    content = @Content(schema = @Schema(implementation = OnboardingResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "유효성 검증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            )
    })
    @PostMapping("/onboarding")
    public ResponseEntity<ApiResponse<OnboardingResponse>> saveOnboarding(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OnboardingRequest request
    ) {
        OnboardingResponse response = userService.saveOnboarding(
                userDetails.getId(),
                request
        );
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "프로필 수정",
            description = """
                    사용자 프로필 정보를 수정합니다.
                    
                    **수정 가능 필드:**
                    - 닉네임 (2-20자)
                    - 연소득 (원 단위)
                    - 월 기존 대출 상환액 (원 단위)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = ProfileUpdateResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "유효성 검증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음"
            )
    })
    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileUpdateResponse>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        ProfileUpdateResponse response = userService.updateProfile(
                userDetails.getId(),
                request
        );
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "회원탈퇴",
            description = """
                    현재 로그인된 사용자의 계정을 삭제합니다.
                    
                    **요구사항:**
                    - 비밀번호 확인 필수
                    
                    **처리 내용:**
                    - Soft Delete (is_deleted = true)
                    - 로그인 불가, 데이터 보존
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "탈퇴 성공",
                    content = @Content(schema = @Schema(implementation = DeleteAccountResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "비밀번호 불일치"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            )
    })
    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse<DeleteAccountResponse>> deleteAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody DeleteAccountRequest request
    ) {
        userService.deleteAccount(userDetails.getUsername(), request.password());
        return ApiResponse.success(new DeleteAccountResponse("회원탈퇴가 완료되었습니다"));
    }

    /**
     * 인테리어 진행 상태 동기화
     * <p>
     * 클라이언트에서 계산된 인테리어 진행 상태를 서버에 저장합니다.
     * 서버는 값을 검증하고 클램핑하여 저장된 실제 값을 반환합니다.
     */
    @Operation(
            summary = "인테리어 진행 상태 동기화",
            description = """
                    클라이언트에서 계산된 인테리어(가구) 진행 상태를 서버에 저장합니다.
                    
                    **저장 정보:**
                    - buildTrack: 현재 트랙 (house/furniture)
                    - furnitureStage: 인테리어 단계 (0-5)
                    - furnitureExp: 현재 단계 내 경험치
                    
                    서버는 값을 검증하고 필요 시 클램핑하여 저장된 실제 값을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/furniture-progress")
    public ResponseEntity<ApiResponse<FurnitureProgressResponse>> updateFurnitureProgress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FurnitureProgressRequest request
    ) {
        FurnitureProgressResponse response = userService.updateFurnitureProgress(
                userDetails.getId(),
                request
        );
        return ApiResponse.success(response);
    }
}
