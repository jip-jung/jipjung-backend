package com.jipjung.project.service;

import com.jipjung.project.controller.dto.request.DreamHomeSetRequest;
import com.jipjung.project.controller.dto.request.SavingsRecordRequest;
import com.jipjung.project.controller.dto.response.DreamHomeSetResponse;
import com.jipjung.project.controller.dto.response.SavingsRecordResponse;
import com.jipjung.project.domain.*;
import com.jipjung.project.global.exception.ErrorCode;
import com.jipjung.project.global.exception.ResourceNotFoundException;
import com.jipjung.project.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 드림홈 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DreamHomeService {

    private final DreamHomeMapper dreamHomeMapper;
    private final SavingsHistoryMapper savingsHistoryMapper;
    private final ApartmentMapper apartmentMapper;
    private final UserMapper userMapper;
    private final GrowthLevelMapper growthLevelMapper;

    // 경험치 설정
    private static final long EXP_PER_UNIT = 100_000L;   // 10만원 단위
    private static final int EXP_AMOUNT = 10;            // 단위당 10 EXP
    private static final int MAX_EXP_PER_SAVINGS = 500;  // 건당 최대 500 EXP
    private static final int MAX_LEVEL = 7;

    // 레벨별 누적 경험치 임계값
    private static final int[] LEVEL_THRESHOLDS = {0, 100, 300, 600, 1000, 1500, 2100};

    // =========================================================================
    // POST /api/dream-home - 드림홈 설정
    // =========================================================================

    @Transactional
    public DreamHomeSetResponse setDreamHome(Long userId, DreamHomeSetRequest request) {
        // 1. 아파트 존재 확인
        Apartment apartment = apartmentMapper.findByAptSeqWithDeals(request.aptSeq())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.APARTMENT_NOT_FOUND));

        // 2. 기존 활성 드림홈 조회
        DreamHome existingDreamHome = dreamHomeMapper.findActiveByUserId(userId);

        DreamHome dreamHome;
        if (existingDreamHome != null) {
            // 3a. 기존 드림홈 있으면 UPDATE (저축액 유지)
            dreamHome = DreamHome.builder()
                    .dreamHomeId(existingDreamHome.getDreamHomeId())
                    .userId(userId)
                    .aptSeq(request.aptSeq())
                    .targetAmount(request.targetAmount())
                    .targetDate(request.targetDate())
                    .monthlyGoal(request.monthlyGoal())
                    .currentSavedAmount(existingDreamHome.getCurrentSavedAmount())  // 저축액 유지
                    .startDate(LocalDate.now())
                    .status(DreamHomeStatus.ACTIVE)
                    .build();
            dreamHomeMapper.updateDreamHome(dreamHome);
            log.info("Dream home updated. userId: {}, dreamHomeId: {}, aptSeq: {}",
                    userId, dreamHome.getDreamHomeId(), request.aptSeq());
        } else {
            // 3b. 기존 드림홈 없으면 INSERT
            dreamHome = DreamHome.builder()
                    .userId(userId)
                    .aptSeq(request.aptSeq())
                    .targetAmount(request.targetAmount())
                    .targetDate(request.targetDate())
                    .monthlyGoal(request.monthlyGoal())
                    .currentSavedAmount(0L)
                    .startDate(LocalDate.now())
                    .status(DreamHomeStatus.ACTIVE)
                    .build();
            dreamHomeMapper.insert(dreamHome);
            log.info("Dream home created. userId: {}, dreamHomeId: {}, aptSeq: {}",
                    userId, dreamHome.getDreamHomeId(), request.aptSeq());
        }

        // 4. 최신 거래가 조회
        Long latestDealPrice = resolveLatestDealPrice(apartment);

        // 5. 응답 반환
        return DreamHomeSetResponse.from(dreamHome, apartment, latestDealPrice);
    }

    // =========================================================================
    // POST /api/dream-home/savings - 저축 기록
    // =========================================================================

    @Transactional
    public SavingsRecordResponse recordSavings(Long userId, SavingsRecordRequest request) {
        // 1. 활성 드림홈 조회
        DreamHome dreamHome = dreamHomeMapper.findActiveByUserId(userId);
        if (dreamHome == null) {
            throw new ResourceNotFoundException(ErrorCode.DREAM_HOME_NOT_FOUND);
        }

        // 2. 저축 내역 저장
        SavingsHistory savings = SavingsHistory.builder()
                .dreamHomeId(dreamHome.getDreamHomeId())
                .amount(request.amount())
                .saveType(request.saveType())
                .memo(request.memo())
                .build();
        savingsHistoryMapper.insert(savings);

        // 3. 드림홈 저축액 업데이트
        long currentSaved = dreamHome.getCurrentSavedAmount() != null ? dreamHome.getCurrentSavedAmount() : 0L;
        long signedAmount = request.saveType() == SaveType.DEPOSIT ? request.amount() : -request.amount();
        long newSavedAmount = Math.max(0, currentSaved + signedAmount);
        dreamHomeMapper.updateCurrentSavedAmount(dreamHome.getDreamHomeId(), newSavedAmount);

        // 4. 목표 달성 체크
        Long targetAmount = dreamHome.getTargetAmount() != null ? dreamHome.getTargetAmount() : 0L;
        boolean completed = newSavedAmount >= targetAmount;
        if (completed) {
            dreamHomeMapper.updateStatus(dreamHome.getDreamHomeId(), DreamHomeStatus.COMPLETED);
            log.info("Dream home completed! userId: {}, dreamHomeId: {}", userId, dreamHome.getDreamHomeId());
        }

        // 5. 경험치 계산 및 반영
        int expChange = calculateExpChange(request.saveType(), request.amount());
        User user = userMapper.findById(userId);
        int oldLevel = user.getCurrentLevel() != null ? user.getCurrentLevel() : 1;
        int oldExp = user.getCurrentExp() != null ? user.getCurrentExp() : 0;

        if (expChange > 0) {
            userMapper.addExp(userId, expChange);
        }

        // 6. 레벨업 체크 및 처리
        int newExp = oldExp + expChange;
        int newLevel = calculateNewLevel(newExp);
        boolean isLevelUp = false;

        if (newLevel > oldLevel && newLevel <= MAX_LEVEL) {
            userMapper.updateLevel(userId, newLevel);
            isLevelUp = true;
            log.info("Level up! userId: {}, {} -> {}", userId, oldLevel, newLevel);
        }

        // 7. 업데이트된 사용자 정보 조회
        User updatedUser = userMapper.findById(userId);

        // 8. 현재 레벨 정보 조회
        GrowthLevel currentLevel = growthLevelMapper.findByLevel(updatedUser.getCurrentLevel() != null ? updatedUser.getCurrentLevel() : 1);

        // 9. 업데이트된 드림홈 정보 구성
        DreamHome updatedDreamHome = DreamHome.builder()
                .dreamHomeId(dreamHome.getDreamHomeId())
                .currentSavedAmount(newSavedAmount)
                .targetAmount(dreamHome.getTargetAmount())
                .status(completed ? DreamHomeStatus.COMPLETED : DreamHomeStatus.ACTIVE)
                .build();

        return SavingsRecordResponse.from(updatedDreamHome, expChange, updatedUser, currentLevel, isLevelUp);
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    /**
     * 최신 거래가 조회 (만원 -> 원 변환)
     */
    private Long resolveLatestDealPrice(Apartment apartment) {
        if (apartment.getDeals() == null || apartment.getDeals().isEmpty()) {
            return null;
        }
        // deals는 deal_date DESC로 정렬되어 있음
        Long dealAmountNum = apartment.getDeals().get(0).getDealAmountNum();
        // deal_amount_num은 이미 만원 단위 숫자, 원으로 변환
        return dealAmountNum != null ? dealAmountNum * 10000 : null;
    }

    /**
     * 경험치 변화량 계산
     * - DEPOSIT: (amount / 10만원) * 10 EXP, 최대 500 EXP
     * - WITHDRAW: 0 EXP
     */
    private int calculateExpChange(SaveType saveType, Long amount) {
        if (saveType == SaveType.WITHDRAW) {
            return 0;
        }
        int units = (int) (amount / EXP_PER_UNIT);
        int exp = units * EXP_AMOUNT;
        return Math.min(exp, MAX_EXP_PER_SAVINGS);
    }

    /**
     * 누적 경험치로 새 레벨 계산
     */
    private int calculateNewLevel(int currentExp) {
        for (int level = MAX_LEVEL; level >= 1; level--) {
            if (currentExp >= LEVEL_THRESHOLDS[level - 1]) {
                return level;
            }
        }
        return 1;
    }
}
