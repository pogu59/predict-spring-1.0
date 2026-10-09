package com.predict.mission;

import com.predict.Mission;
import com.predict.MissionQuestion;
import com.predict.enums.MissionType;

import java.util.List;

/**
 * 응답 품질 점수 v0 (규칙 기반, 기획서 "부정 참여 방지"). 반려 사유는 참여자에게 그대로 보여 준다.
 * <ul>
 *   <li>확인(주의) 문항을 틀리면 반려</li>
 *   <li>문항 수 대비 너무 빨리 끝내면 반려(설문 문항당 1.5초, 밸런스 게임 0.7초 미만)</li>
 *   <li>기준의 2배 안쪽으로 빠르면 -20점, 확인 문항을 뺀 4문항 이상을 전부 같은 번호로 고르면 -40점
 *       (점수만 깎고 반려하지는 않는다 — 데이터를 쓸 때 거르는 용도)</li>
 * </ul>
 * 출석은 문항이 없으므로 항상 통과한다.
 */
public final class QualityPolicy {

    static final long SURVEY_MIN_MS_PER_QUESTION = 1_500;
    static final long BALANCE_MIN_MS_PER_QUESTION = 700;
    static final int FAST_PENALTY = 20;
    static final int STRAIGHT_LINE_PENALTY = 40;
    static final int STRAIGHT_LINE_MIN_QUESTIONS = 4;

    public static final String REASON_ATTENTION = "확인 문항의 답이 달랐어요. 이번 미션은 포인트 없이 끝났어요.";
    public static final String REASON_TOO_FAST = "너무 빨리 답해서 이번 미션은 인정되지 않았어요.";

    private QualityPolicy() {
    }

    /** answers는 MissionService에서 개수·범위 검증을 마친 값이라고 가정한다. */
    public static QualityResult evaluate(Mission mission, List<Integer> answers, long durationMs) {
        if (mission.getType() == MissionType.ATTENDANCE) {
            return QualityResult.approved(100);
        }
        List<MissionQuestion> questions = mission.getQuestions();

        for (int i = 0; i < questions.size(); i++) {
            MissionQuestion question = questions.get(i);
            if (question.isAttentionCheck() && !question.getAttentionAnswerIndex().equals(answers.get(i))) {
                return QualityResult.rejected(REASON_ATTENTION);
            }
        }

        long minMs = minDurationMs(mission.getType(), questions.size());
        if (durationMs < minMs) {
            return QualityResult.rejected(REASON_TOO_FAST);
        }

        int score = 100;
        if (durationMs < minMs * 2) {
            score -= FAST_PENALTY;
        }
        if (isStraightLined(questions, answers)) {
            score -= STRAIGHT_LINE_PENALTY;
        }
        return QualityResult.approved(score);
    }

    static long minDurationMs(MissionType type, int questionCount) {
        long perQuestion = type == MissionType.BALANCE ? BALANCE_MIN_MS_PER_QUESTION : SURVEY_MIN_MS_PER_QUESTION;
        return perQuestion * questionCount;
    }

    private static boolean isStraightLined(List<MissionQuestion> questions, List<Integer> answers) {
        Integer first = null;
        int counted = 0;
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).isAttentionCheck()) {
                continue;
            }
            Integer answer = answers.get(i);
            if (first == null) {
                first = answer;
            } else if (!first.equals(answer)) {
                return false;
            }
            counted++;
        }
        return counted >= STRAIGHT_LINE_MIN_QUESTIONS;
    }
}
