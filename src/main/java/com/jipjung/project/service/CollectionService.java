package com.jipjung.project.service;

import com.jipjung.project.controller.dto.response.CollectionResponse;
import com.jipjung.project.controller.dto.response.CollectionResponse.CollectionItem;
import com.jipjung.project.controller.dto.response.JourneyResponse;
import com.jipjung.project.controller.dto.response.JourneyResponse.*;
import com.jipjung.project.domain.User;
import com.jipjung.project.domain.UserCollection;
import com.jipjung.project.global.exception.BusinessException;
import com.jipjung.project.global.exception.ErrorCode;
import com.jipjung.project.repository.ApartmentMapper;
import com.jipjung.project.repository.CollectionMapper;
import com.jipjung.project.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 컬렉션 서비스
 * <p>
 * 완성된 집 컬렉션 관리 및 저축 여정 조회를 담당합니다.
 * <p>
 * 주요 기능:
 * - 완성된 집 목록 조회
 * - 저축 여정 상세 조회 (Phase별 이벤트)
 * - 대표 컬렉션 설정
 * - 목표 달성 시 자동 등록
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionService {

    private final CollectionMapper collectionMapper;
    private final UserMapper userMapper;
    private final ApartmentMapper apartmentMapper;

    // Phase 관련 상수 (PRD 3.1.3 참조)
    private static final int HOUSE_PHASES = 6;  // 집 짓기 단계
    private static final int FURNITURE_PHASES = 5;  // 가구 배치 단계
    private static final int TOTAL_PHASES = HOUSE_PHASES + FURNITURE_PHASES;

    private static final List<String> HOUSE_PHASE_NAMES = List.of(
            "터파기", "기초 공사", "골조 공사", "외벽 마감", "지붕 공사", "집 완공"
    );

    private static final List<String> FURNITURE_PHASE_NAMES = List.of(
            "바닥·벽 정돈", "휴식 공간", "기능 더하기", "분위기 완성", "인테리어 완성"
    );

    private static final String EVENT_DEPOSIT = "DEPOSIT";
    private static final String EVENT_LEVEL_UP = "LEVEL_UP";
    private static final String EVENT_HOUSE_COMPLETE = "HOUSE_COMPLETE";
    private static final String EVENT_FURNITURE_UNLOCKED = "FURNITURE_UNLOCKED";
    private static final String EVENT_JOURNEY_COMPLETE = "JOURNEY_COMPLETE";

    // =========================================================================
    // 컬렉션 목록 조회
    // =========================================================================

    /**
     * 사용자의 완성된 집 목록 조회
     *
     * @param userId 사용자 ID
     * @return 컬렉션 목록 응답
     */
    @Transactional(readOnly = true)
    public CollectionResponse getCollections(Long userId) {
        List<Map<String, Object>> rawCollections = collectionMapper.findByUserId(userId);
        boolean hasActiveGoal = collectionMapper.hasActiveDreamHome(userId);

        List<CollectionItem> collections = rawCollections.stream()
                .map(CollectionItem::fromMap)
                .toList();

        return new CollectionResponse(collections, collections.size(), hasActiveGoal);
    }

    // =========================================================================
    // 저축 여정 조회
    // =========================================================================

    /**
     * 저축 여정 상세 조회 (리플레이용)
     * <p>
     * 저축 이벤트를 Phase별로 그룹핑하여 반환합니다.
     * Phase 경계는 목표 금액을 11등분하여 계산합니다.
     *
     * @param userId       로그인 사용자 ID
     * @param collectionId 컬렉션 ID
     * @return 저축 여정 상세 응답
     * @throws BusinessException 컬렉션 미존재 또는 접근 권한 없음
     */
    @Transactional(readOnly = true)
    public JourneyResponse getJourney(Long userId, Long collectionId) {
        UserCollection collection = validateOwnership(userId, collectionId);

        if (collection.getDreamHomeId() == null) {
            throw new BusinessException(ErrorCode.COLLECTION_JOURNEY_NOT_AVAILABLE,
                    "이 컬렉션은 여정 정보가 없습니다.");
        }

        // 저축 이벤트 조회
        List<Map<String, Object>> events = collectionMapper.findJourneyEvents(collection.getDreamHomeId());

        // 컬렉션 기본 정보 조립
        Map<String, Object> collectionDetails =
                collectionMapper.findDetailByUserIdAndCollectionId(userId, collectionId);
        if (collectionDetails == null) {
            collectionDetails = Collections.emptyMap();
        }

        Long targetAmount = getLong(collectionDetails, "target_amount");
        if (targetAmount == null) {
            throw new BusinessException(ErrorCode.DREAM_HOME_NOT_FOUND,
                    "드림홈 정보를 찾을 수 없습니다.");
        }

        LocalDate startDate = getLocalDate(collectionDetails, "start_date");
        LocalDate completedDate = collection.getCompletedAt() != null
                ? collection.getCompletedAt().toLocalDate()
                : LocalDate.now();
        if (startDate == null) {
            startDate = completedDate;
        }

        CollectionInfo collectionInfo = new CollectionInfo(
                collectionId,
                getString(collectionDetails, "theme_name"),
                getString(collectionDetails, "theme_code"),
                getString(collectionDetails, "property_name"),
                getString(collectionDetails, "location")
        );

        // 여정 요약
        JourneySummary summary = buildJourneySummary(startDate, completedDate, events, targetAmount);

        // Phase별 그룹핑
        List<PhaseInfo> phases = buildPhasesFromEvents(events, Math.max(1L, targetAmount), collectionInfo.themeCode());

        return new JourneyResponse(collectionInfo, summary, phases);
    }

    // =========================================================================
    // 대표 컬렉션 설정
    // =========================================================================

    /**
     * 대표 컬렉션 설정
     * <p>
     * 기존 대표 컬렉션을 해제하고 새 대표를 설정합니다.
     * 트랜잭션으로 원자성 보장.
     *
     * @param userId       로그인 사용자 ID
     * @param collectionId 컬렉션 ID
     * @throws BusinessException 컬렉션 미존재 또는 접근 권한 없음
     */
    @Transactional
    public void setMainDisplay(Long userId, Long collectionId) {
        validateOwnership(userId, collectionId);

        // 기존 대표 해제 → 새 대표 설정 (원자적)
        collectionMapper.clearMainDisplay(userId);
        collectionMapper.setMainDisplay(collectionId);

        log.info("Main display set. userId: {}, collectionId: {}", userId, collectionId);
    }

    // =========================================================================
    // 목표 달성 시 자동 등록 (DreamHomeService에서 호출)
    // =========================================================================

    /**
     * 목표 달성 시 자동 컬렉션 등록
     * <p>
     * 멱등성 보장: UNIQUE(dream_home_id) 제약으로 중복 시 무시
     *
     * @param userId        사용자 ID
     * @param dreamHome     완료된 드림홈
     * @param newSavedAmount 최종 저축 금액
     */
    @Transactional
    public void registerOnCompletion(Long userId, com.jipjung.project.domain.DreamHome dreamHome, long newSavedAmount) {
        if (dreamHome == null || dreamHome.getDreamHomeId() == null) {
            return;
        }

        if (collectionMapper.findByDreamHomeId(dreamHome.getDreamHomeId()) != null) {
            return;
        }

        try {
            User user = userMapper.findById(userId);
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }

            String houseName = apartmentMapper.findByAptSeq(dreamHome.getAptSeq())
                    .map(a -> a.getAptNm())
                    .orElse(null);

            LocalDateTime completedAt = LocalDateTime.now();
            int durationDays = calculateDurationDays(dreamHome.getStartDate(), completedAt.toLocalDate());

            UserCollection collection = UserCollection.builder()
                    .userId(userId)
                    .themeId(user.getSelectedThemeId() != null ? user.getSelectedThemeId() : 1)
                    .dreamHomeId(dreamHome.getDreamHomeId())
                    .houseName(houseName)
                    .totalSaved(newSavedAmount)
                    .durationDays(durationDays)
                    .completedAt(completedAt)
                    .isMainDisplay(false)
                    .build();

            collectionMapper.insert(collection);
            log.info("Collection registered. userId: {}, dreamHomeId: {}, collectionId: {}",
                    userId, dreamHome.getDreamHomeId(), collection.getCollectionId());

        } catch (DuplicateKeyException e) {
            // 멱등성: 이미 등록된 경우 무시
            log.debug("Collection already exists for dreamHomeId: {}", dreamHome.getDreamHomeId());
        }
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    /**
     * 컬렉션 소유권 검증 (IDOR 방지)
     */
    private UserCollection validateOwnership(Long userId, Long collectionId) {
        UserCollection collection = collectionMapper.findById(collectionId);
        if (collection == null) {
            throw new BusinessException(ErrorCode.COLLECTION_NOT_FOUND);
        }
        if (!collection.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.COLLECTION_ACCESS_DENIED);
        }
        return collection;
    }

    /**
     * 여정 요약 정보 생성
     */
    private JourneySummary buildJourneySummary(
            LocalDate startDate,
            LocalDate completedDate,
            List<Map<String, Object>> events,
            Long targetAmount
    ) {
        int totalDays = (int) ChronoUnit.DAYS.between(startDate, completedDate);

        long totalDeposits = events.stream()
                .filter(e -> EVENT_DEPOSIT.equals(getString(e, "event_type")))
                .count();

        return new JourneySummary(
                startDate,
                completedDate,
                totalDays,
                (int) totalDeposits,
                targetAmount
        );
    }

    /**
     * 이벤트를 Phase별로 그룹핑 (단방향 진행 + 시스템 이벤트 생성)
     * <p>
     * 규칙:
     * - 단계 진행은 단방향(최고 누적합 기준)입니다. WITHDRAW로 누적이 줄어도 단계는 되돌아가지 않습니다.
     * - 한 번의 DEPOSIT로 여러 단계 점프 시, 점프한 각 단계마다 LEVEL_UP 이벤트를 생성합니다.
     */
    private List<PhaseInfo> buildPhasesFromEvents(List<Map<String, Object>> rawEvents,
                                                 long targetAmount,
                                                 String themeCode) {
        long phaseThreshold = Math.max(1L, targetAmount / TOTAL_PHASES);
        Map<Integer, List<JourneyEvent>> phaseEvents = new LinkedHashMap<>();

        for (int i = 1; i <= TOTAL_PHASES; i++) {
            phaseEvents.put(i, new ArrayList<>());
        }

        LocalDateTime[] phaseReachedAt = new LocalDateTime[TOTAL_PHASES + 1];
        Long[] phaseCumulativeAmount = new Long[TOTAL_PHASES + 1];

        long maxCumulativeSoFar = 0L;
        int currentPhase = 1;

        for (Map<String, Object> eventMap : rawEvents) {
            JourneyEvent event = JourneyEvent.fromMap(eventMap);

            long cumulative = nullToZero(event.cumulativeTotal());
            long nextMax = Math.max(maxCumulativeSoFar, cumulative);
            int nextPhase = calculatePhase(nextMax, phaseThreshold);

            if (nextPhase > currentPhase) {
                // 단계 점프: 각 단계별 시스템 이벤트 생성 (LEVEL_UP 등)
                for (int phase = currentPhase + 1; phase <= nextPhase; phase++) {
                    LocalDateTime date = event.date();
                    long phaseCumulative = nextMax;

                    addToPhase(
                            phaseEvents,
                            phase,
                            new JourneyEvent(null, EVENT_LEVEL_UP, date, 0L, buildLevelUpMemo(phase), phaseCumulative),
                            phaseReachedAt,
                            phaseCumulativeAmount,
                            phaseCumulative
                    );

                    if (phase == HOUSE_PHASES) {
                        addToPhase(
                                phaseEvents,
                                phase,
                                new JourneyEvent(null, EVENT_HOUSE_COMPLETE, date, 0L, "🏠 드디어 집 완공!", phaseCumulative),
                                phaseReachedAt,
                                phaseCumulativeAmount,
                                phaseCumulative
                        );
                    }

                    if (phase > HOUSE_PHASES) {
                        addToPhase(
                                phaseEvents,
                                phase,
                                new JourneyEvent(null, EVENT_FURNITURE_UNLOCKED, date, 0L,
                                        "🛋️ 가구 레이어 해금: " + phaseNameOf(phase), phaseCumulative),
                                phaseReachedAt,
                                phaseCumulativeAmount,
                                phaseCumulative
                        );
                    }

                    if (phase == TOTAL_PHASES) {
                        addToPhase(
                                phaseEvents,
                                phase,
                                new JourneyEvent(null, EVENT_JOURNEY_COMPLETE, date, 0L, "🥳 인테리어까지 완성!", phaseCumulative),
                                phaseReachedAt,
                                phaseCumulativeAmount,
                                phaseCumulative
                        );
                    }
                }
            }

            maxCumulativeSoFar = nextMax;
            currentPhase = nextPhase;

            // 실제 저축/인출 이벤트는 현재 단계(최고치 기준)에 포함
            addToPhase(
                    phaseEvents,
                    currentPhase,
                    event,
                    phaseReachedAt,
                    phaseCumulativeAmount,
                    maxCumulativeSoFar
            );
        }

        List<PhaseInfo> result = new ArrayList<>();
        for (int i = 1; i <= TOTAL_PHASES; i++) {
            int stageNumber = i <= HOUSE_PHASES ? i : (i - HOUSE_PHASES);

            result.add(new PhaseInfo(
                    i,
                    phaseNameOf(i),
                    themeCode,
                    stageNumber,
                    phaseReachedAt[i],
                    phaseCumulativeAmount[i],
                    phaseEvents.get(i)
            ));
        }

        return result;
    }

    /**
     * 누적 금액으로 Phase 계산 (1-11)
     */
    private int calculatePhase(long cumulativeAmount, long phaseThreshold) {
        int phase = (int) (cumulativeAmount / phaseThreshold) + 1;
        return Math.max(1, Math.min(phase, TOTAL_PHASES));
    }

    /**
     * 완공까지 걸린 일수 계산
     */
    private int calculateDurationDays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) return 0;
        return (int) ChronoUnit.DAYS.between(startDate, endDate);
    }

    private static void addToPhase(
            Map<Integer, List<JourneyEvent>> phaseEvents,
            int phase,
            JourneyEvent event,
            LocalDateTime[] phaseReachedAt,
            Long[] phaseCumulativeAmount,
            long cumulativeAmount
    ) {
        if (phase < 1 || phase > TOTAL_PHASES) {
            return;
        }

        phaseEvents.get(phase).add(event);

        if (phaseReachedAt[phase] == null && event.date() != null) {
            phaseReachedAt[phase] = event.date();
        }

        Long current = phaseCumulativeAmount[phase];
        if (current == null) {
            phaseCumulativeAmount[phase] = cumulativeAmount;
        } else {
            phaseCumulativeAmount[phase] = Math.max(current, cumulativeAmount);
        }
    }

    private static String buildLevelUpMemo(int phase) {
        return "🎉 레벨업! " + phaseNameOf(phase) + " 시작";
    }

    private static String phaseNameOf(int phase) {
        if (phase <= HOUSE_PHASES) {
            return HOUSE_PHASE_NAMES.get(phase - 1);
        }
        return FURNITURE_PHASE_NAMES.get(phase - HOUSE_PHASES - 1);
    }

    private static long nullToZero(Long val) {
        return val != null ? val : 0L;
    }

    // Map 헬퍼
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

    private static LocalDate getLocalDate(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof LocalDate ld) return ld;
        if (val instanceof java.sql.Date d) return d.toLocalDate();
        if (val instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().toLocalDate();
        return null;
    }
}
