package com.predict.service;

import com.predict.ScoreSettlement;
import com.predict.TierChange;
import com.predict.Issue;
import com.predict.User;
import com.predict.Vote;
import com.predict.enums.Tier;
import com.predict.enums.TierChangeReason;
import com.predict.enums.IssueStatus;
import com.predict.repository.ScoreSettlementRepository;
import com.predict.repository.TierChangeRepository;
import com.predict.repository.IssueRepository;
import com.predict.repository.VoteRepository;
import com.predict.tier.TierPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 오확정 정정 (docs/predict.md 5-3절, schema_8.sql sp_correct_issue_result).
 * 관리자가 잘못된 답으로 확정한 걸 발견했을 때 호출한다. 기존 정산 기록은 삭제하지 않고
 * is_reversed로만 표시해 감사기록을 보존하고, 영향받은 유저의 점수는 남은 정산 기록만으로
 * 시간순 재생(replay)해 0점 하한선까지 정확히 재현한다.
 */
@Service
public class SettlementCorrectionService {

    private final IssueRepository issueRepository;
    private final VoteRepository voteRepository;
    private final ScoreSettlementRepository scoreSettlementRepository;
    private final TierChangeRepository tierChangeRepository;

    public SettlementCorrectionService(IssueRepository issueRepository, VoteRepository voteRepository,
                                        ScoreSettlementRepository scoreSettlementRepository,
                                        TierChangeRepository tierChangeRepository) {
        this.issueRepository = issueRepository;
        this.voteRepository = voteRepository;
        this.scoreSettlementRepository = scoreSettlementRepository;
        this.tierChangeRepository = tierChangeRepository;
    }

    @Transactional
    public void correctIssue(Long issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주제: " + issueId));
        if (issue.getStatus() != IssueStatus.CONFIRMED) {
            throw new IllegalStateException("이 주제는 아직 확정되지 않아 정정할 수 없습니다.");
        }

        // 영향받은 유저 = 이 주제에 정산 기록이 있던 유저 전원. 정정 후 남은 기록이
        // 하나도 없는 유저도 0점으로 재계산해야 하므로, User 참조를 여기서 미리 확보해 둔다.
        Map<Long, User> affectedUsers = new LinkedHashMap<>();
        for (ScoreSettlement settlement : scoreSettlementRepository.findByIssueId(issueId)) {
            affectedUsers.put(settlement.getUser().getId(), settlement.getUser());
        }

        for (ScoreSettlement settlement : scoreSettlementRepository.findByIssueIdAndIsReversedFalse(issueId)) {
            settlement.reverse();
        }

        issue.resetForCorrection();

        for (User user : affectedUsers.values()) {
            replayUserScore(user);
        }
    }

    /**
     * 시작 잔액(User.STARTING_CREDIBILITY_SCORE) + 유효한 정산 기록만으로 잔액을 재생한 뒤,
     * 아직 정산되지 않은(=활성 정산 기록이 없는) 투표의 베팅액을 뺀다 — 그 돈은 지금도
     * 에스크로된 채로 잠겨 있어 재생된 "정산 완료분"에 포함시키면 안 되기 때문이다.
     * 베팅 도입 전(투표에 비용이 없던 시절)에는 이 감산이 필요 없었지만, 지금은 정산 기록만으로
     * 잔액을 100% 재구성할 수 없어 이 보정이 꼭 필요하다.
     */
    private void replayUserScore(User user) {
        List<ScoreSettlement> remaining =
                scoreSettlementRepository.findByUserIdAndIsReversedFalseOrderBySettledAtAscIdAsc(user.getId());

        int running = User.STARTING_CREDIBILITY_SCORE;
        for (ScoreSettlement settlement : remaining) {
            running = Math.max(0, running + settlement.getScoreDelta());
        }

        int openStake = voteRepository.findByUserIdOrderByVotedAtDesc(user.getId()).stream()
                .filter(vote -> scoreSettlementRepository.findByVoteIdAndIsReversedFalse(vote.getId()).isEmpty())
                .mapToInt(Vote::getStake)
                .sum();

        applyReplayedScoreToUser(user, Math.max(0, running - openStake));
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
