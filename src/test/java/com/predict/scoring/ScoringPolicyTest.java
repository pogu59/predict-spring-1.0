package com.predict.scoring;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ScoringPolicyTest {

    @Test
    void computeP_twoOptions_matchesDocExamples() {
        assertThat(ScoringPolicy.computeP(2, 3, 2)).isEqualByComparingTo("0.5385");
        assertThat(ScoringPolicy.computeP(6, 10, 2)).isEqualByComparingTo("0.5500");
        assertThat(ScoringPolicy.computeP(60, 100, 2)).isEqualByComparingTo("0.5909");
        assertThat(ScoringPolicy.computeP(600, 1000, 2)).isEqualByComparingTo("0.5990");
    }

    @Test
    void fiftyFifty_twoOptions_matchesDocTableExactly() {
        BigDecimal p = new BigDecimal("0.5000");

        assertThat(ScoringPolicy.correctScore(p, 2, 100)).isEqualTo(22);
        assertThat(ScoringPolicy.incorrectScore(p, 2, 100)).isEqualTo(-18);
    }

    @Test
    void correctAndIncorrect_twoOptions_areAntisymmetricAroundZero() {
        BigDecimal p = new BigDecimal("0.7000");

        int correct = ScoringPolicy.correctScore(p, 2, 100);
        int incorrect = ScoringPolicy.incorrectScore(p, 2, 100);

        assertThat(correct).isPositive();
        assertThat(incorrect).isNegative();
    }

    @Test
    void score_scalesLinearlyWithStake() {
        BigDecimal p = new BigDecimal("0.5000");

        assertThat(ScoringPolicy.correctScore(p, 2, 50)).isEqualTo(11); // 22의 절반
        assertThat(ScoringPolicy.incorrectScore(p, 2, 50)).isEqualTo(-9); // -18의 절반
    }

    @Test
    void incorrectScore_neverLosesMoreThanFortyPercentOfStake() {
        // p가 1에 가까울수록(극단적 확신에 틀림) 손실이 최대인데, 이때도 스테이크의 40%를 넘지 않는다
        // -> 오답이어도 최소 60%는 항상 남는다(잔액이 음수가 될 수 없는 이유).
        BigDecimal p = new BigDecimal("0.9999");
        int stake = 1000;

        int loss = -ScoringPolicy.incorrectScore(p, 2, stake);

        assertThat(loss).isLessThanOrEqualTo((int) Math.round(stake * 0.4));
    }

    @Test
    void computeP_fourOptions_seedsFiveVotesPerOption() {
        // 4지선다, 전체 실제 득표 0표(가상표만 반영): p = 5 / (0 + 5*4) = 0.25 = 1/N
        assertThat(ScoringPolicy.computeP(0, 0, 4)).isEqualByComparingTo("0.2500");
    }

    @Test
    void bonus_atCenter_isAlwaysMaxRegardlessOfOptionCount() {
        // p가 균등분포 기준점(1/N)과 같으면 선택지 개수와 무관하게 bonus가 항상 최대(2)
        assertThat(ScoringPolicy.bonus(new BigDecimal("0.5000"), 2)).isEqualTo(2.0);
        assertThat(ScoringPolicy.bonus(new BigDecimal("0.3333"), 3)).isCloseTo(2.0, org.assertj.core.data.Offset.offset(0.001));
        assertThat(ScoringPolicy.bonus(new BigDecimal("0.2500"), 4)).isEqualTo(2.0);
    }

    @Test
    void bonus_whenEveryoneChoseThisOption_isZeroRegardlessOfOptionCount() {
        // p=1(전원이 이 선택지를 고름)이면 선택지 개수와 무관하게 bonus가 0
        assertThat(ScoringPolicy.bonus(BigDecimal.ONE, 2)).isEqualTo(0.0);
        assertThat(ScoringPolicy.bonus(BigDecimal.ONE, 5)).isEqualTo(0.0);
    }
}
