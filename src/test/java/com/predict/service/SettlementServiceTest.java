package com.predict.service;

import com.predict.Category;
import com.predict.ScoreSettlement;
import com.predict.TierChange;
import com.predict.Topic;
import com.predict.User;
import com.predict.Vote;
import com.predict.enums.Choice;
import com.predict.enums.SettlementResult;
import com.predict.enums.Tier;
import com.predict.enums.TierChangeReason;
import com.predict.repository.ScoreSettlementRepository;
import com.predict.repository.TierChangeRepository;
import com.predict.repository.TopicRepository;
import com.predict.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private TopicRepository topicRepository;
    @Mock
    private VoteRepository voteRepository;
    @Mock
    private ScoreSettlementRepository scoreSettlementRepository;
    @Mock
    private TierChangeRepository tierChangeRepository;

    private SettlementService settlementService;
    private Topic topic;

    @BeforeEach
    void setUp() {
        settlementService = new SettlementService(topicRepository, voteRepository,
                scoreSettlementRepository, tierChangeRepository);

        Category category = new Category(1, "정치");
        topic = new Topic(category, "테스트 주제", null,
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusHours(1));
        topic.closeForResult(6, 4); // 참여자 10명, p(다수)=0.55, p(소수)=0.45
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));
    }

    @Test
    void confirmTopic_correctVoter_gainsScoreAndSettlementRecordsCorrect() {
        User user = new User("정답자", "direct", null);
        Vote vote = new Vote(user, topic, Choice.YES);
        when(voteRepository.findByTopicId(1L)).thenReturn(List.of(vote));

        settlementService.confirmTopic(1L, Choice.YES);

        ArgumentCaptor<ScoreSettlement> captor = ArgumentCaptor.forClass(ScoreSettlement.class);
        verify(scoreSettlementRepository).save(captor.capture());
        ScoreSettlement saved = captor.getValue();

        assertThat(saved.getResult()).isEqualTo(SettlementResult.CORRECT);
        assertThat(saved.getScoreDelta()).isPositive();
        assertThat(user.getCredibilityScore()).isEqualTo(saved.getScoreDelta());
        assertThat(topic.getCorrectAnswer()).isEqualTo(Choice.YES);
    }

    @Test
    void confirmTopic_incorrectVoter_losesScoreButFloorsAtZero() {
        User user = new User("오답자", "direct", null);
        Vote vote = new Vote(user, topic, Choice.NO);
        when(voteRepository.findByTopicId(1L)).thenReturn(List.of(vote));

        settlementService.confirmTopic(1L, Choice.YES);

        assertThat(user.getCredibilityScore()).isZero();
        verify(tierChangeRepository, never()).save(any());
    }

    @Test
    void confirmTopic_scoreCrossingTierBoundary_recordsNaturalPromotion() {
        User user = new User("승급자", "direct", null);
        // 이전 정산에서 이미 브론즈로 동기화되어 있던 상태(90점, 실버(100) 문턱 바로 아래)를 재현
        user.applyScoreDelta(90);
        user.changeTier(Tier.BRONZE);
        Vote vote = new Vote(user, topic, Choice.YES); // 정답, 다수(p=0.55) -> 대략 +18점
        when(voteRepository.findByTopicId(1L)).thenReturn(List.of(vote));

        settlementService.confirmTopic(1L, Choice.YES);

        ArgumentCaptor<TierChange> captor = ArgumentCaptor.forClass(TierChange.class);
        verify(tierChangeRepository).save(captor.capture());
        TierChange tierChange = captor.getValue();

        assertThat(tierChange.getReason()).isEqualTo(TierChangeReason.NATURAL_PROMOTION);
        assertThat(tierChange.getPreviousTier()).isEqualTo(Tier.BRONZE);
        assertThat(tierChange.getNewTier()).isEqualTo(user.getTier());
    }

    @Test
    void voidTopic_leavesScoreAndTierUntouched() {
        User user = new User("참여자", "direct", null);
        user.applyScoreDelta(250);
        user.changeTier(Tier.GOLD);
        Vote vote = new Vote(user, topic, Choice.YES);
        when(voteRepository.findByTopicId(1L)).thenReturn(List.of(vote));

        settlementService.voidTopic(1L);

        ArgumentCaptor<ScoreSettlement> captor = ArgumentCaptor.forClass(ScoreSettlement.class);
        verify(scoreSettlementRepository).save(captor.capture());
        assertThat(captor.getValue().getResult()).isEqualTo(SettlementResult.VOID);
        assertThat(captor.getValue().getScoreDelta()).isZero();
        assertThat(user.getCredibilityScore()).isEqualTo(250);
        verify(tierChangeRepository, never()).save(any());
    }
}
