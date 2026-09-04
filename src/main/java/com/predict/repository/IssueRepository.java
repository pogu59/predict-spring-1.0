package com.predict.repository;

import com.predict.Issue;
import com.predict.enums.IssueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    /** 마감시각이 지났는데 아직 열려있는 주제 조회(자동 마감 배치용). */
    List<Issue> findByStatusAndVoteDeadlineAtLessThanEqual(IssueStatus status, LocalDateTime deadline);

    /** 관리자 페이지 주제 목록 — 카테고리/상태/제목 검색어 필터 + 페이지네이션(schema_8.sql 참고쿼리 ⑦). */
    @Query("""
            SELECT t FROM Issue t
            WHERE (:categoryId IS NULL OR t.category.id = :categoryId)
              AND (:status IS NULL OR t.status = :status)
              AND (:keyword IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY t.createdAt DESC
            """)
    Page<Issue> search(@Param("categoryId") Integer categoryId,
                        @Param("status") IssueStatus status,
                        @Param("keyword") String keyword,
                        Pageable pageable);
}
