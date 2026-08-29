package com.predict;

import com.predict.enums.Tier;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    /** 유입경로(direct, kakao 등). 알 수 없으면 null */
    @Column(name = "signup_channel", length = 50)
    private String signupChannel;

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected User() {
    }

    public User(String nickname, String signupChannel, ShareClick referredBy) {
        this.nickname = nickname;
        this.signupChannel = signupChannel;
        this.referredBy = referredBy;
        this.credibilityScore = 0;
        this.tier = Tier.UNRANKED;
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

    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public String getSignupChannel() {
        return signupChannel;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}