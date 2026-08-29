package com.predict.repository;

import com.predict.Topic;
import com.predict.enums.TopicStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    /** 마감시각이 지났는데 아직 열려있는 주제 조회(자동 마감 배치용). */
    List<Topic> findByStatusAndVoteDeadlineAtLessThanEqual(TopicStatus status, LocalDateTime deadline);
}
