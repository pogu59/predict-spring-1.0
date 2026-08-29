package com.predict.controller;

import com.predict.Topic;
import com.predict.User;
import com.predict.Vote;
import com.predict.controller.dto.TopicResponse;
import com.predict.controller.dto.VoteRequest;
import com.predict.controller.dto.VoteResponse;
import com.predict.enums.Choice;
import com.predict.repository.TopicRepository;
import com.predict.repository.UserRepository;
import com.predict.repository.VoteRepository;
import com.predict.service.VoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 주제 공개 조회 및 투표. 생성/결과확정/정정/수정 등 관리자 전용 액션은
 * AdminTopicController(/api/admin/topics)로 분리되어 role=admin 게이팅이 걸려 있다.
 */
@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final VoteRepository voteRepository;
    private final VoteService voteService;

    public TopicController(TopicRepository topicRepository, UserRepository userRepository,
                            VoteRepository voteRepository, VoteService voteService) {
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
        this.voteRepository = voteRepository;
        this.voteService = voteService;
    }

    @GetMapping
    public List<TopicResponse> list() {
        return topicRepository.findAll().stream().map(TopicResponse::from).toList();
    }

    @GetMapping("/{topicId}")
    public TopicResponse get(@PathVariable Long topicId) {
        return TopicResponse.from(requireTopic(topicId));
    }

    @PostMapping("/{topicId}/votes")
    @ResponseStatus(HttpStatus.CREATED)
    public VoteResponse castVote(@PathVariable Long topicId, @Valid @RequestBody VoteRequest request) {
        Vote vote = voteService.castVote(requireUser(request.userId()), topicId, request.choice());
        long liveYes = voteRepository.countByTopicIdAndChoice(topicId, Choice.YES);
        long liveNo = voteRepository.countByTopicIdAndChoice(topicId, Choice.NO);
        return VoteResponse.of(vote, (int) liveYes, (int) liveNo);
    }

    private Topic requireTopic(Long topicId) {
        return topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주제: " + topicId));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저: " + userId));
    }
}
