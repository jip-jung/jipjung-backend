package com.jipjung.project.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 컬렉션 목록 조회 응답 DTO
 */
@Schema(description = "컬렉션 목록 조회 응답")
public record CollectionResponse(

        @Schema(description = "완성된 집 목록")
        List<CollectionItem> collections,

        @Schema(description = "총 컬렉션 수", example = "5")
        int totalCount,

        @Schema(description = "활성 드림홈 존재 여부", example = "true")
        boolean activeGoalExists,

        @Schema(description = "진행 중인 드림홈 정보 (없으면 null)")
        InProgressInfo inProgress
) {

    /**
     * 진행 중인 드림홈 요약 정보 (컬렉션 화면 표시용)
     */
    @Schema(description = "진행 중인 드림홈 정보")
    public record InProgressInfo(
            @Schema(description = "드림홈 ID", example = "15")
            Long dreamHomeId,

            @Schema(description = "테마 코드", example = "CLASSIC")
            String themeCode,

            @Schema(description = "매물명", example = "강남 오피스텔")
            String propertyName,

            @Schema(description = "위치", example = "서울 강남구")
            String location,

            @Schema(description = "현재 단계 (1~11)", example = "4")
            int currentPhase,

            @Schema(description = "총 단계 수", example = "11")
            int totalPhases
    ) {
        private static final int TOTAL_PHASES = 11;

        /**
         * Map으로부터 InProgressInfo 생성 (Mapper 결과 변환용)
         */
        public static InProgressInfo fromMap(Map<String, Object> map) {
            if (map == null) return null;
            
            Long targetAmount = getLong(map, "target_amount");
            Long savedAmount = getLong(map, "current_saved_amount");
            int currentPhase = calculatePhase(savedAmount, targetAmount);

            return new InProgressInfo(
                    getLong(map, "dream_home_id"),
                    getString(map, "theme_code"),
                    getString(map, "property_name"),
                    getString(map, "location"),
                    currentPhase,
                    TOTAL_PHASES
            );
        }

        public static InProgressInfo fromMap(Map<String, Object> map, int currentPhase) {
            if (map == null) return null;
            int safePhase = Math.max(1, Math.min(TOTAL_PHASES, currentPhase));

            return new InProgressInfo(
                    getLong(map, "dream_home_id"),
                    getString(map, "theme_code"),
                    getString(map, "property_name"),
                    getString(map, "location"),
                    safePhase,
                    TOTAL_PHASES
            );
        }

        private static int calculatePhase(Long savedAmount, Long targetAmount) {
            if (targetAmount == null || targetAmount <= 0) return 1;
            long saved = savedAmount != null ? savedAmount : 0;
            double progress = (double) saved / targetAmount;
            int phase = (int) Math.floor(progress * TOTAL_PHASES) + 1;
            return Math.max(1, Math.min(TOTAL_PHASES, phase));
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
    }

    /**
     * 컬렉션 아이템 정보
     */
    @Schema(description = "컬렉션 아이템")
    public record CollectionItem(
            @Schema(description = "컬렉션 ID", example = "1")
            Long collectionId,

            @Schema(description = "테마 ID", example = "1")
            Integer themeId,

            @Schema(description = "테마 이름", example = "모던 아파트")
            String themeName,

            @Schema(description = "테마 코드", example = "CLASSIC")
            String themeCode,

            @Schema(description = "드림홈 ID (여정 조회용)", example = "10")
            Long dreamHomeId,

            @Schema(description = "매물명", example = "강남 오피스텔")
            String propertyName,

            @Schema(description = "위치", example = "서울 강남구")
            String location,

            @Schema(description = "목표 금액 (원)", example = "50000000")
            Long targetAmount,

            @Schema(description = "저축 기간 (일)", example = "180")
            Integer savingPeriodDays,

            @Schema(description = "완공일")
            LocalDateTime completedAt,

            @Schema(description = "대표 전시 여부", example = "true")
            Boolean isMainDisplay,

            @Schema(description = "완공까지 모은 총액 (원)", example = "50000000")
            Long totalSaved,

            @Schema(description = "집 이름", example = "우리 첫 보금자리")
            String houseName
    ) {

        /**
         * Map으로부터 CollectionItem 생성 (Mapper 결과 변환용)
         */
        public static CollectionItem fromMap(Map<String, Object> map) {
            return new CollectionItem(
                    getLong(map, "collection_id"),
                    getInt(map, "theme_id"),
                    getString(map, "theme_name"),
                    getString(map, "theme_code"),
                    getLong(map, "dream_home_id"),
                    getString(map, "property_name"),
                    getString(map, "location"),
                    getLong(map, "target_amount"),
                    getInt(map, "saving_period_days"),
                    getLocalDateTime(map, "completed_at"),
                    getBoolean(map, "is_main_display"),
                    getLong(map, "total_saved"),
                    getString(map, "house_name")
            );
        }

        private static Long getLong(Map<String, Object> map, String key) {
            Object val = map.get(key);
            if (val == null) return null;
            if (val instanceof Long l) return l;
            if (val instanceof Number n) return n.longValue();
            return null;
        }

        private static Integer getInt(Map<String, Object> map, String key) {
            Object val = map.get(key);
            if (val == null) return null;
            if (val instanceof Integer i) return i;
            if (val instanceof Number n) return n.intValue();
            return null;
        }

        private static String getString(Map<String, Object> map, String key) {
            Object val = map.get(key);
            return val != null ? val.toString() : null;
        }

        private static Boolean getBoolean(Map<String, Object> map, String key) {
            Object val = map.get(key);
            if (val == null) return null;
            if (val instanceof Boolean b) return b;
            if (val instanceof Number n) return n.intValue() != 0;
            return null;
        }

        private static LocalDateTime getLocalDateTime(Map<String, Object> map, String key) {
            Object val = map.get(key);
            if (val instanceof LocalDateTime ldt) return ldt;
            if (val instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
            return null;
        }
    }
}
