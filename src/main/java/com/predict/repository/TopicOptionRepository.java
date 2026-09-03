package com.predict.repository;

import com.predict.TopicOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicOptionRepository extends JpaRepository<TopicOption, Long> {

    List<TopicOption> findByTopicIdOrderByDisplayOrderAsc(Long topicId);
}
