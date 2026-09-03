package com.predict.service;

import com.predict.Category;
import com.predict.Topic;
import com.predict.enums.TopicStatus;
import com.predict.repository.CategoryRepository;
import com.predict.repository.TopicRepository;
import com.predict.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 페이지의 주제 생성/수정 (docs/predict.md 5-4절, schema_8.sql sp_update_topic /
 * sp_extend_topic_deadline). 참여자가 없으면 전체 수정, 있으면 마감시각 연장만 허용한다 —
 * 이미 투표한 유저가 본 질문이 사후에 바뀌는 공정성 문제를 막기 위함이다.
 */
@Service
public class AdminTopicService {

    private final TopicRepository topicRepository;
    private final CategoryRepository categoryRepository;
    private final VoteRepository voteRepository;

    public AdminTopicService(TopicRepository topicRepository, CategoryRepository categoryRepository,
                              VoteRepository voteRepository) {
        this.topicRepository = topicRepository;
        this.categoryRepository = categoryRepository;
        this.voteRepository = voteRepository;
    }

    @Transactional
    public Topic createTopic(Integer categoryId, String title, String description,
                              LocalDateTime voteStartAt, LocalDateTime voteDeadlineAt, List<String> options) {
        requireFutureVotingWindow(voteStartAt, voteDeadlineAt);
        Category category = requireCategory(categoryId);
        return topicRepository.save(new Topic(category, title, description, voteStartAt, voteDeadlineAt, options));
    }

    @Transactional
    public void updateTopic(Long topicId, Integer categoryId, String title, String description,
                             LocalDateTime voteStartAt, LocalDateTime voteDeadlineAt, List<String> options) {
        Topic topic = requireTopic(topicId);
        if (topic.getStatus() != TopicStatus.OPEN) {
            throw new IllegalStateException("진행중(open) 상태인 주제만 수정할 수 있습니다.");
        }
        if (voteRepository.countByTopicId(topicId) > 0) {
            throw new IllegalStateException("이미 참여자가 있는 주제는 내용을 수정할 수 없습니다. 마감시각 연장만 가능합니다.");
        }
        requireFutureVotingWindow(voteStartAt, voteDeadlineAt);

        Category category = requireCategory(categoryId);
        topic.updateContent(category, title, description, voteStartAt, voteDeadlineAt, options);
    }

    /** 시작/마감 시각은 현재 시각 이후여야 하고, 마감은 시작보다 늦어야 한다. */
    private void requireFutureVotingWindow(LocalDateTime voteStartAt, LocalDateTime voteDeadlineAt) {
        LocalDateTime now = LocalDateTime.now();
        if (voteStartAt.isBefore(now)) {
            throw new IllegalArgumentException("투표 시작 시각은 현재 시각 이후여야 합니다.");
        }
        if (voteDeadlineAt.isBefore(now)) {
            throw new IllegalArgumentException("마감 시각은 현재 시각 이후여야 합니다.");
        }
        if (!voteDeadlineAt.isAfter(voteStartAt)) {
            throw new IllegalArgumentException("마감 시각은 시작 시각보다 늦어야 합니다.");
        }
    }

    @Transactional
    public void extendDeadline(Long topicId, LocalDateTime newDeadline) {
        Topic topic = requireTopic(topicId);
        if (topic.getStatus() != TopicStatus.OPEN) {
            throw new IllegalStateException("진행중(open)인 주제만 마감시각을 조정할 수 있습니다.");
        }
        if (!newDeadline.isAfter(topic.getVoteDeadlineAt())) {
            throw new IllegalStateException("새 마감시각은 기존 마감시각보다 늦어야 합니다 (연장만 가능, 단축 불가).");
        }
        topic.extendDeadline(newDeadline);
    }

    private Topic requireTopic(Long topicId) {
        return topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주제: " + topicId));
    }

    private Category requireCategory(Integer categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리: " + categoryId));
    }
}
