package com.predict;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 유저의 현재 크루(유저당 1개). 크루를 바꾸면 직전 소속을 previous_* 에 남긴다 — 주 중간에 옮긴 유저의
 * 점수를 "정산 시점에 소속된 크루"로 보내려면 joined_at 이전 정산이 어느 크루였는지 알아야 하기 때문이다.
 * 크루 변경은 30일에 한 번이라 한 주 안에 바뀌는 건 많아야 한 번이고, 직전 소속 하나로 충분하다.
 */
@Getter
@Entity
@Table(name = "user_crews")
public class UserCrew {

    /** 크루를 다시 바꿀 수 있을 때까지의 기간. */
    public static final int CHANGE_COOLDOWN_DAYS = 30;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_crew_id")
    private Crew previousCrew;

    @Column(name = "previous_joined_at")
    private LocalDateTime previousJoinedAt;

    protected UserCrew() {
    }

    public UserCrew(User user, Crew crew, LocalDateTime joinedAt) {
        this.user = user;
        this.crew = crew;
        this.joinedAt = joinedAt;
    }

    public LocalDateTime nextChangeAt() {
        return joinedAt.plusDays(CHANGE_COOLDOWN_DAYS);
    }

    public void changeTo(Crew newCrew, LocalDateTime now) {
        this.previousCrew = this.crew;
        this.previousJoinedAt = this.joinedAt;
        this.crew = newCrew;
        this.joinedAt = now;
    }
}
