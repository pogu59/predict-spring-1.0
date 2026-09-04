package com.predict.repository;

import com.predict.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    boolean existsByUserIdAndIssueId(Long userId, Long issueId);

    /** 특정 유저가 특정 주제에 투표했는지 + 어떤 선택지였는지. 공개 조회 API에서 본인 노출 판단에 쓴다. */
    Optional<Vote> findByUserIdAndIssueId(Long userId, Long issueId);

    List<Vote> findByIssueId(Long issueId);

    long countByIssueIdAndIssueOptionId(Long issueId, Long issueOptionId);

    /** 주간 활동성 체크: 특정 유저의 [from, to) 구간 투표 횟수(정답/오답 무관). */
    long countByUserIdAndVotedAtGreaterThanEqualAndVotedAtLessThan(Long userId, LocalDateTime from, LocalDateTime to);

    /** 관리자 페이지 주제 목록/상세의 참여자 수, 주제 수정 가능 여부 판단에 사용. */
    long countByIssueId(Long issueId);

    /** 관리자 페이지 유저 상세의 총 투표수. */
    long countByUserId(Long userId);

    /** 마이페이지 최근 투표 기록. */
    List<Vote> findByUserIdOrderByVotedAtDesc(Long userId);
}
