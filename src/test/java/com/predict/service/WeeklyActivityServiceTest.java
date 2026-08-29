package com.predict.service;

import com.predict.TierChange;
import com.predict.User;
import com.predict.WeeklyActivitySnapshot;
import com.predict.enums.Tier;
import com.predict.enums.TierChangeReason;
import com.predict.repository.TierChangeRepository;
import com.predict.repository.UserRepository;
import com.predict.repository.VoteRepository;
import com.predict.repository.WeeklyActivitySnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyActivityServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private VoteRepository voteRepository;
    @Mock
    private WeeklyActivitySnapshotRepository snapshotRepository;
    @Mock
    private TierChangeRepository tierChangeRepository;

    private WeeklyActivityService weeklyActivityService;

    private static final LocalDate WEEK_START = LocalDate.of(2026, 8, 17);
    private static final LocalDate WEEK_END = LocalDate.of(2026, 8, 23);

    @BeforeEach
    void setUp() {
        weeklyActivityService = new WeeklyActivityService(
                userRepository, voteRepository, snapshotRepository, tierChangeRepository);
        when(snapshotRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void belowThreshold_demotesDisplayTierOnly() throws Exception {
        User user = new User("다이아유저", "direct", null);
        setId(user, 1L);
        user.applyScoreDelta(420); // 다이아 구간
        user.changeTier(Tier.DIAMOND); // 지난주까지는 정상적으로 다이아였던 상태
        when(userRepository.findByCredibilityScoreGreaterThanEqual(400)).thenReturn(List.of(user));
        when(voteRepository.countByUserIdAndVotedAtGreaterThanEqualAndVotedAtLessThan(anyLong(), any(), any()))
                .thenReturn(3L); // 5회 미만

        weeklyActivityService.checkWeeklyActivity(WEEK_START, WEEK_END);

        assertThat(user.getTier()).isEqualTo(Tier.PLATINUM); // 다이아 -> 한 단계 강등
        assertThat(user.getCredibilityScore()).isEqualTo(420); // 점수는 그대로

        ArgumentCaptor<TierChange> tierChangeCaptor = ArgumentCaptor.forClass(TierChange.class);
        verify(tierChangeRepository).save(tierChangeCaptor.capture());
        assertThat(tierChangeCaptor.getValue().getReason()).isEqualTo(TierChangeReason.ACTIVITY_DEMOTION);

        ArgumentCaptor<WeeklyActivitySnapshot> snapshotCaptor = ArgumentCaptor.forClass(WeeklyActivitySnapshot.class);
        verify(snapshotRepository).save(snapshotCaptor.capture());
        assertThat(snapshotCaptor.getValue().isMetRequirement()).isFalse();
    }

    @Test
    void meetsThreshold_restoresPointTier() throws Exception {
        User user = new User("복귀유저", "direct", null);
        setId(user, 2L);
        user.applyScoreDelta(420);
        user.changeTier(Tier.PLATINUM); // 지난주 활동성 강등되어 있던 상태
        when(userRepository.findByCredibilityScoreGreaterThanEqual(400)).thenReturn(List.of(user));
        when(voteRepository.countByUserIdAndVotedAtGreaterThanEqualAndVotedAtLessThan(anyLong(), any(), any()))
                .thenReturn(5L); // 조건 충족

        weeklyActivityService.checkWeeklyActivity(WEEK_START, WEEK_END);

        assertThat(user.getTier()).isEqualTo(Tier.DIAMOND);
        ArgumentCaptor<TierChange> captor = ArgumentCaptor.forClass(TierChange.class);
        verify(tierChangeRepository).save(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo(TierChangeReason.ACTIVITY_RESTORATION);
    }

    @Test
    void alreadyMatchingPointTier_noTierChangeRecorded() throws Exception {
        User user = new User("정상유저", "direct", null);
        setId(user, 3L);
        user.applyScoreDelta(420);
        user.changeTier(Tier.DIAMOND); // 이미 점수 티어와 일치하는 상태
        when(userRepository.findByCredibilityScoreGreaterThanEqual(400)).thenReturn(List.of(user));
        when(voteRepository.countByUserIdAndVotedAtGreaterThanEqualAndVotedAtLessThan(anyLong(), any(), any()))
                .thenReturn(10L);

        weeklyActivityService.checkWeeklyActivity(WEEK_START, WEEK_END);

        assertThat(user.getTier()).isEqualTo(Tier.DIAMOND);
        verify(tierChangeRepository, never()).save(any());
        verify(snapshotRepository).save(any());
    }

    private void setId(User user, Long id) throws Exception {
        Field field = User.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
    }
}
