package com.predict.service;

import com.predict.ScoreSettlement;
import com.predict.TierChange;
import com.predict.Topic;
import com.predict.User;
import com.predict.enums.Tier;
import com.predict.enums.TierChangeReason;
import com.predict.enums.TopicStatus;
import com.predict.repository.ScoreSettlementRepository;
import com.predict.repository.TierChangeRepository;
import com.predict.repository.TopicRepository;
import com.predict.tier.TierPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 오확정 정정 (docs/predict.md 5-3절, schema_8.sql sp_correct_topic_result).
 * 관리자가 잘못된 답으로 확정한 걸 발견했을 때 호출한다. 기존 정산 기록은 삭제하지 않고
 * is_reversed로만 표시해 감사기록을 보존하고, 영향받은 유저의 점수는 남은 정산 기록만으로
 * 시간순 재생(replay)해 0점 하한선까지 정확히 재현한다.
 */
@Service
public class SettlementCorrectionService {

    private final TopicRepository topicRepository;
    private final ScoreSettlementRepository scoreSettlementRepository;
    private final TierChangeRepository tierChangeRepository;

    public SettlementCorrectionService(TopicRepository topicRepository,
                                        ScoreSettlementRepository scoreSettlementRepository,
                                        TierChangeRepository tierChangeRepository) {
        this.topicRepository = topicRepository;
        this.scoreSettlementRepository = scoreSettlementRepository;
        this.tierChangeRepository = tierChangeRepository;
    }

    @Transactional
    public void correctTopic(Long topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주제: " + topicId));
        if (topic.getStatus() != TopicStatus.CONFIRMED) {
            throw new IllegalStateException("이 주제는 아직 확정되지 않아 정정할 수 없습니다.");
        }

        // 영향받은 유저 = 이 주제에 정산 기록이 있던 유저 전원. 정정 후 남은 기록이
        // 하나도 없는 유저도 0점으로 재계산해야 하므로, User 참조를 여기서 미리 확보해 둔다.
        Map<Long, User> affectedUsers = new LinkedHashMap<>();
        for (ScoreSettlement settlement : scoreSettlementRepository.findByTopicId(topicId)) {
            affectedUsers.put(settlement.getUser().getId(), settlement.getUser());
        }

        for (ScoreSettlement settlement : scoreSettlementRepository.findByTopicIdAndIsReversedFalse(topicId)) {
            settlement.reverse();
        }

        topic.resetForCorrection();

        for (User user : affectedUsers.values()) {
            replayUserScore(user);
        }
    }

    private void replayUserScore(User user) {
        List<ScoreSettlement> remaining =
                scoreSettlementRepository.findByUserIdAndIsReversedFalseOrderBySettledAtAscIdAsc(user.getId());

        int running = 0;
        for (ScoreSettlement settlement : remaining) {
            running = Math.max(0, running + settlement.getScoreDelta());
        }
        applyReplayedScoreToUser(user, running);
    }

    private void applyReplayedScoreToUser(User user, int replayedScore) {
        Tier previousTier = user.getTier();
        user.resetCredibilityScore(replayedScore);

        Tier newTier = TierPolicy.fromScore(user.getCredibilityScore(), user.isActivitySuppressed());
        if (newTier != previousTier) {
            tierChangeRepository.save(new TierChange(user, previousTier, newTier, TierChangeReason.CORRECTION, null));
            user.changeTier(newTier);
        }
    }
}
