package com.jipjung.project.repository;

import com.jipjung.project.domain.StreakMilestoneReward;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 스트릭 마일스톤 보상 Mapper
 * <p>
 * 마일스톤 보상 수령 기록 관리
 */
@Mapper
public interface StreakMilestoneRewardMapper {

    /**
     * 마일스톤 보상 수령 여부 확인
     * <p>
     * 사용자당 각 마일스톤은 1회만 수령 가능
     *
     * @param userId        사용자 ID
     * @param milestoneDays 마일스톤 일수 (7, 14, 21, 28)
     * @return 이미 수령한 경우 true
     */
    boolean existsByUserAndMilestone(
            @Param("userId") Long userId,
            @Param("milestoneDays") int milestoneDays
    );

    /**
     * 보상 기록 삽입
     *
     * @param reward 마일스톤 보상 정보
     * @return 영향받은 행 수
     */
    int insert(StreakMilestoneReward reward);

    /**
     * 사용자가 수령한 모든 마일스톤 조회
     *
     * @param userId 사용자 ID
     * @return 수령한 마일스톤 목록
     */
    List<StreakMilestoneReward> findByUserId(@Param("userId") Long userId);
}
