package com.predict.controller;

import com.predict.Category;
import com.predict.Topic;
import com.predict.TopicOption;
import com.predict.User;
import com.predict.Vote;
import com.predict.controller.dto.TopicResponse;
import com.predict.controller.dto.VoteRequest;
import com.predict.controller.dto.VoteResponse;
import com.predict.repository.TopicRepository;
import com.predict.repository.UserRepository;
import com.predict.repository.VoteRepository;
import com.predict.service.VoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopicControllerTest {

    @Mock
    private TopicRepository topicRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VoteRepository voteRepository;
    @Mock
    private VoteService voteService;

    private TopicController topicController;
    private Category category;

    @BeforeEach
    void setUp() {
        topicController = new TopicController(topicRepository, userRepository, voteRepository, voteService);
        category = new Category(1, "정치");
    }

    @Test
    void get_openTopic_hidesOptionVoteCounts() {
        Topic topic = new Topic(category, "제목", null, LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                List.of("예", "아니오"));
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));

        TopicResponse response = topicController.get(1L, null);

        assertThat(response.options()).allSatisfy(option -> assertThat(option.voteCount()).isNull());
        assertThat(response.myOptionId()).isNull();
    }

    @Test
    void get_openTopic_votedByRequester_exposesOptionVoteCounts() throws Exception {
        Topic topic = new Topic(category, "제목", null, LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                List.of("예", "아니오"));
        setId(topic, 1L);
        TopicOption yesOption = topic.getOptions().get(0);
        setId(yesOption, 100L);
        setId(topic.getOptions().get(1), 200L);
        User user = new User("유저", "direct", null);
        Vote vote = new Vote(user, topic, yesOption);
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));
        when(voteRepository.findByUserIdAndTopicId(9L, 1L)).thenReturn(Optional.of(vote));
        when(voteRepository.countByTopicIdAndTopicOptionId(1L, 100L)).thenReturn(3L);
        when(voteRepository.countByTopicIdAndTopicOptionId(1L, 200L)).thenReturn(1L);

        TopicResponse response = topicController.get(1L, 9L);

        assertThat(response.myOptionId()).isEqualTo(100L);
        assertThat(response.options()).noneMatch(option -> option.voteCount() == null);
    }

    @Test
    void get_confirmedTopic_exposesOptionVoteCounts() throws Exception {
        Topic topic = new Topic(category, "제목", null, LocalDateTime.now().minusDays(2), LocalDateTime.now().minusHours(1),
                List.of("예", "아니오"));
        TopicOption yesOption = topic.getOptions().get(0);
        setId(yesOption, 100L);
        setId(topic.getOptions().get(1), 200L);
        topic.closeForResult(Map.of(100L, 6, 200L, 4));
        topic.confirm(yesOption, LocalDateTime.now(), null);
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));

        TopicResponse response = topicController.get(1L, null);

        assertThat(response.options().get(0).voteCount()).isEqualTo(6);
        assertThat(response.options().get(1).voteCount()).isEqualTo(4);
    }

    @Test
    void castVote_returnsLiveCountsFromRepository() throws Exception {
        User user = new User("유저", "direct", null);
        Topic topic = new Topic(category, "제목", null, LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                List.of("예", "아니오"));
        TopicOption yesOption = topic.getOptions().get(0);
        TopicOption noOption = topic.getOptions().get(1);
        setId(yesOption, 100L);
        setId(noOption, 200L);
        Vote vote = new Vote(user, topic, yesOption);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(voteService.castVote(eq(user), eq(5L), eq(100L))).thenReturn(vote);
        when(voteRepository.countByTopicIdAndTopicOptionId(5L, 100L)).thenReturn(7L);
        when(voteRepository.countByTopicIdAndTopicOptionId(5L, 200L)).thenReturn(3L);

        VoteResponse response = topicController.castVote(5L, new VoteRequest(1L, 100L));

        assertThat(response.liveCounts()).hasSize(2);
        assertThat(response.liveCounts().get(0).voteCount()).isEqualTo(7);
        assertThat(response.liveCounts().get(1).voteCount()).isEqualTo(3);
    }

    private void setId(TopicOption option, Long id) throws Exception {
        setId((Object) option, TopicOption.class, id);
    }

    private void setId(Topic topic, Long id) throws Exception {
        setId((Object) topic, Topic.class, id);
    }

    private void setId(Object target, Class<?> type, Long id) throws Exception {
        Field field = type.getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
