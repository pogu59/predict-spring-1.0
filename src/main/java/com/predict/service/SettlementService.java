package com.predict.service;

import com.predict.ScoreSettlement;
import com.predict.TierChange;
import com.predict.Topic;
import com.predict.TopicOption;
import com.predict.User;
import com.predict.Vote;
import com.predict.enums.SettlementResult;
import com.predict.enums.Tier;
import com.predict.enums.TierChangeReason;
import com.predict.enums.TopicStatus;
import com.predict.repository.ScoreSettlementRepository;
import com.predict.repository.TierChangeRepository;
import com.predict.repository.TopicRepository;
import com.predict.repository.VoteRepository;
import com.predict.scoring.ScoringPolicy;
import com.predict.tier.TierPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결과 확정 & 점수 정산 (docs/predict.md 4-1절).
 * 하나의 트랜잭션 안에서 주제 상태 전환, 참여자 전원의 점수 정산, 티어 재계산까지 처리하고
 * 일부라도 실패하면 전체 롤백된다.
 */
@Service
public class SettlementService {

    private final TopicRepository topicRepository;
    private final VoteRepository voteRepository;
    private final ScoreSettlementRepository scoreSettlementRepository;
    private final TierChangeRepository tierChangeRepository;

    public SettlementService(TopicRepository topicRepository, VoteRepository voteRepository,
                              ScoreSettlementRepository scoreSettlementRepository,
                              TierChangeRepository tierChangeRepository) {
        this.topicRepository = topicRepository;
        this.voteRepository = voteRepository;
        this.scoreSettlementRepository = scoreSettlementRepository;
        this.tierChangeRepository = tierChangeRepository;
    }

    @Transactional
    public void confirmTopic(Long topicId, Long correctOptionId, User admin) {
        Topic topic = requirePendingTopic(topicId);
        TopicOption correctOption = requireOption(topic, correctOptionId);
        topic.confirm(correctOption, LocalDateTime.now(), admin);

        int optionCount = topic.getOptions().size();
        int totalVotes = topic.getOptions().stream()
                .mapToInt(option -> voteCountOf(option))
                .sum();

        for (Vote vote : voteRepository.findByTopicId(topicId)) {
            TopicOption chosen = vote.getTopicOption();
            BigDecimal p = ScoringPolicy.computeP(voteCountOf(chosen), totalVotes, optionCount);
            boolean correct = chosen.getId().equals(correctOption.getId());
            int scoreDelta = correct
                    ? ScoringPolicy.correctScore(p, optionCount)
                    : ScoringPolicy.incorrectScore(p, optionCount);

            User user = vote.getUser();
            int scoreAfter = user.applyScoreDelta(scoreDelta);

            scoreSettlementRepository.save(new ScoreSettlement(vote, user, topic, chosen,
                    correct ? SettlementResult.CORRECT : SettlementResult.INCORRECT, p, scoreDelta, scoreAfter));

            applyNaturalTierChange(user, scoreAfter);
        }
    }

    private Topic requirePendingTopic(Long topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주제: " + topicId));
        if (topic.getStatus() != TopicStatus.PENDING_RESULT) {
            throw new IllegalStateException("결과대기 상태의 주제만 정산할 수 있습니다: " + topic.getStatus());
        }
        return topic;
    }

    private TopicOption requireOption(Topic topic, Long optionId) {
        return topic.getOptions().stream()
                .filter(option -> option.getId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("이 주제에 속하지 않는 선택지입니다: " + optionId));
    }

    private int voteCountOf(TopicOption option) {
        return option.getVoteCount() == null ? 0 : option.getVoteCount();
    }

    private void applyNaturalTierChange(User user, int scoreAfter) {
        Tier previousTier = user.getTier();
        Tier newTier = TierPolicy.fromScore(scoreAfter, user.isActivitySuppressed());
        if (newTier == previousTier) {
            return;
        }
        TierChangeReason reason = newTier.compareTo(previousTier) > 0
                ? TierChangeReason.NATURAL_PROMOTION
                : TierChangeReason.NATURAL_DEMOTION;
        tierChangeRepository.save(new TierChange(user, previousTier, newTier, reason, null));
        user.changeTier(newTier);
    }
}
