package com.predict.service;

import com.predict.Category;
import com.predict.ScoreSettlement;
import com.predict.TierChange;
import com.predict.Issue;
import com.predict.IssueOption;
import com.predict.User;
import com.predict.Vote;
import com.predict.enums.SettlementResult;
import com.predict.enums.Tier;
import com.predict.enums.TierChangeReason;
import com.predict.repository.ScoreSettlementRepository;
import com.predict.repository.TierChangeRepository;
import com.predict.repository.IssueRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private IssueRepository issueRepository;
    @Mock
    private VoteRepository voteRepository;
    @Mock
    private ScoreSettlementRepository scoreSettlementRepository;
    @Mock
    private TierChangeRepository tierChangeRepository;

    private SettlementService settlementService;
    private Issue issue;
    private IssueOption yesOption;
    private IssueOption noOption;

    @BeforeEach
    void setUp() throws Exception {
        settlementService = new SettlementService(issueRepository, voteRepository,
                scoreSettlementRepository, tierChangeRepository);

        Category category = new Category(1, "정치");
        issue = new Issue(category, "테스트 주제", null,
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusHours(1), List.of("예", "아니오"));
        yesOption = issue.getOptions().get(0);
        noOption = issue.getOptions().get(1);
        setId(yesOption, 100L);
        setId(noOption, 200L);
        issue.closeForResult(Map.of(100L, 6, 200L, 4)); // 참여자 10명, p(다수)=0.55, p(소수)=0.45
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
    }

    @Test
    void confirmIssue_correctVoter_gainsScoreAndSettlementRecordsCorrect() {
        User user = new User("정답자", "direct", null); // 100점(STARTING_CREDIBILITY_SCORE)으로 시작
        int stake = 100;
        Vote vote = new Vote(user, issue, yesOption, stake);
        user.applyScoreDelta(-stake); // VoteService.castVote가 하는 에스크로를 재현 -> 0점
        when(voteRepository.findByIssueId(1L)).thenReturn(List.of(vote));

        settlementService.confirmIssue(1L, 100L, null);

        ArgumentCaptor<ScoreSettlement> captor = ArgumentCaptor.forClass(ScoreSettlement.class);
        verify(scoreSettlementRepository).save(captor.capture());
        ScoreSettlement saved = captor.getValue();

        assertThat(saved.getResult()).isEqualTo(SettlementResult.CORRECT);
        assertThat(saved.getScoreDelta()).isPositive();
        // 에스크로로 0점이 된 상태에서 정산 크레딧(stake + scoreDelta)만큼 돌려받는다.
        assertThat(user.getCredibilityScore()).isEqualTo(stake + saved.getScoreDelta());
        assertThat(issue.getCorrectOption()).isEqualTo(yesOption);
    }

    @Test
    void confirmIssue_incorrectVoter_losesPartOfStakeButNeverGoesNegative() {
        User user = new User("오답자", "direct", null);
        int stake = 100;
        Vote vote = new Vote(user, issue, noOption, stake);
        user.applyScoreDelta(-stake); // 에스크로 -> 0점
        when(voteRepository.findByIssueId(1L)).thenReturn(List.of(vote));

        settlementService.confirmIssue(1L, 100L, null);

        // 오답 손실은 베팅액의 40%를 넘지 않도록 설계돼 있어(ScoringPolicy), 정산 크레딧
        // (stake + scoreDelta)은 항상 양수다 — 베팅 도입 전엔 "0으로 플로어"됐던 지점이지만,
        // 지금은 손실이 스테이크 안에서만 발생해 그 상황 자체가 구조적으로 없어졌다.
        assertThat(user.getCredibilityScore()).isPositive();
    }

    @Test
    void confirmIssue_scoreCrossingTierBoundary_recordsNaturalPromotion() {
        User user = new User("승급자", "direct", null);
        // 정산 직전 잔액이 이미 브론즈로 동기화되어 있던 상태(90점, 실버(100) 문턱 바로 아래)를 재현
        user.resetCredibilityScore(90);
        user.changeTier(Tier.BRONZE);
        Vote vote = new Vote(user, issue, yesOption, 100); // 정답, 다수(p=0.55) -> 대략 +18점 크레딧
        when(voteRepository.findByIssueId(1L)).thenReturn(List.of(vote));

        settlementService.confirmIssue(1L, 100L, null);

        ArgumentCaptor<TierChange> captor = ArgumentCaptor.forClass(TierChange.class);
        verify(tierChangeRepository).save(captor.capture());
        TierChange tierChange = captor.getValue();

        assertThat(tierChange.getReason()).isEqualTo(TierChangeReason.NATURAL_PROMOTION);
        assertThat(tierChange.getPreviousTier()).isEqualTo(Tier.BRONZE);
        assertThat(tierChange.getNewTier()).isEqualTo(user.getTier());
    }

    @Test
    void confirmIssue_activitySuppressedUser_neverRecalculatedAboveDiamondEvenIfScoreQualifies() {
        User user = new User("활동성강등유저", "direct", null);
        user.resetCredibilityScore(485); // 다이아 구간, 마스터(500) 문턱 바로 아래
        user.changeTier(Tier.DIAMOND);
        user.setActivitySuppressed(true); // 지난주 활동성 체크 미달로 강등 상태
        Vote vote = new Vote(user, issue, yesOption, 100); // 정답, 다수(p=0.55) -> 대략 +18점 크레딧 -> 500점 이상(마스터 구간)
        when(voteRepository.findByIssueId(1L)).thenReturn(List.of(vote));

        settlementService.confirmIssue(1L, 100L, null);

        assertThat(user.getCredibilityScore()).isGreaterThanOrEqualTo(500);
        assertThat(user.getTier()).isEqualTo(Tier.PLATINUM); // 점수는 마스터 구간이어도 강등 상태라 플래티넘 캡
    }

    private void setId(IssueOption option, Long id) throws Exception {
        Field field = IssueOption.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(option, id);
    }
}
