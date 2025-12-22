package com.jipjung.project.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 저축 여정 상세 조회 응답 DTO
 */
@Schema(description = "저축 여정 상세 조회 응답")
public record JourneyResponse(

        @Schema(description = "컬렉션 정보")
        CollectionInfo collection,

        @Schema(description = "여정 요약")
        JourneySummary summary,

        @Schema(description = "Phase별 이벤트 목록")
        List<PhaseInfo> phases
) {

    /**
     * 컬렉션 기본 정보
     */
    @Schema(description = "컬렉션 정보")
    public record CollectionInfo(
            @Schema(description = "컬렉션 ID", example = "1")
            Long collectionId,

            @Schema(description = "테마 이름", example = "모던 아파트")
            String themeName,

            @Schema(description = "테마 코드", example = "CLASSIC")
            String themeCode,

            @Schema(description = "매물명", example = "강남 오피스텔")
            String propertyName,

            @Schema(description = "위치", example = "서울 강남구")
            String location
    ) {}

    /**
     * 여정 요약 정보
     */
    @Schema(description = "여정 요약")
    public record JourneySummary(
            @Schema(description = "시작일")
            LocalDate startDate,

            @Schema(description = "완료일")
            LocalDate completedDate,

            @Schema(description = "총 소요 일수", example = "152")
            int totalDays,

            @Schema(description = "총 저축 횟수", example = "25")
            int totalDeposits,

            @Schema(description = "목표 금액 (원)", example = "50000000")
            Long targetAmount,

            @Schema(description = "목표 XP", example = "5000")
            Integer targetExp,

            @Schema(description = "현재 누적 XP", example = "820")
            Integer totalExp,

            @Schema(description = "현재 Phase (1-11)", example = "4")
            Integer currentPhase
    ) {}

    /**
     * Phase 정보 (집 짓기 6단계 + 가구 배치 5단계 = 11단계)
     */
    @Schema(description = "Phase 정보")
    public record PhaseInfo(
            @Schema(description = "Phase 번호 (1-11)", example = "1")
            int phaseNumber,

            @Schema(description = "Phase 이름", example = "터파기")
            String phaseName,

            @Schema(description = "테마 코드", example = "CLASSIC")
            String themeCode,

            @Schema(description = "단계 번호 (1-6 for house, 1-5 for furniture)", example = "1")
            int stageNumber,

            @Schema(description = "Phase 도달 시각")
            LocalDateTime reachedAt,

            @Schema(description = "누적 저축 금액 (원, optional)", example = "5000000")
            Long cumulativeAmount,

            @Schema(description = "누적 XP", example = "120")
            Integer cumulativeExp,

            @Schema(description = "이 Phase의 이벤트 목록")
            List<JourneyEvent> events
    ) {}

    /**
     * 저축 이벤트 정보
     */
    @Schema(description = "여정 이벤트")
    public record JourneyEvent(
            @Schema(description = "이벤트 ID", example = "1")
            Long eventId,

            @Schema(description = "이벤트 타입", example = "DEPOSIT")
            String eventType,

            @Schema(description = "이벤트 날짜")
            LocalDateTime date,

            @Schema(description = "금액 (원, optional)", example = "500000")
            Long amount,

            @Schema(description = "메모", example = "월급날 저축!")
            String memo,

            @Schema(description = "누적 저축 합계 (원, optional)", example = "5000000")
            Long cumulativeTotal,

            @Schema(description = "경험치 변화량", example = "20")
            Integer expChange,

            @Schema(description = "누적 경험치", example = "120")
            Integer cumulativeExp
    ) {
        /**
         * Map으로부터 JourneyEvent 생성
         */
        public static JourneyEvent fromMap(Map<String, Object> map) {
            return new JourneyEvent(
                    getLong(map, "event_id"),
                    getString(map, "event_type"),
                    getLocalDateTime(map, "date"),
                    getLong(map, "amount"),
                    getString(map, "memo"),
                    getLong(map, "cumulative_total"),
                    getInt(map, "exp_change"),
                    getInt(map, "cumulative_exp")
            );
        }

        private static Long getLong(Map<String, Object> map, String key) {
            Object val = map.get(key);
            if (val == null) return null;
            if (val instanceof Long l) return l;
            if (val instanceof Number n) return n.longValue();
            return null;
        }

        private static String getString(Map<String, Object> map, String key) {
            Object val = map.get(key);
            return val != null ? val.toString() : null;
        }

        private static LocalDateTime getLocalDateTime(Map<String, Object> map, String key) {
            Object val = map.get(key);
            if (val instanceof LocalDateTime ldt) return ldt;
            if (val instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
            return null;
        }

        private static Integer getInt(Map<String, Object> map, String key) {
            Object val = map.get(key);
            if (val == null) return null;
            if (val instanceof Integer i) return i;
            if (val instanceof Number n) return n.intValue();
            return null;
        }
    }
}
