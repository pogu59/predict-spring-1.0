package com.predict.controller;

import com.predict.Issue;
import com.predict.User;
import com.predict.controller.dto.AdminIssueDetailResponse;
import com.predict.controller.dto.AdminIssueListItemResponse;
import com.predict.controller.dto.IssueConfirmRequest;
import com.predict.controller.dto.IssueCreateRequest;
import com.predict.controller.dto.IssueExtendDeadlineRequest;
import com.predict.controller.dto.PageResponse;
import com.predict.enums.IssueStatus;
import com.predict.repository.IssueRepository;
import com.predict.repository.VoteRepository;
import com.predict.service.AdminIssueService;
import com.predict.service.CurrentUserService;
import com.predict.service.SettlementCorrectionService;
import com.predict.service.SettlementService;
import com.predict.service.VoteCountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 관리자 페이지 — 주제 생성/조회/확정/정정/수정/마감 연장/삭제 (docs/predict.md 5장).
 * 모든 엔드포인트는 관리자(role=admin)만 호출할 수 있다.
 */
@RestController
@RequestMapping("/api/admin/issues")
public class AdminIssueController {

    private final IssueRepository issueRepository;
    private final VoteRepository voteRepository;
    private final CurrentUserService currentUserService;
    private final AdminIssueService adminIssueService;
    private final SettlementService settlementService;
    private final SettlementCorrectionService settlementCorrectionService;
    private final VoteCountService voteCountService;

    public AdminIssueController(IssueRepository issueRepository, VoteRepository voteRepository,
                                 CurrentUserService currentUserService, AdminIssueService adminIssueService,
                                 SettlementService settlementService,
                                 SettlementCorrectionService settlementCorrectionService,
                                 VoteCountService voteCountService) {
        this.issueRepository = issueRepository;
        this.voteRepository = voteRepository;
        this.currentUserService = currentUserService;
        this.adminIssueService = adminIssueService;
        this.settlementService = settlementService;
        this.settlementCorrectionService = settlementCorrectionService;
        this.voteCountService = voteCountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminIssueDetailResponse create(@RequestHeader("Authorization") String authorization,
                                            @Valid @RequestBody IssueCreateRequest request) {
        currentUserService.requireAdmin(authorization);
        Issue issue = adminIssueService.createIssue(request.categoryId(), request.title(), request.description(),
                request.voteStartAt(), request.voteDeadlineAt(), request.options(), request.coverImageUrl());
        return toDetail(issue);
    }

    @GetMapping
    public PageResponse<AdminIssueListItemResponse> list(@RequestHeader("Authorization") String authorization,
                                                           @RequestParam(required = false) Integer categoryId,
                                                           @RequestParam(required = false) IssueStatus status,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        currentUserService.requireAdmin(authorization);
        var result = issueRepository.search(categoryId, status, keyword, PageRequest.of(page, Math.min(size, 500)));
        Map<Long, Integer> counts = voteCountService.countsByOption(result.getContent());
        return PageResponse.from(result, issue -> AdminIssueListItemResponse.from(issue, counts));
    }

    @GetMapping("/{issueId}")
    public AdminIssueDetailResponse get(@RequestHeader("Authorization") String authorization,
                                         @PathVariable Long issueId) {
        currentUserService.requireAdmin(authorization);
        return toDetail(requireIssue(issueId));
    }

    @PostMapping("/{issueId}/confirm")
    public AdminIssueDetailResponse confirm(@RequestHeader("Authorization") String authorization,
                                             @PathVariable Long issueId,
                                             @Valid @RequestBody IssueConfirmRequest request) {
        User admin = currentUserService.requireAdmin(authorization);
        requireIssue(issueId);
        settlementService.confirmIssue(issueId, request.correctOptionId(), admin);
        return toDetail(requireIssue(issueId));
    }

    @PostMapping("/{issueId}/correct")
    public AdminIssueDetailResponse correct(@RequestHeader("Authorization") String authorization,
                                             @PathVariable Long issueId) {
        currentUserService.requireAdmin(authorization);
        requireIssue(issueId);
        settlementCorrectionService.correctIssue(issueId);
        return toDetail(requireIssue(issueId));
    }

    @PutMapping("/{issueId}")
    public AdminIssueDetailResponse update(@RequestHeader("Authorization") String authorization,
                                            @PathVariable Long issueId,
                                            @Valid @RequestBody IssueCreateRequest request) {
        currentUserService.requireAdmin(authorization);
        adminIssueService.updateIssue(issueId, request.categoryId(), request.title(), request.description(),
                request.voteStartAt(), request.voteDeadlineAt(), request.options(), request.coverImageUrl());
        return toDetail(requireIssue(issueId));
    }

    @PostMapping("/{issueId}/extend-deadline")
    public AdminIssueDetailResponse extendDeadline(@RequestHeader("Authorization") String authorization,
                                                     @PathVariable Long issueId,
                                                     @Valid @RequestBody IssueExtendDeadlineRequest request) {
        currentUserService.requireAdmin(authorization);
        adminIssueService.extendDeadline(issueId, request.newDeadline());
        return toDetail(requireIssue(issueId));
    }

    /** 이슈와 댓글을 숨기고(소프트 삭제), 정산 전이면 걸린 신용도를 참여자에게 전액 돌려준다. */
    @DeleteMapping("/{issueId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("Authorization") String authorization, @PathVariable Long issueId) {
        currentUserService.requireAdmin(authorization);
        adminIssueService.deleteIssue(issueId);
    }

    private AdminIssueDetailResponse toDetail(Issue issue) {
        return AdminIssueDetailResponse.from(issue, voteCountService.countsByOption(issue),
                voteRepository.countByIssueId(issue.getId()));
    }

    private Issue requireIssue(Long issueId) {
        return issueRepository.findById(issueId)
                .filter(issue -> !issue.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주제: " + issueId));
    }
}
