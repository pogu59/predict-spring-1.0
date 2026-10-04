package com.predict.repository;

import com.predict.CrewWeeklyScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CrewWeeklyScoreRepository extends JpaRepository<CrewWeeklyScore, Long> {

    List<CrewWeeklyScore> findByWeekStart(LocalDate weekStart);

    boolean existsByWeekStart(LocalDate weekStart);
}
