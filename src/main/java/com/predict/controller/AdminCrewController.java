package com.predict.controller;

import com.predict.controller.dto.CrewDtos.AdminCrewRequest;
import com.predict.controller.dto.CrewDtos.AdminCrewResponse;
import com.predict.service.CrewService;
import com.predict.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 관리자 크루 관리 — 추가·수정·비활성화(삭제 대신 active=false). */
@RestController
@RequestMapping("/api/admin/crews")
public class AdminCrewController {

    private final CrewService crewService;
    private final CurrentUserService currentUserService;

    public AdminCrewController(CrewService crewService, CurrentUserService currentUserService) {
        this.crewService = crewService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<AdminCrewResponse> list(@RequestHeader("Authorization") String authorization) {
        currentUserService.requireAdmin(authorization);
        return crewService.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCrewResponse create(@RequestHeader("Authorization") String authorization,
                                    @Valid @RequestBody AdminCrewRequest request) {
        currentUserService.requireAdmin(authorization);
        return crewService.create(request);
    }

    @PutMapping("/{crewId}")
    public AdminCrewResponse update(@RequestHeader("Authorization") String authorization,
                                    @PathVariable Long crewId, @Valid @RequestBody AdminCrewRequest request) {
        currentUserService.requireAdmin(authorization);
        return crewService.update(crewId, request);
    }
}
