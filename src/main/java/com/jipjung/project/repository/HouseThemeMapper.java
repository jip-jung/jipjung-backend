package com.jipjung.project.repository;

import com.jipjung.project.domain.HouseTheme;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 하우스 테마 Mapper
 * <p>
 * 테마 조회 및 유효성 검증을 위한 데이터 접근 계층.
 * 
 * @see com.jipjung.project.domain.HouseTheme
 */
@Mapper
public interface HouseThemeMapper {

    /**
     * 모든 활성 테마 조회
     * <p>
     * is_active=true, is_deleted=false 조건의 테마 목록 반환.
     * 테마 선택 UI에서 사용.
     *
     * @return 활성 테마 목록 (theme_id 순 정렬)
     */
    List<HouseTheme> findAllActive();

    /**
     * 테마 ID로 단건 조회
     * <p>
     * is_deleted=false 조건만 적용 (비활성 테마도 조회 가능).
     * 
     * @param themeId 테마 ID
     * @return 테마 정보 또는 null
     */
    HouseTheme findById(@Param("themeId") Integer themeId);

    /**
     * 테마가 존재하고 활성 상태인지 확인
     * <p>
     * 드림홈 설정 시 테마 유효성 검증에 사용.
     *
     * @param themeId 테마 ID
     * @return true if 존재 && 활성
     */
    boolean existsAndActive(@Param("themeId") Integer themeId);
}
