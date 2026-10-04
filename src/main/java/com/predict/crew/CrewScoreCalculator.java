package com.predict.crew;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 크루 대항전 주간 점수 계산(순수 함수). 저장소에 의존하지 않아 단위 테스트로 규칙을 고정한다.
 *
 * <ul>
 *   <li>주: 월요일 00:00 ~ 일요일 23:59 (서버 주간 체크와 같은 기준)</li>
 *   <li>유효 정산: is_reversed=false 인 정산만 센다</li>
 *   <li>정산은 그 시각에 소속된 크루로 간다 — joined_at 이후면 현재 크루, 그 전이면 직전 크루</li>
 *   <li>활성 멤버: 그 주 그 크루로 잡힌 유효 정산이 3건 이상인 크루원</li>
 *   <li>인원당 점수 = 활성 멤버들의 score_delta 합 / 활성 멤버 수. 활성 5명 미만 크루는 순위 없음("집계 중")</li>
 * </ul>
 */
public final class CrewScoreCalculator {

    public static final int MIN_SETTLEMENTS_FOR_ACTIVE = 3;
    public static final int MIN_ACTIVE_MEMBERS_FOR_RANK = 5;

    private CrewScoreCalculator() {
    }

    /** 유저의 크루 소속(현재 + 직전). previous*는 없을 수 있다. */
    public record Membership(Long userId, Long crewId, LocalDateTime joinedAt,
                             Long previousCrewId, LocalDateTime previousJoinedAt) {

        /** 그 시각에 소속된 크루 id. 어느 크루에도 없었으면 null. */
        public Long crewAt(LocalDateTime at) {
            if (!at.isBefore(joinedAt)) {
                return crewId;
            }
            if (previousCrewId != null && (previousJoinedAt == null || !at.isBefore(previousJoinedAt))) {
                return previousCrewId;
            }
            return null;
        }
    }

    public record Settlement(Long userId, LocalDateTime settledAt, int scoreDelta, boolean reversed) {
    }

    /** 크루 한 곳의 주간 결과. rank가 null이면 집계 중. */
    public record CrewWeek(Long crewId, int activeMembers, int scoreSum, BigDecimal scorePerMember, Integer rank) {
    }

    /** 크루 안에서 유저 한 명의 주간 기여(정산 수, 점수 합). */
    public record MemberWeek(Long crewId, Long userId, int settlements, int scoreGain) {
        public boolean active() {
            return settlements >= MIN_SETTLEMENTS_FOR_ACTIVE;
        }
    }

    public static LocalDate weekStartOf(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** [weekStart 00:00, 다음 주 월요일 00:00) 안의 유효 정산을 (크루, 유저)별로 모은다. */
    public static List<MemberWeek> memberWeeks(LocalDate weekStart, Collection<Membership> memberships,
                                               Collection<Settlement> settlements) {
        LocalDateTime from = weekStart.atStartOfDay();
        LocalDateTime to = weekStart.plusDays(7).atStartOfDay();
        Map<Long, Membership> byUser = new HashMap<>();
        memberships.forEach(m -> byUser.put(m.userId(), m));

        Map<List<Long>, int[]> acc = new HashMap<>();
        for (Settlement s : settlements) {
            if (s.reversed() || s.settledAt().isBefore(from) || !s.settledAt().isBefore(to)) {
                continue;
            }
            Membership m = byUser.get(s.userId());
            Long crewId = m == null ? null : m.crewAt(s.settledAt());
            if (crewId == null) {
                continue;
            }
            int[] counts = acc.computeIfAbsent(List.of(crewId, s.userId()), k -> new int[2]);
            counts[0]++;
            counts[1] += s.scoreDelta();
        }
        List<MemberWeek> result = new ArrayList<>();
        acc.forEach((key, v) -> result.add(new MemberWeek(key.get(0), key.get(1), v[0], v[1])));
        return result;
    }

    /**
     * 크루별 주간 결과. crewIds의 모든 크루가 결과에 들어간다(활동 없는 크루는 0명·0점·집계 중).
     * 순위: 인원당 점수 내림차순 → 활성 인원 많은 순 → crewId 오름차순.
     */
    public static List<CrewWeek> rank(Collection<Long> crewIds, List<MemberWeek> memberWeeks) {
        Map<Long, int[]> byCrew = new HashMap<>();
        crewIds.forEach(id -> byCrew.put(id, new int[2]));
        for (MemberWeek mw : memberWeeks) {
            if (!mw.active()) {
                continue;
            }
            int[] v = byCrew.computeIfAbsent(mw.crewId(), k -> new int[2]);
            v[0]++;
            v[1] += mw.scoreGain();
        }

        List<CrewWeek> unranked = new ArrayList<>();
        byCrew.forEach((crewId, v) -> unranked.add(new CrewWeek(crewId, v[0], v[1], perMember(v[1], v[0]), null)));

        Comparator<CrewWeek> order = Comparator.comparing(CrewWeek::scorePerMember).reversed()
                .thenComparing(Comparator.comparingInt(CrewWeek::activeMembers).reversed())
                .thenComparing(CrewWeek::crewId);
        List<CrewWeek> eligible = unranked.stream()
                .filter(c -> c.activeMembers() >= MIN_ACTIVE_MEMBERS_FOR_RANK)
                .sorted(order)
                .toList();
        List<CrewWeek> pending = unranked.stream()
                .filter(c -> c.activeMembers() < MIN_ACTIVE_MEMBERS_FOR_RANK)
                .sorted(order)
                .toList();

        List<CrewWeek> result = new ArrayList<>();
        for (int i = 0; i < eligible.size(); i++) {
            CrewWeek c = eligible.get(i);
            result.add(new CrewWeek(c.crewId(), c.activeMembers(), c.scoreSum(), c.scorePerMember(), i + 1));
        }
        result.addAll(pending);
        return result;
    }

    static BigDecimal perMember(int scoreSum, int activeMembers) {
        if (activeMembers == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(scoreSum).divide(BigDecimal.valueOf(activeMembers), 2, RoundingMode.HALF_UP);
    }
}
