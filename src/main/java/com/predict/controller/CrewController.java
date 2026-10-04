package com.predict.controller;

import com.predict.controller.dto.CrewDtos.CrewDetailResponse;
import com.predict.controller.dto.CrewDtos.CrewRankingItemResponse;
import com.predict.controller.dto.CrewDtos.CrewResponse;
import com.predict.controller.dto.CrewDtos.CrewTopMemberResponse;
import com.predict.service.CrewService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** 크루 대항전 공개 조회 API. week는 그 주 아무 날짜(YYYY-MM-DD)나 받아 월요일로 맞춘다. */
@RestController
@RequestMapping("/api/crews")
public class CrewController {

    private final CrewService crewService;

    public CrewController(CrewService crewService) {
        this.crewService = crewService;
    }

    @GetMapping
    public List<CrewResponse> list() {
        return crewService.listActive();
    }

    @GetMapping("/ranking")
    public List<CrewRankingItemResponse> ranking(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        return crewService.ranking(week);
    }

    @GetMapping("/{crewId}")
    public CrewDetailResponse get(@PathVariable Long crewId) {
        return crewService.detail(crewId);
    }

    @GetMapping("/{crewId}/top-members")
    public List<CrewTopMemberResponse> topMembers(
            @PathVariable Long crewId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        return crewService.topMembers(crewId, week);
    }
}
