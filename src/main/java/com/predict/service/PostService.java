package com.predict.service;

import com.predict.HiddenAuthor;
import com.predict.Post;
import com.predict.PostLike;
import com.predict.User;
import com.predict.controller.dto.CommunityRequests.LikeResponse;
import com.predict.controller.dto.PageResponse;
import com.predict.controller.dto.PostDetailResponse;
import com.predict.controller.dto.PostListItemResponse;
import com.predict.enums.PostTopic;
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
import java.util.Locale;
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
    public Post create(User author, String title, String content, List<String> images, PostTopic topic) {
        return postRepository.save(new Post(author, title, content, images, topic));
    }

    @Transactional
    public Post update(Long postId, User requester, String title, String content, List<String> images,
                       PostTopic topic) {
        Post post = requirePost(postId);
        if (!post.isAuthor(requester)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 글만 수정할 수 있습니다");
        }
        post.update(title, content, images, topic);
        return post;
    }

    /**
     * 게시판 목록 조건. keyword는 공백으로 나눈 단어가 모두 들어간 글만(AND), scope로 찾는 곳을 정한다.
     * sort: hot(좋아요 + 댓글x3) | new | comments(댓글 많은 순) | views(조회 많은 순).
     * period: all | day(24시간) | week(7일) | month(30일). hasImage면 사진 있는 글만.
     */
    public record PostQuery(String keyword, String scope, String sort, PostTopic topic, String period,
                            boolean hasImage, boolean mineOnly) {
    }

    /**
     * 게시판 목록. viewer가 "이 사용자의 글 숨기기"를 한 작성자의 글은 뺀다.
     * 지인 베타 규모라 후보 전체를 메모리에 올려 필터·정렬한 뒤 페이지를 자른다(PostRepository.findVisible 참고).
     */
    @Transactional(readOnly = true)
    public PageResponse<PostListItemResponse> list(PostQuery query, User viewer, int page, int size) {
        if (query.mineOnly() && viewer == null) {
            return new PageResponse<>(List.of(), page, size, 0, 0);
        }
        List<Post> posts = postRepository.findVisible(null, query.mineOnly() ? viewer.getId() : null);
        if (viewer != null && !query.mineOnly()) {
            Set<Long> hidden = new HashSet<>(hiddenAuthorRepository.findHiddenUserIds(viewer.getId()));
            posts = posts.stream().filter(p -> !hidden.contains(p.getAuthor().getId())).toList();
        }
        LocalDateTime since = periodStart(query.period());
        List<String> words = keywords(query.keyword());
        posts = posts.stream()
                .filter(p -> query.topic() == null || query.topic() == p.getTopic())
                .filter(p -> since == null || !p.getCreatedAt().isBefore(since))
                .filter(p -> !query.hasImage() || !p.getImages().isEmpty())
                .filter(p -> words.stream().allMatch(w -> searchTarget(p, query.scope()).contains(w)))
                .toList();

        List<Long> ids = posts.stream().map(Post::getId).toList();
        Map<Long, Long> likeCounts = toCountMap(ids.isEmpty() ? List.of() : postLikeRepository.countByPostIds(ids));
        Map<Long, Long> replyCounts = toCountMap(ids.isEmpty() ? List.of() : replyRepository.countByPostIds(ids));
        Set<Long> liked = viewer == null || ids.isEmpty()
                ? Set.of()
                : new HashSet<>(postLikeRepository.findLikedPostIds(viewer.getId(), ids));

        Comparator<PostListItemResponse> newest = Comparator.comparing(PostListItemResponse::createdAt,
                Comparator.reverseOrder());
        Comparator<PostListItemResponse> order = switch (query.sort() == null ? "new" : query.sort()) {
            case "hot" -> Comparator.comparingLong((PostListItemResponse p) -> p.likeCount() + p.replyCount() * 3)
                    .reversed().thenComparing(newest);
            case "comments" -> Comparator.comparingLong(PostListItemResponse::replyCount).reversed()
                    .thenComparing(newest);
            case "views" -> Comparator.comparingInt(PostListItemResponse::viewCount).reversed()
                    .thenComparing(newest);
            default -> newest;
        };
        List<PostListItemResponse> items = posts.stream()
                .map(p -> PostListItemResponse.from(p, likeCounts.getOrDefault(p.getId(), 0L), liked.contains(p.getId()),
                        replyCounts.getOrDefault(p.getId(), 0L)))
                .sorted(order)
                .toList();

        int safeSize = Math.max(1, Math.min(size, 100));
        int from = Math.min(page * safeSize, items.size());
        int to = Math.min(from + safeSize, items.size());
        int totalPages = (int) Math.ceil(items.size() / (double) safeSize);
        return new PageResponse<>(items.subList(from, to), page, safeSize, items.size(), totalPages);
    }

    private static List<String> keywords(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return List.of(keyword.trim().toLowerCase(Locale.ROOT).split("\\s+"));
    }

    /** scope: title(제목) | author(닉네임) | 그 외(제목 + 본문 + 닉네임). */
    private static String searchTarget(Post post, String scope) {
        String text = switch (scope == null ? "all" : scope) {
            case "title" -> post.getTitle();
            case "author" -> post.getAuthor().getNickname();
            default -> post.getTitle() + "\n" + post.getContent() + "\n" + post.getAuthor().getNickname();
        };
        return text.toLowerCase(Locale.ROOT);
    }

    private static LocalDateTime periodStart(String period) {
        LocalDateTime now = LocalDateTime.now();
        return switch (period == null ? "all" : period) {
            case "day" -> now.minusDays(1);
            case "week" -> now.minusDays(7);
            case "month" -> now.minusDays(30);
            default -> null;
        };
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
