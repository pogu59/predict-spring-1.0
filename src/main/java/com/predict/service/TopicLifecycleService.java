package com.predict.service;

import com.predict.Topic;
import com.predict.enums.Choice;
import com.predict.enums.TopicStatus;
import com.predict.repository.TopicRepository;
import com.predict.repository.VoteRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 투표 마감시각 도달을 자동으로 감지해 결과대기 상태로 전환한다(docs/predict.md 4-1절 흐름도).
 */
@Service
public class TopicLifecycleService {

    private final TopicRepository topicRepository;
    private final VoteRepository voteRepository;

    public TopicLifecycleService(TopicRepository topicRepository, VoteRepository voteRepository) {
        this.topicRepository = topicRepository;
        this.voteRepository = voteRepository;
    }

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void closeExpiredTopics() {
        LocalDateTime now = LocalDateTime.now();
        List<Topic> expiredTopics = topicRepository.findByStatusAndVoteDeadlineAtLessThanEqual(TopicStatus.OPEN, now);
        for (Topic topic : expiredTopics) {
            long yesCount = voteRepository.countByTopicIdAndChoice(topic.getId(), Choice.YES);
            long noCount = voteRepository.countByTopicIdAndChoice(topic.getId(), Choice.NO);
            topic.closeForResult((int) yesCount, (int) noCount);
        }
    }
}
