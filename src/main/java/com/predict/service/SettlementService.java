package com.predict.service;

import com.predict.ScoreSettlement;
import com.predict.TierChange;
import com.predict.Topic;
import com.predict.User;
import com.predict.Vote;
import com.predict.enums.Choice;
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
import java.util.List;

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
    public void confirmTopic(Long topicId, Choice correctAnswer, User admin) {
        Topic topic = requirePendingTopic(topicId);
        topic.confirm(correctAnswer, LocalDateTime.now(), admin);

        int totalVotes = topic.getYesCount() + topic.getNoCount();
        for (Vote vote : voteRepository.findByTopicId(topicId)) {
            BigDecimal p = computeP(topic, vote, totalVotes);
            boolean correct = vote.getChoice() == correctAnswer;
            int scoreDelta = correct ? ScoringPolicy.correctScore(p) : ScoringPolicy.incorrectScore(p);

            User user = vote.getUser();
            int scoreAfter = user.applyScoreDelta(scoreDelta);

            scoreSettlementRepository.save(new ScoreSettlement(vote, user, topic, vote.getChoice(),
                    correct ? SettlementResult.CORRECT : SettlementResult.INCORRECT, p, scoreDelta, scoreAfter));

            applyNaturalTierChange(user, scoreAfter);
        }
    }

    @Transactional
    public void voidTopic(Long topicId, User admin) {
        Topic topic = requirePendingTopic(topicId);
        topic.voidTopic(LocalDateTime.now(), admin);

        int totalVotes = topic.getYesCount() + topic.getNoCount();
        for (Vote vote : voteRepository.findByTopicId(topicId)) {
            BigDecimal p = computeP(topic, vote, totalVotes);
            User user = vote.getUser();
            scoreSettlementRepository.save(new ScoreSettlement(vote, user, topic, vote.getChoice(),
                    SettlementResult.VOID, p, 0, user.getCredibilityScore()));
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

    private BigDecimal computeP(Topic topic, Vote vote, int totalVotes) {
        int chosenSideVotes = vote.getChoice() == Choice.YES ? topic.getYesCount() : topic.getNoCount();
        return ScoringPolicy.computeP(chosenSideVotes, totalVotes);
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
