package com.predict.repository;

import com.predict.Mission;
import com.predict.enums.MissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    /** 지금 참여할 수 있는 미션 — 오늘의 미션을 앞에, 그다음 만든 순서. */
    @Query("SELECT m FROM Mission m WHERE m.status = :status AND m.startsAt <= :now AND m.endsAt > :now "
            + "ORDER BY m.daily DESC, m.id ASC")
    List<Mission> findAvailable(@Param("status") MissionStatus status, @Param("now") LocalDateTime now);

    /** 관리자 미션 목록(최신순). */
    List<Mission> findAllByOrderByIdDesc();
}
