package com.predict.repository;

import com.predict.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    /** 게시판 목록 — 제목 검색어 필터 + 최신순 페이지네이션. */
    @Query("""
            SELECT p FROM Post p
            WHERE p.deleted = false
              AND (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY p.createdAt DESC
            """)
    Page<Post> search(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 게시판 목록 후보 — 삭제·숨김 제외, 검색어(제목/본문/닉네임) + 작성자 필터. 인기순 정렬(좋아요 + 댓글x3)과
     * "이 사용자의 글 숨기기" 필터를 서비스에서 적용한 뒤 페이지를 자르므로 Page가 아니라 List로 받는다.
     * 지인 베타 규모라 전체를 메모리에 올려도 충분하다 — 글이 많아지면 집계 컬럼을 두고 DB 정렬로 바꾼다.
     */
    @Query("""
            SELECT p FROM Post p JOIN FETCH p.author a
            WHERE p.deleted = false AND p.hidden = false
              AND (:authorId IS NULL OR a.id = :authorId)
              AND (:keyword IS NULL
                   OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(a.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY p.createdAt DESC
            """)
    List<Post> findVisible(@Param("keyword") String keyword, @Param("authorId") Long authorId);

    /** 관리자 커뮤니티 관리 — 숨김 포함, 삭제 제외. */
    List<Post> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);
}
