package com.predict;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 공유 버튼 클릭 로그. referralCode는 users.referred_by_code가 참조하는 추천 코드다.
 */
@Getter
@Entity
@Table(name = "share_clicks")
public class ShareClick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "share_click_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "channel", nullable = false, length = 30)
    private String channel;

    @Column(name = "referral_code", nullable = false, unique = true, length = 20)
    private String referralCode;

    @CreationTimestamp
    @Column(name = "clicked_at", nullable = false, updatable = false)
    private LocalDateTime clickedAt;

    protected ShareClick() {
    }

    public ShareClick(User user, String channel, String referralCode) {
        this.user = user;
        this.channel = channel;
        this.referralCode = referralCode;
    }

}
