package com.jipjung.project.repository;

import com.jipjung.project.domain.DreamHome;
import com.jipjung.project.domain.DreamHomeStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 드림홈 Mapper
 */
@Mapper
public interface DreamHomeMapper {

    /**
     * 사용자의 활성 드림홈 조회
     * - status = 'ACTIVE', is_deleted = false
     * - 아파트 정보 JOIN 포함
     */
    DreamHome findActiveByUserId(@Param("userId") Long userId);

    /**
     * 대시보드용 드림홈 조회
     * - ACTIVE가 있으면 ACTIVE를 우선, 없으면 최근 COMPLETED를 반환
     * - is_deleted = false
     * - 아파트 정보 JOIN 포함
     */
    DreamHome findLatestForDashboardByUserId(@Param("userId") Long userId);

    /**
     * 드림홈 단건 조회
     * - is_deleted = false
     * - 아파트 정보 JOIN 포함
     */
    DreamHome findById(@Param("dreamHomeId") Long dreamHomeId);

    /**
     * 드림홈 생성
     */
    int insert(DreamHome dreamHome);

    /**
     * 드림홈 목표 업데이트 (저축액 유지)
     * - aptSeq, targetAmount, targetDate, monthlyGoal, startDate만 변경
     * - currentSavedAmount는 유지됨
     */
    int updateDreamHome(DreamHome dreamHome);

    /**
     * 드림홈 상태 업데이트
     */
    int updateStatus(@Param("dreamHomeId") Long dreamHomeId, @Param("status") DreamHomeStatus status);

    /**
     * 현재 저축액 업데이트
     */
    int updateCurrentSavedAmount(@Param("dreamHomeId") Long dreamHomeId, @Param("currentSavedAmount") Long currentSavedAmount);
}
