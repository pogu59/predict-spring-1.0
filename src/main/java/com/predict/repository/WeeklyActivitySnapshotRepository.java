package com.predict.repository;

import com.predict.WeeklyActivitySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyActivitySnapshotRepository extends JpaRepository<WeeklyActivitySnapshot, Long> {
}
