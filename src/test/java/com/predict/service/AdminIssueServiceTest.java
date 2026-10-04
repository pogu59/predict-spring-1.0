package com.predict.service;

import com.predict.Category;
import com.predict.Issue;
import com.predict.IssueOption;
import com.predict.User;
import com.predict.Vote;
import com.predict.enums.IssueStatus;
import com.predict.repository.CategoryRepository;
import com.predict.repository.IssueRepository;
import com.predict.repository.VoteRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminIssueServiceTest {

    @Mock
    private IssueRepository issueRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private VoteRepository voteRepository;

    private AdminIssueService service;
    private final Category category = new Category(1, "정치");

    @BeforeEach
    void setUp() {
        service = new AdminIssueService(issueRepository, categoryRepository, voteRepository);
    }

    @Test
    void deleteIssue_beforeSettlement_refundsEveryStake() {
        Issue issue = new Issue(category, "제목", null, LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                List.of("예", "아니오"));
        User user = new User("참여자", "direct", null);
        user.applyScoreDelta(-150); // 투표 시 에스크로된 상태
        Vote vote = new Vote(user, issue, issue.getOptions().get(0), 150);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(voteRepository.findByIssueId(1L)).thenReturn(List.of(vote));

        service.deleteIssue(1L);

        assertThat(user.getCredibilityScore()).isEqualTo(User.STARTING_CREDIBILITY_SCORE);
        assertThat(issue.isDeleted()).isTrue();
    }

    @Test
    void deleteIssue_confirmed_doesNotRefundAgain() throws Exception {
        Issue issue = confirmedIssue();
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));

        service.deleteIssue(1L);

        verify(voteRepository, never()).findByIssueId(1L);
        assertThat(issue.isDeleted()).isTrue();
    }

    @Test
    void deleteIssue_twice_isRejected() {
        Issue issue = new Issue(category, "제목", null, LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                List.of("예", "아니오"));
        issue.markDeleted(LocalDateTime.now());
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));

        assertThatThrownBy(() -> service.deleteIssue(1L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void extendDeadline_pendingIssueIntoFuture_reopensAndClearsSnapshot() throws Exception {
        Issue issue = new Issue(category, "제목", null, LocalDateTime.now().minusDays(2), LocalDateTime.now().minusHours(1),
                List.of("예", "아니오"));
        setId(issue.getOptions().get(0), 100L);
        setId(issue.getOptions().get(1), 200L);
        issue.closeForResult(Map.of(100L, 3, 200L, 1));
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));

        LocalDateTime newDeadline = LocalDateTime.now().plusDays(1);
        service.extendDeadline(1L, newDeadline);

        assertThat(issue.getStatus()).isEqualTo(IssueStatus.OPEN);
        assertThat(issue.getVoteDeadlineAt()).isEqualTo(newDeadline);
        assertThat(issue.getOptions()).allSatisfy(option -> assertThat(option.getVoteCount()).isNull());
    }

    @Test
    void extendDeadline_confirmedIssue_isRejected() throws Exception {
        Issue issue = confirmedIssue();
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));

        assertThatThrownBy(() -> service.extendDeadline(1L, LocalDateTime.now().plusDays(1)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createIssue_duplicateOptions_isRejected() {
        assertThatThrownBy(() -> service.createIssue(1, "제목", null, LocalDateTime.now().plusMinutes(1),
                LocalDateTime.now().plusDays(1), List.of("예", "예 "), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("같은 선택지가 있어요");
    }

    @Test
    void createIssue_withoutCategory_usesFirstCategory() {
        when(categoryRepository.findAll(org.springframework.data.domain.Sort.by("id"))).thenReturn(List.of(category));
        when(issueRepository.save(org.mockito.ArgumentMatchers.any(Issue.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Issue issue = service.createIssue(null, " 제목 ", " ", LocalDateTime.now().plusMinutes(1),
                LocalDateTime.now().plusDays(1), List.of("예", "아니오"), "https://img/cover.png");

        assertThat(issue.getCategory()).isEqualTo(category);
        assertThat(issue.getTitle()).isEqualTo("제목");
        assertThat(issue.getDescription()).isNull();
        assertThat(issue.getCoverImageUrl()).isEqualTo("https://img/cover.png");
    }

    private Issue confirmedIssue() throws Exception {
        Issue issue = new Issue(category, "제목", null, LocalDateTime.now().minusDays(2), LocalDateTime.now().minusHours(1),
                List.of("예", "아니오"));
        IssueOption yes = issue.getOptions().get(0);
        setId(yes, 100L);
        setId(issue.getOptions().get(1), 200L);
        issue.closeForResult(Map.of(100L, 1, 200L, 1));
        issue.confirm(yes, LocalDateTime.now(), null);
        return issue;
    }

    private void setId(IssueOption option, Long id) throws Exception {
        Field field = IssueOption.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(option, id);
    }
}
