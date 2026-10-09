package com.predict.repository;

import com.predict.RewardWallet;
import org.springframework.data.jpa.repository.JpaRepository;

/** 키는 user_id(지갑은 유저당 하나). */
public interface RewardWalletRepository extends JpaRepository<RewardWallet, Long> {
}
