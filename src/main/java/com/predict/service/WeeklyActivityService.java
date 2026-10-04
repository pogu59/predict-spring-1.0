package com.predict.service;

import com.predict.TierChange;
import com.predict.User;
import com.predict.WeeklyActivitySnapshot;
import com.predict.enums.Tier;
import com.predict.enums.TierChangeReason;
import com.predict.repository.TierChangeRepository;
import com.predict.repository.UserRepository;
import com.predict.repository.VoteRepository;
import com.predict.repository.WeeklyActivitySnapshotRepository;
import com.predict.tier.TierPolicy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 다이아/마스터 전용 주간 활동성 오버레이 (docs/predict.md 3-2절 ②, 3-3절).
 * 점수 기반 자연 승급/강등(SettlementService)과는 별개의 축으로, 참여 횟수만 본다.
 */
@Service
public class WeeklyActivityService {

    private static final int MIN_WEEKLY_VOTES = 5;
    /** 다이아 하한. 이 점수 미만이면 애초에 활동성 체크 대상이 아니다. */
    private static final int ACTIVITY_CHECK_THRESHOLD_SCORE = TierPolicy.DIAMOND_MIN_SCORE;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final VoteRepository voteRepository;
    private final WeeklyActivitySnapshotRepository snapshotRepository;
    private final TierChangeRepository tierChangeRepository;

    public WeeklyActivityService(UserRepository userRepository, VoteRepository voteRepository,
                                  WeeklyActivitySnapshotRepository snapshotRepository,
                                  TierChangeRepository tierChangeRepository) {
        this.userRepository = userRepository;
        this.voteRepository = voteRepository;
        this.snapshotRepository = snapshotRepository;
        this.tierChangeRepository = tierChangeRepository;
    }

    /** 매주 일요일 자정(KST) = 다음 월요일 00:00 에 방금 끝난 주(월~일)를 체크한다. */
    @Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Seoul")
    @Transactional
    public void checkWeeklyActivity() {
        LocalDate weekEnd = LocalDate.now(KST).minusDays(1);
        LocalDate weekStart = weekEnd.minusDays(6);
        checkWeeklyActivity(weekStart, weekEnd);
    }

    /** 테스트 및 수동 재실행을 위해 대상 주를 직접 지정하는 오버로드. */
    @Transactional
    void checkWeeklyActivity(LocalDate weekStart, LocalDate weekEnd) {
        LocalDateTime from = weekStart.atStartOfDay();
        LocalDateTime to = weekEnd.plusDays(1).atStartOfDay();

        for (User user : userRepository.findByCredibilityScoreGreaterThanEqual(ACTIVITY_CHECK_THRESHOLD_SCORE)) {
            long voteCount = voteRepository.countByUserIdAndVotedAtGreaterThanEqualAndVotedAtLessThan(
                    user.getId(), from, to);
            boolean metRequirement = voteCount >= MIN_WEEKLY_VOTES;

            Tier pointTier = TierPolicy.fromScore(user.getCredibilityScore());
            Tier tierBefore = user.getTier();
            Tier tierAfter = metRequirement ? pointTier : TierPolicy.oneStepDown(pointTier);

            WeeklyActivitySnapshot snapshot = snapshotRepository.save(new WeeklyActivitySnapshot(
                    user, weekStart, weekEnd, (int) voteCount, metRequirement, tierBefore, tierAfter));

            user.setActivitySuppressed(!metRequirement);

            if (tierAfter != tierBefore) {
                TierChangeReason reason = metRequirement
                        ? TierChangeReason.ACTIVITY_RESTORATION
                        : TierChangeReason.ACTIVITY_DEMOTION;
                tierChangeRepository.save(new TierChange(user, tierBefore, tierAfter, reason, snapshot));
                user.changeTier(tierAfter);
            }
        }
    }
}
