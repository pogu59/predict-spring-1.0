package com.predict;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RewardWalletTest {

    @Test
    void earn_addsToBalance() {
        RewardWallet wallet = new RewardWallet(1L);

        assertThat(wallet.earn(50)).isEqualTo(50);
        assertThat(wallet.earn(10)).isEqualTo(60);
        assertThat(wallet.getBalance()).isEqualTo(60);
    }

    @Test
    void spend_moreThanBalance_isRejectedAndKeepsBalance() {
        RewardWallet wallet = new RewardWallet(1L);
        wallet.earn(100);

        assertThatThrownBy(() -> wallet.spend(101))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("포인트가 부족해요");
        assertThat(wallet.getBalance()).isEqualTo(100);
    }

    @Test
    void spend_exactBalance_leavesZero() {
        RewardWallet wallet = new RewardWallet(1L);
        wallet.earn(1_000);

        assertThat(wallet.spend(1_000)).isZero();
    }

    @Test
    void nonPositiveAmounts_areRejected() {
        RewardWallet wallet = new RewardWallet(1L);

        assertThatThrownBy(() -> wallet.earn(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> wallet.spend(-5)).isInstanceOf(IllegalArgumentException.class);
    }
}
