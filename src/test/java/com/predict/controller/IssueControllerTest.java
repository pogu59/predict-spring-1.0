package com.predict.controller;

import com.predict.Category;
import com.predict.Issue;
import com.predict.IssueOption;
import com.predict.User;
import com.predict.Vote;
import com.predict.controller.dto.IssueResponse;
import com.predict.controller.dto.VoteRequest;
import com.predict.controller.dto.VoteResponse;
import com.predict.repository.IssueRepository;
import com.predict.repository.UserRepository;
import com.predict.repository.VoteRepository;
import com.predict.service.CurrentUserService;
import com.predict.service.ReplyService;
import com.predict.service.ReportService;
import com.predict.service.VoteCountService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueControllerTest {

    @Mock
    private IssueRepository issueRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VoteRepository voteRepository;
    @Mock
    private VoteService voteService;
    @Mock
    private ReplyService replyService;
    @Mock
    private ReportService reportService;
    @Mock
    private CurrentUserService currentUserService;

    private IssueController issueController;
    private Category category;

    @BeforeEach
    void setUp() {
        issueController = new IssueController(issueRepository, userRepository, voteRepository, voteService,
                new VoteCountService(voteRepository), replyService, reportService, currentUserService);
        category = new Category(1, "정치");
    }

    @Test
    void get_openIssue_exposesPercentsButNeverVoteCounts() throws Exception {
        Issue issue = openIssueWithOptionIds();
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(voteRepository.countByOptionForIssues(anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{100L, 3L}, new Object[]{200L, 1L}));

        IssueResponse response = issueController.get(1L, null);

        assertThat(response.options()).allSatisfy(option -> assertThat(option.voteCount()).isNull());
        assertThat(response.options().get(0).percent()).isEqualTo(75);
        assertThat(response.options().get(1).percent()).isEqualTo(25);
        assertThat(response.myOptionId()).isNull();
    }

    @Test
    void get_openIssue_votedByRequester_returnsMyOptionAndStake() throws Exception {
        Issue issue = openIssueWithOptionIds();
        IssueOption yesOption = issue.getOptions().get(0);
        User user = new User("유저", "direct", null);
        Vote vote = new Vote(user, issue, yesOption, 10);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(voteRepository.findByUserIdAndIssueId(9L, 1L)).thenReturn(Optional.of(vote));
        when(voteRepository.countByOptionForIssues(anyCollection())).thenReturn(List.<Object[]>of(new Object[]{100L, 1L}));

        IssueResponse response = issueController.get(1L, 9L);

        assertThat(response.myOptionId()).isEqualTo(100L);
        assertThat(response.myStake()).isEqualTo(10);
        assertThat(response.options().get(0).percent()).isEqualTo(100);
    }

    @Test
    void get_confirmedIssue_usesSnapshotForPercents() throws Exception {
        Issue issue = new Issue(category, "제목", null, LocalDateTime.now().minusDays(2), LocalDateTime.now().minusHours(1),
                List.of("예", "아니오", "모름"));
        setId(issue.getOptions().get(0), 100L);
        setId(issue.getOptions().get(1), 200L);
        setId(issue.getOptions().get(2), 300L);
        issue.closeForResult(Map.of(100L, 1, 200L, 1, 300L, 1));
        issue.confirm(issue.getOptions().get(0), LocalDateTime.now(), null);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));

        IssueResponse response = issueController.get(1L, null);

        // 33/33/33 -> 합 99라 가장 큰 값(동률이면 앞)에 1을 더한다.
        assertThat(response.options()).extracting(o -> o.percent()).containsExactly(34, 33, 33);
        assertThat(response.options()).allSatisfy(option -> assertThat(option.voteCount()).isNull());
    }

    @Test
    void get_deletedIssue_isNotFound() throws Exception {
        Issue issue = openIssueWithOptionIds();
        issue.markDeleted(LocalDateTime.now());
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));

        assertThatThrownBy(() -> issueController.get(1L, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void castVote_returnsPercentsAndRemainingCredibility() throws Exception {
        User user = new User("유저", "direct", null);
        Issue issue = openIssueWithOptionIds();
        Vote vote = new Vote(user, issue, issue.getOptions().get(0), 10);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(voteService.castVote(eq(user), eq(5L), eq(100L), eq(10))).thenReturn(vote);
        when(voteRepository.countByOptionForIssues(anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{100L, 7L}, new Object[]{200L, 3L}));

        VoteResponse response = issueController.castVote(5L, new VoteRequest(1L, 100L, 10));

        assertThat(response.liveCounts()).extracting(o -> o.percent()).containsExactly(70, 30);
        assertThat(response.remainingCredibility()).isEqualTo(user.getCredibilityScore());
    }

    private Issue openIssueWithOptionIds() throws Exception {
        Issue issue = new Issue(category, "제목", null, LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                List.of("예", "아니오"));
        setId(issue, Issue.class, 1L);
        setId(issue.getOptions().get(0), 100L);
        setId(issue.getOptions().get(1), 200L);
        return issue;
    }

    private void setId(IssueOption option, Long id) throws Exception {
        setId(option, IssueOption.class, id);
    }

    private void setId(Object target, Class<?> type, Long id) throws Exception {
        Field field = type.getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
