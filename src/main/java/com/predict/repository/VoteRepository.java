package com.predict.repository;

import com.predict.Vote;
import com.predict.enums.Choice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    boolean existsByUserIdAndTopicId(Long userId, Long topicId);

    List<Vote> findByTopicId(Long topicId);

    long countByTopicIdAndChoice(Long topicId, Choice choice);

    /** 주간 활동성 체크: 특정 유저의 [from, to) 구간 투표 횟수(정답/오답/void 무관). */
    long countByUserIdAndVotedAtGreaterThanEqualAndVotedAtLessThan(Long userId, LocalDateTime from, LocalDateTime to);
}
