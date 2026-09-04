package com.predict;

import com.predict.enums.Role;
import com.predict.enums.Tier;
import com.predict.tier.TierPolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 유저. 누적 신용도 점수와 현재 티어를 함께 보관한다.
 */
@Entity
@Table(name = "users")
public class User {

    /**
     * 신규 유저에게 기본으로 지급되는 신용도. 이슈 투표가 이제 이 점수를 베팅하는 방식이라
     * (VoteService.castVote 참고) 0으로 시작하면 아무것도 걸 수 없어 100을 시드로 준다.
     * 100은 티어 구간표(3-1절)상 실버 문턱이라, 신규 유저는 언랭크/브론즈를 건너뛰고
     * 바로 실버로 시작한다 — 의도된 동작이다.
     */
    public static final int STARTING_CREDIBILITY_SCORE = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    /** 유입경로(direct, kakao 등). 알 수 없으면 null */
    @Column(name = "signup_channel", length = 50)
    private String signupChannel;

    /** 카카오 로그인으로 가입한 경우의 카카오 회원번호. 재로그인 시 이 값으로 유저를 식별한다. */
    @Column(name = "kakao_id", unique = true, length = 50)
    private String kakaoId;

    /**
     * 가입 시 사용한 추천 코드. FK가 share_clicks의 PK가 아니라
     * UNIQUE 컬럼(referral_code)을 가리키므로 referencedColumnName을 명시한다.
     * 공유 링크를 안 타고 가입할 수 있으므로 null 허용.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referred_by_code", referencedColumnName = "referral_code")
    private ShareClick referredBy;

    /** 누적 신용도 점수. 하한 0은 DB가 아니라 여기서 강제한다. */
    @Column(name = "credibility_score", nullable = false)
    private int credibilityScore;

    @Column(name = "tier", nullable = false, length = 20)
    private Tier tier = Tier.UNRANKED;

    /** 관리자 페이지 접근 권한. 부여/해제는 API 없이 DB에서 직접 처리한다. */
    @Column(name = "role", nullable = false, length = 10)
    private Role role = Role.USER;

    /**
     * 다이아/마스터 주간 활동성 미달로 강등된 상태인지(WeeklyActivityService가 갱신).
     * true인 동안은 점수가 충분해도 실시간 정산(SettlementService)으로 다이아/마스터에
     * 복귀하지 못하고 플래티넘까지만 허용된다 — 오직 다음 주간 체크로만 해제된다.
     */
    @Column(name = "activity_suppressed", nullable = false, columnDefinition = "boolean default false")
    private boolean activitySuppressed = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected User() {
    }

    public User(String nickname, String signupChannel, ShareClick referredBy) {
        this(nickname, signupChannel, referredBy, null);
    }

    public User(String nickname, String signupChannel, ShareClick referredBy, String kakaoId) {
        this.nickname = nickname;
        this.signupChannel = signupChannel;
        this.referredBy = referredBy;
        this.kakaoId = kakaoId;
        this.credibilityScore = STARTING_CREDIBILITY_SCORE;
        this.tier = TierPolicy.fromScore(STARTING_CREDIBILITY_SCORE);
    }

    /**
     * 정산 결과를 누적 점수에 반영한다. 0 아래로는 내려가지 않는다.
     *
     * @return 반영 후 점수(score_settlements.score_after 에 그대로 기록)
     */
    public int applyScoreDelta(int delta) {
        this.credibilityScore = Math.max(0, this.credibilityScore + delta);
        return this.credibilityScore;
    }

    public void changeTier(Tier newTier) {
        this.tier = newTier;
    }

    public void setActivitySuppressed(boolean activitySuppressed) {
        this.activitySuppressed = activitySuppressed;
    }

    /**
     * 오확정 정정(SettlementCorrectionService) 재생(replay) 전용.
     * applyScoreDelta는 "기존 점수 + 델타"를 누적하는 반면, 정정은 남은 정산 기록으로
     * 처음부터 다시 계산한 값을 그대로 덮어써야 하므로 별도 메서드로 분리한다.
     */
    public void resetCredibilityScore(int score) {
        this.credibilityScore = Math.max(0, score);
    }

    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public String getSignupChannel() {
        return signupChannel;
    }

    public String getKakaoId() {
        return kakaoId;
    }

    public ShareClick getReferredBy() {
        return referredBy;
    }

    public int getCredibilityScore() {
        return credibilityScore;
    }

    public Tier getTier() {
        return tier;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActivitySuppressed() {
        return activitySuppressed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}