package com.predict.repository;

import com.predict.User;
import com.predict.enums.Role;
import com.predict.enums.Tier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /** 다이아/마스터 점수 구간(400+) 등 특정 구간 이상 유저 조회용. */
    List<User> findByCredibilityScoreGreaterThanEqual(int credibilityScore);

    Optional<User> findByKakaoId(String kakaoId);

    /** 관리자 페이지 유저 검색 — 닉네임 부분일치 + role/티어 필터 + 페이지네이션(schema_8.sql 참고쿼리 ⑫). */
    @Query("""
            SELECT u FROM User u
            WHERE (:keyword IS NULL OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:role IS NULL OR u.role = :role)
              AND (:tier IS NULL OR u.tier = :tier)
            ORDER BY u.createdAt DESC
            """)
    Page<User> search(@Param("keyword") String keyword,
                       @Param("role") Role role,
                       @Param("tier") Tier tier,
                       Pageable pageable);
}
