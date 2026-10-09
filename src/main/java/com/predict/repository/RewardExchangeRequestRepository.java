package com.predict.repository;

import com.predict.RewardExchangeRequest;
import com.predict.enums.ExchangeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardExchangeRequestRepository extends JpaRepository<RewardExchangeRequest, Long> {

    /** 지갑 화면 — 내 교환 신청(최신순). */
    List<RewardExchangeRequest> findByUserIdOrderByIdDesc(Long userId);

    /** 관리자 승인 큐 — 오래 기다린 신청부터. */
    List<RewardExchangeRequest> findByStatusOrderByIdAsc(ExchangeStatus status);

    List<RewardExchangeRequest> findAllByOrderByIdDesc();
}
