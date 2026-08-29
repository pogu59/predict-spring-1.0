package com.predict.service;

import com.predict.Topic;
import com.predict.User;
import com.predict.Vote;
import com.predict.enums.Choice;
import com.predict.enums.TopicStatus;
import com.predict.repository.TopicRepository;
import com.predict.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class VoteService {

    private final VoteRepository voteRepository;
    private final TopicRepository topicRepository;

    public VoteService(VoteRepository voteRepository, TopicRepository topicRepository) {
        this.voteRepository = voteRepository;
        this.topicRepository = topicRepository;
    }

    @Transactional
    public Vote castVote(User user, Long topicId, Choice choice) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주제: " + topicId));

        if (topic.getStatus() != TopicStatus.OPEN) {
            throw new IllegalStateException("투표할 수 없는 상태의 주제입니다: " + topic.getStatus());
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(topic.getVoteStartAt()) || !now.isBefore(topic.getVoteDeadlineAt())) {
            throw new IllegalStateException("투표 가능 시간이 아닙니다.");
        }
        if (voteRepository.existsByUserIdAndTopicId(user.getId(), topicId)) {
            throw new IllegalStateException("이미 투표한 주제입니다.");
        }

        return voteRepository.save(new Vote(user, topic, choice));
    }
}
