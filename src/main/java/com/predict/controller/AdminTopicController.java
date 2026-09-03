package com.predict.controller;

import com.predict.Topic;
import com.predict.User;
import com.predict.controller.dto.AdminTopicDetailResponse;
import com.predict.controller.dto.AdminTopicListItemResponse;
import com.predict.controller.dto.PageResponse;
import com.predict.controller.dto.TopicConfirmRequest;
import com.predict.controller.dto.TopicCreateRequest;
import com.predict.controller.dto.TopicExtendDeadlineRequest;
import com.predict.enums.TopicStatus;
import com.predict.repository.TopicRepository;
import com.predict.repository.VoteRepository;
import com.predict.service.AdminTopicService;
import com.predict.service.CurrentUserService;
import com.predict.service.SettlementCorrectionService;
import com.predict.service.SettlementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 페이지 — 주제 생성/조회/확정/정정/수정 (docs/predict.md 5장). 모든 엔드포인트는
 * 관리자(role=admin)만 호출할 수 있다.
 */
@RestController
@RequestMapping("/api/admin/topics")
public class AdminTopicController {

    private final TopicRepository topicRepository;
    private final VoteRepository voteRepository;
    private final CurrentUserService currentUserService;
    private final AdminTopicService adminTopicService;
    private final SettlementService settlementService;
    private final SettlementCorrectionService settlementCorrectionService;

    public AdminTopicController(TopicRepository topicRepository, VoteRepository voteRepository,
                                 CurrentUserService currentUserService, AdminTopicService adminTopicService,
                                 SettlementService settlementService,
                                 SettlementCorrectionService settlementCorrectionService) {
        this.topicRepository = topicRepository;
        this.voteRepository = voteRepository;
        this.currentUserService = currentUserService;
        this.adminTopicService = adminTopicService;
        this.settlementService = settlementService;
        this.settlementCorrectionService = settlementCorrectionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminTopicDetailResponse create(@RequestHeader("Authorization") String authorization,
                                            @Valid @RequestBody TopicCreateRequest request) {
        currentUserService.requireAdmin(authorization);
        Topic topic = adminTopicService.createTopic(request.categoryId(), request.title(),
                request.description(), request.voteStartAt(), request.voteDeadlineAt(), request.options());
        return toDetail(topic);
    }

    @GetMapping
    public PageResponse<AdminTopicListItemResponse> list(@RequestHeader("Authorization") String authorization,
                                                           @RequestParam(required = false) Integer categoryId,
                                                           @RequestParam(required = false) TopicStatus status,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        currentUserService.requireAdmin(authorization);
        var result = topicRepository.search(categoryId, status, keyword, PageRequest.of(page, size));
        return PageResponse.from(result, topic -> AdminTopicListItemResponse.from(topic, voteRepository.countByTopicId(topic.getId())));
    }

    @GetMapping("/{topicId}")
    public AdminTopicDetailResponse get(@RequestHeader("Authorization") String authorization,
                                         @PathVariable Long topicId) {
        currentUserService.requireAdmin(authorization);
        return toDetail(requireTopic(topicId));
    }

    @PostMapping("/{topicId}/confirm")
    public AdminTopicDetailResponse confirm(@RequestHeader("Authorization") String authorization,
                                             @PathVariable Long topicId,
                                             @Valid @RequestBody TopicConfirmRequest request) {
        User admin = currentUserService.requireAdmin(authorization);
        settlementService.confirmTopic(topicId, request.correctOptionId(), admin);
        return toDetail(requireTopic(topicId));
    }

    @PostMapping("/{topicId}/correct")
    public AdminTopicDetailResponse correct(@RequestHeader("Authorization") String authorization,
                                             @PathVariable Long topicId) {
        currentUserService.requireAdmin(authorization);
        settlementCorrectionService.correctTopic(topicId);
        return toDetail(requireTopic(topicId));
    }

    @PutMapping("/{topicId}")
    public AdminTopicDetailResponse update(@RequestHeader("Authorization") String authorization,
                                            @PathVariable Long topicId,
                                            @Valid @RequestBody TopicCreateRequest request) {
        currentUserService.requireAdmin(authorization);
        adminTopicService.updateTopic(topicId, request.categoryId(), request.title(),
                request.description(), request.voteStartAt(), request.voteDeadlineAt(), request.options());
        return toDetail(requireTopic(topicId));
    }

    @PostMapping("/{topicId}/extend-deadline")
    public AdminTopicDetailResponse extendDeadline(@RequestHeader("Authorization") String authorization,
                                                     @PathVariable Long topicId,
                                                     @Valid @RequestBody TopicExtendDeadlineRequest request) {
        currentUserService.requireAdmin(authorization);
        adminTopicService.extendDeadline(topicId, request.newDeadline());
        return toDetail(requireTopic(topicId));
    }

    private AdminTopicDetailResponse toDetail(Topic topic) {
        return AdminTopicDetailResponse.from(topic, voteRepository.countByTopicId(topic.getId()));
    }

    private Topic requireTopic(Long topicId) {
        return topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주제: " + topicId));
    }
}
