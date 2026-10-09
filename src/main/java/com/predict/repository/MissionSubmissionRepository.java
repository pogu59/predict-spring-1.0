package com.predict.repository;

import com.predict.MissionSubmission;
import com.predict.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MissionSubmissionRepository extends JpaRepository<MissionSubmission, Long> {

    /** 출석 외 미션은 미션당 1회. */
    boolean existsByMissionIdAndUserId(Long missionId, Long userId);

    /** 출석은 하루 1회. */
    boolean existsByMissionIdAndUserIdAndSubmittedOn(Long missionId, Long userId, LocalDate submittedOn);

    /** 오늘의 미션 모두 완료 보너스 판단용 — 오늘 통과한 제출들. */
    List<MissionSubmission> findByUserIdAndSubmittedOnAndStatus(Long userId, LocalDate submittedOn, SubmissionStatus status);

    /** 미션 목록에 "내 상태"를 붙일 때 한 번에 가져온다. */
    List<MissionSubmission> findByUserIdAndMissionIdIn(Long userId, Collection<Long> missionIds);

    /** 결과 비율 계산 — 통과한 제출만 센다. */
    List<MissionSubmission> findByMissionIdAndStatus(Long missionId, SubmissionStatus status);

    Optional<MissionSubmission> findFirstByMissionIdAndUserIdOrderByIdDesc(Long missionId, Long userId);

    /** 관리자 미션 목록의 통과/반려 수. */
    long countByMissionIdAndStatus(Long missionId, SubmissionStatus status);

    /** 관리자 교환 승인 화면의 위험 신호(유저별 통과/반려 수). */
    long countByUserIdAndStatus(Long userId, SubmissionStatus status);
}
