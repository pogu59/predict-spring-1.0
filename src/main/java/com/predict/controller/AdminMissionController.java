package com.predict.controller;

import com.predict.Mission;
import com.predict.controller.dto.MissionRequests.AdminCreateRequest;
import com.predict.controller.dto.MissionResponses.AdminListItem;
import com.predict.enums.SubmissionStatus;
import com.predict.repository.MissionRepository;
import com.predict.repository.MissionSubmissionRepository;
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

import java.util.List;

/** 관리자 미션 관리 — 만들기(문항 포함), 공개, 닫기. 공개한 미션의 문항은 바꿀 수 없다. */
@RestController
@RequestMapping("/api/admin/missions")
public class AdminMissionController {

    private final MissionService missionService;
    private final MissionRepository missionRepository;
    private final MissionSubmissionRepository submissionRepository;
    private final CurrentUserService currentUserService;

    public AdminMissionController(MissionService missionService, MissionRepository missionRepository,
                                  MissionSubmissionRepository submissionRepository,
                                  CurrentUserService currentUserService) {
        this.missionService = missionService;
        this.missionRepository = missionRepository;
        this.submissionRepository = submissionRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<AdminListItem> list(@RequestHeader("Authorization") String authorization) {
        currentUserService.requireAdmin(authorization);
        return missionRepository.findAllByOrderByIdDesc().stream().map(this::toItem).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminListItem create(@RequestHeader("Authorization") String authorization,
                                @Valid @RequestBody AdminCreateRequest request) {
        currentUserService.requireAdmin(authorization);
        Mission mission = missionService.create(request.type(), request.title(), request.description(),
                request.rewardPoints(), request.daily(), request.startsAt(), request.endsAt(),
                request.questionDrafts(), request.openNow());
        return toItem(mission);
    }

    @PostMapping("/{missionId}/open")
    public AdminListItem open(@RequestHeader("Authorization") String authorization, @PathVariable Long missionId) {
        currentUserService.requireAdmin(authorization);
        return toItem(missionService.open(missionId));
    }

    @PostMapping("/{missionId}/close")
    public AdminListItem close(@RequestHeader("Authorization") String authorization, @PathVariable Long missionId) {
        currentUserService.requireAdmin(authorization);
        return toItem(missionService.close(missionId));
    }

    private AdminListItem toItem(Mission mission) {
        return AdminListItem.of(mission,
                submissionRepository.countByMissionIdAndStatus(mission.getId(), SubmissionStatus.APPROVED),
                submissionRepository.countByMissionIdAndStatus(mission.getId(), SubmissionStatus.REJECTED));
    }
}
