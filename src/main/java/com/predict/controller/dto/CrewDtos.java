package com.predict.controller.dto;

import com.predict.Crew;
import com.predict.enums.Tier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 크루 대항전 요청/응답 DTO 모음(CommunityRequests와 같은 이유로 한 파일에 둔다). */
public final class CrewDtos {

    private CrewDtos() {
    }

    public record CrewResponse(Long id, String name, String slug, String description, long memberCount) {
        public static CrewResponse from(Crew crew, long memberCount) {
            return new CrewResponse(crew.getId(), crew.getName(), crew.getSlug(), crew.getDescription(), memberCount);
        }
    }

    /** 한 주의 크루 성적. rank가 null이면 집계 중(활성 멤버 5명 미만). */
    public record CrewWeekResponse(LocalDate weekStart, Integer rank, BigDecimal scorePerMember, int activeMembers) {
    }

    public record CrewDetailResponse(Long id, String name, String slug, String description, long memberCount,
                                     CrewWeekResponse thisWeek) {
    }

    public record CrewRankingItemResponse(Integer rank, Long crewId, String name, BigDecimal scorePerMember,
                                          int activeMembers, long memberCount) {
    }

    /** 닉네임·티어 외 개인정보는 싣지 않는다. */
    public record CrewTopMemberResponse(String nickname, Tier tier, int scoreGain) {
    }

    /**
     * 내 크루. 가입 전이면 crew·joinedAt·nextChangeAt이 null.
     * weekScoreGain/weekSettlements: 이번 주 내가 이 크루로 낸 점수 증가와 유효 정산 수(3건부터 활성 멤버).
     */
    public record MyCrewResponse(CrewResponse crew, LocalDateTime joinedAt, LocalDateTime nextChangeAt,
                                 int weekScoreGain, int weekSettlements) {
    }

    public record CrewJoinRequest(@NotNull Long crewId) {
    }

    /** slug를 비우면 이름으로 만든다. */
    public record AdminCrewRequest(
            @NotBlank @Size(max = 30) String name,
            @Size(max = 40) @Pattern(regexp = "^[a-z0-9-]*$", message = "slug는 영문 소문자·숫자·-만 쓸 수 있어요") String slug,
            @Size(max = 200) String description,
            boolean active) {
    }

    public record AdminCrewResponse(Long id, String name, String slug, String description, boolean active,
                                    long memberCount, LocalDateTime createdAt) {
        public static AdminCrewResponse from(Crew crew, long memberCount) {
            return new AdminCrewResponse(crew.getId(), crew.getName(), crew.getSlug(), crew.getDescription(),
                    crew.isActive(), memberCount, crew.getCreatedAt());
        }
    }
}
