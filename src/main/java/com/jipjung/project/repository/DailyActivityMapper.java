package com.jipjung.project.repository;

import com.jipjung.project.domain.DailyActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 일일 활동 Mapper
 * <p>
 * 활동 기반 스트릭 시스템을 위한 일일 활동 기록 관리.
 * 
 * @see com.jipjung.project.domain.DailyActivity
 * @see com.jipjung.project.service.StreakService
 */
@Mapper
public interface DailyActivityMapper {

    /**
     * 특정 날짜에 해당 활동 유형으로 이미 참여했는지 확인
     * 
     * @param userId 사용자 ID
     * @param activityDate 활동 날짜 (KST)
     * @param activityType 활동 유형 (enum name)
     * @return 존재 여부
     */
    boolean existsByUserIdAndDateAndType(
            @Param("userId") Long userId,
            @Param("activityDate") LocalDate activityDate,
            @Param("activityType") String activityType
    );

    /**
     * 특정 날짜의 활동 건수 조회
     * <p>
     * 오늘 첫 활동인지 판단하는 데 사용됩니다.
     * 
     * @param userId 사용자 ID
     * @param activityDate 활동 날짜 (KST)
     * @return 활동 건수
     */
    int countByUserIdAndDate(
            @Param("userId") Long userId,
            @Param("activityDate") LocalDate activityDate
    );

    /**
     * 특정 날짜의 총 획득 EXP 조회
     * <p>
     * 일일 EXP 상한 체크에 사용됩니다.
     * 
     * @param userId 사용자 ID
     * @param activityDate 활동 날짜 (KST)
     * @return 총 획득 EXP (없으면 0)
     */
    int sumExpByUserIdAndDate(
            @Param("userId") Long userId,
            @Param("activityDate") LocalDate activityDate
    );

    /**
     * 일일 활동 기록 삽입
     * <p>
     * 복합 유니크 제약(user_id, activity_date, activity_type)으로 
     * 같은 날 같은 활동은 중복 삽입 시 예외 발생합니다.
     * 
     * @param dailyActivity 일일 활동 정보
     * @return 영향받은 행 수
     */
    int insert(DailyActivity dailyActivity);

    /**
     * 기간 내 활동 이벤트 조회 (created_at 기준)
     *
     * @param userId 사용자 ID
     * @param startAt 시작 시각
     * @param endAt 종료 시각
     * @return 활동 이벤트 목록
     */
    java.util.List<java.util.Map<String, Object>> findExpEventsByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startAt") java.time.LocalDateTime startAt,
            @Param("endAt") java.time.LocalDateTime endAt
    );
}
