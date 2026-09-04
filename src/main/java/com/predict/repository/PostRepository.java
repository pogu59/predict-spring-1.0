package com.predict.repository;

import com.predict.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    /** 게시판 목록 — 제목 검색어 필터 + 최신순 페이지네이션. */
    @Query("""
            SELECT p FROM Post p
            WHERE p.deleted = false
              AND (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY p.createdAt DESC
            """)
    Page<Post> search(@Param("keyword") String keyword, Pageable pageable);
}
