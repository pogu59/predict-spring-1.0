package com.predict.controller;

import com.predict.Post;
import com.predict.Reply;
import com.predict.User;
import com.predict.controller.dto.PageResponse;
import com.predict.controller.dto.PostCreateRequest;
import com.predict.controller.dto.PostDetailResponse;
import com.predict.controller.dto.PostListItemResponse;
import com.predict.controller.dto.ReplyCreateRequest;
import com.predict.controller.dto.ReplyResponse;
import com.predict.repository.PostRepository;
import com.predict.repository.ReplyRepository;
import com.predict.service.CurrentUserService;
import com.predict.service.PostService;
import com.predict.service.ReplyService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
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

import java.util.List;

/**
 * 자유 게시판 — 글 목록/상세/작성/삭제 + 댓글. 댓글은 ReplyService를 IssueController와
 * 공유한다(설계 근거: 커뮤니티 기능 설계안 02절 — 댓글 테이블 하나에 issue_id/post_id
 * nullable FK 두 개로 이슈 댓글과 게시판 댓글을 함께 수용).
 */
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostRepository postRepository;
    private final ReplyRepository replyRepository;
    private final PostService postService;
    private final ReplyService replyService;
    private final CurrentUserService currentUserService;

    public PostController(PostRepository postRepository, ReplyRepository replyRepository,
                           PostService postService, ReplyService replyService,
                           CurrentUserService currentUserService) {
        this.postRepository = postRepository;
        this.replyRepository = replyRepository;
        this.postService = postService;
        this.replyService = replyService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public PageResponse<PostListItemResponse> list(@RequestParam(required = false) String keyword,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        var result = postRepository.search(keyword, PageRequest.of(page, size));
        return PageResponse.from(result,
                post -> PostListItemResponse.from(post, replyRepository.countByPostIdAndDeletedFalse(post.getId())));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostDetailResponse create(@RequestHeader("Authorization") String authorization,
                                      @Valid @RequestBody PostCreateRequest request) {
        User author = currentUserService.requireUser(authorization);
        Post post = postService.create(author, request.title(), request.content());
        return PostDetailResponse.from(post);
    }

    @GetMapping("/{postId}")
    public PostDetailResponse get(@PathVariable Long postId) {
        return PostDetailResponse.from(postService.viewDetail(postId));
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("Authorization") String authorization, @PathVariable Long postId) {
        User requester = currentUserService.requireUser(authorization);
        postService.delete(postId, requester);
    }

    @GetMapping("/{postId}/replies")
    public List<ReplyResponse> listReplies(@PathVariable Long postId) {
        return replyService.listForPost(postId).stream().map(ReplyResponse::from).toList();
    }

    @PostMapping("/{postId}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    public ReplyResponse createReply(@RequestHeader("Authorization") String authorization,
                                      @PathVariable Long postId,
                                      @Valid @RequestBody ReplyCreateRequest request) {
        User author = currentUserService.requireUser(authorization);
        Reply reply = replyService.createForPost(author, postId, request.content());
        return ReplyResponse.from(reply);
    }

    @DeleteMapping("/{postId}/replies/{replyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReply(@RequestHeader("Authorization") String authorization,
                             @PathVariable Long postId, @PathVariable Long replyId) {
        User requester = currentUserService.requireUser(authorization);
        replyService.delete(replyId, requester);
    }
}
