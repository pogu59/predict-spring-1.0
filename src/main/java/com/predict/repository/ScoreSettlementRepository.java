package com.predict.repository;

import com.predict.ScoreSettlement;
import com.predict.enums.SettlementResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ScoreSettlementRepository extends JpaRepository<ScoreSettlement, Long> {

    /** 마이페이지 투표 기록 — 특정 투표의 현재 유효한(무효화되지 않은) 정산 결과. */
    Optional<ScoreSettlement> findByVoteIdAndIsReversedFalse(Long voteId);

    /** 오확정 정정 시 이 주제와 관련된 모든 정산 기록(무효 포함)에서 영향받은 유저를 찾는 용도. */
    List<ScoreSettlement> findByIssueId(Long issueId);

    /** 오확정 정정 1단계: 이 주제의 아직 무효화되지 않은 정산 기록만 골라 reverse() 처리. */
    List<ScoreSettlement> findByIssueIdAndIsReversedFalse(Long issueId);

    /** 오확정 정정 3단계: 유저 점수를 처음부터 시간순으로 재생(replay)하기 위한 조회. */
    List<ScoreSettlement> findByUserIdAndIsReversedFalseOrderBySettledAtAscIdAsc(Long userId);

    /** 관리자 페이지 유저 상세 통계 — 정답 수. */
    long countByUserIdAndResultAndIsReversedFalse(Long userId, SettlementResult result);

    /** 관리자 페이지 유저 상세 통계 — 채점 완료(정답+오답) 수. 정답률 = 정답수/이 값. */
    long countByUserIdAndResultInAndIsReversedFalse(Long userId, Collection<SettlementResult> results);

    /** 크루 대항전 주간 집계 — 기간 안의 유효 정산. */
    List<ScoreSettlement> findBySettledAtGreaterThanEqualAndSettledAtLessThanAndIsReversedFalse(
            LocalDateTime from, LocalDateTime to);
}
