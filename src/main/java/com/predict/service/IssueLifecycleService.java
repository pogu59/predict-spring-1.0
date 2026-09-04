package com.predict.service;

import com.predict.Issue;
import com.predict.IssueOption;
import com.predict.enums.IssueStatus;
import com.predict.repository.IssueRepository;
import com.predict.repository.VoteRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 투표 마감시각 도달을 자동으로 감지해 결과대기 상태로 전환한다(docs/predict.md 4-1절 흐름도).
 */
@Service
public class IssueLifecycleService {

    private final IssueRepository issueRepository;
    private final VoteRepository voteRepository;

    public IssueLifecycleService(IssueRepository issueRepository, VoteRepository voteRepository) {
        this.issueRepository = issueRepository;
        this.voteRepository = voteRepository;
    }

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void closeExpiredIssues() {
        LocalDateTime now = LocalDateTime.now();
        List<Issue> expiredIssues = issueRepository.findByStatusAndVoteDeadlineAtLessThanEqual(IssueStatus.OPEN, now);
        for (Issue issue : expiredIssues) {
            Map<Long, Integer> voteCountsByOptionId = new HashMap<>();
            for (IssueOption option : issue.getOptions()) {
                long count = voteRepository.countByIssueIdAndIssueOptionId(issue.getId(), option.getId());
                voteCountsByOptionId.put(option.getId(), (int) count);
            }
            issue.closeForResult(voteCountsByOptionId);
        }
    }
}
