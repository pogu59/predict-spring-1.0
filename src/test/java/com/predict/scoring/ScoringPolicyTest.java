package com.predict.scoring;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ScoringPolicyTest {

    @Test
    void computeP_matchesDocExamples() {
        assertThat(ScoringPolicy.computeP(2, 3)).isEqualByComparingTo("0.5385");
        assertThat(ScoringPolicy.computeP(6, 10)).isEqualByComparingTo("0.5500");
        assertThat(ScoringPolicy.computeP(60, 100)).isEqualByComparingTo("0.5909");
        assertThat(ScoringPolicy.computeP(600, 1000)).isEqualByComparingTo("0.5990");
    }

    @Test
    void fiftyFifty_matchesDocTableExactly() {
        BigDecimal p = new BigDecimal("0.5000");

        assertThat(ScoringPolicy.correctScore(p)).isEqualTo(22);
        assertThat(ScoringPolicy.incorrectScore(p)).isEqualTo(-18);
    }

    @Test
    void correctAndIncorrect_areAntisymmetricAroundZero() {
        BigDecimal p = new BigDecimal("0.7000");

        int correct = ScoringPolicy.correctScore(p);
        int incorrect = ScoringPolicy.incorrectScore(p);

        assertThat(correct).isPositive();
        assertThat(incorrect).isNegative();
    }
}
