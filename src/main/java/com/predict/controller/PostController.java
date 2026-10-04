package com.predict.controller;

import com.predict.Post;
import com.predict.Reply;
import com.predict.Report;
import com.predict.User;
import com.predict.controller.dto.CommunityRequests.LikeResponse;
import com.predict.controller.dto.CommunityRequests.ReportRequest;
import com.predict.controller.dto.PageResponse;
import com.predict.controller.dto.PostCreateRequest;
import com.predict.controller.dto.PostDetailResponse;
import com.predict.controller.dto.PostListItemResponse;
import com.predict.controller.dto.ReplyCreateRequest;
import com.predict.controller.dto.ReplyResponse;
import com.predict.service.CurrentUserService;
import com.predict.service.PostService;
import com.predict.service.ReplyService;
import com.predict.service.ReportService;
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

import java.util.List;

/**
 * 자유 게시판 — 글 목록/상세/작성/수정/삭제, 좋아요·신고·작성자 숨기기 + 댓글(1단계 대댓글).
 * 댓글은 ReplyService를 IssueController와 공유한다.
 */
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final ReplyService replyService;
    private final ReportService reportService;
    private final CurrentUserService currentUserService;

    public PostController(PostService postService, ReplyService replyService, ReportService reportService,
                          CurrentUserService currentUserService) {
        this.postService = postService;
        this.replyService = replyService;
        this.reportService = reportService;
        this.currentUserService = currentUserService;
    }

    /** sort: hot(좋아요+댓글x3) | new(최신). author=me면 로그인한 유저의 글만. */
    @GetMapping
    public PageResponse<PostListItemResponse> list(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(defaultValue = "new") String sort,
                                                     @RequestParam(required = false) String author,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        User viewer = currentUserService.optionalUser(authorization);
        return postService.list(keyword, sort, "me".equals(author), viewer, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostDetailResponse create(@RequestHeader("Authorization") String authorization,
                                      @Valid @RequestBody PostCreateRequest request) {
        User author = currentUserService.requireActiveUser(authorization);
        Post post = postService.create(author, request.title().trim(), request.content(), request.imagesOrEmpty());
        return postService.toDetail(post);
    }

    @GetMapping("/{postId}")
    public PostDetailResponse get(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @PathVariable Long postId) {
        return postService.viewDetail(postId, currentUserService.optionalUser(authorization));
    }

    @PutMapping("/{postId}")
    public PostDetailResponse update(@RequestHeader("Authorization") String authorization,
                                     @PathVariable Long postId, @Valid @RequestBody PostCreateRequest request) {
        User requester = currentUserService.requireActiveUser(authorization);
        Post post = postService.update(postId, requester, request.title().trim(), request.content(),
                request.imagesOrEmpty());
        return postService.toDetail(post);
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("Authorization") String authorization, @PathVariable Long postId) {
        User requester = currentUserService.requireUser(authorization);
        postService.delete(postId, requester);
    }

    @PostMapping("/{postId}/like")
    public LikeResponse like(@RequestHeader("Authorization") String authorization, @PathVariable Long postId) {
        return postService.toggleLike(postId, currentUserService.requireUser(authorization));
    }

    @PostMapping("/{postId}/reports")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@RequestHeader("Authorization") String authorization, @PathVariable Long postId,
                       @Valid @RequestBody ReportRequest request) {
        reportService.report(Report.TargetType.POST, postId, currentUserService.requireUser(authorization),
                request.reason());
    }

    /** "이 사용자의 글 숨기기" — 이 글의 작성자 글을 내 게시판 목록에서 뺀다. */
    @PostMapping("/{postId}/hide-author")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hideAuthor(@RequestHeader("Authorization") String authorization, @PathVariable Long postId) {
        postService.hideAuthor(postId, currentUserService.requireUser(authorization));
    }

    @GetMapping("/{postId}/replies")
    public List<ReplyResponse> listReplies(@RequestHeader(value = "Authorization", required = false) String authorization,
                                           @PathVariable Long postId) {
        return replyService.listForPost(postId, currentUserService.optionalUser(authorization));
    }

    @PostMapping("/{postId}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    public ReplyResponse createReply(@RequestHeader("Authorization") String authorization,
                                      @PathVariable Long postId,
                                      @Valid @RequestBody ReplyCreateRequest request) {
        User author = currentUserService.requireActiveUser(authorization);
        Reply reply = replyService.createForPost(author, postId, request.content(), request.parentId());
        return ReplyResponse.from(reply);
    }

    @DeleteMapping("/{postId}/replies/{replyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReply(@RequestHeader("Authorization") String authorization,
                             @PathVariable Long postId, @PathVariable Long replyId) {
        User requester = currentUserService.requireUser(authorization);
        replyService.delete(replyId, requester);
    }

    @PostMapping("/{postId}/replies/{replyId}/like")
    public LikeResponse likeReply(@RequestHeader("Authorization") String authorization,
                                  @PathVariable Long postId, @PathVariable Long replyId) {
        return replyService.toggleLike(replyId, currentUserService.requireUser(authorization));
    }

    @PostMapping("/{postId}/replies/{replyId}/reports")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reportReply(@RequestHeader("Authorization") String authorization,
                            @PathVariable Long postId, @PathVariable Long replyId,
                            @Valid @RequestBody ReportRequest request) {
        reportService.report(Report.TargetType.REPLY, replyId, currentUserService.requireUser(authorization),
                request.reason());
    }
}
