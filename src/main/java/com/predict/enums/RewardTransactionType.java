package com.predict.enums;

/**
 * reward_transactions.type. 금액 부호: earn/bonus/refund/adjust(+)는 적립, exchange는 차감(-).
 * adjust는 관리자 수동 조정용(음수도 가능)으로 자리만 잡아 둔다.
 */
public enum RewardTransactionType {
    EARN("earn"),
    BONUS("bonus"),
    EXCHANGE("exchange"),
    REFUND("refund"),
    ADJUST("adjust");

    private final String dbValue;

    RewardTransactionType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static RewardTransactionType fromDbValue(String dbValue) {
        for (RewardTransactionType type : values()) {
            if (type.dbValue.equals(dbValue)) {
                return type;
            }
        }
        throw new IllegalArgumentException("알 수 없는 reward transaction type 값: " + dbValue);
    }
}
