package com.predict.controller;

import com.predict.Mission;
import com.predict.MissionSubmission;
import com.predict.User;
import com.predict.controller.dto.MissionRequests.SubmitRequest;
import com.predict.controller.dto.MissionResponses.Detail;
import com.predict.controller.dto.MissionResponses.ListItem;
import com.predict.controller.dto.MissionResponses.QuestionResultResponse;
import com.predict.controller.dto.MissionResponses.ResultsResponse;
import com.predict.controller.dto.MissionResponses.SubmitResponse;
import com.predict.enums.MissionStatus;
import com.predict.service.CurrentUserService;
import com.predict.service.MissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 참여자 미션 API. 목록·상세는 비로그인도 볼 수 있고(내 상태만 비어 있음), 제출과 결과는 로그인 필요.
 * 응답에는 참여 인원과 확인 문항의 정답을 넣지 않는다.
 */
@RestController
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionService missionService;
    private final CurrentUserService currentUserService;

    public MissionController(MissionService missionService, CurrentUserService currentUserService) {
        this.missionService = missionService;
        this.currentUserService = currentUserService;
    }

    /** 지금 참여할 수 있는 미션(오늘의 미션이 앞). */
    @GetMapping
    public List<ListItem> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        User user = currentUserService.optionalUser(authorization);
        LocalDateTime now = LocalDateTime.now();
        List<Mission> missions = missionService.availableMissions(now);
        Map<Long, MissionSubmission> mine = missionService.mySubmissions(user, missions, now.toLocalDate());
        return missions.stream().map(mission -> ListItem.of(mission, mine.get(mission.getId()))).toList();
    }

    @GetMapping("/{missionId}")
    public Detail get(@RequestHeader(value = "Authorization", required = false) String authorization,
                      @PathVariable Long missionId) {
        User user = currentUserService.optionalUser(authorization);
        LocalDateTime now = LocalDateTime.now();
        Mission mission = missionService.require(missionId);
        if (mission.getStatus() == MissionStatus.DRAFT) {
            throw new IllegalArgumentException("존재하지 않는 미션: " + missionId);
        }
        MissionSubmission mine = missionService.mySubmissions(user, List.of(mission), now.toLocalDate()).get(missionId);
        return Detail.of(mission, mine, now);
    }

    @PostMapping("/{missionId}/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmitResponse submit(@RequestHeader("Authorization") String authorization,
                                 @PathVariable Long missionId, @Valid @RequestBody SubmitRequest request) {
        User user = currentUserService.requireActiveUser(authorization);
        return SubmitResponse.from(missionService.submit(user, missionId, request.answers(), request.durationMs()));
    }

    /** 참여한 사람에게만 문항별 응답 비율. */
    @GetMapping("/{missionId}/results")
    public ResultsResponse results(@RequestHeader("Authorization") String authorization,
                                   @PathVariable Long missionId) {
        User user = currentUserService.requireUser(authorization);
        Mission mission = missionService.require(missionId);
        List<QuestionResultResponse> questions = missionService.results(user, missionId).stream()
                .map(QuestionResultResponse::from)
                .toList();
        return new ResultsResponse(mission.getId(), mission.getTitle(), questions);
    }
}
