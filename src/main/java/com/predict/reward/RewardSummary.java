package com.predict.reward;

import com.predict.RewardExchangeRequest;
import com.predict.RewardTransaction;

import java.util.List;

/**
 * 지갑 화면 한 번에 필요한 값. pendingExchangePoints는 확인 중(requested)인 교환에 묶인 포인트,
 * monthEarned는 이번 달 미션 적립 + 보너스 합계.
 */
public record RewardSummary(int balance, int pendingExchangePoints, int monthEarned,
                            List<RewardTransaction> transactions, List<RewardExchangeRequest> exchanges) {
}
