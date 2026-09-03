package com.predict.controller;

import com.predict.Topic;
import com.predict.TopicOption;
import com.predict.User;
import com.predict.Vote;
import com.predict.controller.dto.TopicOptionResponse;
import com.predict.controller.dto.TopicResponse;
import com.predict.controller.dto.VoteRequest;
import com.predict.controller.dto.VoteResponse;
import com.predict.enums.TopicStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public List<TopicResponse> list(@RequestParam(required = false) Long userId) {
        List<Topic> topics = topicRepository.findAll();
        if (userId == null) {
            return topics.stream().map(TopicResponse::from).toList();
        }
        Map<Long, Long> myOptionIdByTopicId = new HashMap<>();
        for (Vote vote : voteRepository.findByUserIdOrderByVotedAtDesc(userId)) {
            myOptionIdByTopicId.put(vote.getTopic().getId(), vote.getTopicOption().getId());
        }
        return topics.stream()
                .map(topic -> {
                    Long myOptionId = myOptionIdByTopicId.get(topic.getId());
                    Map<Long, Integer> liveCounts = needsLiveCounts(topic, myOptionId)
                            ? liveCountsByOptionId(topic)
                            : null;
                    return TopicResponse.from(topic, myOptionId, liveCounts);
                })
                .toList();
    }

    @GetMapping("/{topicId}")
    public TopicResponse get(@PathVariable Long topicId, @RequestParam(required = false) Long userId) {
        Topic topic = requireTopic(topicId);
        if (userId == null) {
            return TopicResponse.from(topic);
        }
        Long myOptionId = voteRepository.findByUserIdAndTopicId(userId, topicId)
                .map(vote -> vote.getTopicOption().getId())
                .orElse(null);
        Map<Long, Integer> liveCounts = needsLiveCounts(topic, myOptionId) ? liveCountsByOptionId(topic) : null;
        return TopicResponse.from(topic, myOptionId, liveCounts);
    }

    @PostMapping("/{topicId}/votes")
    @ResponseStatus(HttpStatus.CREATED)
    public VoteResponse castVote(@PathVariable Long topicId, @Valid @RequestBody VoteRequest request) {
        Vote vote = voteService.castVote(requireUser(request.userId()), topicId, request.optionId());
        Topic topic = vote.getTopic();
        List<TopicOptionResponse> liveCounts = topic.getOptions().stream()
                .map(option -> new TopicOptionResponse(option.getId(), option.getText(),
                        (int) voteRepository.countByTopicIdAndTopicOptionId(topicId, option.getId())))
                .toList();
        return VoteResponse.of(vote, liveCounts);
    }

    private boolean needsLiveCounts(Topic topic, Long myOptionId) {
        return topic.getStatus() == TopicStatus.OPEN && myOptionId != null;
    }

    /** OPEN 상태에서 본인에게 노출할 실시간 득표수. TopicOption.voteCount는 마감 전엔 null이라 직접 집계한다. */
    private Map<Long, Integer> liveCountsByOptionId(Topic topic) {
        Map<Long, Integer> counts = new HashMap<>();
        for (TopicOption option : topic.getOptions()) {
            counts.put(option.getId(), (int) voteRepository.countByTopicIdAndTopicOptionId(topic.getId(), option.getId()));
        }
        return counts;
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
