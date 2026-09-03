package com.predict.service;

import com.predict.Category;
import com.predict.ScoreSettlement;
import com.predict.TierChange;
import com.predict.Topic;
import com.predict.TopicOption;
import com.predict.User;
import com.predict.Vote;
import com.predict.enums.SettlementResult;
import com.predict.enums.Tier;
import com.predict.enums.TierChangeReason;
import com.predict.enums.TopicStatus;
import com.predict.repository.ScoreSettlementRepository;
import com.predict.repository.TierChangeRepository;
import com.predict.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementCorrectionServiceTest {

    @Mock
    private TopicRepository topicRepository;
    @Mock
    private ScoreSettlementRepository scoreSettlementRepository;
    @Mock
    private TierChangeRepository tierChangeRepository;

    private SettlementCorrectionService correctionService;
    private Category category;

    @BeforeEach
    void setUp() {
        correctionService = new SettlementCorrectionService(topicRepository, scoreSettlementRepository, tierChangeRepository);
        category = new Category(1, "정치");
    }

    @Test
    void correctTopic_reversesSettlementsAndReplaysScoreToZeroWhenNoneRemain() throws Exception {
        Topic topic = new Topic(category, "테스트", null,
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1), List.of("예", "아니오"));
        TopicOption yesOption = topic.getOptions().get(0);
        setId(yesOption, 100L);
        setId(topic.getOptions().get(1), 200L);
        topic.closeForResult(Map.of(100L, 6, 200L, 4));
        topic.confirm(yesOption, LocalDateTime.now(), null);
        setId(topic, 1L);

        User user = new User("유저", "direct", null);
        setId(user, 10L);
        user.applyScoreDelta(20);
        user.changeTier(Tier.BRONZE);
        Vote vote = new Vote(user, topic, yesOption);
        ScoreSettlement settlement = new ScoreSettlement(vote, user, topic, yesOption,
                SettlementResult.CORRECT, BigDecimal.valueOf(0.55), 20, 20);

        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));
        when(scoreSettlementRepository.findByTopicId(1L)).thenReturn(List.of(settlement));
        when(scoreSettlementRepository.findByTopicIdAndIsReversedFalse(1L)).thenReturn(List.of(settlement));
        when(scoreSettlementRepository.findByUserIdAndIsReversedFalseOrderBySettledAtAscIdAsc(anyLong()))
                .thenReturn(List.of());

        correctionService.correctTopic(1L);

        assertThat(settlement.isReversed()).isTrue();
        assertThat(topic.getStatus()).isEqualTo(TopicStatus.PENDING_RESULT);
        assertThat(topic.getCorrectOption()).isNull();
        assertThat(topic.getConfirmedBy()).isNull();
        assertThat(user.getCredibilityScore()).isZero();

        ArgumentCaptor<TierChange> captor = ArgumentCaptor.forClass(TierChange.class);
        verify(tierChangeRepository).save(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo(TierChangeReason.CORRECTION);
    }

    @Test
    void correctTopic_openTopic_throws() {
        Topic topic = new Topic(category, "테스트", null, LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                List.of("예", "아니오"));
        when(topicRepository.findById(2L)).thenReturn(Optional.of(topic));

        assertThatThrownBy(() -> correctionService.correctTopic(2L))
                .isInstanceOf(IllegalStateException.class);
    }

    private void setId(Topic topic, Long id) throws Exception {
        Field field = Topic.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(topic, id);
    }

    private void setId(User user, Long id) throws Exception {
        Field field = User.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
    }

    private void setId(TopicOption option, Long id) throws Exception {
        Field field = TopicOption.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(option, id);
    }
}
