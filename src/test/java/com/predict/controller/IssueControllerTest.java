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
    private CurrentUserService currentUserService;

    private IssueController issueController;
    private Category category;

    @BeforeEach
    void setUp() {
        issueController = new IssueController(issueRepository, userRepository, voteRepository, voteService,
                replyService, currentUserService);
        category = new Category(1, "정치");
    }

    @Test
    void get_openIssue_hidesOptionVoteCounts() {
        Issue issue = new Issue(category, "제목", null, LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                List.of("예", "아니오"));
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));

        IssueResponse response = issueController.get(1L, null);

        assertThat(response.options()).allSatisfy(option -> assertThat(option.voteCount()).isNull());
        assertThat(response.myOptionId()).isNull();
    }

    @Test
    void get_openIssue_votedByRequester_exposesOptionVoteCounts() throws Exception {
        Issue issue = new Issue(category, "제목", null, LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                List.of("예", "아니오"));
        setId(issue, 1L);
        IssueOption yesOption = issue.getOptions().get(0);
        setId(yesOption, 100L);
        setId(issue.getOptions().get(1), 200L);
        User user = new User("유저", "direct", null);
        Vote vote = new Vote(user, issue, yesOption, 10);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(voteRepository.findByUserIdAndIssueId(9L, 1L)).thenReturn(Optional.of(vote));
        when(voteRepository.countByIssueIdAndIssueOptionId(1L, 100L)).thenReturn(3L);
        when(voteRepository.countByIssueIdAndIssueOptionId(1L, 200L)).thenReturn(1L);

        IssueResponse response = issueController.get(1L, 9L);

        assertThat(response.myOptionId()).isEqualTo(100L);
        assertThat(response.options()).noneMatch(option -> option.voteCount() == null);
    }

    @Test
    void get_confirmedIssue_exposesOptionVoteCounts() throws Exception {
        Issue issue = new Issue(category, "제목", null, LocalDateTime.now().minusDays(2), LocalDateTime.now().minusHours(1),
                List.of("예", "아니오"));
        IssueOption yesOption = issue.getOptions().get(0);
        setId(yesOption, 100L);
        setId(issue.getOptions().get(1), 200L);
        issue.closeForResult(Map.of(100L, 6, 200L, 4));
        issue.confirm(yesOption, LocalDateTime.now(), null);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));

        IssueResponse response = issueController.get(1L, null);

        assertThat(response.options().get(0).voteCount()).isEqualTo(6);
        assertThat(response.options().get(1).voteCount()).isEqualTo(4);
    }

    @Test
    void castVote_returnsLiveCountsFromRepository() throws Exception {
        User user = new User("유저", "direct", null);
        Issue issue = new Issue(category, "제목", null, LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                List.of("예", "아니오"));
        IssueOption yesOption = issue.getOptions().get(0);
        IssueOption noOption = issue.getOptions().get(1);
        setId(yesOption, 100L);
        setId(noOption, 200L);
        Vote vote = new Vote(user, issue, yesOption, 10);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(voteService.castVote(eq(user), eq(5L), eq(100L), eq(10))).thenReturn(vote);
        when(voteRepository.countByIssueIdAndIssueOptionId(5L, 100L)).thenReturn(7L);
        when(voteRepository.countByIssueIdAndIssueOptionId(5L, 200L)).thenReturn(3L);

        VoteResponse response = issueController.castVote(5L, new VoteRequest(1L, 100L, 10));

        assertThat(response.liveCounts()).hasSize(2);
        assertThat(response.liveCounts().get(0).voteCount()).isEqualTo(7);
        assertThat(response.liveCounts().get(1).voteCount()).isEqualTo(3);
    }

    private void setId(IssueOption option, Long id) throws Exception {
        setId((Object) option, IssueOption.class, id);
    }

    private void setId(Issue issue, Long id) throws Exception {
        setId((Object) issue, Issue.class, id);
    }

    private void setId(Object target, Class<?> type, Long id) throws Exception {
        Field field = type.getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
