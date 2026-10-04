package com.predict.service;

import com.predict.Category;
import com.predict.Issue;
import com.predict.IssueOption;
import com.predict.User;
import com.predict.Vote;
import com.predict.repository.IssueRepository;
import com.predict.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @Mock
    private VoteRepository voteRepository;
    @Mock
    private IssueRepository issueRepository;

    private VoteService voteService;
    private Issue issue;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        voteService = new VoteService(voteRepository, issueRepository);
        issue = new Issue(new Category(1, "정치"), "제목", null, LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusDays(1), List.of("예", "아니오"));
        setId(issue.getOptions().get(0), 100L);
        setId(issue.getOptions().get(1), 200L);
        user = new User("유저", "direct", null);
        setUserId(user, 7L);
    }

    @Test
    void castVote_escrowsStake() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(voteRepository.save(any(Vote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        voteService.castVote(user, 1L, 100L, 120);

        assertThat(user.getCredibilityScore()).isEqualTo(User.STARTING_CREDIBILITY_SCORE - 120);
    }

    @Test
    void castVote_moreThanBalance_isRejected() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));

        assertThatThrownBy(() -> voteService.castVote(user, 1L, 100L, User.STARTING_CREDIBILITY_SCORE + 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("보유 신용도가 부족해요");
    }

    @Test
    void castVote_suspendedUser_isForbidden() {
        user.setSuspended(true);

        assertThatThrownBy(() -> voteService.castVote(user, 1L, 100L, 10))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void changeVote_switchesOptionWithoutTouchingStakeOrBalance() {
        Vote vote = new Vote(user, issue, issue.getOptions().get(0), 100);
        user.applyScoreDelta(-100);
        int balance = user.getCredibilityScore();
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(voteRepository.findByUserIdAndIssueId(7L, 1L)).thenReturn(Optional.of(vote));

        voteService.changeVote(user, 1L, 200L);

        assertThat(vote.getIssueOption().getId()).isEqualTo(200L);
        assertThat(vote.getStake()).isEqualTo(100);
        assertThat(user.getCredibilityScore()).isEqualTo(balance);
    }

    @Test
    void changeVote_withoutExistingVote_isRejected() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(voteRepository.findByUserIdAndIssueId(7L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voteService.changeVote(user, 1L, 200L)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void changeVote_afterDeadline_isRejected() throws Exception {
        Field deadline = Issue.class.getDeclaredField("voteDeadlineAt");
        deadline.setAccessible(true);
        deadline.set(issue, LocalDateTime.now().minusSeconds(1));
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));

        assertThatThrownBy(() -> voteService.changeVote(user, 1L, 200L)).isInstanceOf(IllegalStateException.class);
    }

    private void setId(IssueOption option, Long id) throws Exception {
        Field field = IssueOption.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(option, id);
    }

    private void setUserId(User target, Long id) throws Exception {
        Field field = User.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
