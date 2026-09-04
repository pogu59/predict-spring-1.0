package com.predict.controller;

import com.predict.Issue;
import com.predict.IssueOption;
import com.predict.Reply;
import com.predict.User;
import com.predict.Vote;
import com.predict.controller.dto.IssueOptionResponse;
import com.predict.controller.dto.IssueResponse;
import com.predict.controller.dto.ReplyCreateRequest;
import com.predict.controller.dto.ReplyResponse;
import com.predict.controller.dto.VoteRequest;
import com.predict.controller.dto.VoteResponse;
import com.predict.enums.IssueStatus;
import com.predict.repository.IssueRepository;
import com.predict.repository.UserRepository;
import com.predict.repository.VoteRepository;
import com.predict.service.CurrentUserService;
import com.predict.service.ReplyService;
import com.predict.service.VoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
 * 주제 공개 조회, 투표, 댓글. 생성/결과확정/정정/수정 등 관리자 전용 액션은
 * AdminIssueController(/api/admin/issues)로 분리되어 role=admin 게이팅이 걸려 있다.
 */
@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final VoteRepository voteRepository;
    private final VoteService voteService;
    private final ReplyService replyService;
    private final CurrentUserService currentUserService;

    public IssueController(IssueRepository issueRepository, UserRepository userRepository,
                            VoteRepository voteRepository, VoteService voteService,
                            ReplyService replyService, CurrentUserService currentUserService) {
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.voteRepository = voteRepository;
        this.voteService = voteService;
        this.replyService = replyService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<IssueResponse> list(@RequestParam(required = false) Long userId) {
        List<Issue> issues = issueRepository.findAll();
        if (userId == null) {
            return issues.stream().map(IssueResponse::from).toList();
        }
        Map<Long, Vote> myVoteByIssueId = new HashMap<>();
        for (Vote vote : voteRepository.findByUserIdOrderByVotedAtDesc(userId)) {
            myVoteByIssueId.put(vote.getIssue().getId(), vote);
        }
        return issues.stream()
                .map(issue -> {
                    Vote myVote = myVoteByIssueId.get(issue.getId());
                    Long myOptionId = myVote != null ? myVote.getIssueOption().getId() : null;
                    Integer myStake = myVote != null ? myVote.getStake() : null;
                    Map<Long, Integer> liveCounts = needsLiveCounts(issue, myOptionId)
                            ? liveCountsByOptionId(issue)
                            : null;
                    return IssueResponse.from(issue, myOptionId, myStake, liveCounts);
                })
                .toList();
    }

    @GetMapping("/{issueId}")
    public IssueResponse get(@PathVariable Long issueId, @RequestParam(required = false) Long userId) {
        Issue issue = requireIssue(issueId);
        if (userId == null) {
            return IssueResponse.from(issue);
        }
        Vote myVote = voteRepository.findByUserIdAndIssueId(userId, issueId).orElse(null);
        Long myOptionId = myVote != null ? myVote.getIssueOption().getId() : null;
        Integer myStake = myVote != null ? myVote.getStake() : null;
        Map<Long, Integer> liveCounts = needsLiveCounts(issue, myOptionId) ? liveCountsByOptionId(issue) : null;
        return IssueResponse.from(issue, myOptionId, myStake, liveCounts);
    }

    @PostMapping("/{issueId}/votes")
    @ResponseStatus(HttpStatus.CREATED)
    public VoteResponse castVote(@PathVariable Long issueId, @Valid @RequestBody VoteRequest request) {
        User user = requireUser(request.userId());
        Vote vote = voteService.castVote(user, issueId, request.optionId(), request.stake());
        Issue issue = vote.getIssue();
        List<IssueOptionResponse> liveCounts = issue.getOptions().stream()
                .map(option -> new IssueOptionResponse(option.getId(), option.getText(),
                        (int) voteRepository.countByIssueIdAndIssueOptionId(issueId, option.getId())))
                .toList();
        return VoteResponse.of(vote, liveCounts, user.getCredibilityScore());
    }

    @GetMapping("/{issueId}/replies")
    public List<ReplyResponse> listReplies(@PathVariable Long issueId) {
        return replyService.listForIssue(issueId).stream().map(ReplyResponse::from).toList();
    }

    @PostMapping("/{issueId}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    public ReplyResponse createReply(@RequestHeader("Authorization") String authorization,
                                      @PathVariable Long issueId,
                                      @Valid @RequestBody ReplyCreateRequest request) {
        User author = currentUserService.requireUser(authorization);
        Reply reply = replyService.createForIssue(author, issueId, request.content());
        return ReplyResponse.from(reply);
    }

    @DeleteMapping("/{issueId}/replies/{replyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReply(@RequestHeader("Authorization") String authorization,
                             @PathVariable Long issueId, @PathVariable Long replyId) {
        User requester = currentUserService.requireUser(authorization);
        replyService.delete(replyId, requester);
    }

    private boolean needsLiveCounts(Issue issue, Long myOptionId) {
        return issue.getStatus() == IssueStatus.OPEN && myOptionId != null;
    }

    /** OPEN 상태에서 본인에게 노출할 실시간 득표수. IssueOption.voteCount는 마감 전엔 null이라 직접 집계한다. */
    private Map<Long, Integer> liveCountsByOptionId(Issue issue) {
        Map<Long, Integer> counts = new HashMap<>();
        for (IssueOption option : issue.getOptions()) {
            counts.put(option.getId(), (int) voteRepository.countByIssueIdAndIssueOptionId(issue.getId(), option.getId()));
        }
        return counts;
    }

    private Issue requireIssue(Long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주제: " + issueId));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저: " + userId));
    }
}
