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

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private TopicOption yesOption;
    private TopicOption noOption;

    @BeforeEach
    void setUp() throws Exception {
        settlementService = new SettlementService(topicRepository, voteRepository,
                scoreSettlementRepository, tierChangeRepository);

        Category category = new Category(1, "정치");
        topic = new Topic(category, "테스트 주제", null,
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusHours(1), List.of("예", "아니오"));
        yesOption = topic.getOptions().get(0);
        noOption = topic.getOptions().get(1);
        setId(yesOption, 100L);
        setId(noOption, 200L);
        topic.closeForResult(Map.of(100L, 6, 200L, 4)); // 참여자 10명, p(다수)=0.55, p(소수)=0.45
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));
    }

    @Test
    void confirmTopic_correctVoter_gainsScoreAndSettlementRecordsCorrect() {
        User user = new User("정답자", "direct", null);
        Vote vote = new Vote(user, topic, yesOption);
        when(voteRepository.findByTopicId(1L)).thenReturn(List.of(vote));

        settlementService.confirmTopic(1L, 100L, null);

        ArgumentCaptor<ScoreSettlement> captor = ArgumentCaptor.forClass(ScoreSettlement.class);
        verify(scoreSettlementRepository).save(captor.capture());
        ScoreSettlement saved = captor.getValue();

        assertThat(saved.getResult()).isEqualTo(SettlementResult.CORRECT);
        assertThat(saved.getScoreDelta()).isPositive();
        assertThat(user.getCredibilityScore()).isEqualTo(saved.getScoreDelta());
        assertThat(topic.getCorrectOption()).isEqualTo(yesOption);
    }

    @Test
    void confirmTopic_incorrectVoter_losesScoreButFloorsAtZero() {
        User user = new User("오답자", "direct", null);
        Vote vote = new Vote(user, topic, noOption);
        when(voteRepository.findByTopicId(1L)).thenReturn(List.of(vote));

        settlementService.confirmTopic(1L, 100L, null);

        assertThat(user.getCredibilityScore()).isZero();
        verify(tierChangeRepository, never()).save(any());
    }

    @Test
    void confirmTopic_scoreCrossingTierBoundary_recordsNaturalPromotion() {
        User user = new User("승급자", "direct", null);
        // 이전 정산에서 이미 브론즈로 동기화되어 있던 상태(90점, 실버(100) 문턱 바로 아래)를 재현
        user.applyScoreDelta(90);
        user.changeTier(Tier.BRONZE);
        Vote vote = new Vote(user, topic, yesOption); // 정답, 다수(p=0.55) -> 대략 +18점
        when(voteRepository.findByTopicId(1L)).thenReturn(List.of(vote));

        settlementService.confirmTopic(1L, 100L, null);

        ArgumentCaptor<TierChange> captor = ArgumentCaptor.forClass(TierChange.class);
        verify(tierChangeRepository).save(captor.capture());
        TierChange tierChange = captor.getValue();

        assertThat(tierChange.getReason()).isEqualTo(TierChangeReason.NATURAL_PROMOTION);
        assertThat(tierChange.getPreviousTier()).isEqualTo(Tier.BRONZE);
        assertThat(tierChange.getNewTier()).isEqualTo(user.getTier());
    }

    @Test
    void confirmTopic_activitySuppressedUser_neverRecalculatedAboveDiamondEvenIfScoreQualifies() {
        User user = new User("활동성강등유저", "direct", null);
        user.applyScoreDelta(485); // 다이아 구간, 마스터(500) 문턱 바로 아래
        user.changeTier(Tier.DIAMOND);
        user.setActivitySuppressed(true); // 지난주 활동성 체크 미달로 강등 상태
        Vote vote = new Vote(user, topic, yesOption); // 정답, 다수(p=0.55) -> 대략 +18점 -> 503점(마스터 구간)
        when(voteRepository.findByTopicId(1L)).thenReturn(List.of(vote));

        settlementService.confirmTopic(1L, 100L, null);

        assertThat(user.getCredibilityScore()).isGreaterThanOrEqualTo(500);
        assertThat(user.getTier()).isEqualTo(Tier.PLATINUM); // 점수는 마스터 구간이어도 강등 상태라 플래티넘 캡
    }

    private void setId(TopicOption option, Long id) throws Exception {
        Field field = TopicOption.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(option, id);
    }
}
