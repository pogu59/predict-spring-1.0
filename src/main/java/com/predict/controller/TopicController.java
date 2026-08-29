package com.predict.controller;

import com.predict.Category;
import com.predict.Topic;
import com.predict.User;
import com.predict.Vote;
import com.predict.controller.dto.SettlementConfirmRequest;
import com.predict.controller.dto.TopicCreateRequest;
import com.predict.controller.dto.TopicResponse;
import com.predict.controller.dto.VoteRequest;
import com.predict.controller.dto.VoteResponse;
import com.predict.enums.Choice;
import com.predict.repository.CategoryRepository;
import com.predict.repository.TopicRepository;
import com.predict.repository.UserRepository;
import com.predict.repository.VoteRepository;
import com.predict.service.SettlementService;
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
 * 주제 등록/조회/투표/정산. 등록·정산 엔드포인트는 사실상 관리자 전용이지만
 * 이 프로젝트에는 아직 인증 체계가 없어 접근 제한은 걸려있지 않다(알려진 한계).
 */
@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicRepository topicRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final VoteRepository voteRepository;
    private final VoteService voteService;
    private final SettlementService settlementService;

    public TopicController(TopicRepository topicRepository, CategoryRepository categoryRepository,
                            UserRepository userRepository, VoteRepository voteRepository,
                            VoteService voteService, SettlementService settlementService) {
        this.topicRepository = topicRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.voteRepository = voteRepository;
        this.voteService = voteService;
        this.settlementService = settlementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TopicResponse create(@Valid @RequestBody TopicCreateRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리: " + request.categoryId()));
        Topic topic = new Topic(category, request.title(), request.description(),
                request.voteStartAt(), request.voteDeadlineAt());
        return TopicResponse.from(topicRepository.save(topic));
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

    @PostMapping("/{topicId}/settlement/confirm")
    public TopicResponse confirmSettlement(@PathVariable Long topicId, @Valid @RequestBody SettlementConfirmRequest request) {
        settlementService.confirmTopic(topicId, request.correctAnswer());
        return TopicResponse.from(requireTopic(topicId));
    }

    @PostMapping("/{topicId}/settlement/void")
    public TopicResponse voidSettlement(@PathVariable Long topicId) {
        settlementService.voidTopic(topicId);
        return TopicResponse.from(requireTopic(topicId));
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
