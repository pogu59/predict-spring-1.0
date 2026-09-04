package com.predict.service;

import com.predict.Issue;
import com.predict.Post;
import com.predict.Reply;
import com.predict.User;
import com.predict.enums.Role;
import com.predict.repository.IssueRepository;
import com.predict.repository.ReplyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 이슈 상세 댓글과 게시판 댓글을 함께 다루는 서비스. Reply 엔티티가 issue_id/post_id 중
 * 정확히 하나만 채워지는 구조를 그대로 반영해, 생성 메서드도 createForIssue/createForPost로
 * 나눈다 — IssueController와 PostController 양쪽이 이 서비스 하나를 공유한다.
 */
@Service
public class ReplyService {

    private final ReplyRepository replyRepository;
    private final IssueRepository issueRepository;
    private final PostService postService;

    public ReplyService(ReplyRepository replyRepository, IssueRepository issueRepository, PostService postService) {
        this.replyRepository = replyRepository;
        this.issueRepository = issueRepository;
        this.postService = postService;
    }

    @Transactional
    public Reply createForIssue(User author, Long issueId, String content) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이슈: " + issueId));
        return replyRepository.save(Reply.forIssue(author, issue, content));
    }

    @Transactional
    public Reply createForPost(User author, Long postId, String content) {
        Post post = postService.requirePost(postId);
        return replyRepository.save(Reply.forPost(author, post, content));
    }

    public List<Reply> listForIssue(Long issueId) {
        return replyRepository.findByIssueIdAndDeletedFalseOrderByCreatedAtAsc(issueId);
    }

    public List<Reply> listForPost(Long postId) {
        return replyRepository.findByPostIdAndDeletedFalseOrderByCreatedAtAsc(postId);
    }

    @Transactional
    public void delete(Long replyId, User requester) {
        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글: " + replyId));
        if (!reply.isAuthor(requester) && requester.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 댓글이거나 관리자만 삭제할 수 있습니다");
        }
        reply.delete(LocalDateTime.now());
    }
}
