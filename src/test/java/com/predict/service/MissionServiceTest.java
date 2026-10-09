package com.predict.service;

import com.predict.Mission;
import com.predict.MissionSubmission;
import com.predict.RewardTransaction;
import com.predict.RewardWallet;
import com.predict.User;
import com.predict.enums.MissionStatus;
import com.predict.enums.MissionType;
import com.predict.enums.RewardTransactionType;
import com.predict.enums.SubmissionStatus;
import com.predict.mission.QualityResult;
import com.predict.mission.QuestionResult;
import com.predict.mission.SubmitResult;
import com.predict.repository.MissionRepository;
import com.predict.repository.MissionSubmissionRepository;
import com.predict.repository.RewardExchangeRequestRepository;
import com.predict.repository.RewardTransactionRepository;
import com.predict.repository.RewardWalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MissionService는 진짜 RewardService(리포지토리만 목)와 함께 돌려서 "제출 → 검수 → 적립 → 보너스"가
 * 한 흐름으로 맞는지 본다.
 */
@ExtendWith(MockitoExtension.class)
class MissionServiceTest {

    @Mock
    private MissionRepository missionRepository;
    @Mock
    private MissionSubmissionRepository submissionRepository;
    @Mock
    private RewardWalletRepository walletRepository;
    @Mock
    private RewardTransactionRepository transactionRepository;
    @Mock
    private RewardExchangeRequestRepository exchangeRepository;

    private MissionService missionService;
    private User user;
    private RewardWallet wallet;
    private final AtomicLong submissionIds = new AtomicLong(100);

    @BeforeEach
    void setUp() throws Exception {
        RewardService rewardService = new RewardService(walletRepository, transactionRepository, exchangeRepository);
        missionService = new MissionService(missionRepository, submissionRepository, rewardService);
        user = new User("유저", "direct", null);
        setField(user, "id", 7L);
        wallet = new RewardWallet(7L);

        lenient().when(walletRepository.findById(7L)).thenReturn(Optional.of(wallet));
        lenient().when(transactionRepository.existsByIdempotencyKey(anyString())).thenReturn(false);
        lenient().when(transactionRepository.save(any(RewardTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(submissionRepository.save(any(MissionSubmission.class))).thenAnswer(inv -> {
            MissionSubmission submission = inv.getArgument(0);
            setField(submission, "id", submissionIds.incrementAndGet());
            return submission;
        });
    }

    private static Mission openSurvey(long id, int points, boolean daily, boolean withAttentionCheck) throws Exception {
        Mission mission = new Mission(MissionType.SURVEY, "설문 " + id, null, points, daily,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        mission.addQuestion("하루에 커피를 몇 잔 마시나요?", List.of("안 마셔요", "1잔", "2잔 이상"), null);
        if (withAttentionCheck) {
            mission.addQuestion("이 문항은 '1잔'을 골라 주세요", List.of("안 마셔요", "1잔", "2잔 이상"), 1);
        }
        mission.open();
        setField(mission, "id", id);
        return mission;
    }

    @Test
    void submit_approved_earnsMissionPoints() throws Exception {
        Mission mission = openSurvey(1L, 50, false, true);
        when(missionRepository.findById(1L)).thenReturn(Optional.of(mission));
        when(submissionRepository.existsByMissionIdAndUserId(1L, 7L)).thenReturn(false);

        SubmitResult result = missionService.submit(user, 1L, List.of(2, 1), 30_000);

        assertThat(result.submission().getStatus()).isEqualTo(SubmissionStatus.APPROVED);
        assertThat(result.earnedPoints()).isEqualTo(50);
        assertThat(result.bonusPoints()).isZero();
        assertThat(result.balance()).isEqualTo(50);
        ArgumentCaptor<RewardTransaction> saved = ArgumentCaptor.forClass(RewardTransaction.class);
        verify(transactionRepository).save(saved.capture());
        assertThat(saved.getValue().getType()).isEqualTo(RewardTransactionType.EARN);
        assertThat(saved.getValue().getIdempotencyKey()).isEqualTo("earn:submission:" + result.submission().getId());
    }

    @Test
    void submit_failedAttentionCheck_isRecordedWithoutPoints() throws Exception {
        Mission mission = openSurvey(1L, 50, false, true);
        when(missionRepository.findById(1L)).thenReturn(Optional.of(mission));
        when(submissionRepository.existsByMissionIdAndUserId(1L, 7L)).thenReturn(false);

        SubmitResult result = missionService.submit(user, 1L, List.of(2, 0), 30_000);

        assertThat(result.submission().getStatus()).isEqualTo(SubmissionStatus.REJECTED);
        assertThat(result.submission().getRejectReason()).isNotBlank();
        assertThat(result.earnedPoints()).isZero();
        assertThat(wallet.getBalance()).isZero();
        verify(submissionRepository).save(any(MissionSubmission.class));
        verify(transactionRepository, never()).save(any(RewardTransaction.class));
    }

    @Test
    void submit_twice_isRejected() throws Exception {
        Mission mission = openSurvey(1L, 50, false, false);
        when(missionRepository.findById(1L)).thenReturn(Optional.of(mission));
        when(submissionRepository.existsByMissionIdAndUserId(1L, 7L)).thenReturn(true);

        assertThatThrownBy(() -> missionService.submit(user, 1L, List.of(1), 30_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 참여한 미션이에요");
    }

    @Test
    void submit_closedMission_isRejected() throws Exception {
        Mission mission = openSurvey(1L, 50, false, false);
        mission.close();
        when(missionRepository.findById(1L)).thenReturn(Optional.of(mission));

        assertThatThrownBy(() -> missionService.submit(user, 1L, List.of(1), 30_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("지금은 참여할 수 없는 미션이에요");
    }

    @Test
    void submit_missingAnswers_isBadRequest() throws Exception {
        Mission mission = openSurvey(1L, 50, false, true);
        when(missionRepository.findById(1L)).thenReturn(Optional.of(mission));
        when(submissionRepository.existsByMissionIdAndUserId(1L, 7L)).thenReturn(false);

        assertThatThrownBy(() -> missionService.submit(user, 1L, List.of(1), 30_000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("모든 문항에 답해 주세요");
    }

    @Test
    void submit_suspendedUser_isForbidden() {
        user.setSuspended(true);

        assertThatThrownBy(() -> missionService.submit(user, 1L, List.of(1), 30_000))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void attendance_onceADay() throws Exception {
        Mission attendance = new Mission(MissionType.ATTENDANCE, "출석 체크", null, 10, false,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        attendance.open();
        setField(attendance, "id", 5L);
        when(missionRepository.findById(5L)).thenReturn(Optional.of(attendance));
        when(submissionRepository.existsByMissionIdAndUserIdAndSubmittedOn(eq(5L), eq(7L), any(LocalDate.class)))
                .thenReturn(false, true);

        SubmitResult first = missionService.submit(user, 5L, List.of(), 0);

        assertThat(first.earnedPoints()).isEqualTo(10);
        assertThatThrownBy(() -> missionService.submit(user, 5L, List.of(), 0))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void finishingAllDailyMissions_grantsBonusOnce() throws Exception {
        Mission first = openSurvey(1L, 10, true, false);
        Mission second = openSurvey(2L, 50, true, false);
        when(missionRepository.findById(2L)).thenReturn(Optional.of(second));
        when(submissionRepository.existsByMissionIdAndUserId(2L, 7L)).thenReturn(false);
        when(missionRepository.findAvailable(eq(MissionStatus.OPEN), any(LocalDateTime.class)))
                .thenReturn(List.of(first, second));
        MissionSubmission doneEarlier = new MissionSubmission(first, user, LocalDate.now(), List.of(0), 20_000,
                QualityResult.approved(100));
        setField(doneEarlier, "id", 50L);
        when(submissionRepository.findByUserIdAndSubmittedOnAndStatus(eq(7L), any(LocalDate.class), eq(SubmissionStatus.APPROVED)))
                .thenAnswer(inv -> List.of(doneEarlier, new MissionSubmission(second, user, LocalDate.now(), List.of(1),
                        20_000, QualityResult.approved(100))));

        SubmitResult result = missionService.submit(user, 2L, List.of(1), 20_000);

        assertThat(result.earnedPoints()).isEqualTo(50);
        assertThat(result.bonusPoints()).isEqualTo(MissionService.DAILY_BONUS_POINTS);
        assertThat(result.balance()).isEqualTo(50 + MissionService.DAILY_BONUS_POINTS);
        verify(transactionRepository, atLeastOnce())
                .existsByIdempotencyKey("bonus:daily:7:" + LocalDate.now());
    }

    @Test
    void dailyBonus_notGivenWhileSomeDailyMissionsRemain() throws Exception {
        Mission first = openSurvey(1L, 10, true, false);
        Mission second = openSurvey(2L, 50, true, false);
        when(missionRepository.findById(2L)).thenReturn(Optional.of(second));
        when(submissionRepository.existsByMissionIdAndUserId(2L, 7L)).thenReturn(false);
        when(missionRepository.findAvailable(eq(MissionStatus.OPEN), any(LocalDateTime.class)))
                .thenReturn(List.of(first, second));
        when(submissionRepository.findByUserIdAndSubmittedOnAndStatus(eq(7L), any(LocalDate.class), eq(SubmissionStatus.APPROVED)))
                .thenAnswer(inv -> List.of(new MissionSubmission(second, user, LocalDate.now(), List.of(1),
                        20_000, QualityResult.approved(100))));

        SubmitResult result = missionService.submit(user, 2L, List.of(1), 20_000);

        assertThat(result.bonusPoints()).isZero();
        assertThat(result.balance()).isEqualTo(50);
    }

    @Test
    void results_areOnlyForParticipants() throws Exception {
        Mission mission = openSurvey(1L, 50, false, false);
        when(missionRepository.findById(1L)).thenReturn(Optional.of(mission));
        when(submissionRepository.findFirstByMissionIdAndUserIdOrderByIdDesc(1L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> missionService.results(user, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("참여한 사람만 결과를 볼 수 있어요");
    }

    @Test
    void results_givePercentsOnly_andSkipAttentionChecks() throws Exception {
        Mission mission = openSurvey(1L, 50, false, true);
        when(missionRepository.findById(1L)).thenReturn(Optional.of(mission));
        MissionSubmission mine = new MissionSubmission(mission, user, LocalDate.now(), List.of(1, 1), 20_000,
                QualityResult.approved(100));
        when(submissionRepository.findFirstByMissionIdAndUserIdOrderByIdDesc(1L, 7L)).thenReturn(Optional.of(mine));
        User other = new User("다른 유저", "direct", null);
        when(submissionRepository.findByMissionIdAndStatus(1L, SubmissionStatus.APPROVED)).thenReturn(List.of(
                mine,
                new MissionSubmission(mission, other, LocalDate.now(), List.of(1, 1), 20_000, QualityResult.approved(100)),
                new MissionSubmission(mission, other, LocalDate.now(), List.of(0, 1), 20_000, QualityResult.approved(100))));

        List<QuestionResult> results = missionService.results(user, 1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).percents()).containsExactly(33, 67, 0);
        assertThat(results.get(0).myAnswer()).isEqualTo(1);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
