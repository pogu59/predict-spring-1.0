package com.predict.service;

import com.predict.Crew;
import com.predict.CrewWeeklyScore;
import com.predict.User;
import com.predict.UserCrew;
import com.predict.controller.dto.CrewDtos.AdminCrewRequest;
import com.predict.controller.dto.CrewDtos.AdminCrewResponse;
import com.predict.controller.dto.CrewDtos.CrewDetailResponse;
import com.predict.controller.dto.CrewDtos.CrewRankingItemResponse;
import com.predict.controller.dto.CrewDtos.CrewResponse;
import com.predict.controller.dto.CrewDtos.CrewTopMemberResponse;
import com.predict.controller.dto.CrewDtos.CrewWeekResponse;
import com.predict.controller.dto.CrewDtos.MyCrewResponse;
import com.predict.crew.CrewScoreCalculator;
import com.predict.crew.CrewScoreCalculator.CrewWeek;
import com.predict.crew.CrewScoreCalculator.MemberWeek;
import com.predict.crew.CrewScoreCalculator.Membership;
import com.predict.crew.CrewScoreCalculator.Settlement;
import com.predict.repository.CrewRepository;
import com.predict.repository.CrewWeeklyScoreRepository;
import com.predict.repository.ScoreSettlementRepository;
import com.predict.repository.UserCrewRepository;
import com.predict.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 크루 대항전 — 크루 목록·가입, 주간 순위(이번 주는 요청 시 실시간 계산, 지난주는 스냅샷), 관리자 크루 관리.
 * 점수 규칙은 CrewScoreCalculator에 모아 두었다.
 */
@Service
public class CrewService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int TOP_MEMBERS = 5;

    private final CrewRepository crewRepository;
    private final UserCrewRepository userCrewRepository;
    private final CrewWeeklyScoreRepository weeklyScoreRepository;
    private final ScoreSettlementRepository scoreSettlementRepository;
    private final UserRepository userRepository;

    public CrewService(CrewRepository crewRepository, UserCrewRepository userCrewRepository,
                       CrewWeeklyScoreRepository weeklyScoreRepository,
                       ScoreSettlementRepository scoreSettlementRepository, UserRepository userRepository) {
        this.crewRepository = crewRepository;
        this.userCrewRepository = userCrewRepository;
        this.weeklyScoreRepository = weeklyScoreRepository;
        this.scoreSettlementRepository = scoreSettlementRepository;
        this.userRepository = userRepository;
    }

    // ---- 조회 ----

    @Transactional(readOnly = true)
    public List<CrewResponse> listActive() {
        Map<Long, Long> counts = memberCounts();
        return crewRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(c -> CrewResponse.from(c, counts.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public CrewDetailResponse detail(Long crewId) {
        Crew crew = requireCrew(crewId);
        LocalDate weekStart = currentWeekStart();
        CrewWeek week = liveWeek(weekStart).stream()
                .filter(w -> w.crewId().equals(crewId))
                .findFirst()
                .orElse(new CrewWeek(crewId, 0, 0, BigDecimal.ZERO, null));
        return new CrewDetailResponse(crew.getId(), crew.getName(), crew.getSlug(), crew.getDescription(),
                userCrewRepository.countByCrewId(crewId),
                new CrewWeekResponse(weekStart, week.rank(), week.scorePerMember(), week.activeMembers()));
    }

    /** week를 생략하면 이번 주. 지난주 이전은 스냅샷이 있으면 그것을, 없으면 실시간 계산을 쓴다. */
    @Transactional(readOnly = true)
    public List<CrewRankingItemResponse> ranking(LocalDate week) {
        LocalDate weekStart = week == null ? currentWeekStart() : CrewScoreCalculator.weekStartOf(week);
        Map<Long, Long> counts = memberCounts();

        if (weekStart.isBefore(currentWeekStart()) && weeklyScoreRepository.existsByWeekStart(weekStart)) {
            return weeklyScoreRepository.findByWeekStart(weekStart).stream()
                    .sorted(Comparator.comparing((CrewWeeklyScore s) -> s.getRank() == null ? Integer.MAX_VALUE : s.getRank())
                            .thenComparing(CrewWeeklyScore::getScorePerMember, Comparator.reverseOrder()))
                    .map(s -> new CrewRankingItemResponse(s.getRank(), s.getCrew().getId(), s.getCrew().getName(),
                            s.getScorePerMember(), s.getActiveMembers(), counts.getOrDefault(s.getCrew().getId(), 0L)))
                    .toList();
        }

        Map<Long, Crew> crews = crewRepository.findByActiveTrueOrderByNameAsc().stream()
                .collect(Collectors.toMap(Crew::getId, Function.identity()));
        return liveWeek(weekStart).stream()
                .filter(w -> crews.containsKey(w.crewId()))
                .map(w -> new CrewRankingItemResponse(w.rank(), w.crewId(), crews.get(w.crewId()).getName(),
                        w.scorePerMember(), w.activeMembers(), counts.getOrDefault(w.crewId(), 0L)))
                .toList();
    }

    /** 그 주 이 크루로 잡힌 점수 증가 상위 5명. */
    @Transactional(readOnly = true)
    public List<CrewTopMemberResponse> topMembers(Long crewId, LocalDate week) {
        requireCrew(crewId);
        LocalDate weekStart = week == null ? currentWeekStart() : CrewScoreCalculator.weekStartOf(week);
        List<MemberWeek> top = memberWeeks(weekStart).stream()
                .filter(mw -> mw.crewId().equals(crewId))
                .sorted(Comparator.comparingInt(MemberWeek::scoreGain).reversed()
                        .thenComparing(MemberWeek::userId))
                .limit(TOP_MEMBERS)
                .toList();
        Map<Long, User> users = userRepository.findAllById(top.stream().map(MemberWeek::userId).toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return top.stream()
                .filter(mw -> users.containsKey(mw.userId()))
                .map(mw -> {
                    User u = users.get(mw.userId());
                    return new CrewTopMemberResponse(u.getNickname(), u.getTier(), mw.scoreGain());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public MyCrewResponse myCrew(User user) {
        return userCrewRepository.findById(user.getId())
                .map(uc -> {
                    MemberWeek mine = memberWeeks(currentWeekStart()).stream()
                            .filter(mw -> mw.userId().equals(user.getId()) && mw.crewId().equals(uc.getCrew().getId()))
                            .findFirst()
                            .orElse(null);
                    return new MyCrewResponse(
                            CrewResponse.from(uc.getCrew(), userCrewRepository.countByCrewId(uc.getCrew().getId())),
                            uc.getJoinedAt(), uc.nextChangeAt(),
                            mine == null ? 0 : mine.scoreGain(), mine == null ? 0 : mine.settlements());
                })
                .orElse(new MyCrewResponse(null, null, null, 0, 0));
    }

    /** 처음 가입은 언제든, 변경은 마지막 가입 후 30일이 지나야 한다(409). */
    @Transactional
    public MyCrewResponse join(User user, Long crewId) {
        Crew crew = requireCrew(crewId);
        if (!crew.isActive()) {
            throw new IllegalArgumentException("지금은 들어갈 수 없는 크루예요");
        }
        LocalDateTime now = LocalDateTime.now();
        UserCrew current = userCrewRepository.findById(user.getId()).orElse(null);
        if (current == null) {
            userCrewRepository.save(new UserCrew(user, crew, now));
        } else if (!current.getCrew().getId().equals(crewId)) {
            if (now.isBefore(current.nextChangeAt())) {
                LocalDateTime next = current.nextChangeAt();
                throw new IllegalStateException(
                        "크루는 " + next.getMonthValue() + "월 " + next.getDayOfMonth() + "일부터 바꿀 수 있어요");
            }
            current.changeTo(crew, now);
        }
        return myCrew(user);
    }

    /** 댓글·게시글 작성자 옆 크루 배지 — userId → 크루 이름(크루 없는 유저는 빠진다). */
    @Transactional(readOnly = true)
    public Map<Long, String> crewNamesFor(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userCrewRepository.findWithCrewByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(UserCrew::getUserId, uc -> uc.getCrew().getName()));
    }

    // ---- 관리자 ----

    @Transactional(readOnly = true)
    public List<AdminCrewResponse> listAll() {
        Map<Long, Long> counts = memberCounts();
        return crewRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(c -> AdminCrewResponse.from(c, counts.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Transactional
    public AdminCrewResponse create(AdminCrewRequest request) {
        String name = request.name().trim();
        if (crewRepository.existsByName(name)) {
            throw new IllegalStateException("이미 있는 크루 이름이에요");
        }
        String slug = resolveSlug(request.slug(), name, null);
        Crew crew = crewRepository.save(new Crew(name, slug, blankToNull(request.description()), request.active()));
        return AdminCrewResponse.from(crew, 0);
    }

    @Transactional
    public AdminCrewResponse update(Long crewId, AdminCrewRequest request) {
        Crew crew = requireCrew(crewId);
        String name = request.name().trim();
        if (crewRepository.existsByNameAndIdNot(name, crewId)) {
            throw new IllegalStateException("이미 있는 크루 이름이에요");
        }
        String slug = resolveSlug(request.slug(), name, crewId);
        crew.update(name, slug, blankToNull(request.description()), request.active());
        return AdminCrewResponse.from(crew, userCrewRepository.countByCrewId(crewId));
    }

    // ---- 주간 배치 ----

    /** 매주 일요일 자정(KST) = 월요일 00:00 에 방금 끝난 주의 스냅샷을 만든다(WeeklyActivityService와 같은 방식). */
    @Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Seoul")
    @Transactional
    public void snapshotLastWeek() {
        snapshotWeek(currentWeekStart().minusWeeks(1));
    }

    /** 이미 스냅샷이 있는 주는 건너뛴다(재실행해도 중복 없음). */
    @Transactional
    public void snapshotWeek(LocalDate weekStart) {
        if (weeklyScoreRepository.existsByWeekStart(weekStart)) {
            return;
        }
        Map<Long, Crew> crews = crewRepository.findAll().stream()
                .collect(Collectors.toMap(Crew::getId, Function.identity()));
        for (CrewWeek w : CrewScoreCalculator.rank(activeCrewIds(), memberWeeks(weekStart))) {
            Crew crew = crews.get(w.crewId());
            if (crew != null) {
                weeklyScoreRepository.save(new CrewWeeklyScore(crew, weekStart, w.activeMembers(), w.scoreSum(),
                        w.scorePerMember(), w.rank()));
            }
        }
    }

    // ---- 내부 ----

    private LocalDate currentWeekStart() {
        return CrewScoreCalculator.weekStartOf(LocalDate.now(KST));
    }

    private List<Long> activeCrewIds() {
        return crewRepository.findByActiveTrueOrderByNameAsc().stream().map(Crew::getId).toList();
    }

    private List<CrewWeek> liveWeek(LocalDate weekStart) {
        return CrewScoreCalculator.rank(activeCrewIds(), memberWeeks(weekStart));
    }

    private List<MemberWeek> memberWeeks(LocalDate weekStart) {
        List<Membership> memberships = userCrewRepository.findAll().stream()
                .map(uc -> new Membership(uc.getUserId(), uc.getCrew().getId(), uc.getJoinedAt(),
                        uc.getPreviousCrew() == null ? null : uc.getPreviousCrew().getId(), uc.getPreviousJoinedAt()))
                .toList();
        List<Settlement> settlements = scoreSettlementRepository
                .findBySettledAtGreaterThanEqualAndSettledAtLessThanAndIsReversedFalse(
                        weekStart.atStartOfDay(), weekStart.plusDays(7).atStartOfDay())
                .stream()
                .map(s -> new Settlement(s.getUser().getId(), s.getSettledAt(), s.getScoreDelta(), s.isReversed()))
                .toList();
        return CrewScoreCalculator.memberWeeks(weekStart, memberships, settlements);
    }

    private Map<Long, Long> memberCounts() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : userCrewRepository.countByCrew()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private Crew requireCrew(Long crewId) {
        return crewRepository.findById(crewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 크루: " + crewId));
    }

    /** slug를 비우면 이름의 영문·숫자로 만들고, 한글뿐이면 임의 값을 쓴다. 겹치면 -2, -3…을 붙인다. */
    private String resolveSlug(String requested, String name, Long selfId) {
        String base = requested == null || requested.isBlank()
                ? name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "")
                : requested.trim();
        if (base.isEmpty()) {
            base = "crew-" + UUID.randomUUID().toString().substring(0, 6);
        }
        base = base.length() > 36 ? base.substring(0, 36) : base;
        String slug = base;
        for (int i = 2; slugTaken(slug, selfId); i++) {
            slug = base + "-" + i;
        }
        return slug;
    }

    private boolean slugTaken(String slug, Long selfId) {
        return selfId == null ? crewRepository.existsBySlug(slug) : crewRepository.existsBySlugAndIdNot(slug, selfId);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
