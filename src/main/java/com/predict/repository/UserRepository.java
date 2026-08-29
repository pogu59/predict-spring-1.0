package com.predict.repository;

import com.predict.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    /** 다이아/마스터 점수 구간(400+) 등 특정 구간 이상 유저 조회용. */
    List<User> findByCredibilityScoreGreaterThanEqual(int credibilityScore);
}
