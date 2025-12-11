package com.jipjung.project.controller;

import com.jipjung.project.controller.dto.request.ConfirmExtractedDataRequest;
import com.jipjung.project.controller.dto.request.JudgmentRequest;
import com.jipjung.project.controller.dto.request.SpendingAnalyzeRequest;
import com.jipjung.project.controller.dto.response.AiHistoryResponse;
import com.jipjung.project.controller.dto.response.JudgmentResponse;
import com.jipjung.project.controller.dto.response.SpendingAnalyzeResponse;
import com.jipjung.project.domain.ConversationStatus;
import com.jipjung.project.global.openapi.SwaggerBody;
import com.jipjung.project.global.response.ApiResponse;
import com.jipjung.project.service.AiManagerService;
import com.jipjung.project.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * AI 매니저 컨트롤러
 * <p>
 * "레제" 캐릭터의 지출 분석 및 판결 API 제공.
 */
@Tag(name = "AI 매니저", description = "AI 기반 지출 분석 및 판결 API")
@RestController
@RequestMapping("/api/ai-manager")
@RequiredArgsConstructor
public class AiManagerController {

    private final AiManagerService aiManagerService;

    // =========================================================================
    // 지출 분석 (MANUAL / IMAGE)
    // =========================================================================

    @Operation(
            summary = "지출 분석",
            description = """
                    사용자의 지출 정보를 분석하고 "레제" 캐릭터가 심문합니다.
                    
                    **입력 모드:**
                    - MANUAL: 수기 입력 → 바로 분석 (ANALYZED)
                    - IMAGE: 영수증 사진 → 정보 추출 후 확인 필요 (EXTRACTING)
                    
                    **응답 정보:**
                    - MANUAL: 영수증 정보, AI 페르소나 반응, 변명 선택지
                    - IMAGE: 추출 상태, 추출된 데이터, 누락 필드, 첫 반응
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "분석 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "AI 서비스 오류"
            )
    })
    @PostMapping(value = "/analyze", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<SpendingAnalyzeResponse>> analyzeSpendingJson(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SpendingAnalyzeRequest request
    ) {
        SpendingAnalyzeResponse response = aiManagerService.analyzeSpending(
                userDetails.getId(),
                request,
                null
        );
        String message = resolveAnalyzeMessage(response.status());
        return ResponseEntity.ok(ApiResponse.successBody(message, response));
    }

    @Operation(
            summary = "지출 분석 (이미지 포함)",
            description = """
                    영수증 이미지를 포함한 지출 분석 요청.
                    
                    **요청 형식:** multipart/form-data
                    - request: JSON (inputMode=IMAGE)
                    - image: 영수증 이미지 (jpg, png, webp)
                    """
    )
    @SwaggerBody(content = @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            encoding = @Encoding(name = "request", contentType = MediaType.APPLICATION_JSON_VALUE)
    ))
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SpendingAnalyzeResponse>> analyzeSpendingMultipart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestPart("request") SpendingAnalyzeRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        SpendingAnalyzeResponse response = aiManagerService.analyzeSpending(
                userDetails.getId(),
                request,
                image
        );
        String message = resolveAnalyzeMessage(response.status());
        return ResponseEntity.ok(ApiResponse.successBody(message, response));
    }

    // =========================================================================
    // 추출 데이터 확인 (IMAGE 모드 전용)
    // =========================================================================

    @Operation(
            summary = "추출 데이터 확인",
            description = """
                    영수증에서 추출된 정보를 확인/수정하고 분석을 진행합니다.
                    
                    **사용 조건:**
                    - IMAGE 모드로 분석 후 EXTRACTING 상태인 대화만 가능
                    - 모든 필드(금액, 가게명, 카테고리, 결제일) 필수
                    
                    **응답:** MANUAL 분석과 동일 (status=ANALYZED)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "확인 완료"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 대화 상태"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "대화를 찾을 수 없음"
            )
    })
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<SpendingAnalyzeResponse>> confirmExtractedData(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ConfirmExtractedDataRequest request
    ) {
        SpendingAnalyzeResponse response = aiManagerService.confirmExtractedData(
                userDetails.getId(),
                request
        );
        return ResponseEntity.ok(ApiResponse.successBody("레제가 지출을 분석했습니다.", response));
    }

    // =========================================================================
    // 최종 판결
    // =========================================================================

    @Operation(
            summary = "최종 판결",
            description = """
                    사용자가 선택한 변명을 기반으로 "레제"가 최종 판결을 내립니다.
                    
                    **판결 결과:**
                    - REASONABLE (합리적): +50 EXP
                    - WASTE (낭비): -30 EXP
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "판결 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 또는 대화 상태"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "대화를 찾을 수 없음"
            )
    })
    @PostMapping("/judgment")
    public ResponseEntity<ApiResponse<JudgmentResponse>> processJudgment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody JudgmentRequest request
    ) {
        JudgmentResponse response = aiManagerService.processJudgment(
                userDetails.getId(),
                request
        );
        String message = "REASONABLE".equals(response.judgment().result())
                ? "판결 완료: 경험치 획득"
                : "판결 완료: 경험치 차감";
        return ResponseEntity.ok(ApiResponse.successBody(message, response));
    }

    // =========================================================================
    // 분석 내역 조회
    // =========================================================================

    @Operation(
            summary = "분석 내역 조회",
            description = "사용자의 AI 분석 내역을 조회합니다. (판결 완료된 것만)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"
            )
    })
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<AiHistoryResponse>>> getHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<AiHistoryResponse> history = aiManagerService.getHistory(
                userDetails.getId(),
                limit
        );
        return ResponseEntity.ok(ApiResponse.successBody("조회 성공", history));
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    /**
     * 분석 상태에 따른 응답 메시지 결정
     */
    private String resolveAnalyzeMessage(String status) {
        if (ConversationStatus.EXTRACTING.name().equals(status)) {
            return "영수증에서 정보를 추출했습니다. 확인 후 진행해주세요.";
        }
        return "레제가 지출을 분석했습니다.";
    }
}
