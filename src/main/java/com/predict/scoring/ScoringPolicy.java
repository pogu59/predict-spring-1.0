package com.predict.scoring;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 신용도 점수 계산 (docs/predict.md 2-2/2-3절, 이지선다 공식을 N개 선택지로 일반화).
 * 가상 시딩(선택지당 5표)을 깔아 둔 득표비율 p를 기준으로 정답/오답 시 점수 변동을 계산한다.
 * optionCount=2일 때는 기존 이지선다 공식과 완전히 동일한 값을 낸다.
 */
public final class ScoringPolicy {

    private static final int VIRTUAL_VOTES_PER_OPTION = 5;
    private static final double POINT_SCALE = 40.0;
    private static final int P_SCALE = 4;

    private ScoringPolicy() {
    }

    /** p = (내가 고른 선택지 실제 득표 + 5) / (전체 실제 득표 + 5 x 선택지 수) */
    public static BigDecimal computeP(int chosenOptionVotes, int totalVotes, int optionCount) {
        double p = (chosenOptionVotes + VIRTUAL_VOTES_PER_OPTION)
                / (double) (totalVotes + VIRTUAL_VOTES_PER_OPTION * optionCount);
        return BigDecimal.valueOf(p).setScale(P_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * bonus(p) = 2 x [1 - k x (p - center)^2].
     * center = 1/optionCount (선택지가 균등하게 나뉘었을 때의 기준점).
     * k = 1 / (1 - center)^2 — p=1(전원이 이 선택지를 고름)에서 bonus가 정확히 0이 되도록 하는 스케일.
     * optionCount=2일 때 center=0.5, k=4로 기존 이지선다 공식과 정확히 일치한다.
     */
    public static double bonus(BigDecimal p, int optionCount) {
        double pd = p.doubleValue();
        double center = 1.0 / optionCount;
        double k = 1.0 / Math.pow(1.0 - center, 2);
        return 2 * (1 - k * Math.pow(pd - center, 2));
    }

    /**
     * 정답 시: round(stake x [40x(1-p) + bonus(p)] / 100).
     * 원래 공식(문서 2-3절)은 "베팅액 100"을 가정한 결과이므로, 베팅액에 비례해 스케일링한다
     * — stake=100이면 기존 공식과 정확히 같은 값이 나온다.
     */
    public static int correctScore(BigDecimal p, int optionCount, int stake) {
        double pd = p.doubleValue();
        double raw = POINT_SCALE * (1 - pd) + bonus(p, optionCount);
        return (int) Math.round(raw * stake / 100.0);
    }

    /**
     * 오답 시: round(-stake x [40xp - bonus(p)] / 100).
     * raw의 최대 크기가 40(포인트 스케일)을 넘지 않도록 설계돼 있어(p<=1, bonus>=0),
     * 이 값의 크기는 항상 stake의 40%를 넘지 않는다 — 오답이어도 베팅액을 전부 잃지는 않는다.
     */
    public static int incorrectScore(BigDecimal p, int optionCount, int stake) {
        double pd = p.doubleValue();
        double raw = -(POINT_SCALE * pd - bonus(p, optionCount));
        return (int) Math.round(raw * stake / 100.0);
    }
}
