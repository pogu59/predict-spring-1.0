package com.predict.crew;

import com.predict.crew.CrewScoreCalculator.CrewWeek;
import com.predict.crew.CrewScoreCalculator.MemberWeek;
import com.predict.crew.CrewScoreCalculator.Membership;
import com.predict.crew.CrewScoreCalculator.Settlement;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CrewScoreCalculatorTest {

    /** 2026-10-12(월) ~ 2026-10-18(일) */
    private static final LocalDate WEEK = LocalDate.of(2026, 10, 12);
    private static final LocalDateTime LONG_AGO = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final long CREW_A = 1L;
    private static final long CREW_B = 2L;

    private static LocalDateTime day(int dayOfWeek, int hour) {
        return WEEK.plusDays(dayOfWeek).atTime(hour, 0);
    }

    private static Membership member(long userId, long crewId) {
        return new Membership(userId, crewId, LONG_AGO, null, null);
    }

    /** userId가 이번 주 화요일에 delta짜리 정산을 count번 받는다. */
    private static List<Settlement> settled(long userId, int count, int delta) {
        List<Settlement> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new Settlement(userId, day(1, 10 + i), delta, false));
        }
        return list;
    }

    @Test
    void weekStartOf_normalizesToMonday() {
        assertThat(CrewScoreCalculator.weekStartOf(LocalDate.of(2026, 10, 18))).isEqualTo(WEEK); // 일요일
        assertThat(CrewScoreCalculator.weekStartOf(WEEK)).isEqualTo(WEEK);
        assertThat(CrewScoreCalculator.weekStartOf(LocalDate.of(2026, 10, 19))).isEqualTo(WEEK.plusWeeks(1));
    }

    @Test
    void onlyMembersWithThreeSettlementsAreActive() {
        List<Membership> members = List.of(member(1, CREW_A), member(2, CREW_A));
        List<Settlement> settlements = new ArrayList<>();
        settlements.addAll(settled(1, 3, 10)); // 활성: +30
        settlements.addAll(settled(2, 2, 50)); // 2건이라 비활성 → 점수 합에서 빠짐

        List<MemberWeek> weeks = CrewScoreCalculator.memberWeeks(WEEK, members, settlements);
        CrewWeek a = CrewScoreCalculator.rank(List.of(CREW_A), weeks).get(0);

        assertThat(a.activeMembers()).isEqualTo(1);
        assertThat(a.scoreSum()).isEqualTo(30);
        assertThat(a.scorePerMember()).isEqualByComparingTo("30.00");
    }

    @Test
    void crewsWithFewerThanFiveActiveMembersAreUnranked() {
        List<Membership> members = new ArrayList<>();
        List<Settlement> settlements = new ArrayList<>();
        for (long u = 1; u <= 5; u++) { // A: 활성 5명, 인원당 +9
            members.add(member(u, CREW_A));
            settlements.addAll(settled(u, 3, 3)); // +9
        }
        for (long u = 11; u <= 14; u++) { // B: 활성 4명, 인원당 점수가 더 높아도 순위 없음
            members.add(member(u, CREW_B));
            settlements.addAll(settled(u, 3, 100));
        }

        List<CrewWeek> ranked = CrewScoreCalculator.rank(List.of(CREW_A, CREW_B),
                CrewScoreCalculator.memberWeeks(WEEK, members, settlements));

        assertThat(ranked).extracting(CrewWeek::crewId).containsExactly(CREW_A, CREW_B);
        assertThat(ranked.get(0).rank()).isEqualTo(1);
        assertThat(ranked.get(1).rank()).isNull(); // 집계 중
        assertThat(ranked.get(1).activeMembers()).isEqualTo(4);
    }

    @Test
    void rankingIsPerMemberNotTotal() {
        List<Membership> members = new ArrayList<>();
        List<Settlement> settlements = new ArrayList<>();
        for (long u = 1; u <= 10; u++) { // A: 10명 × +3 = 합 30, 인원당 3
            members.add(member(u, CREW_A));
            settlements.addAll(settled(u, 3, 1));
        }
        for (long u = 11; u <= 15; u++) { // B: 5명 × +15 = 합 75, 인원당 15 → 인원은 적어도 B가 이긴다
            members.add(member(u, CREW_B));
            settlements.addAll(settled(u, 3, 5));
        }

        List<CrewWeek> ranked = CrewScoreCalculator.rank(List.of(CREW_A, CREW_B),
                CrewScoreCalculator.memberWeeks(WEEK, members, settlements));

        assertThat(ranked.get(0).crewId()).isEqualTo(CREW_B);
        assertThat(ranked.get(0).scorePerMember()).isEqualByComparingTo("15.00");
        assertThat(ranked.get(1).crewId()).isEqualTo(CREW_A);
        assertThat(ranked.get(1).rank()).isEqualTo(2);
    }

    @Test
    void midWeekTransferSendsEachSettlementToCrewAtThatTime() {
        // 수요일 12시에 A → B로 옮김
        LocalDateTime movedAt = day(2, 12);
        Membership moved = new Membership(1L, CREW_B, movedAt, CREW_A, LONG_AGO);
        List<Settlement> settlements = List.of(
                new Settlement(1L, day(0, 9), 10, false),   // 월: A
                new Settlement(1L, day(1, 9), 10, false),   // 화: A
                new Settlement(1L, day(2, 11), 10, false),  // 수 11시(이적 전): A
                new Settlement(1L, day(3, 9), 7, false),    // 목: B
                new Settlement(1L, day(4, 9), 7, false));   // 금: B

        List<MemberWeek> weeks = CrewScoreCalculator.memberWeeks(WEEK, List.of(moved), settlements);

        assertThat(weeks).containsExactlyInAnyOrder(
                new MemberWeek(CREW_A, 1L, 3, 30),  // A에서는 3건이라 활성
                new MemberWeek(CREW_B, 1L, 2, 14)); // B에서는 2건이라 비활성
    }

    @Test
    void settlementsBeforeFirstJoinCountForNoCrew() {
        Membership joinedWednesday = new Membership(1L, CREW_A, day(2, 0), null, null);
        List<Settlement> settlements = List.of(
                new Settlement(1L, day(0, 9), 10, false), // 가입 전 → 어느 크루에도 안 감
                new Settlement(1L, day(3, 9), 10, false));

        List<MemberWeek> weeks = CrewScoreCalculator.memberWeeks(WEEK, List.of(joinedWednesday), settlements);

        assertThat(weeks).containsExactly(new MemberWeek(CREW_A, 1L, 1, 10));
    }

    @Test
    void reversedAndOutOfWeekSettlementsAreIgnored() {
        List<Settlement> settlements = List.of(
                new Settlement(1L, day(1, 9), 10, false),
                new Settlement(1L, day(1, 10), 99, true),                 // 오확정 정정으로 무효
                new Settlement(1L, WEEK.minusDays(1).atTime(23, 59), 50, false), // 지난주 일요일
                new Settlement(1L, WEEK.plusDays(7).atStartOfDay(), 50, false)); // 다음 주 월요일 00:00

        List<MemberWeek> weeks = CrewScoreCalculator.memberWeeks(WEEK, List.of(member(1, CREW_A)), settlements);

        assertThat(weeks).containsExactly(new MemberWeek(CREW_A, 1L, 1, 10));
    }

    @Test
    void crewWithoutActivityStillAppearsAsUnranked() {
        List<CrewWeek> ranked = CrewScoreCalculator.rank(List.of(CREW_A), List.of());

        assertThat(ranked).containsExactly(new CrewWeek(CREW_A, 0, 0, new BigDecimal("0.00"), null));
    }
}
