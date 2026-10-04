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
        CurrentUserService.requireNotSuspended(user);
        Issue issue = requireVotableIssue(issueId);
        if (voteRepository.existsByUserIdAndIssueId(user.getId(), issueId)) {
            throw new IllegalStateException("이미 투표한 주제입니다.");
        }
        if (stake < 1) {
            throw new IllegalArgumentException("신용도를 1 이상 걸어주세요.");
        }
        if (stake > user.getCredibilityScore()) {
            throw new IllegalStateException("보유 신용도가 부족해요");
        }
        IssueOption option = requireOption(issue, issueOptionId);

        // 베팅액을 즉시 에스크로한다 — 결과 확정 시 SettlementService가 stake + scoreDelta만큼
        // 돌려준다(정답이면 원금+수익, 오답이면 원금 중 일부). 잔액 검증을 이미 통과했으니
        // 여기서 max(0,...) 플로어에 걸릴 일은 없다.
        user.applyScoreDelta(-stake);

        return voteRepository.save(new Vote(user, issue, option, stake));
    }

    /**
     * 마감 전 선택 변경. 스테이크는 최초 투표 때 건 그대로 두고 선택지만 바꾼다(추가 차감 없음).
     * 정산은 마감 시점 득표 스냅샷과 그때의 선택지로 하므로, 마감 전에 바뀐 선택은 자연스럽게 반영된다.
     */
    @Transactional
    public Vote changeVote(User user, Long issueId, Long issueOptionId) {
        CurrentUserService.requireNotSuspended(user);
        Issue issue = requireVotableIssue(issueId);
        Vote vote = voteRepository.findByUserIdAndIssueId(user.getId(), issueId)
                .orElseThrow(() -> new IllegalStateException("아직 투표하지 않은 주제입니다."));
        vote.changeOption(requireOption(issue, issueOptionId));
        return vote;
    }

    private Issue requireVotableIssue(Long issueId) {
        Issue issue = issueRepository.findById(issueId)
                .filter(i -> !i.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주제: " + issueId));
        if (issue.getStatus() != IssueStatus.OPEN) {
            throw new IllegalStateException("마감된 이슈예요");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(issue.getVoteStartAt()) || !now.isBefore(issue.getVoteDeadlineAt())) {
            throw new IllegalStateException("투표 가능 시간이 아닙니다.");
        }
        return issue;
    }

    private IssueOption requireOption(Issue issue, Long issueOptionId) {
        return issue.getOptions().stream()
                .filter(option -> option.getId().equals(issueOptionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("이 주제에 속하지 않는 선택지입니다: " + issueOptionId));
    }
}
