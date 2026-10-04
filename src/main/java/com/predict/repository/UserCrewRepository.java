package com.predict.repository;

import com.predict.UserCrew;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface UserCrewRepository extends JpaRepository<UserCrew, Long> {

    /** 댓글·게시글 작성자 옆 크루 배지용 — 작성자 id 목록의 현재 크루를 한 번에. */
    @Query("select uc from UserCrew uc join fetch uc.crew where uc.userId in :userIds")
    List<UserCrew> findWithCrewByUserIdIn(Collection<Long> userIds);

    /** 크루별 현재 인원 수 — [crewId, count] */
    @Query("select uc.crew.id, count(uc) from UserCrew uc group by uc.crew.id")
    List<Object[]> countByCrew();

    long countByCrewId(Long crewId);
}
