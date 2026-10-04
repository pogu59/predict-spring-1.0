package com.predict;

import com.predict.enums.IssueStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 투표 주제. 선택지(IssueOption)별 득표수는 마감 시점에 스냅샷으로 고정해 둔다(정산용).
 */
@Getter
@Entity
@Table(name = "issues")
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issue_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "status", nullable = false, length = 20)
    private IssueStatus status;

    @Column(name = "vote_start_at", nullable = false)
    private LocalDateTime voteStartAt;

    @Column(name = "vote_deadline_at", nullable = false)
    private LocalDateTime voteDeadlineAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    /** 누가 이 결과를 확정했는지(책임 소재 추적용). 정정 시 다시 null로 되돌아간다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    /** 정답 선택지. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "correct_option_id")
    private IssueOption correctOption;

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<IssueOption> options = new ArrayList<>();

    /** 관리자가 올린 커버 이미지 URL(UploadController). 없으면 null. */
    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    /** 소프트 삭제. 삭제된 이슈는 모든 공개/관리자 목록에서 빠진다(AdminIssueService.deleteIssue). */
    @Column(name = "is_deleted", nullable = false, columnDefinition = "boolean default false")
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Issue() {
    }

    public Issue(Category category, String title, String description,
                 LocalDateTime voteStartAt, LocalDateTime voteDeadlineAt, List<String> optionTexts) {
        this(category, title, description, voteStartAt, voteDeadlineAt, optionTexts, null);
    }

    public Issue(Category category, String title, String description,
                 LocalDateTime voteStartAt, LocalDateTime voteDeadlineAt, List<String> optionTexts,
                 String coverImageUrl) {
        this.category = category;
        this.title = title;
        this.description = description;
        this.voteStartAt = voteStartAt;
        this.voteDeadlineAt = voteDeadlineAt;
        this.coverImageUrl = coverImageUrl;
        this.status = IssueStatus.OPEN;
        addOptions(optionTexts);
    }

    /** 마감 시점 득표수를 선택지별 스냅샷으로 기록하고 결과대기 상태로 전환한다. */
    public void closeForResult(Map<Long, Integer> voteCountsByOptionId) {
        for (IssueOption option : options) {
            option.recordVoteCount(voteCountsByOptionId.getOrDefault(option.getId(), 0));
        }
        this.status = IssueStatus.PENDING_RESULT;
    }

    /** 정답을 확정한다. */
    public void confirm(IssueOption correctOption, LocalDateTime confirmedAt, User confirmedBy) {
        this.correctOption = correctOption;
        this.confirmedAt = confirmedAt;
        this.confirmedBy = confirmedBy;
        this.status = IssueStatus.CONFIRMED;
    }

    /**
     * 오확정 정정(SettlementCorrectionService)에서 호출. 결과대기 상태로 되돌려
     * sp_confirm_issue_result에 해당하는 정산 절차를 올바른 정답으로 재실행할 수 있게 한다.
     */
    public void resetForCorrection() {
        this.correctOption = null;
        this.confirmedAt = null;
        this.confirmedBy = null;
        this.status = IssueStatus.PENDING_RESULT;
    }

    /** 참여자 0명일 때만 허용되는 전체 내용 수정(AdminIssueService). 선택지도 통째로 교체된다. */
    public void updateContent(Category category, String title, String description,
                               LocalDateTime voteStartAt, LocalDateTime voteDeadlineAt, List<String> optionTexts,
                               String coverImageUrl) {
        this.category = category;
        this.coverImageUrl = coverImageUrl;
        this.title = title;
        this.description = description;
        this.voteStartAt = voteStartAt;
        this.voteDeadlineAt = voteDeadlineAt;
        this.options.clear();
        addOptions(optionTexts);
    }

    /** 마감시각 연장 전용(AdminIssueService). 단축은 허용하지 않는다(서비스 레이어에서 검증). */
    public void extendDeadline(LocalDateTime newDeadline) {
        this.voteDeadlineAt = newDeadline;
    }

    /**
     * 결과대기 이슈의 마감을 미래로 연장하면 다시 진행중으로 되돌린다. 마감 시점 득표 스냅샷은
     * 다음 마감 때 다시 찍히므로 비워 둔다(진행중 동안 voteCount는 항상 null이라는 불변식 유지).
     */
    public void reopen(LocalDateTime newDeadline) {
        this.voteDeadlineAt = newDeadline;
        this.status = IssueStatus.OPEN;
        for (IssueOption option : options) {
            option.clearVoteCount();
        }
    }

    public void markDeleted(LocalDateTime deletedAt) {
        this.deleted = true;
        this.deletedAt = deletedAt;
    }

    private void addOptions(List<String> optionTexts) {
        int order = 0;
        for (String text : optionTexts) {
            this.options.add(new IssueOption(this, text, order++));
        }
    }

}
