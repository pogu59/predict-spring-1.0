package com.predict.service;

import com.predict.Issue;
import com.predict.Post;
import com.predict.Reply;
import com.predict.ReplyLike;
import com.predict.User;
import com.predict.Vote;
import com.predict.controller.dto.CommunityRequests.LikeResponse;
import com.predict.controller.dto.ReplyResponse;
import com.predict.enums.Role;
import com.predict.repository.IssueRepository;
import com.predict.repository.ReplyLikeRepository;
import com.predict.repository.ReplyRepository;
import com.predict.repository.VoteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 이슈 상세 댓글과 게시판 댓글을 함께 다루는 서비스. Reply 엔티티가 issue_id/post_id 중
 * 정확히 하나만 채워지는 구조를 그대로 반영해, 생성 메서드도 createForIssue/createForPost로
 * 나눈다 — IssueController와 PostController 양쪽이 이 서비스 하나를 공유한다.
 */
@Service
public class ReplyService {

    private final ReplyRepository replyRepository;
    private final ReplyLikeRepository replyLikeRepository;
    private final IssueRepository issueRepository;
    private final VoteRepository voteRepository;
    private final PostService postService;

    public ReplyService(ReplyRepository replyRepository, ReplyLikeRepository replyLikeRepository,
                        IssueRepository issueRepository, VoteRepository voteRepository, PostService postService) {
        this.replyRepository = replyRepository;
        this.replyLikeRepository = replyLikeRepository;
        this.issueRepository = issueRepository;
        this.voteRepository = voteRepository;
        this.postService = postService;
    }

    @Transactional
    public Reply createForIssue(User author, Long issueId, String content) {
        Issue issue = issueRepository.findById(issueId)
                .filter(i -> !i.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이슈: " + issueId));
        return replyRepository.save(Reply.forIssue(author, issue, content));
    }

    /** parentId가 대댓글을 가리키면 그 대댓글의 부모(최상위 댓글)에 붙인다 — 대댓글은 1단계까지만. */
    @Transactional
    public Reply createForPost(User author, Long postId, String content, Long parentId) {
        Post post = postService.requirePost(postId);
        Reply parent = null;
        if (parentId != null) {
            parent = replyRepository.findById(parentId)
                    .filter(r -> !r.isDeleted() && r.getPost() != null && r.getPost().getId().equals(postId))
                    .orElseThrow(() -> new IllegalArgumentException("답글을 달 수 없는 댓글이에요"));
            if (parent.getParent() != null) {
                parent = parent.getParent();
            }
        }
        return replyRepository.save(Reply.forPost(author, post, content, parent));
    }

    /** 이슈 댓글 — 작성자가 투표했다면 현재 선택지(authorOptionId)를 함께 싣는다. */
    @Transactional(readOnly = true)
    public List<ReplyResponse> listForIssue(Long issueId, User viewer) {
        List<Reply> replies = replyRepository.findByIssueIdAndDeletedFalseAndHiddenFalseOrderByCreatedAtAsc(issueId);
        Map<Long, Long> optionByUserId = new HashMap<>();
        for (Vote vote : voteRepository.findByIssueId(issueId)) {
            optionByUserId.put(vote.getUser().getId(), vote.getIssueOption().getId());
        }
        LikeInfo likes = likeInfo(replies, viewer);
        return replies.stream()
                .map(r -> ReplyResponse.from(r, likes.count(r), likes.liked(r),
                        optionByUserId.get(r.getAuthor().getId()), List.of()))
                .toList();
    }

    /** 게시판 댓글 — 최상위 댓글 아래 1단계 대댓글을 묶어서 돌려준다. 부모가 지워진 대댓글은 최상위로 올린다. */
    @Transactional(readOnly = true)
    public List<ReplyResponse> listForPost(Long postId, User viewer) {
        List<Reply> replies = replyRepository.findByPostIdAndDeletedFalseAndHiddenFalseOrderByCreatedAtAsc(postId);
        LikeInfo likes = likeInfo(replies, viewer);
        Set<Long> visibleIds = new HashSet<>();
        replies.forEach(r -> visibleIds.add(r.getId()));

        Map<Long, List<ReplyResponse>> childrenByParent = new HashMap<>();
        for (Reply r : replies) {
            if (r.getParent() != null && visibleIds.contains(r.getParent().getId())) {
                childrenByParent.computeIfAbsent(r.getParent().getId(), k -> new ArrayList<>())
                        .add(ReplyResponse.from(r, likes.count(r), likes.liked(r), null, List.of()));
            }
        }
        Map<Long, ReplyResponse> roots = new LinkedHashMap<>();
        for (Reply r : replies) {
            boolean nested = r.getParent() != null && visibleIds.contains(r.getParent().getId());
            if (!nested) {
                roots.put(r.getId(), ReplyResponse.from(r, likes.count(r), likes.liked(r), null,
                        childrenByParent.getOrDefault(r.getId(), List.of())));
            }
        }
        return new ArrayList<>(roots.values());
    }

    @Transactional
    public void delete(Long replyId, User requester) {
        Reply reply = requireReply(replyId);
        if (!reply.isAuthor(requester) && requester.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 댓글이거나 관리자만 삭제할 수 있습니다");
        }
        reply.delete(LocalDateTime.now());
    }

    /** 좋아요 토글 — 이미 눌렀으면 취소한다. */
    @Transactional
    public LikeResponse toggleLike(Long replyId, User user) {
        Reply reply = requireReply(replyId);
        boolean liked = replyLikeRepository.findByReplyIdAndUserId(replyId, user.getId())
                .map(existing -> {
                    replyLikeRepository.delete(existing);
                    return false;
                })
                .orElseGet(() -> {
                    replyLikeRepository.save(new ReplyLike(reply, user));
                    return true;
                });
        replyLikeRepository.flush();
        return new LikeResponse(liked, replyLikeRepository.countByReplyId(replyId));
    }

    public Reply requireReply(Long replyId) {
        return replyRepository.findById(replyId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글: " + replyId));
    }

    private LikeInfo likeInfo(List<Reply> replies, User viewer) {
        if (replies.isEmpty()) return new LikeInfo(Map.of(), Set.of());
        List<Long> ids = replies.stream().map(Reply::getId).toList();
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : replyLikeRepository.countByReplyIds(ids)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        Set<Long> liked = viewer == null ? Set.of() : new HashSet<>(replyLikeRepository.findLikedReplyIds(viewer.getId(), ids));
        return new LikeInfo(counts, liked);
    }

    private record LikeInfo(Map<Long, Long> counts, Set<Long> likedIds) {
        long count(Reply reply) {
            return counts.getOrDefault(reply.getId(), 0L);
        }

        boolean liked(Reply reply) {
            return likedIds.contains(reply.getId());
        }
    }
}
