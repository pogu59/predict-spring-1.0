package com.predict.service;

import com.predict.Issue;
import com.predict.IssueOption;
import com.predict.enums.IssueStatus;
import com.predict.repository.VoteRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 선택지별 득표수(optionId -> count). 진행중 이슈는 votes를 실시간 집계하고(IssueOption.voteCount는
 * 마감 시점 스냅샷이라 진행중엔 null), 마감된 이슈는 스냅샷을 쓴다. 응답에는 이 값으로 계산한 비율만
 * 실린다(VotePercentages) — 득표수 자체는 사용자에게 내려주지 않는다.
 */
@Service
public class VoteCountService {

    private final VoteRepository voteRepository;

    public VoteCountService(VoteRepository voteRepository) {
        this.voteRepository = voteRepository;
    }

    public Map<Long, Integer> countsByOption(Issue issue) {
        return countsByOption(List.of(issue));
    }

    public Map<Long, Integer> countsByOption(Collection<Issue> issues) {
        Map<Long, Integer> counts = new HashMap<>();
        List<Long> openIssueIds = issues.stream()
                .filter(issue -> issue.getStatus() == IssueStatus.OPEN)
                .map(Issue::getId)
                .toList();
        if (!openIssueIds.isEmpty()) {
            for (Object[] row : voteRepository.countByOptionForIssues(openIssueIds)) {
                counts.put((Long) row[0], ((Long) row[1]).intValue());
            }
        }
        for (Issue issue : issues) {
            if (issue.getStatus() == IssueStatus.OPEN) continue;
            for (IssueOption option : issue.getOptions()) {
                counts.put(option.getId(), option.getVoteCount() == null ? 0 : option.getVoteCount());
            }
        }
        return counts;
    }
}
