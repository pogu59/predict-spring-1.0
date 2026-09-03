package com.predict.repository;

import com.predict.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    boolean existsByUserIdAndTopicId(Long userId, Long topicId);

    List<Vote> findByTopicId(Long topicId);

    long countByTopicIdAndTopicOptionId(Long topicId, Long topicOptionId);

    /** 주간 활동성 체크: 특정 유저의 [from, to) 구간 투표 횟수(정답/오답 무관). */
    long countByUserIdAndVotedAtGreaterThanEqualAndVotedAtLessThan(Long userId, LocalDateTime from, LocalDateTime to);

    /** 관리자 페이지 주제 목록/상세의 참여자 수, 주제 수정 가능 여부 판단에 사용. */
    long countByTopicId(Long topicId);

    /** 관리자 페이지 유저 상세의 총 투표수. */
    long countByUserId(Long userId);

    /** 마이페이지 최근 투표 기록. */
    List<Vote> findByUserIdOrderByVotedAtDesc(Long userId);
}
