package com.predict;

import com.predict.enums.Tier;
import com.predict.enums.TierChangeReason;
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
 * 티어 변경 이력. snapshot은 activity_* 사유일 때만 채워진다.
 */
@Getter
@Entity
@Table(name = "tier_changes")
public class TierChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tier_change_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "previous_tier", nullable = false, length = 20)
    private Tier previousTier;

    @Column(name = "new_tier", nullable = false, length = 20)
    private Tier newTier;

    @Column(name = "reason", nullable = false, length = 30)
    private TierChangeReason reason;

    /** activity_demotion/activity_restoration 사유일 때만 채워진다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id")
    private WeeklyActivitySnapshot snapshot;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    protected TierChange() {
    }

    public TierChange(User user, Tier previousTier, Tier newTier,
                       TierChangeReason reason, WeeklyActivitySnapshot snapshot) {
        this.user = user;
        this.previousTier = previousTier;
        this.newTier = newTier;
        this.reason = reason;
        this.snapshot = snapshot;
    }

}
