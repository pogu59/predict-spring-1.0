package com.predict.service;

import com.predict.Post;
import com.predict.User;
import com.predict.controller.dto.PostListItemResponse;
import com.predict.enums.PostTopic;
import com.predict.repository.HiddenAuthorRepository;
import com.predict.repository.PostLikeRepository;
import com.predict.repository.PostRepository;
import com.predict.repository.ReplyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostServiceListTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private PostLikeRepository postLikeRepository;
    @Mock
    private ReplyRepository replyRepository;
    @Mock
    private HiddenAuthorRepository hiddenAuthorRepository;

    private PostService postService;
    private final User alice = user(1L, "앨리스");
    private final User bob = user(2L, "밥");

    @BeforeEach
    void setUp() throws Exception {
        postService = new PostService(postRepository, postLikeRepository, replyRepository, hiddenAuthorRepository);
        LocalDateTime now = LocalDateTime.now();
        List<Post> posts = List.of(
                post(10L, alice, "롤드컵 우승 예상", "LCK가 이긴다고 봐요", PostTopic.ANALYSIS, List.of(), now.minusHours(2), 5),
                post(11L, bob, "가을야구 직관 후기", "사진 올려요", PostTopic.CHAT, List.of("/u/a.jpg"), now.minusDays(3), 40),
                post(12L, alice, "점수 계산 질문", "소수 쪽은 왜 더 오르나요", PostTopic.QUESTION, List.of(), now.minusDays(20), 1),
                post(13L, bob, "말머리 없는 옛 글", "롤드컵 이야기", null, List.of(), now.minusDays(60), 0));
        when(postRepository.findVisible(isNull(), isNull())).thenReturn(posts);
        when(postLikeRepository.countByPostIds(any())).thenReturn(List.of());
        when(replyRepository.countByPostIds(any())).thenReturn(List.<Object[]>of(
                new Object[]{12L, 9L}, new Object[]{10L, 2L}));
    }

    private List<Long> ids(String keyword, String scope, String sort, PostTopic topic, String period, boolean hasImage) {
        var query = new PostService.PostQuery(keyword, scope, sort, topic, period, hasImage, false);
        return postService.list(query, null, 0, 20).items().stream().map(PostListItemResponse::id).toList();
    }

    @Test
    void filtersByTopic() {
        assertThat(ids(null, "all", "new", PostTopic.QUESTION, "all", false)).containsExactly(12L);
    }

    @Test
    void filtersByPeriod() {
        assertThat(ids(null, "all", "new", null, "day", false)).containsExactly(10L);
        assertThat(ids(null, "all", "new", null, "week", false)).containsExactly(10L, 11L);
        assertThat(ids(null, "all", "new", null, "month", false)).containsExactly(10L, 11L, 12L);
    }

    @Test
    void filtersPostsWithImages() {
        assertThat(ids(null, "all", "new", null, "all", true)).containsExactly(11L);
    }

    @Test
    void keywordWordsMustAllMatchWithinScope() {
        assertThat(ids("롤드컵", "all", "new", null, "all", false)).containsExactly(10L, 13L);
        assertThat(ids("롤드컵 LCK", "all", "new", null, "all", false)).containsExactly(10L);
        assertThat(ids("롤드컵", "title", "new", null, "all", false)).containsExactly(10L);
        assertThat(ids("밥", "author", "new", null, "all", false)).containsExactly(11L, 13L);
    }

    @Test
    void sortsByCommentsAndViews() {
        assertThat(ids(null, "all", "comments", null, "all", false)).startsWith(12L, 10L);
        assertThat(ids(null, "all", "views", null, "all", false)).startsWith(11L, 10L, 12L);
    }

    private static User user(Long id, String nickname) {
        User user = new User(nickname, "direct", null);
        set(User.class, user, "id", id);
        return user;
    }

    private static Post post(Long id, User author, String title, String content, PostTopic topic,
                             List<String> images, LocalDateTime createdAt, int views) {
        Post post = new Post(author, title, content, images, topic);
        set(Post.class, post, "id", id);
        set(Post.class, post, "createdAt", createdAt);
        set(Post.class, post, "viewCount", views);
        return post;
    }

    private static void set(Class<?> type, Object target, String field, Object value) {
        try {
            Field f = type.getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
