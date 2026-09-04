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
import com.predict.enums.IssueStatus;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementCorrectionServiceTest {

    @Mock
    private IssueRepository issueRepository;
    @Mock
    private VoteRepository voteRepository;
    @Mock
    private ScoreSettlementRepository scoreSettlementRepository;
    @Mock
    private TierChangeRepository tierChangeRepository;

    private SettlementCorrectionService correctionService;
    private Category category;

    @BeforeEach
    void setUp() {
        correctionService = new SettlementCorrectionService(issueRepository, voteRepository,
                scoreSettlementRepository, tierChangeRepository);
        category = new Category(1, "정치");
    }

    @Test
    void correctIssue_reversesSettlementsAndReplaysScoreToZeroWhenNoneRemain() throws Exception {
        Issue issue = new Issue(category, "테스트", null,
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1), List.of("예", "아니오"));
        IssueOption yesOption = issue.getOptions().get(0);
        setId(yesOption, 100L);
        setId(issue.getOptions().get(1), 200L);
        issue.closeForResult(Map.of(100L, 6, 200L, 4));
        issue.confirm(yesOption, LocalDateTime.now(), null);
        setId(issue, 1L);

        User user = new User("유저", "direct", null); // 100점(STARTING_CREDIBILITY_SCORE)으로 시작
        setId(user, 10L);
        user.changeTier(Tier.BRONZE);
        // 이 투표는 100을 베팅했고, 정정 대상 정산이 무효화되면 다시 "정산 안 된" 상태로
        // 돌아가 에스크로된 채로 잡힌다 — 아래 findByVoteIdAndIsReversedFalse가 그 상태를 흉내낸다.
        Vote vote = new Vote(user, issue, yesOption, 100);
        ScoreSettlement settlement = new ScoreSettlement(vote, user, issue, yesOption,
                SettlementResult.CORRECT, BigDecimal.valueOf(0.55), 20, 20);

        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(scoreSettlementRepository.findByIssueId(1L)).thenReturn(List.of(settlement));
        when(scoreSettlementRepository.findByIssueIdAndIsReversedFalse(1L)).thenReturn(List.of(settlement));
        when(scoreSettlementRepository.findByUserIdAndIsReversedFalseOrderBySettledAtAscIdAsc(anyLong()))
                .thenReturn(List.of());
        when(voteRepository.findByUserIdOrderByVotedAtDesc(anyLong())).thenReturn(List.of(vote));
        when(scoreSettlementRepository.findByVoteIdAndIsReversedFalse(any())).thenReturn(Optional.empty());

        correctionService.correctIssue(1L);

        assertThat(settlement.isReversed()).isTrue();
        assertThat(issue.getStatus()).isEqualTo(IssueStatus.PENDING_RESULT);
        assertThat(issue.getCorrectOption()).isNull();
        assertThat(issue.getConfirmedBy()).isNull();
        // 재생된 잔액(정산 기록 없음 -> 시작값 100) - 아직 안 풀린 이 투표의 에스크로(100) = 0
        assertThat(user.getCredibilityScore()).isZero();

        ArgumentCaptor<TierChange> captor = ArgumentCaptor.forClass(TierChange.class);
        verify(tierChangeRepository).save(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo(TierChangeReason.CORRECTION);
    }

    @Test
    void correctIssue_openIssue_throws() {
        Issue issue = new Issue(category, "테스트", null, LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                List.of("예", "아니오"));
        when(issueRepository.findById(2L)).thenReturn(Optional.of(issue));

        assertThatThrownBy(() -> correctionService.correctIssue(2L))
                .isInstanceOf(IllegalStateException.class);
    }

    private void setId(Issue issue, Long id) throws Exception {
        Field field = Issue.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(issue, id);
    }

    private void setId(User user, Long id) throws Exception {
        Field field = User.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
    }

    private void setId(IssueOption option, Long id) throws Exception {
        Field field = IssueOption.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(option, id);
    }
}
