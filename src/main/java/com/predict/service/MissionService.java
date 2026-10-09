package com.predict.service;

import com.predict.Mission;
import com.predict.MissionQuestion;
import com.predict.MissionSubmission;
import com.predict.RewardTransaction;
import com.predict.User;
import com.predict.enums.MissionStatus;
import com.predict.enums.MissionType;
import com.predict.enums.RewardTransactionType;
import com.predict.enums.SubmissionStatus;
import com.predict.mission.QualityPolicy;
import com.predict.mission.QualityResult;
import com.predict.mission.QuestionDraft;
import com.predict.mission.QuestionResult;
import com.predict.mission.SubmitResult;
import com.predict.repository.MissionRepository;
import com.predict.repository.MissionSubmissionRepository;
import com.predict.scoring.VotePercentages;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 미션 참여(제출 → 규칙 검수 → 적립)와 결과 비율, 관리자 미션 생성·공개.
 * 적립·보너스는 RewardService를 거치며, 신용도(예측)와는 연결되지 않는다.
 */
@Service
public class MissionService {

    /** 그날 열린 오늘의 미션(출석 제외)을 모두 끝내면 주는 보너스. */
    public static final int DAILY_BONUS_POINTS = 30;

    private final MissionRepository missionRepository;
    private final MissionSubmissionRepository submissionRepository;
    private final RewardService rewardService;

    public MissionService(MissionRepository missionRepository, MissionSubmissionRepository submissionRepository,
                          RewardService rewardService) {
        this.missionRepository = missionRepository;
        this.submissionRepository = submissionRepository;
        this.rewardService = rewardService;
    }

    @Transactional(readOnly = true)
    public List<Mission> availableMissions(LocalDateTime now) {
        return missionRepository.findAvailable(MissionStatus.OPEN, now);
    }

    @Transactional(readOnly = true)
    public Mission require(Long missionId) {
        return missionRepository.findById(missionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 미션: " + missionId));
    }

    /** 미션별 내 제출. 출석은 오늘 것만 본다(내일이면 다시 할 수 있으므로). */
    @Transactional(readOnly = true)
    public Map<Long, MissionSubmission> mySubmissions(User user, Collection<Mission> missions, LocalDate today) {
        if (user == null || missions.isEmpty()) {
            return Map.of();
        }
        Map<Long, Mission> missionById = missions.stream()
                .collect(Collectors.toMap(Mission::getId, mission -> mission, (a, b) -> a));
        Map<Long, MissionSubmission> result = new HashMap<>();
        for (MissionSubmission submission : submissionRepository.findByUserIdAndMissionIdIn(user.getId(), missionById.keySet())) {
            Mission mission = missionById.get(submission.getMission().getId());
            if (mission == null) {
                continue;
            }
            if (mission.getType() == MissionType.ATTENDANCE && !today.equals(submission.getSubmittedOn())) {
                continue;
            }
            result.merge(mission.getId(), submission, (a, b) -> a.getId() >= b.getId() ? a : b);
        }
        return result;
    }

    /**
     * 제출 → 규칙 검수(QualityPolicy) → 통과하면 같은 트랜잭션에서 적립 → 오늘의 미션을 모두
     * 끝냈으면 보너스. 반려돼도 제출 기록은 남는다(같은 미션 재도전 불가).
     */
    @Transactional
    public SubmitResult submit(User user, Long missionId, List<Integer> answers, long durationMs) {
        CurrentUserService.requireNotSuspended(user);
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        Mission mission = require(missionId);
        if (!mission.isAvailableAt(now)) {
            throw new IllegalStateException("지금은 참여할 수 없는 미션이에요");
        }
        boolean alreadyDone = mission.getType() == MissionType.ATTENDANCE
                ? submissionRepository.existsByMissionIdAndUserIdAndSubmittedOn(missionId, user.getId(), today)
                : submissionRepository.existsByMissionIdAndUserId(missionId, user.getId());
        if (alreadyDone) {
            throw new IllegalStateException("이미 참여한 미션이에요");
        }
        List<Integer> safeAnswers = answers == null ? List.of() : answers;
        validateAnswers(mission, safeAnswers);
        if (durationMs < 0) {
            throw new IllegalArgumentException("참여 시간이 올바르지 않아요");
        }

        QualityResult quality = QualityPolicy.evaluate(mission, safeAnswers, durationMs);
        MissionSubmission submission = submissionRepository.save(
                new MissionSubmission(mission, user, today, safeAnswers, durationMs, quality));

        int earned = 0;
        int bonus = 0;
        if (submission.isApproved() && mission.getRewardPoints() > 0) {
            RewardTransaction earning = rewardService.credit(user, RewardTransactionType.EARN, mission.getRewardPoints(),
                    "earn:submission:" + submission.getId(), submission, null, mission.getTitle());
            earned = earning == null ? 0 : mission.getRewardPoints();
        }
        if (submission.isApproved() && mission.isDaily() && mission.getType() != MissionType.ATTENDANCE) {
            bonus = grantDailyBonusIfComplete(user, now);
        }
        return new SubmitResult(submission, earned, bonus, rewardService.balanceOf(user));
    }

    /**
     * 참여한 사람에게만 문항별 응답 비율을 보여 준다(통과한 제출만 집계, 확인 문항은 뺀다).
     * 참여 인원은 공개하지 않는다.
     */
    @Transactional(readOnly = true)
    public List<QuestionResult> results(User user, Long missionId) {
        Mission mission = require(missionId);
        MissionSubmission mine = submissionRepository.findFirstByMissionIdAndUserIdOrderByIdDesc(missionId, user.getId())
                .orElseThrow(() -> new IllegalStateException("참여한 사람만 결과를 볼 수 있어요"));
        List<MissionSubmission> approved = submissionRepository.findByMissionIdAndStatus(missionId, SubmissionStatus.APPROVED);
        List<MissionQuestion> questions = mission.getQuestions();
        List<QuestionResult> results = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            MissionQuestion question = questions.get(i);
            if (question.isAttentionCheck()) {
                continue;
            }
            int[] counts = new int[question.getOptions().size()];
            for (MissionSubmission submission : approved) {
                List<Integer> picked = submission.getAnswers();
                if (i < picked.size() && picked.get(i) >= 0 && picked.get(i) < counts.length) {
                    counts[picked.get(i)]++;
                }
            }
            List<Integer> percents = Arrays.stream(VotePercentages.ofCounts(counts)).boxed().toList();
            Integer myAnswer = i < mine.getAnswers().size() ? mine.getAnswers().get(i) : null;
            results.add(new QuestionResult(question, percents, myAnswer));
        }
        return results;
    }

    @Transactional
    public Mission create(MissionType type, String title, String description, int rewardPoints, boolean daily,
                          LocalDateTime startsAt, LocalDateTime endsAt, List<QuestionDraft> questions, boolean openNow) {
        Mission mission = new Mission(type, title, description, rewardPoints, daily, startsAt, endsAt);
        for (QuestionDraft draft : questions == null ? List.<QuestionDraft>of() : questions) {
            if (type == MissionType.BALANCE && (draft.options() == null || draft.options().size() != 2)) {
                throw new IllegalArgumentException("밸런스 게임은 보기가 2개예요");
            }
            mission.addQuestion(draft.text(), draft.options(), draft.attentionAnswerIndex());
        }
        Mission saved = missionRepository.save(mission);
        if (openNow) {
            saved.open();
        }
        return saved;
    }

    @Transactional
    public Mission open(Long missionId) {
        Mission mission = require(missionId);
        mission.open();
        return mission;
    }

    @Transactional
    public Mission close(Long missionId) {
        Mission mission = require(missionId);
        mission.close();
        return mission;
    }

    private void validateAnswers(Mission mission, List<Integer> answers) {
        if (mission.getType() == MissionType.ATTENDANCE) {
            if (!answers.isEmpty()) {
                throw new IllegalArgumentException("출석에는 답이 필요 없어요");
            }
            return;
        }
        List<MissionQuestion> questions = mission.getQuestions();
        if (answers.size() != questions.size()) {
            throw new IllegalArgumentException("모든 문항에 답해 주세요");
        }
        for (int i = 0; i < questions.size(); i++) {
            Integer answer = answers.get(i);
            if (answer == null || answer < 0 || answer >= questions.get(i).getOptions().size()) {
                throw new IllegalArgumentException((i + 1) + "번 문항의 답이 올바르지 않아요");
            }
        }
    }

    /** 지금 열린 오늘의 미션(출석 제외)을 오늘 모두 통과했으면 하루 한 번 보너스. */
    private int grantDailyBonusIfComplete(User user, LocalDateTime now) {
        List<Mission> dailies = missionRepository.findAvailable(MissionStatus.OPEN, now).stream()
                .filter(Mission::isDaily)
                .filter(mission -> mission.getType() != MissionType.ATTENDANCE)
                .toList();
        if (dailies.isEmpty()) {
            return 0;
        }
        Set<Long> doneToday = submissionRepository
                .findByUserIdAndSubmittedOnAndStatus(user.getId(), now.toLocalDate(), SubmissionStatus.APPROVED)
                .stream()
                .map(submission -> submission.getMission().getId())
                .collect(Collectors.toSet());
        boolean allDone = dailies.stream().allMatch(mission -> doneToday.contains(mission.getId()));
        if (!allDone) {
            return 0;
        }
        RewardTransaction bonus = rewardService.credit(user, RewardTransactionType.BONUS, DAILY_BONUS_POINTS,
                "bonus:daily:" + user.getId() + ":" + now.toLocalDate(), null, null, "오늘의 미션 모두 완료 보너스");
        return bonus == null ? 0 : DAILY_BONUS_POINTS;
    }
}
