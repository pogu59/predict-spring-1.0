package com.predict.repository;

import com.predict.ShareClick;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShareClickRepository extends JpaRepository<ShareClick, Long> {

    boolean existsByReferralCode(String referralCode);

    Optional<ShareClick> findByReferralCode(String referralCode);
}
