package com.predict.controller;

import com.predict.User;
import com.predict.controller.dto.CrewDtos.CrewJoinRequest;
import com.predict.controller.dto.CrewDtos.MyCrewResponse;
import com.predict.service.CrewService;
import com.predict.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 내 크루 조회·가입·변경. 변경은 마지막 가입 후 30일이 지나야 한다(409 + message). */
@RestController
@RequestMapping("/api/users/me/crew")
public class MyCrewController {

    private final CrewService crewService;
    private final CurrentUserService currentUserService;

    public MyCrewController(CrewService crewService, CurrentUserService currentUserService) {
        this.crewService = crewService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public MyCrewResponse get(@RequestHeader("Authorization") String authorization) {
        User user = currentUserService.requireUser(authorization);
        return crewService.myCrew(user);
    }

    @PutMapping
    public MyCrewResponse join(@RequestHeader("Authorization") String authorization,
                               @Valid @RequestBody CrewJoinRequest request) {
        User user = currentUserService.requireActiveUser(authorization);
        return crewService.join(user, request.crewId());
    }
}
