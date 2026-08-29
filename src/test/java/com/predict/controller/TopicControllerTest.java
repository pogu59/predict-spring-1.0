package com.predict.controller;

import com.predict.Category;
import com.predict.Topic;
import com.predict.User;
import com.predict.Vote;
import com.predict.controller.dto.TopicResponse;
import com.predict.controller.dto.VoteRequest;
import com.predict.controller.dto.VoteResponse;
import com.predict.enums.Choice;
import com.predict.repository.TopicRepository;
import com.predict.repository.UserRepository;
import com.predict.repository.VoteRepository;
import com.predict.service.VoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    void get_openTopic_hidesYesNoCounts() {
        Topic topic = new Topic(category, "제목", null, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));

        TopicResponse response = topicController.get(1L);

        assertThat(response.yesCount()).isNull();
        assertThat(response.noCount()).isNull();
    }

    @Test
    void get_confirmedTopic_exposesYesNoCounts() {
        Topic topic = new Topic(category, "제목", null, LocalDateTime.now().minusDays(2), LocalDateTime.now().minusHours(1));
        topic.closeForResult(6, 4);
        topic.confirm(Choice.YES, LocalDateTime.now(), null);
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));

        TopicResponse response = topicController.get(1L);

        assertThat(response.yesCount()).isEqualTo(6);
        assertThat(response.noCount()).isEqualTo(4);
    }

    @Test
    void castVote_returnsLiveTallyFromRepository() {
        User user = new User("유저", "direct", null);
        Topic topic = new Topic(category, "제목", null, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        Vote vote = new Vote(user, topic, Choice.YES);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(voteService.castVote(eq(user), eq(5L), eq(Choice.YES))).thenReturn(vote);
        when(voteRepository.countByTopicIdAndChoice(5L, Choice.YES)).thenReturn(7L);
        when(voteRepository.countByTopicIdAndChoice(5L, Choice.NO)).thenReturn(3L);

        VoteResponse response = topicController.castVote(5L, new VoteRequest(1L, Choice.YES));

        assertThat(response.liveYesCount()).isEqualTo(7);
        assertThat(response.liveNoCount()).isEqualTo(3);
    }
}
