package com.predict.repository;

import com.predict.RewardTransaction;
import com.predict.enums.RewardTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface RewardTransactionRepository extends JpaRepository<RewardTransaction, Long> {

    /** 같은 사건이 이미 기록됐는지(중복 적립 방지). */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /** 지갑 화면의 최근 내역. */
    List<RewardTransaction> findTop30ByUserIdOrderByIdDesc(Long userId);

    /** 기간 내 특정 종류의 합계(예: 이번 달 적립 = earn + bonus). */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM RewardTransaction t "
            + "WHERE t.user.id = :userId AND t.type IN :types AND t.createdAt >= :from")
    long sumAmountSince(@Param("userId") Long userId,
                        @Param("types") Collection<RewardTransactionType> types,
                        @Param("from") LocalDateTime from);
}
