package com.predict.service;

import com.predict.Category;
import com.predict.Issue;
import com.predict.enums.IssueStatus;
import com.predict.repository.CategoryRepository;
import com.predict.repository.IssueRepository;
import com.predict.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 페이지의 주제 생성/수정 (docs/predict.md 5-4절, schema_8.sql sp_update_issue /
 * sp_extend_issue_deadline). 참여자가 없으면 전체 수정, 있으면 마감시각 연장만 허용한다 —
 * 이미 투표한 유저가 본 질문이 사후에 바뀌는 공정성 문제를 막기 위함이다.
 */
@Service
public class AdminIssueService {

    private final IssueRepository issueRepository;
    private final CategoryRepository categoryRepository;
    private final VoteRepository voteRepository;

    public AdminIssueService(IssueRepository issueRepository, CategoryRepository categoryRepository,
                              VoteRepository voteRepository) {
        this.issueRepository = issueRepository;
        this.categoryRepository = categoryRepository;
        this.voteRepository = voteRepository;
    }

    @Transactional
    public Issue createIssue(Integer categoryId, String title, String description,
                              LocalDateTime voteStartAt, LocalDateTime voteDeadlineAt, List<String> options) {
        requireFutureVotingWindow(voteStartAt, voteDeadlineAt);
        Category category = requireCategory(categoryId);
        return issueRepository.save(new Issue(category, title, description, voteStartAt, voteDeadlineAt, options));
    }

    @Transactional
    public void updateIssue(Long issueId, Integer categoryId, String title, String description,
                             LocalDateTime voteStartAt, LocalDateTime voteDeadlineAt, List<String> options) {
        Issue issue = requireIssue(issueId);
        if (issue.getStatus() != IssueStatus.OPEN) {
            throw new IllegalStateException("진행중(open) 상태인 주제만 수정할 수 있습니다.");
        }
        if (voteRepository.countByIssueId(issueId) > 0) {
            throw new IllegalStateException("이미 참여자가 있는 주제는 내용을 수정할 수 없습니다. 마감시각 연장만 가능합니다.");
        }
        requireFutureVotingWindow(voteStartAt, voteDeadlineAt);

        Category category = requireCategory(categoryId);
        issue.updateContent(category, title, description, voteStartAt, voteDeadlineAt, options);
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
    public void extendDeadline(Long issueId, LocalDateTime newDeadline) {
        Issue issue = requireIssue(issueId);
        if (issue.getStatus() != IssueStatus.OPEN) {
            throw new IllegalStateException("진행중(open)인 주제만 마감시각을 조정할 수 있습니다.");
        }
        if (!newDeadline.isAfter(issue.getVoteDeadlineAt())) {
            throw new IllegalStateException("새 마감시각은 기존 마감시각보다 늦어야 합니다 (연장만 가능, 단축 불가).");
        }
        issue.extendDeadline(newDeadline);
    }

    private Issue requireIssue(Long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주제: " + issueId));
    }

    private Category requireCategory(Integer categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리: " + categoryId));
    }
}
