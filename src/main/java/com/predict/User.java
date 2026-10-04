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
     * 신규 유저에게 기본으로 지급되는 신용도. 이슈 투표가 이 점수를 베팅하는 방식이라
     * (VoteService.castVote 참고) 넉넉히 500을 시드로 준다 — 티어 구간표(TierPolicy)상 골드 시작.
     */
    public static final int STARTING_CREDIBILITY_SCORE = 500;

    /** 시작 신용도가 100이던 시절에 가입한 유저의 시작값. 컬럼 추가 시 기존 행은 이 값으로 채워진다. */
    public static final int LEGACY_STARTING_CREDIBILITY_SCORE = 100;

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

    /** 관리자 페이지 접근 권한. 관리자 페이지(AdminUserController)에서 부여/해제한다. */
    @Column(name = "role", nullable = false, length = 10)
    private Role role = Role.USER;

    /** 이메일 가입 유저만 채워진다(소셜 가입은 null). */
    @Column(name = "email", unique = true, length = 100)
    private String email;

    /** BCrypt 해시. 이메일 가입 유저만 채워진다. */
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    /**
     * 가입 시점의 시작 신용도. 오확정 정정(SettlementCorrectionService)이 잔액을 처음부터 재생할 때
     * 기준점으로 쓴다 — 시작값이 100에서 500으로 바뀌어서 유저마다 다를 수 있다.
     */
    @Column(name = "starting_credibility_score", nullable = false, columnDefinition = "int default 100")
    private int startingCredibilityScore = LEGACY_STARTING_CREDIBILITY_SCORE;

    /** 관리자가 활동을 정지한 상태. true면 투표·댓글·글쓰기를 막는다(CurrentUserService.requireActiveUser). */
    @Column(name = "suspended", nullable = false, columnDefinition = "boolean default false")
    private boolean suspended = false;

    @Column(name = "marketing_agreed", nullable = false, columnDefinition = "boolean default false")
    private boolean marketingAgreed = false;

    /** 필수 약관 동의 시각. 약관 동의 단계 이전에 가입한 유저는 null. */
    @Column(name = "terms_agreed_at")
    private LocalDateTime termsAgreedAt;

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
        this.startingCredibilityScore = STARTING_CREDIBILITY_SCORE;
        this.tier = TierPolicy.fromScore(STARTING_CREDIBILITY_SCORE);
    }

    /** 이메일 가입. */
    public static User forEmail(String nickname, String email, String passwordHash, boolean marketingAgreed) {
        User user = new User(nickname, "email", null, null);
        user.email = email;
        user.passwordHash = passwordHash;
        user.agreeTerms(marketingAgreed);
        return user;
    }

    /** 소셜 가입 마무리(닉네임·약관 단계) — 소셜 로그인 직후 자동 생성된 유저의 닉네임을 바꾸고 약관 동의를 기록한다. */
    public void completeProfile(String nickname, boolean marketingAgreed) {
        this.nickname = nickname;
        agreeTerms(marketingAgreed);
    }

    private void agreeTerms(boolean marketingAgreed) {
        this.marketingAgreed = marketingAgreed;
        this.termsAgreedAt = LocalDateTime.now();
    }

    public void changeRole(Role role) {
        this.role = role;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
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

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public int getStartingCredibilityScore() {
        return startingCredibilityScore;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public boolean isMarketingAgreed() {
        return marketingAgreed;
    }

    public LocalDateTime getTermsAgreedAt() {
        return termsAgreedAt;
    }
}