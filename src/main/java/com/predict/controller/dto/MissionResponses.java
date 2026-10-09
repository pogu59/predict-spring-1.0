package com.predict.controller.dto;

import com.predict.Mission;
import com.predict.MissionQuestion;
import com.predict.MissionSubmission;
import com.predict.enums.MissionStatus;
import com.predict.enums.MissionType;
import com.predict.enums.SubmissionStatus;
import com.predict.mission.QuestionResult;
import com.predict.mission.SubmitResult;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 미션 응답 DTO 모음. 참여자용 응답에는 확인 문항의 정답(attentionAnswerIndex)과 참여 인원을
 * 절대 넣지 않는다 — 관리자 목록(AdminListItem)에만 통과/반려 수가 있다.
 */
public final class MissionResponses {

    private MissionResponses() {
    }

    /** myStatus: 내 제출 상태(없으면 null). 출석은 오늘 제출만 본다. */
    public record ListItem(Long id, MissionType type, String title, String description, int rewardPoints,
                           boolean daily, LocalDateTime endsAt, int questionCount,
                           SubmissionStatus myStatus, String myRejectReason) {

        public static ListItem of(Mission mission, MissionSubmission mine) {
            return new ListItem(mission.getId(), mission.getType(), mission.getTitle(), mission.getDescription(),
                    mission.getRewardPoints(), mission.isDaily(), mission.getEndsAt(), mission.getQuestions().size(),
                    mine == null ? null : mine.getStatus(), mine == null ? null : mine.getRejectReason());
        }
    }

    public record Question(Long id, int sortOrder, String text, List<String> options) {

        public static Question from(MissionQuestion question) {
            return new Question(question.getId(), question.getSortOrder(), question.getText(), question.getOptions());
        }
    }

    /** available: 지금 참여할 수 있는지(공개 기간·상태 기준, 내 참여 여부와는 별개). */
    public record Detail(Long id, MissionType type, String title, String description, int rewardPoints,
                         boolean daily, LocalDateTime endsAt, boolean available, List<Question> questions,
                         SubmissionStatus myStatus, String myRejectReason) {

        public static Detail of(Mission mission, MissionSubmission mine, LocalDateTime now) {
            return new Detail(mission.getId(), mission.getType(), mission.getTitle(), mission.getDescription(),
                    mission.getRewardPoints(), mission.isDaily(), mission.getEndsAt(), mission.isAvailableAt(now),
                    mission.getQuestions().stream().map(Question::from).toList(),
                    mine == null ? null : mine.getStatus(), mine == null ? null : mine.getRejectReason());
        }
    }

    /** balance: 처리 후 리워드 포인트 잔액. bonusPoints: 오늘의 미션 모두 완료 보너스(없으면 0). */
    public record SubmitResponse(Long submissionId, SubmissionStatus status, String rejectReason,
                                 int earnedPoints, int bonusPoints, int balance) {

        public static SubmitResponse from(SubmitResult result) {
            MissionSubmission submission = result.submission();
            return new SubmitResponse(submission.getId(), submission.getStatus(), submission.getRejectReason(),
                    result.earnedPoints(), result.bonusPoints(), result.balance());
        }
    }

    /** percents: 보기 순서대로 0~100 정수, 합 100(응답 0건이면 모두 0). */
    public record QuestionResultResponse(Long questionId, String text, List<String> options,
                                         List<Integer> percents, Integer myAnswer) {

        public static QuestionResultResponse from(QuestionResult result) {
            MissionQuestion question = result.question();
            return new QuestionResultResponse(question.getId(), question.getText(), question.getOptions(),
                    result.percents(), result.myAnswer());
        }
    }

    public record ResultsResponse(Long missionId, String title, List<QuestionResultResponse> questions) {
    }

    public record AdminListItem(Long id, MissionType type, String title, int rewardPoints, boolean daily,
                                MissionStatus status, LocalDateTime startsAt, LocalDateTime endsAt,
                                int questionCount, long approvedCount, long rejectedCount) {

        public static AdminListItem of(Mission mission, long approvedCount, long rejectedCount) {
            return new AdminListItem(mission.getId(), mission.getType(), mission.getTitle(), mission.getRewardPoints(),
                    mission.isDaily(), mission.getStatus(), mission.getStartsAt(), mission.getEndsAt(),
                    mission.getQuestions().size(), approvedCount, rejectedCount);
        }
    }
}
