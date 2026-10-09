package com.predict.mission;

import com.predict.Mission;
import com.predict.enums.MissionType;
import com.predict.enums.SubmissionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QualityPolicyTest {

    private static Mission survey(int plainQuestions, boolean withAttentionCheck) {
        Mission mission = new Mission(MissionType.SURVEY, "설문", null, 50, false,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        for (int i = 0; i < plainQuestions; i++) {
            mission.addQuestion("문항 " + i, List.of("가", "나", "다"), null);
        }
        if (withAttentionCheck) {
            mission.addQuestion("이 문항은 '나'를 골라 주세요", List.of("가", "나", "다"), 1);
        }
        return mission;
    }

    @Test
    void attendance_alwaysPasses() {
        Mission attendance = new Mission(MissionType.ATTENDANCE, "출석", null, 10, false,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));

        QualityResult result = QualityPolicy.evaluate(attendance, List.of(), 0);

        assertThat(result.status()).isEqualTo(SubmissionStatus.APPROVED);
        assertThat(result.score()).isEqualTo(100);
    }

    @Test
    void wrongAttentionAnswer_isRejected() {
        Mission mission = survey(2, true);

        QualityResult result = QualityPolicy.evaluate(mission, List.of(0, 2, 0), 60_000);

        assertThat(result.status()).isEqualTo(SubmissionStatus.REJECTED);
        assertThat(result.rejectReason()).isEqualTo(QualityPolicy.REASON_ATTENTION);
    }

    @Test
    void tooFast_isRejected() {
        Mission mission = survey(4, false);
        long tooFast = QualityPolicy.SURVEY_MIN_MS_PER_QUESTION * 4 - 1;

        QualityResult result = QualityPolicy.evaluate(mission, List.of(0, 1, 2, 0), tooFast);

        assertThat(result.status()).isEqualTo(SubmissionStatus.REJECTED);
        assertThat(result.rejectReason()).isEqualTo(QualityPolicy.REASON_TOO_FAST);
    }

    @Test
    void normalAnswer_passesWithFullScore() {
        Mission mission = survey(3, true);

        QualityResult result = QualityPolicy.evaluate(mission, List.of(0, 2, 1, 1), 60_000);

        assertThat(result.status()).isEqualTo(SubmissionStatus.APPROVED);
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.rejectReason()).isNull();
    }

    @Test
    void fastButAllowed_losesPointsButPasses() {
        Mission mission = survey(2, false);
        long fastish = QualityPolicy.SURVEY_MIN_MS_PER_QUESTION * 2 + 1;

        QualityResult result = QualityPolicy.evaluate(mission, List.of(0, 1), fastish);

        assertThat(result.status()).isEqualTo(SubmissionStatus.APPROVED);
        assertThat(result.score()).isEqualTo(100 - QualityPolicy.FAST_PENALTY);
    }

    @Test
    void straightLining_losesPointsButPasses() {
        Mission mission = survey(4, true);

        QualityResult result = QualityPolicy.evaluate(mission, List.of(2, 2, 2, 2, 1), 60_000);

        assertThat(result.status()).isEqualTo(SubmissionStatus.APPROVED);
        assertThat(result.score()).isEqualTo(100 - QualityPolicy.STRAIGHT_LINE_PENALTY);
    }

    @Test
    void balanceGame_quickTapIsAllowed() {
        Mission balance = new Mission(MissionType.BALANCE, "한식 vs 양식", null, 10, true,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        balance.addQuestion("점심 메뉴는?", List.of("한식", "양식"), null);

        QualityResult result = QualityPolicy.evaluate(balance, List.of(0), 900);

        assertThat(result.status()).isEqualTo(SubmissionStatus.APPROVED);
    }
}
