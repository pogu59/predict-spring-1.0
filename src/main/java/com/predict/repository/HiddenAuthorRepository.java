package com.predict.repository;

import com.predict.HiddenAuthor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HiddenAuthorRepository extends JpaRepository<HiddenAuthor, Long> {

    boolean existsByUserIdAndHiddenUserId(Long userId, Long hiddenUserId);

    @Query("SELECT h.hiddenUser.id FROM HiddenAuthor h WHERE h.user.id = :userId")
    List<Long> findHiddenUserIds(@Param("userId") Long userId);
}
