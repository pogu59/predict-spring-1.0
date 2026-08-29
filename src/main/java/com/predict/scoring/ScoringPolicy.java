package com.predict.scoring;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 신용도 점수 계산 (docs/predict.md 2-2/2-3절).
 * 가상 시딩(Yes 5 / No 5)을 깔아 둔 득표비율 p를 기준으로 정답/오답 시 점수 변동을 계산한다.
 */
public final class ScoringPolicy {

    private static final int VIRTUAL_VOTES_PER_SIDE = 5;
    private static final int VIRTUAL_VOTES_TOTAL = 10;
    private static final double POINT_SCALE = 40.0;
    private static final int P_SCALE = 4;

    private ScoringPolicy() {
    }

    /** p = (내가 고른 쪽 실제 득표 + 5) / (전체 실제 득표 + 10) */
    public static BigDecimal computeP(int chosenSideVotes, int totalVotes) {
        double p = (chosenSideVotes + VIRTUAL_VOTES_PER_SIDE) / (double) (totalVotes + VIRTUAL_VOTES_TOTAL);
        return BigDecimal.valueOf(p).setScale(P_SCALE, RoundingMode.HALF_UP);
    }

    /** bonus(p) = 2 x [1 - 4x(p-0.5)^2] */
    public static double bonus(BigDecimal p) {
        double pd = p.doubleValue();
        return 2 * (1 - 4 * Math.pow(pd - 0.5, 2));
    }

    /** 정답 시: round(40x(1-p) + bonus(p)) */
    public static int correctScore(BigDecimal p) {
        double pd = p.doubleValue();
        return (int) Math.round(POINT_SCALE * (1 - pd) + bonus(p));
    }

    /** 오답 시: round(-(40xp - bonus(p))) */
    public static int incorrectScore(BigDecimal p) {
        double pd = p.doubleValue();
        return (int) Math.round(-(POINT_SCALE * pd - bonus(p)));
    }
}
