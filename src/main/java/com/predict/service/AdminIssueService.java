package com.predict.service;

import com.predict.Category;
import com.predict.Issue;
import com.predict.Vote;
import com.predict.enums.IssueStatus;
import com.predict.repository.CategoryRepository;
import com.predict.repository.IssueRepository;
import com.predict.repository.VoteRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

/**
 * 관리자 페이지의 주제 생성/수정/마감 연장/삭제 (docs/predict.md 5-4절). 참여자가 없으면 전체 수정,
 * 있으면 마감시각 연장만 허용한다 — 이미 투표한 유저가 본 질문이 사후에 바뀌는 공정성 문제를 막기 위함이다.
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
                              LocalDateTime voteStartAt, LocalDateTime voteDeadlineAt, List<String> options,
                              String coverImageUrl) {
        requireFutureVotingWindow(voteStartAt, voteDeadlineAt);
        List<String> cleaned = requireValidOptions(options);
        Category category = resolveCategory(categoryId);
        return issueRepository.save(new Issue(category, title.trim(), blankToNull(description), voteStartAt,
                voteDeadlineAt, cleaned, blankToNull(coverImageUrl)));
    }

    @Transactional
    public void updateIssue(Long issueId, Integer categoryId, String title, String description,
                             LocalDateTime voteStartAt, LocalDateTime voteDeadlineAt, List<String> options,
                             String coverImageUrl) {
        Issue issue = requireIssue(issueId);
        if (issue.getStatus() != IssueStatus.OPEN) {
            throw new IllegalStateException("진행중(open) 상태인 주제만 수정할 수 있습니다.");
        }
        if (voteRepository.countByIssueId(issueId) > 0) {
            throw new IllegalStateException("이미 참여자가 있는 주제는 내용을 수정할 수 없습니다. 마감시각 연장만 가능합니다.");
        }
        // 이미 시작된 이슈를 수정할 때 시작 시각은 그대로 둘 수 있게, 시작 시각 검증은 바뀐 경우에만 한다.
        LocalDateTime start = voteStartAt.equals(issue.getVoteStartAt()) ? null : voteStartAt;
        requireFutureVotingWindow(start, voteDeadlineAt);
        if (!voteDeadlineAt.isAfter(voteStartAt)) {
            throw new IllegalArgumentException("마감 시각은 시작 시각보다 늦어야 합니다.");
        }
        List<String> cleaned = requireValidOptions(options);
        Category category = categoryId != null ? resolveCategory(categoryId) : issue.getCategory();
        issue.updateContent(category, title.trim(), blankToNull(description), voteStartAt, voteDeadlineAt, cleaned,
                blankToNull(coverImageUrl));
    }

    /**
     * 마감 연장. 진행중이면 마감만 늦추고, 결과대기 이슈를 현재 시각 이후로 연장하면 다시 진행중으로 되돌린다.
     * 확정된 이슈는 정산이 끝났으므로 연장할 수 없다.
     */
    @Transactional
    public void extendDeadline(Long issueId, LocalDateTime newDeadline) {
        Issue issue = requireIssue(issueId);
        if (issue.getStatus() == IssueStatus.CONFIRMED) {
            throw new IllegalStateException("확정된 주제는 마감시각을 조정할 수 없습니다.");
        }
        if (!newDeadline.isAfter(issue.getVoteDeadlineAt())) {
            throw new IllegalStateException("현재 마감보다 이후로 설정해 주세요");
        }
        if (issue.getStatus() == IssueStatus.PENDING_RESULT) {
            if (!newDeadline.isAfter(LocalDateTime.now())) {
                throw new IllegalStateException("결과 대기 이슈는 현재 시각 이후로 연장해야 다시 열 수 있어요");
            }
            issue.reopen(newDeadline);
            return;
        }
        issue.extendDeadline(newDeadline);
    }

    /**
     * 이슈 삭제(소프트). 아직 정산되지 않은 이슈라면 참여자 전원의 베팅액을 전액 돌려준다 —
     * 확정된 이슈는 이미 정산으로 원금이 돌아갔으므로 환불하지 않는다.
     */
    @Transactional
    public void deleteIssue(Long issueId) {
        Issue issue = requireIssue(issueId);
        if (issue.getStatus() != IssueStatus.CONFIRMED) {
            for (Vote vote : voteRepository.findByIssueId(issueId)) {
                vote.getUser().applyScoreDelta(vote.getStake());
            }
        }
        issue.markDeleted(LocalDateTime.now());
    }

    /** 시작(주어졌을 때)/마감 시각은 현재 시각 이후여야 하고, 마감은 시작보다 늦어야 한다. */
    private void requireFutureVotingWindow(LocalDateTime voteStartAt, LocalDateTime voteDeadlineAt) {
        LocalDateTime now = LocalDateTime.now();
        if (voteStartAt != null && voteStartAt.isBefore(now.minusMinutes(1))) {
            throw new IllegalArgumentException("투표 시작 시각은 현재 시각 이후여야 합니다.");
        }
        if (!voteDeadlineAt.isAfter(now)) {
            throw new IllegalArgumentException("현재 시각 이후로 설정해 주세요");
        }
        if (voteStartAt != null && !voteDeadlineAt.isAfter(voteStartAt)) {
            throw new IllegalArgumentException("마감 시각은 시작 시각보다 늦어야 합니다.");
        }
    }

    private List<String> requireValidOptions(List<String> options) {
        List<String> cleaned = options.stream().map(String::trim).toList();
        if (cleaned.size() < 2 || cleaned.size() > 6) {
            throw new IllegalArgumentException("선택지는 2개 이상 6개 이하로 입력해 주세요.");
        }
        if (cleaned.stream().anyMatch(String::isEmpty)) {
            throw new IllegalArgumentException("빈 선택지를 채우거나 삭제해 주세요");
        }
        if (new HashSet<>(cleaned).size() != cleaned.size()) {
            throw new IllegalArgumentException("같은 선택지가 있어요");
        }
        return cleaned;
    }

    private Issue requireIssue(Long issueId) {
        return issueRepository.findById(issueId)
                .filter(issue -> !issue.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주제: " + issueId));
    }

    /** 카테고리는 UI에서 없앴다 — 지정이 없으면 가장 앞 카테고리에 넣는다. */
    private Category resolveCategory(Integer categoryId) {
        if (categoryId != null) {
            return categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리: " + categoryId));
        }
        return categoryRepository.findAll(Sort.by("id")).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("카테고리가 하나도 없습니다."));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
