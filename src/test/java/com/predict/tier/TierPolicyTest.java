package com.predict.tier;

import com.predict.enums.Tier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TierPolicyTest {

    @Test
    void fromScore_matchesDocBoundaries() {
        assertThat(TierPolicy.fromScore(0)).isEqualTo(Tier.UNRANKED);
        assertThat(TierPolicy.fromScore(1)).isEqualTo(Tier.BRONZE);
        assertThat(TierPolicy.fromScore(99)).isEqualTo(Tier.BRONZE);
        assertThat(TierPolicy.fromScore(100)).isEqualTo(Tier.SILVER);
        assertThat(TierPolicy.fromScore(199)).isEqualTo(Tier.SILVER);
        assertThat(TierPolicy.fromScore(200)).isEqualTo(Tier.GOLD);
        assertThat(TierPolicy.fromScore(299)).isEqualTo(Tier.GOLD);
        assertThat(TierPolicy.fromScore(300)).isEqualTo(Tier.PLATINUM);
        assertThat(TierPolicy.fromScore(399)).isEqualTo(Tier.PLATINUM);
        assertThat(TierPolicy.fromScore(400)).isEqualTo(Tier.DIAMOND);
        assertThat(TierPolicy.fromScore(499)).isEqualTo(Tier.DIAMOND);
        assertThat(TierPolicy.fromScore(500)).isEqualTo(Tier.MASTER);
        assertThat(TierPolicy.fromScore(999_999)).isEqualTo(Tier.MASTER);
    }

    @Test
    void fromScore_withActivitySuppressed_capsDiamondAndMasterAtPlatinum() {
        assertThat(TierPolicy.fromScore(450, true)).isEqualTo(Tier.PLATINUM);
        assertThat(TierPolicy.fromScore(600, true)).isEqualTo(Tier.PLATINUM);
        assertThat(TierPolicy.fromScore(250, true)).isEqualTo(Tier.GOLD); // 캡 대상 아닌 구간은 그대로
        assertThat(TierPolicy.fromScore(450, false)).isEqualTo(Tier.DIAMOND);
    }

    @Test
    void oneStepDown_onlyAppliesToDiamondAndMaster() {
        assertThat(TierPolicy.oneStepDown(Tier.MASTER)).isEqualTo(Tier.DIAMOND);
        assertThat(TierPolicy.oneStepDown(Tier.DIAMOND)).isEqualTo(Tier.PLATINUM);
        assertThrows(IllegalArgumentException.class, () -> TierPolicy.oneStepDown(Tier.GOLD));
    }
}
