package com.jipjung.project.service;

import com.jipjung.project.controller.dto.response.CollectionResponse;
import com.jipjung.project.controller.dto.response.CollectionResponse.CollectionItem;
import com.jipjung.project.controller.dto.response.JourneyResponse;
import com.jipjung.project.controller.dto.response.JourneyResponse.CollectionInfo;
import com.jipjung.project.controller.dto.response.JourneyResponse.JourneyEvent;
import com.jipjung.project.controller.dto.response.JourneyResponse.JourneySummary;
import com.jipjung.project.controller.dto.response.JourneyResponse.PhaseInfo;
import com.jipjung.project.domain.ActivityType;
import com.jipjung.project.domain.Apartment;
import com.jipjung.project.domain.DreamHome;
import com.jipjung.project.domain.DreamHomeStatus;
import com.jipjung.project.domain.User;
import com.jipjung.project.domain.UserCollection;
import com.jipjung.project.global.exception.BusinessException;
import com.jipjung.project.global.exception.ErrorCode;
import com.jipjung.project.repository.AiConversationMapper;
import com.jipjung.project.repository.ApartmentMapper;
import com.jipjung.project.repository.CollectionMapper;
import com.jipjung.project.repository.DailyActivityMapper;
import com.jipjung.project.repository.DreamHomeMapper;
import com.jipjung.project.repository.StreakMilestoneRewardMapper;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private final DreamHomeMapper dreamHomeMapper;
    private final AiConversationMapper aiConversationMapper;
    private final DailyActivityMapper dailyActivityMapper;
    private final StreakMilestoneRewardMapper milestoneRewardMapper;

    // Phase 관련 상수 (PRD 3.1.3 참조)
    private static final int HOUSE_PHASES = 6;  // 집 짓기 단계
    private static final int FURNITURE_PHASES = 5;  // 가구 배치 단계
    private static final int TOTAL_PHASES = HOUSE_PHASES + FURNITURE_PHASES;

    private static final int DEFAULT_THEME_ID = 1;

    private static final List<String> HOUSE_PHASE_NAMES = List.of(
            "터파기", "기초 공사", "골조 공사", "외벽 마감", "지붕 공사", "집 완공"
    );

    private static final List<String> FURNITURE_PHASE_NAMES = List.of(
            "바닥·벽 정돈", "휴식 공간", "기능 더하기", "분위기 완성", "인테리어 완성"
    );

    private static final String EVENT_SAVINGS_DEPOSIT = "SAVINGS_DEPOSIT";
    private static final String EVENT_SAVINGS_WITHDRAW = "SAVINGS_WITHDRAW";
    private static final String EVENT_AI_JUDGMENT = "AI_JUDGMENT";
    private static final String EVENT_STREAK_PREFIX = "STREAK_";
    private static final String EVENT_STREAK_MILESTONE = "STREAK_MILESTONE";
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
    @Transactional
    public CollectionResponse getCollections(Long userId) {
        checkAndUpdateCompletionByExp(userId);

        List<Map<String, Object>> rawCollections = collectionMapper.findByUserId(userId);
        Map<String, Object> inProgressData = collectionMapper.findInProgressSummary(userId);
        boolean hasActiveGoal = inProgressData != null || collectionMapper.hasActiveDreamHome(userId);

        List<CollectionItem> collections = rawCollections.stream()
                .map(CollectionItem::fromMap)
                .toList();

        // 진행 중인 드림홈 정보 조회 (XP 기반 단계 계산)
        CollectionResponse.InProgressInfo inProgress = buildInProgressInfo(userId, inProgressData);

        return new CollectionResponse(collections, collections.size(), hasActiveGoal, inProgress);
    }

    // =========================================================================
    // 진행 중 드림홈 여정 조회
    // =========================================================================

    /**
     * 진행 중인 드림홈의 저축 여정 조회
     * <p>
     * 현재 ACTIVE 상태의 드림홈을 기준으로 여정 데이터를 조회합니다.
     * 완성된 컬렉션 여정과 동일한 응답 형식을 사용합니다.
     *
     * @param userId 사용자 ID
     * @return 저축 여정 응답 (완성된 여정과 동일한 형식)
     * @throws BusinessException 진행 중인 드림홈이 없는 경우
     */
    @Transactional
    public JourneyResponse getInProgressJourney(Long userId) {
        checkAndUpdateCompletionByExp(userId);

        Map<String, Object> inProgressData = collectionMapper.findInProgressSummary(userId);
        if (inProgressData == null) {
            throw new BusinessException(ErrorCode.DREAM_HOME_NOT_FOUND,
                    "진행 중인 드림홈이 없습니다.");
        }

        Long dreamHomeId = getLong(inProgressData, "dream_home_id");
        if (dreamHomeId == null) {
            throw new BusinessException(ErrorCode.DREAM_HOME_NOT_FOUND);
        }
        DreamHome dreamHome = dreamHomeMapper.findById(dreamHomeId);

        Long targetAmount = dreamHome != null ? dreamHome.getTargetAmount() : getLong(inProgressData, "target_amount");
        int targetExp = ExpPolicy.calculateTargetExp(targetAmount);
        int safeTargetExp = Math.max(1, targetExp);

        String themeCode = Objects.requireNonNullElse(getString(inProgressData, "theme_code"), "CLASSIC");
        String propertyName = getString(inProgressData, "property_name");
        String location = getString(inProgressData, "location");

        LocalDateTime startAt = resolveJourneyStart(dreamHome);
        LocalDateTime endAt = LocalDateTime.now();

        JourneyEventData eventData = loadJourneyEvents(userId, dreamHomeId, startAt, endAt);
        JourneyPhaseResult phaseResult = buildPhasesFromEvents(eventData.events(), safeTargetExp, themeCode);
        int totalExp = phaseResult.totalExp();
        int currentPhase = phaseResult.currentPhase();

        LocalDate startDate = resolveJourneyStartDate(dreamHome, eventData.events());

        CollectionInfo collectionInfo = new CollectionInfo(
                null, // 진행 중이므로 collectionId 없음
                null, // themeName은 optional
                themeCode,
                propertyName,
                location
        );

        // 진행 중이므로 완료일은 null, 현재까지 소요 기간
        int totalDays = Math.max(0, (int) ChronoUnit.DAYS.between(startDate, LocalDate.now()));

        JourneySummary summary = buildJourneySummary(
                startDate,
                null,
                totalDays,
                eventData.totalDeposits(),
                targetAmount,
                targetExp,
                totalExp,
                currentPhase
        );

        return new JourneyResponse(collectionInfo, summary, phaseResult.phases());
    }

    // =========================================================================
    // 저축 여정 조회
    // =========================================================================

    /**
     * 여정 상세 조회 (리플레이용)
     * <p>
     * 저축 이벤트를 Phase별로 그룹핑하여 반환합니다.
     * Phase 경계는 목표 XP 대비 누적 XP 비율로 계산합니다.
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

        Map<String, Object> detailMap =
                collectionMapper.findDetailByUserIdAndCollectionId(userId, collectionId);
        if (detailMap == null) {
            throw new BusinessException(ErrorCode.DREAM_HOME_NOT_FOUND,
                    "드림홈 정보를 찾을 수 없습니다.");
        }

        JourneyCollectionDetail detail = JourneyCollectionDetail.fromMap(detailMap);
        if (detail.targetAmount() == null) {
            throw new BusinessException(ErrorCode.DREAM_HOME_NOT_FOUND,
                    "드림홈 정보를 찾을 수 없습니다.");
        }

        LocalDate completedDate = collection.getCompletedAt() != null
                ? collection.getCompletedAt().toLocalDate()
                : LocalDate.now();
        LocalDate startDate = detail.startDateOr(completedDate);

        CollectionInfo collectionInfo = new CollectionInfo(
                collectionId,
                detail.themeName(),
                detail.themeCode(),
                detail.propertyName(),
                detail.location()
        );

        DreamHome dreamHome = dreamHomeMapper.findById(collection.getDreamHomeId());
        Long targetAmount = detail.targetAmount();
        int targetExp = ExpPolicy.calculateTargetExp(targetAmount);
        int safeTargetExp = Math.max(1, targetExp);

        LocalDateTime startAt = resolveJourneyStart(dreamHome, startDate);
        LocalDateTime endAt = collection.getCompletedAt() != null
                ? collection.getCompletedAt()
                : LocalDateTime.now();

        JourneyEventData eventData = loadJourneyEvents(userId, collection.getDreamHomeId(), startAt, endAt);
        JourneyPhaseResult phaseResult = buildPhasesFromEvents(eventData.events(), safeTargetExp, collectionInfo.themeCode());
        int totalExp = phaseResult.totalExp();
        int currentPhase = phaseResult.currentPhase();

        JourneySummary summary = buildJourneySummary(
                startDate,
                completedDate,
                calculateDurationDays(startDate, completedDate),
                eventData.totalDeposits(),
                targetAmount,
                targetExp,
                totalExp,
                currentPhase
        );

        return new JourneyResponse(collectionInfo, summary, phaseResult.phases());
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
    // 목표 XP 달성 체크
    // =========================================================================

    /**
     * 목표 XP 달성 여부 확인 및 완료 처리
     * <p>
     * 총 XP가 목표 XP에 도달하면 드림홈을 COMPLETED로 전환하고
     * 컬렉션을 자동 등록합니다.
     */
    @Transactional
    public GoalCompletionResult checkAndUpdateCompletionByExp(Long userId) {
        DreamHome dreamHome = dreamHomeMapper.findActiveByUserId(userId);
        if (dreamHome == null) {
            return new GoalCompletionResult(false, false, null);
        }
        return checkAndUpdateCompletionByExp(userId, dreamHome, nullToZero(dreamHome.getCurrentSavedAmount()));
    }

    /**
     * 목표 XP 달성 여부 확인 및 완료 처리 (드림홈 지정)
     */
    @Transactional
    public GoalCompletionResult checkAndUpdateCompletionByExp(Long userId, DreamHome dreamHome, long currentSavedAmount) {
        if (dreamHome == null || dreamHome.getDreamHomeId() == null) {
            return new GoalCompletionResult(false, false, null);
        }

        boolean wasCompleted = dreamHome.getStatus() == DreamHomeStatus.COMPLETED;
        int targetExp = ExpPolicy.calculateTargetExp(dreamHome.getTargetAmount());
        if (targetExp <= 0) {
            return new GoalCompletionResult(wasCompleted, false, null);
        }

        int safeTargetExp = Math.max(1, targetExp);
        LocalDateTime startAt = resolveJourneyStart(dreamHome);
        JourneyEventData eventData = loadJourneyEvents(userId, dreamHome.getDreamHomeId(), startAt, LocalDateTime.now());
        JourneyProgressSnapshot snapshot = calculateProgressSnapshot(eventData.events(), safeTargetExp);
        int totalExp = snapshot.totalExp();

        boolean isCompleted = wasCompleted || totalExp >= targetExp;
        boolean justCompleted = !wasCompleted && totalExp >= targetExp;
        Long completedCollectionId = null;

        if (justCompleted) {
            dreamHomeMapper.updateStatus(dreamHome.getDreamHomeId(), DreamHomeStatus.COMPLETED);
            completedCollectionId = registerOnCompletion(userId, dreamHome, currentSavedAmount);
        }

        return new GoalCompletionResult(isCompleted, justCompleted, completedCollectionId);
    }

    /**
     * 목표 XP 진행 현황 조회 (대시보드/요약용)
     */
    @Transactional(readOnly = true)
    public GoalProgress getGoalProgress(Long userId, DreamHome dreamHome) {
        if (dreamHome == null || dreamHome.getDreamHomeId() == null) {
            return GoalProgress.empty();
        }

        int targetExp = ExpPolicy.calculateTargetExp(dreamHome.getTargetAmount());
        if (targetExp <= 0) {
            return GoalProgress.empty();
        }

        int safeTargetExp = Math.max(1, targetExp);
        LocalDateTime startAt = resolveJourneyStart(dreamHome);
        JourneyEventData eventData = loadJourneyEvents(userId, dreamHome.getDreamHomeId(), startAt, LocalDateTime.now());
        JourneyProgressSnapshot snapshot = calculateProgressSnapshot(eventData.events(), safeTargetExp);

        int totalExp = snapshot.totalExp();
        int currentPhase = snapshot.currentPhase();
        double percent = Math.min(100.0, Math.max(0.0, (totalExp * 100.0) / targetExp));
        double roundedPercent = Math.round(percent * 10.0) / 10.0;

        return new GoalProgress(targetExp, totalExp, currentPhase, roundedPercent);
    }

    /**
     * 목표 XP 진행 현황 조회 (활성 드림홈 기준)
     */
    @Transactional(readOnly = true)
    public GoalProgress getGoalProgress(Long userId) {
        DreamHome dreamHome = dreamHomeMapper.findActiveByUserId(userId);
        if (dreamHome == null) {
            return GoalProgress.empty();
        }
        return getGoalProgress(userId, dreamHome);
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
    public Long registerOnCompletion(Long userId, DreamHome dreamHome, long newSavedAmount) {
        if (dreamHome == null || dreamHome.getDreamHomeId() == null) {
            return null;
        }

        UserCollection existingCollection = collectionMapper.findByDreamHomeId(dreamHome.getDreamHomeId());
        if (existingCollection != null) {
            return existingCollection.getCollectionId();
        }

        User user = requireUser(userId);
        UserCollection collection = buildCompletedCollection(userId, user, dreamHome, newSavedAmount);

        try {
            collectionMapper.insert(collection);
            log.info("Collection registered. userId: {}, dreamHomeId: {}, collectionId: {}",
                    userId, dreamHome.getDreamHomeId(), collection.getCollectionId());
            return collection.getCollectionId();
        } catch (DuplicateKeyException e) {
            // 멱등성: 이미 등록된 경우 무시 (경쟁 조건 포함)
            log.debug("Collection already exists for dreamHomeId: {}", dreamHome.getDreamHomeId());
            UserCollection duplicate = collectionMapper.findByDreamHomeId(dreamHome.getDreamHomeId());
            return duplicate != null ? duplicate.getCollectionId() : null;
        }
    }

    private User requireUser(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private UserCollection buildCompletedCollection(Long userId, User user, DreamHome dreamHome, long newSavedAmount) {
        Integer themeId = user.getSelectedThemeId() != null ? user.getSelectedThemeId() : DEFAULT_THEME_ID;

        LocalDateTime completedAt = LocalDateTime.now();
        int durationDays = calculateDurationDays(dreamHome.getStartDate(), completedAt.toLocalDate());

        return UserCollection.builder()
                .userId(userId)
                .themeId(themeId)
                .dreamHomeId(dreamHome.getDreamHomeId())
                .houseName(resolveHouseName(dreamHome))
                .totalSaved(newSavedAmount)
                .durationDays(durationDays)
                .completedAt(completedAt)
                .isMainDisplay(false)
                .build();
    }

    private String resolveHouseName(DreamHome dreamHome) {
        String houseName = dreamHome.getHouseName();
        if (houseName != null && !houseName.isBlank()) {
            return houseName;
        }
        if (dreamHome.getAptSeq() == null) {
            return null;
        }
        return apartmentMapper.findByAptSeq(dreamHome.getAptSeq())
                .map(Apartment::getAptNm)
                .orElse(null);
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
        if (!Objects.equals(collection.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.COLLECTION_ACCESS_DENIED);
        }
        return collection;
    }

    private CollectionResponse.InProgressInfo buildInProgressInfo(Long userId, Map<String, Object> inProgressData) {
        if (inProgressData == null) {
            return null;
        }

        Long dreamHomeId = getLong(inProgressData, "dream_home_id");
        if (dreamHomeId == null) {
            return CollectionResponse.InProgressInfo.fromMap(inProgressData);
        }

        DreamHome dreamHome = dreamHomeMapper.findById(dreamHomeId);
        Long targetAmount = dreamHome != null ? dreamHome.getTargetAmount() : getLong(inProgressData, "target_amount");
        int targetExp = ExpPolicy.calculateTargetExp(targetAmount);
        int safeTargetExp = Math.max(1, targetExp);

        LocalDateTime startAt = resolveJourneyStart(dreamHome);
        JourneyEventData eventData = loadJourneyEvents(userId, dreamHomeId, startAt, LocalDateTime.now());
        JourneyProgressSnapshot snapshot = calculateProgressSnapshot(eventData.events(), safeTargetExp);

        return CollectionResponse.InProgressInfo.fromMap(inProgressData, snapshot.currentPhase());
    }

    /**
     * 여정 요약 정보 생성
     */
    private JourneySummary buildJourneySummary(
            LocalDate startDate,
            LocalDate completedDate,
            int totalDays,
            int totalDeposits,
            Long targetAmount,
            int targetExp,
            int totalExp,
            int currentPhase
    ) {
        return new JourneySummary(
                startDate,
                completedDate,
                totalDays,
                totalDeposits,
                targetAmount,
                targetExp > 0 ? targetExp : null,
                totalExp,
                currentPhase
        );
    }

    private JourneyEventData loadJourneyEvents(
            Long userId,
            Long dreamHomeId,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        LocalDateTime safeStart = startAt != null ? startAt : LocalDateTime.now();
        LocalDateTime safeEnd = endAt != null ? endAt : LocalDateTime.now();
        if (safeEnd.isBefore(safeStart)) {
            LocalDateTime tmp = safeStart;
            safeStart = safeEnd;
            safeEnd = tmp;
        }

        List<JourneyXpEvent> events = new ArrayList<>();
        int totalDeposits = 0;

        List<Map<String, Object>> savingsEvents = collectionMapper.findJourneyEvents(dreamHomeId);
        for (Map<String, Object> raw : savingsEvents) {
            LocalDateTime date = getLocalDateTime(raw, "date");
            if (!isWithinRange(date, safeStart, safeEnd)) {
                continue;
            }

            String saveType = getString(raw, "event_type");
            Long amount = getLong(raw, "amount");
            String memo = getString(raw, "memo");

            boolean isDeposit = "DEPOSIT".equalsIgnoreCase(saveType);
            int expChange = isDeposit ? ExpPolicy.calculateSavingsExp(amount) : 0;
            if (isDeposit) {
                totalDeposits++;
            }

            String eventType = isDeposit ? EVENT_SAVINGS_DEPOSIT : EVENT_SAVINGS_WITHDRAW;
            events.add(new JourneyXpEvent(
                    getLong(raw, "event_id"),
                    eventType,
                    date,
                    expChange,
                    memo,
                    amount
            ));
        }

        List<Map<String, Object>> aiEvents = aiConversationMapper.findJudgedEventsByUserIdAndDateRange(
                userId, safeStart, safeEnd
        );
        for (Map<String, Object> raw : aiEvents) {
            LocalDateTime date = getLocalDateTime(raw, "updated_at");
            if (!isWithinRange(date, safeStart, safeEnd)) {
                continue;
            }
            int expChange = nullToZero(getInt(raw, "exp_change"));
            String result = getString(raw, "judgment_result");
            Integer score = getInt(raw, "judgment_score");
            String memo = buildAiMemo(result, score);

            events.add(new JourneyXpEvent(
                    getLong(raw, "conversation_id"),
                    EVENT_AI_JUDGMENT,
                    date,
                    expChange,
                    memo,
                    null
            ));
        }

        List<Map<String, Object>> activityEvents = dailyActivityMapper.findExpEventsByUserIdAndDateRange(
                userId, safeStart, safeEnd
        );
        for (Map<String, Object> raw : activityEvents) {
            LocalDateTime date = getLocalDateTime(raw, "created_at");
            if (!isWithinRange(date, safeStart, safeEnd)) {
                continue;
            }
            String activityType = getString(raw, "activity_type");
            int expEarned = nullToZero(getInt(raw, "exp_earned"));
            String memo = buildActivityMemo(activityType);
            String eventType = EVENT_STREAK_PREFIX + normalizeEventType(activityType);

            events.add(new JourneyXpEvent(
                    getLong(raw, "activity_id"),
                    eventType,
                    date,
                    expEarned,
                    memo,
                    null
            ));
        }

        List<Map<String, Object>> milestoneEvents = milestoneRewardMapper.findRewardsByUserIdAndDateRange(
                userId, safeStart, safeEnd
        );
        for (Map<String, Object> raw : milestoneEvents) {
            LocalDateTime date = getLocalDateTime(raw, "claimed_at");
            if (!isWithinRange(date, safeStart, safeEnd)) {
                continue;
            }
            int expReward = nullToZero(getInt(raw, "exp_reward"));
            int milestoneDays = nullToZero(getInt(raw, "milestone_days"));
            String memo = milestoneDays > 0
                    ? "마일스톤 " + milestoneDays + "일 보상"
                    : "마일스톤 보상";

            events.add(new JourneyXpEvent(
                    getLong(raw, "reward_id"),
                    EVENT_STREAK_MILESTONE,
                    date,
                    expReward,
                    memo,
                    null
            ));
        }

        events.sort(Comparator.comparing(JourneyXpEvent::date)
                .thenComparing(event -> nullToZero(event.eventId()))
                .thenComparing(JourneyXpEvent::eventType, Comparator.nullsLast(Comparator.naturalOrder())));

        return new JourneyEventData(events, totalDeposits);
    }

    private JourneyProgressSnapshot calculateProgressSnapshot(List<JourneyXpEvent> events, int targetExp) {
        int cumulativeExp = 0;
        int currentPhase = 1;

        for (JourneyXpEvent event : events) {
            cumulativeExp = Math.max(0, cumulativeExp + nullToZero(event.expChange()));
            currentPhase = calculatePhase(cumulativeExp, targetExp);
        }

        return new JourneyProgressSnapshot(cumulativeExp, currentPhase);
    }

    private LocalDateTime resolveJourneyStart(DreamHome dreamHome) {
        if (dreamHome != null && dreamHome.getCreatedAt() != null) {
            return dreamHome.getCreatedAt();
        }
        if (dreamHome != null && dreamHome.getStartDate() != null) {
            return dreamHome.getStartDate().atStartOfDay();
        }
        return LocalDateTime.now();
    }

    private LocalDateTime resolveJourneyStart(DreamHome dreamHome, LocalDate fallbackDate) {
        if (dreamHome != null && dreamHome.getCreatedAt() != null) {
            return dreamHome.getCreatedAt();
        }
        if (fallbackDate != null) {
            return fallbackDate.atStartOfDay();
        }
        return LocalDateTime.now();
    }

    private LocalDate resolveJourneyStartDate(DreamHome dreamHome, List<JourneyXpEvent> events) {
        if (dreamHome != null && dreamHome.getStartDate() != null) {
            return dreamHome.getStartDate();
        }
        for (JourneyXpEvent event : events) {
            if (event.date() != null) {
                return event.date().toLocalDate();
            }
        }
        return LocalDate.now();
    }

    private static String buildAiMemo(String result, Integer score) {
        if (result == null && score == null) {
            return null;
        }
        String label = result;
        if (label == null) {
            label = "AI 판결";
        } else {
            label = switch (label) {
                case "REASONABLE" -> "합리적 소비";
                case "WASTE" -> "낭비";
                default -> label;
            };
        }
        if (score == null) {
            return "AI 판결: " + label;
        }
        return "AI 판결: " + label + " (" + score + "점)";
    }

    private static String buildActivityMemo(String activityType) {
        if (activityType == null) {
            return null;
        }
        try {
            return ActivityType.valueOf(activityType).getLabel();
        } catch (IllegalArgumentException e) {
            return activityType;
        }
    }

    private static String normalizeEventType(String rawType) {
        return rawType != null ? rawType.toUpperCase() : "UNKNOWN";
    }

    private static JourneyEvent toJourneyEvent(JourneyXpEvent event, int cumulativeExp) {
        return new JourneyEvent(
                event.eventId(),
                event.eventType(),
                event.date(),
                event.amount(),
                event.memo(),
                null,
                event.expChange(),
                cumulativeExp
        );
    }

    private static boolean isWithinRange(LocalDateTime date, LocalDateTime startAt, LocalDateTime endAt) {
        if (date == null) {
            return false;
        }
        if (startAt != null && date.isBefore(startAt)) {
            return false;
        }
        return endAt == null || !date.isAfter(endAt);
    }

    /**
     * 이벤트를 Phase별로 그룹핑 (XP 기준)
     * <p>
     * 규칙:
     * - 누적 XP 기준으로 Phase를 계산하며, XP 감소 시 단계도 내려갈 수 있습니다 (1 미만 불가).
     * - 한 번의 이벤트로 여러 단계 점프 시, 점프한 각 단계마다 LEVEL_UP 이벤트를 생성합니다.
     */
    private JourneyPhaseResult buildPhasesFromEvents(List<JourneyXpEvent> rawEvents,
                                                     int targetExp,
                                                     String themeCode) {
        List<JourneyXpEvent> events = rawEvents.stream()
                .filter(event -> event.date() != null)
                .sorted(Comparator.comparing(JourneyXpEvent::date)
                        .thenComparing(event -> nullToZero(event.eventId()))
                        .thenComparing(JourneyXpEvent::eventType, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        Map<Integer, List<JourneyEvent>> phaseEvents = new LinkedHashMap<>();

        for (int i = 1; i <= TOTAL_PHASES; i++) {
            phaseEvents.put(i, new ArrayList<>());
        }

        LocalDateTime[] phaseReachedAt = new LocalDateTime[TOTAL_PHASES + 1];
        Integer[] phaseCumulativeExp = new Integer[TOTAL_PHASES + 1];

        int cumulativeExp = 0;
        int currentPhase = 1;

        for (JourneyXpEvent event : events) {
            int expChange = nullToZero(event.expChange());
            cumulativeExp = Math.max(0, cumulativeExp + expChange);
            int nextPhase = calculatePhase(cumulativeExp, targetExp);

            if (nextPhase > currentPhase) {
                addPhaseJumpSystemEvents(
                        phaseEvents,
                        currentPhase,
                        nextPhase,
                        event,
                        phaseReachedAt,
                        phaseCumulativeExp,
                        cumulativeExp
                );
            }

            currentPhase = nextPhase;

            addToPhase(
                    phaseEvents,
                    currentPhase,
                    toJourneyEvent(event, cumulativeExp),
                    phaseReachedAt,
                    phaseCumulativeExp,
                    cumulativeExp
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
                    null,
                    phaseCumulativeExp[i],
                    phaseEvents.get(i)
            ));
        }

        return new JourneyPhaseResult(result, cumulativeExp, currentPhase);
    }

    private static void addPhaseJumpSystemEvents(
            Map<Integer, List<JourneyEvent>> phaseEvents,
            int currentPhase,
            int nextPhase,
            JourneyXpEvent triggerEvent,
            LocalDateTime[] phaseReachedAt,
            Integer[] phaseCumulativeExp,
            int cumulativeExp
    ) {
        // 단계 점프: 각 단계별 시스템 이벤트 생성 (LEVEL_UP 등)
        for (int phase = currentPhase + 1; phase <= nextPhase; phase++) {
            LocalDateTime date = triggerEvent.date();

            addToPhase(
                    phaseEvents,
                    phase,
                    systemEvent(EVENT_LEVEL_UP, date, buildLevelUpMemo(phase), cumulativeExp),
                    phaseReachedAt,
                    phaseCumulativeExp,
                    cumulativeExp
            );

            if (phase == HOUSE_PHASES) {
                addToPhase(
                        phaseEvents,
                        phase,
                        systemEvent(EVENT_HOUSE_COMPLETE, date, "🏠 드디어 집 완공!", cumulativeExp),
                        phaseReachedAt,
                        phaseCumulativeExp,
                        cumulativeExp
                );
            }

            if (phase > HOUSE_PHASES) {
                addToPhase(
                        phaseEvents,
                        phase,
                        systemEvent(
                                EVENT_FURNITURE_UNLOCKED,
                                date,
                                "🛋️ 가구 레이어 해금: " + phaseNameOf(phase),
                                cumulativeExp
                        ),
                        phaseReachedAt,
                        phaseCumulativeExp,
                        cumulativeExp
                );
            }

            if (phase == TOTAL_PHASES) {
                addToPhase(
                        phaseEvents,
                        phase,
                        systemEvent(EVENT_JOURNEY_COMPLETE, date, "🥳 인테리어까지 완성!", cumulativeExp),
                        phaseReachedAt,
                        phaseCumulativeExp,
                        cumulativeExp
                );
            }
        }
    }

    private static JourneyEvent systemEvent(String eventType, LocalDateTime date, String memo, int cumulativeExp) {
        return new JourneyEvent(null, eventType, date, null, memo, null, 0, cumulativeExp);
    }

    /**
     * 누적 XP로 Phase 계산 (1-11)
     */
    private int calculatePhase(int totalExp, int targetExp) {
        int safeTargetExp = Math.max(1, targetExp);
        long numerator = (long) Math.max(0, totalExp) * TOTAL_PHASES;
        int phase = (int) (numerator / safeTargetExp) + 1;
        return Math.max(1, Math.min(phase, TOTAL_PHASES));
    }

    /**
     * 완공까지 걸린 일수 계산
     */
    private int calculateDurationDays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) return 0;
        return Math.max(0, (int) ChronoUnit.DAYS.between(startDate, endDate));
    }

    private static void addToPhase(
            Map<Integer, List<JourneyEvent>> phaseEvents,
            int phase,
            JourneyEvent event,
            LocalDateTime[] phaseReachedAt,
            Integer[] phaseCumulativeExp,
            int cumulativeExp
    ) {
        if (phase < 1 || phase > TOTAL_PHASES) {
            return;
        }

        phaseEvents.get(phase).add(event);

        if (phaseReachedAt[phase] == null && event.date() != null) {
            phaseReachedAt[phase] = event.date();
        }

        Integer current = phaseCumulativeExp[phase];
        if (current == null) {
            phaseCumulativeExp[phase] = cumulativeExp;
        } else {
            phaseCumulativeExp[phase] = Math.max(current, cumulativeExp);
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

    private static int nullToZero(Integer val) {
        return val != null ? val : 0;
    }

    // Map 헬퍼
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

    private static LocalDate getLocalDate(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof LocalDate ld) return ld;
        if (val instanceof java.sql.Date d) return d.toLocalDate();
        if (val instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().toLocalDate();
        return null;
    }

    private static LocalDateTime getLocalDateTime(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof LocalDateTime ldt) return ldt;
        if (val instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        return null;
    }

    private record JourneyXpEvent(
            Long eventId,
            String eventType,
            LocalDateTime date,
            Integer expChange,
            String memo,
            Long amount
    ) {}

    private record JourneyEventData(List<JourneyXpEvent> events, int totalDeposits) {}

    private record JourneyPhaseResult(List<PhaseInfo> phases, int totalExp, int currentPhase) {}

    private record JourneyProgressSnapshot(int totalExp, int currentPhase) {}

    public record GoalCompletionResult(
            boolean isCompleted,
            boolean justCompleted,
            Long completedCollectionId
    ) {}

    public record GoalProgress(
            int targetExp,
            int totalExp,
            int currentPhase,
            double expProgress
    ) {
        public static GoalProgress empty() {
            return new GoalProgress(0, 0, 1, 0.0);
        }
    }

    private record JourneyCollectionDetail(
            Long targetAmount,
            LocalDate startDate,
            String themeName,
            String themeCode,
            String propertyName,
            String location
    ) {
        static JourneyCollectionDetail fromMap(Map<String, Object> map) {
            return new JourneyCollectionDetail(
                    getLong(map, "target_amount"),
                    getLocalDate(map, "start_date"),
                    getString(map, "theme_name"),
                    getString(map, "theme_code"),
                    getString(map, "property_name"),
                    getString(map, "location")
            );
        }

        LocalDate startDateOr(LocalDate fallback) {
            return startDate != null ? startDate : fallback;
        }
    }
}
