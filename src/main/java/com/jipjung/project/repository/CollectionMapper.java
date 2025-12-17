package com.jipjung.project.repository;

import com.jipjung.project.domain.UserCollection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 컬렉션 Mapper
 * <p>
 * 완성된 집 컬렉션 CRUD 및 여정 조회를 담당합니다.
 */
@Mapper
public interface CollectionMapper {

    /**
     * 사용자의 완성된 집 목록 조회 (상세 정보 포함)
     *
     * @param userId 사용자 ID
     * @return 컬렉션 목록 (테마, 아파트, 위치 정보 조인)
     */
    List<Map<String, Object>> findByUserId(@Param("userId") Long userId);

    /**
     * 컬렉션 상세 조회 (목록 조회와 동일한 조인 결과 단건)
     *
     * @param userId       사용자 ID
     * @param collectionId 컬렉션 ID
     * @return 컬렉션 상세 (테마, 아파트, 위치 정보 조인), 없으면 null
     */
    Map<String, Object> findDetailByUserIdAndCollectionId(
            @Param("userId") Long userId,
            @Param("collectionId") Long collectionId
    );

    /**
     * 컬렉션 단건 조회
     *
     * @param collectionId 컬렉션 ID
     * @return 컬렉션 정보
     */
    UserCollection findById(@Param("collectionId") Long collectionId);

    /**
     * 드림홈 ID로 컬렉션 존재 여부 확인
     * <p>
     * 멱등성 보장을 위해 사용됩니다.
     *
     * @param dreamHomeId 드림홈 ID
     * @return 컬렉션 존재 시 해당 컬렉션, 없으면 null
     */
    UserCollection findByDreamHomeId(@Param("dreamHomeId") Long dreamHomeId);

    /**
     * 컬렉션 등록
     * <p>
     * dream_home_id에 UNIQUE 제약이 있어 중복 시 DuplicateKeyException 발생
     *
     * @param collection 등록할 컬렉션
     */
    void insert(UserCollection collection);

    /**
     * 사용자의 모든 대표 컬렉션 해제
     * <p>
     * 새 대표 설정 전 기존 대표를 해제합니다.
     *
     * @param userId 사용자 ID
     */
    void clearMainDisplay(@Param("userId") Long userId);

    /**
     * 대표 컬렉션 설정
     *
     * @param collectionId 컬렉션 ID
     */
    void setMainDisplay(@Param("collectionId") Long collectionId);

    /**
     * 활성 드림홈 존재 여부 확인
     *
     * @param userId 사용자 ID
     * @return 활성 드림홈 존재 시 true
     */
    boolean hasActiveDreamHome(@Param("userId") Long userId);

    /**
     * 저축 여정 상세 조회 (Phase별 이벤트)
     * <p>
     * 윈도우 함수를 사용하여 누적합 계산 최적화
     *
     * @param dreamHomeId 드림홈 ID
     * @return 저축 이벤트 목록 (누적합 포함)
     */
    List<Map<String, Object>> findJourneyEvents(@Param("dreamHomeId") Long dreamHomeId);
}
