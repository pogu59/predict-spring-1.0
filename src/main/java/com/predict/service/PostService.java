package com.predict.service;

import com.predict.HiddenAuthor;
import com.predict.Post;
import com.predict.PostLike;
import com.predict.User;
import com.predict.controller.dto.CommunityRequests.LikeResponse;
import com.predict.controller.dto.PageResponse;
import com.predict.controller.dto.PostDetailResponse;
import com.predict.controller.dto.PostListItemResponse;
import com.predict.enums.Role;
import com.predict.repository.HiddenAuthorRepository;
import com.predict.repository.PostLikeRepository;
import com.predict.repository.PostRepository;
import com.predict.repository.ReplyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final ReplyRepository replyRepository;
    private final HiddenAuthorRepository hiddenAuthorRepository;

    public PostService(PostRepository postRepository, PostLikeRepository postLikeRepository,
                       ReplyRepository replyRepository, HiddenAuthorRepository hiddenAuthorRepository) {
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
        this.replyRepository = replyRepository;
        this.hiddenAuthorRepository = hiddenAuthorRepository;
    }

    @Transactional
    public Post create(User author, String title, String content, List<String> images) {
        return postRepository.save(new Post(author, title, content, images));
    }

    @Transactional
    public Post update(Long postId, User requester, String title, String content, List<String> images) {
        Post post = requirePost(postId);
        if (!post.isAuthor(requester)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 글만 수정할 수 있습니다");
        }
        post.update(title, content, images);
        return post;
    }

    /**
     * 게시판 목록. sort=hot은 좋아요 + 댓글x3 내림차순, new는 최신순. mineOnly면 viewer의 글만.
     * viewer가 "이 사용자의 글 숨기기"를 한 작성자의 글은 뺀다.
     */
    @Transactional(readOnly = true)
    public PageResponse<PostListItemResponse> list(String keyword, String sort, boolean mineOnly, User viewer,
                                                   int page, int size) {
        if (mineOnly && viewer == null) {
            return new PageResponse<>(List.of(), page, size, 0, 0);
        }
        String trimmed = keyword == null || keyword.isBlank() ? null : keyword.trim();
        List<Post> posts = postRepository.findVisible(trimmed, mineOnly ? viewer.getId() : null);
        if (viewer != null && !mineOnly) {
            Set<Long> hidden = new HashSet<>(hiddenAuthorRepository.findHiddenUserIds(viewer.getId()));
            posts = posts.stream().filter(p -> !hidden.contains(p.getAuthor().getId())).toList();
        }

        List<Long> ids = posts.stream().map(Post::getId).toList();
        Map<Long, Long> likeCounts = toCountMap(ids.isEmpty() ? List.of() : postLikeRepository.countByPostIds(ids));
        Map<Long, Long> replyCounts = toCountMap(ids.isEmpty() ? List.of() : replyRepository.countByPostIds(ids));
        Set<Long> liked = viewer == null || ids.isEmpty()
                ? Set.of()
                : new HashSet<>(postLikeRepository.findLikedPostIds(viewer.getId(), ids));

        List<PostListItemResponse> items = posts.stream()
                .map(p -> PostListItemResponse.from(p, likeCounts.getOrDefault(p.getId(), 0L), liked.contains(p.getId()),
                        replyCounts.getOrDefault(p.getId(), 0L)))
                .sorted("hot".equals(sort)
                        ? Comparator.comparingLong((PostListItemResponse p) -> p.likeCount() + p.replyCount() * 3).reversed()
                                .thenComparing(PostListItemResponse::createdAt, Comparator.reverseOrder())
                        : Comparator.comparing(PostListItemResponse::createdAt, Comparator.reverseOrder()))
                .toList();

        int safeSize = Math.max(1, Math.min(size, 100));
        int from = Math.min(page * safeSize, items.size());
        int to = Math.min(from + safeSize, items.size());
        int totalPages = (int) Math.ceil(items.size() / (double) safeSize);
        return new PageResponse<>(items.subList(from, to), page, safeSize, items.size(), totalPages);
    }

    /** 상세 조회 시 조회수 +1. 관리자가 숨긴 글은 작성자와 관리자만 볼 수 있다. */
    @Transactional
    public PostDetailResponse viewDetail(Long postId, User viewer) {
        Post post = requirePost(postId);
        boolean privileged = viewer != null && (post.isAuthor(viewer) || viewer.getRole() == Role.ADMIN);
        if (post.isHidden() && !privileged) {
            throw new IllegalArgumentException("숨겨진 게시글이에요");
        }
        post.increaseViewCount();
        boolean liked = viewer != null && postLikeRepository.findByPostIdAndUserId(postId, viewer.getId()).isPresent();
        return PostDetailResponse.from(post, postLikeRepository.countByPostId(postId), liked);
    }

    public PostDetailResponse toDetail(Post post) {
        return PostDetailResponse.from(post, postLikeRepository.countByPostId(post.getId()), false);
    }

    @Transactional
    public void delete(Long postId, User requester) {
        Post post = requirePost(postId);
        if (!post.isAuthor(requester) && requester.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 글이거나 관리자만 삭제할 수 있습니다");
        }
        post.delete(LocalDateTime.now());
    }

    @Transactional
    public LikeResponse toggleLike(Long postId, User user) {
        Post post = requirePost(postId);
        boolean liked = postLikeRepository.findByPostIdAndUserId(postId, user.getId())
                .map(existing -> {
                    postLikeRepository.delete(existing);
                    return false;
                })
                .orElseGet(() -> {
                    postLikeRepository.save(new PostLike(post, user));
                    return true;
                });
        postLikeRepository.flush();
        return new LikeResponse(liked, postLikeRepository.countByPostId(postId));
    }

    /** "이 사용자의 글 숨기기". 자기 자신은 숨길 수 없고, 이미 숨겼으면 아무것도 하지 않는다. */
    @Transactional
    public void hideAuthor(Long postId, User viewer) {
        Post post = requirePost(postId);
        User author = post.getAuthor();
        if (author.getId().equals(viewer.getId())
                || hiddenAuthorRepository.existsByUserIdAndHiddenUserId(viewer.getId(), author.getId())) {
            return;
        }
        hiddenAuthorRepository.save(new HiddenAuthor(viewer, author));
    }

    public Post requirePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글: " + postId));
        if (post.isDeleted()) {
            throw new IllegalArgumentException("삭제된 게시글이에요");
        }
        return post;
    }

    static Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }
}
