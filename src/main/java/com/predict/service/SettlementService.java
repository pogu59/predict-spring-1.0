package com.predict.service;

import com.predict.ScoreSettlement;
import com.predict.TierChange;
import com.predict.Issue;
import com.predict.IssueOption;
import com.predict.User;
import com.predict.Vote;
import com.predict.enums.SettlementResult;
import com.predict.enums.Tier;
import com.predict.enums.TierChangeReason;
import com.predict.enums.IssueStatus;
import com.predict.repository.ScoreSettlementRepository;
import com.predict.repository.TierChangeRepository;
import com.predict.repository.IssueRepository;
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

    private final IssueRepository issueRepository;
    private final VoteRepository voteRepository;
    private final ScoreSettlementRepository scoreSettlementRepository;
    private final TierChangeRepository tierChangeRepository;

    public SettlementService(IssueRepository issueRepository, VoteRepository voteRepository,
                              ScoreSettlementRepository scoreSettlementRepository,
                              TierChangeRepository tierChangeRepository) {
        this.issueRepository = issueRepository;
        this.voteRepository = voteRepository;
        this.scoreSettlementRepository = scoreSettlementRepository;
        this.tierChangeRepository = tierChangeRepository;
    }

    @Transactional
    public void confirmIssue(Long issueId, Long correctOptionId, User admin) {
        Issue issue = requirePendingIssue(issueId);
        IssueOption correctOption = requireOption(issue, correctOptionId);
        issue.confirm(correctOption, LocalDateTime.now(), admin);

        int optionCount = issue.getOptions().size();
        int totalVotes = issue.getOptions().stream()
                .mapToInt(option -> voteCountOf(option))
                .sum();

        for (Vote vote : voteRepository.findByIssueId(issueId)) {
            IssueOption chosen = vote.getIssueOption();
            BigDecimal p = ScoringPolicy.computeP(voteCountOf(chosen), totalVotes, optionCount);
            boolean correct = chosen.getId().equals(correctOption.getId());
            int stake = vote.getStake();
            int scoreDelta = correct
                    ? ScoringPolicy.correctScore(p, optionCount, stake)
                    : ScoringPolicy.incorrectScore(p, optionCount, stake);

            User user = vote.getUser();
            // stake는 투표 시점에 이미 에스크로(차감)되어 있으므로, 정산에서는 원금(stake)을
            // 돌려주면서 scoreDelta(오답이면 음수)만큼 가감한다. scoreDelta의 크기는 항상
            // stake의 40% 이하로 설계돼 있어 이 값은 절대 음수가 될 수 없다.
            int creditedAmount = stake + scoreDelta;
            int scoreAfter = user.applyScoreDelta(creditedAmount);

            scoreSettlementRepository.save(new ScoreSettlement(vote, user, issue, chosen,
                    correct ? SettlementResult.CORRECT : SettlementResult.INCORRECT, p, scoreDelta, scoreAfter));

            applyNaturalTierChange(user, scoreAfter);
        }
    }

    private Issue requirePendingIssue(Long issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주제: " + issueId));
        if (issue.getStatus() != IssueStatus.PENDING_RESULT) {
            throw new IllegalStateException("결과대기 상태의 주제만 정산할 수 있습니다: " + issue.getStatus());
        }
        return issue;
    }

    private IssueOption requireOption(Issue issue, Long optionId) {
        return issue.getOptions().stream()
                .filter(option -> option.getId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("이 주제에 속하지 않는 선택지입니다: " + optionId));
    }

    private int voteCountOf(IssueOption option) {
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
