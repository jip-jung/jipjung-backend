package com.jipjung.project.controller.dto.request;

import com.jipjung.project.domain.SaveType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 저축 기록 요청 DTO
 */
@Schema(description = "저축 기록 요청")
public record SavingsRecordRequest(

        @Schema(description = "금액 (원 단위)", example = "1000000", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "금액은 필수입니다")
        @Min(value = 1, message = "금액은 1원 이상이어야 합니다")
        Long amount,

        @Schema(description = "저축 유형 (DEPOSIT: 입금, WITHDRAW: 출금)", example = "DEPOSIT", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "저축 유형은 필수입니다")
        SaveType saveType,

        @Schema(description = "메모 (선택)", example = "12월 월급 저축")
        @Size(max = 100, message = "메모는 100자 이하여야 합니다")
        String memo
) {
}
