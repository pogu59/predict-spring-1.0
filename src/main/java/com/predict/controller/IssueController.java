package com.predict.controller;

import com.predict.Issue;
import com.predict.Reply;
import com.predict.Report;
import com.predict.User;
import com.predict.Vote;
import com.predict.controller.dto.CommunityRequests.ChangeVoteRequest;
import com.predict.controller.dto.CommunityRequests.LikeResponse;
import com.predict.controller.dto.CommunityRequests.ReportRequest;
import com.predict.controller.dto.IssueOptionResponse;
import com.predict.controller.dto.IssueResponse;
import com.predict.controller.dto.ReplyCreateRequest;
import com.predict.controller.dto.ReplyResponse;
import com.predict.controller.dto.VoteRequest;
import com.predict.controller.dto.VoteResponse;
import com.predict.repository.IssueRepository;
import com.predict.repository.UserRepository;
import com.predict.repository.VoteRepository;
import com.predict.service.CurrentUserService;
import com.predict.service.ReplyService;
import com.predict.service.ReportService;
import com.predict.service.VoteCountService;
import com.predict.service.VoteService;
import jakarta.validation.Valid;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 주제 공개 조회, 투표/선택 변경, 댓글. 생성/결과확정/정정/수정/삭제 등 관리자 전용 액션은
 * AdminIssueController(/api/admin/issues)로 분리되어 role=admin 게이팅이 걸려 있다.
 */
@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final VoteRepository voteRepository;
    private final VoteService voteService;
    private final VoteCountService voteCountService;
    private final ReplyService replyService;
    private final ReportService reportService;
    private final CurrentUserService currentUserService;

    public IssueController(IssueRepository issueRepository, UserRepository userRepository,
                            VoteRepository voteRepository, VoteService voteService, VoteCountService voteCountService,
                            ReplyService replyService, ReportService reportService,
                            CurrentUserService currentUserService) {
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.voteRepository = voteRepository;
        this.voteService = voteService;
        this.voteCountService = voteCountService;
        this.replyService = replyService;
        this.reportService = reportService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<IssueResponse> list(@RequestParam(required = false) Long userId) {
        List<Issue> issues = issueRepository.findByDeletedFalse();
        Map<Long, Integer> counts = voteCountService.countsByOption(issues);
        Map<Long, Vote> myVoteByIssueId = new HashMap<>();
        if (userId != null) {
            for (Vote vote : voteRepository.findByUserIdOrderByVotedAtDesc(userId)) {
                myVoteByIssueId.put(vote.getIssue().getId(), vote);
            }
        }
        return issues.stream()
                .map(issue -> toResponse(issue, counts, myVoteByIssueId.get(issue.getId())))
                .toList();
    }

    @GetMapping("/{issueId}")
    public IssueResponse get(@PathVariable Long issueId, @RequestParam(required = false) Long userId) {
        Issue issue = requireIssue(issueId);
        Vote myVote = userId == null ? null : voteRepository.findByUserIdAndIssueId(userId, issueId).orElse(null);
        return toResponse(issue, voteCountService.countsByOption(issue), myVote);
    }

    @PostMapping("/{issueId}/votes")
    @ResponseStatus(HttpStatus.CREATED)
    public VoteResponse castVote(@PathVariable Long issueId, @Valid @RequestBody VoteRequest request) {
        User user = requireUser(request.userId());
        Vote vote = voteService.castVote(user, issueId, request.optionId(), request.stake());
        return VoteResponse.of(vote, percentsOf(vote.getIssue()), user.getCredibilityScore());
    }

    /** 마감 전 선택 변경 — 스테이크는 최초 투표 때 건 그대로 유지된다. */
    @PutMapping("/{issueId}/votes/me")
    public VoteResponse changeVote(@RequestHeader("Authorization") String authorization,
                                    @PathVariable Long issueId, @Valid @RequestBody ChangeVoteRequest request) {
        User user = currentUserService.requireUser(authorization);
        Vote vote = voteService.changeVote(user, issueId, request.optionId());
        return VoteResponse.of(vote, percentsOf(vote.getIssue()), user.getCredibilityScore());
    }

    @GetMapping("/{issueId}/replies")
    public List<ReplyResponse> listReplies(@RequestHeader(value = "Authorization", required = false) String authorization,
                                           @PathVariable Long issueId) {
        return replyService.listForIssue(issueId, currentUserService.optionalUser(authorization));
    }

    @PostMapping("/{issueId}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    public ReplyResponse createReply(@RequestHeader("Authorization") String authorization,
                                      @PathVariable Long issueId,
                                      @Valid @RequestBody ReplyCreateRequest request) {
        User author = currentUserService.requireActiveUser(authorization);
        Reply reply = replyService.createForIssue(author, issueId, request.content());
        Long authorOptionId = voteRepository.findByUserIdAndIssueId(author.getId(), issueId)
                .map(vote -> vote.getIssueOption().getId())
                .orElse(null);
        return ReplyResponse.from(reply, 0, false, authorOptionId, List.of(), replyService.crewNameOf(author));
    }

    @DeleteMapping("/{issueId}/replies/{replyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReply(@RequestHeader("Authorization") String authorization,
                             @PathVariable Long issueId, @PathVariable Long replyId) {
        User requester = currentUserService.requireUser(authorization);
        replyService.delete(replyId, requester);
    }

    @PostMapping("/{issueId}/replies/{replyId}/like")
    public LikeResponse likeReply(@RequestHeader("Authorization") String authorization,
                                  @PathVariable Long issueId, @PathVariable Long replyId) {
        return replyService.toggleLike(replyId, currentUserService.requireUser(authorization));
    }

    @PostMapping("/{issueId}/replies/{replyId}/reports")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reportReply(@RequestHeader("Authorization") String authorization,
                            @PathVariable Long issueId, @PathVariable Long replyId,
                            @Valid @RequestBody ReportRequest request) {
        reportService.report(Report.TargetType.REPLY, replyId, currentUserService.requireUser(authorization),
                request.reason());
    }

    private IssueResponse toResponse(Issue issue, Map<Long, Integer> counts, Vote myVote) {
        return IssueResponse.from(issue, counts,
                myVote != null ? myVote.getIssueOption().getId() : null,
                myVote != null ? myVote.getStake() : null);
    }

    private List<IssueOptionResponse> percentsOf(Issue issue) {
        return IssueOptionResponse.percentsOf(issue.getOptions(), voteCountService.countsByOption(issue));
    }

    private Issue requireIssue(Long issueId) {
        return issueRepository.findById(issueId)
                .filter(issue -> !issue.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주제: " + issueId));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저: " + userId));
    }
}
