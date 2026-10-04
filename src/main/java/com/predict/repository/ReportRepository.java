package com.predict.repository;

import com.predict.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByTargetTypeAndTargetIdAndReporterId(Report.TargetType targetType, Long targetId, Long reporterId);

    List<Report> findByTargetTypeAndTargetIdAndStatus(Report.TargetType targetType, Long targetId, Report.Status status);

    List<Report> findByStatusOrderByCreatedAtDesc(Report.Status status);

    List<Report> findByStatusInOrderByCreatedAtDesc(Collection<Report.Status> statuses);

    /** 관리자 커뮤니티 목록의 대상별 누적 신고 수 — [targetId, count] 행. */
    @Query("SELECT r.targetId, COUNT(r) FROM Report r WHERE r.targetType = :type AND r.targetId IN :ids GROUP BY r.targetId")
    List<Object[]> countByTargets(@Param("type") Report.TargetType type, @Param("ids") Collection<Long> ids);
}
