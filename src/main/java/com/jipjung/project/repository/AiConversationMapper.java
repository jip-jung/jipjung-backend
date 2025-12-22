package com.jipjung.project.repository;

import com.jipjung.project.domain.AiConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI 대화 Mapper
 */
@Mapper
public interface AiConversationMapper {

    /**
     * AI 대화 저장 (신규)
     */
    void insert(AiConversation conversation);

    /**
     * ID + 사용자 ID로 대화 조회 (권한 확인용)
     */
    @Select("""
        SELECT * FROM ai_conversation
        WHERE conversation_id = #{conversationId}
        AND user_id = #{userId}
    """)
    AiConversation findByIdAndUserId(
        @Param("conversationId") Long conversationId,
        @Param("userId") Long userId
    );

    /**
     * 분석 결과 업데이트
     */
    void updateAnalysis(AiConversation conversation);

    /**
     * 이미지 추출 결과 업데이트 (EXTRACTING)
     */
    void updateExtraction(AiConversation conversation);

    /**
     * 판결 결과 업데이트
     */
    void updateJudgment(AiConversation conversation);

    /**
     * 지출 정보 + 분석 결과 업데이트 (confirm용)
     */
    void updateConfirm(AiConversation conversation);

    /**
     * 사용자의 분석 내역 조회 (판결 완료된 것만)
     */
    @Select("""
        SELECT * FROM ai_conversation
        WHERE user_id = #{userId}
        AND status = 'JUDGED'
        ORDER BY created_at DESC
        LIMIT #{limit}
    """)
    List<AiConversation> findHistoryByUserId(
        @Param("userId") Long userId,
        @Param("limit") int limit
    );

    /**
     * 판결 완료 대화 이벤트 조회 (기간 필터)
     */
    List<java.util.Map<String, Object>> findJudgedEventsByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startAt") java.time.LocalDateTime startAt,
        @Param("endAt") java.time.LocalDateTime endAt
    );
}
