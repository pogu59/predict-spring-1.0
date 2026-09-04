package com.predict.service;

import com.predict.Issue;
import com.predict.IssueOption;
import com.predict.User;
import com.predict.Vote;
import com.predict.enums.IssueStatus;
import com.predict.repository.IssueRepository;
import com.predict.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class VoteService {

    private final VoteRepository voteRepository;
    private final IssueRepository issueRepository;

    public VoteService(VoteRepository voteRepository, IssueRepository issueRepository) {
        this.voteRepository = voteRepository;
        this.issueRepository = issueRepository;
    }

    @Transactional
    public Vote castVote(User user, Long issueId, Long issueOptionId, int stake) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주제: " + issueId));

        if (issue.getStatus() != IssueStatus.OPEN) {
            throw new IllegalStateException("투표할 수 없는 상태의 주제입니다: " + issue.getStatus());
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(issue.getVoteStartAt()) || !now.isBefore(issue.getVoteDeadlineAt())) {
            throw new IllegalStateException("투표 가능 시간이 아닙니다.");
        }
        if (voteRepository.existsByUserIdAndIssueId(user.getId(), issueId)) {
            throw new IllegalStateException("이미 투표한 주제입니다.");
        }
        if (stake > user.getCredibilityScore()) {
            throw new IllegalStateException("보유 신용도(" + user.getCredibilityScore() + ")보다 많이 베팅할 수 없습니다.");
        }
        IssueOption option = requireOption(issue, issueOptionId);

        // 베팅액을 즉시 에스크로한다 — 결과 확정 시 SettlementService가 stake + scoreDelta만큼
        // 돌려준다(정답이면 원금+수익, 오답이면 원금 중 일부). 잔액 검증을 이미 통과했으니
        // 여기서 max(0,...) 플로어에 걸릴 일은 없다.
        user.applyScoreDelta(-stake);

        return voteRepository.save(new Vote(user, issue, option, stake));
    }

    private IssueOption requireOption(Issue issue, Long issueOptionId) {
        return issue.getOptions().stream()
                .filter(option -> option.getId().equals(issueOptionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("이 주제에 속하지 않는 선택지입니다: " + issueOptionId));
    }
}
