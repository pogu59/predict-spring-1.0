package com.predict.enums;

/**
 * reward_exchange_requests.status. requested=확인 대기(포인트는 이미 차감됨),
 * sent=운영자가 기프티콘을 보냄, rejected=운영자 반려(포인트 환불), canceled=신청자가 취소(포인트 환불).
 * requested에서만 다른 상태로 바뀔 수 있다.
 */
public enum ExchangeStatus {
    REQUESTED("requested"),
    SENT("sent"),
    REJECTED("rejected"),
    CANCELED("canceled");

    private final String dbValue;

    ExchangeStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static ExchangeStatus fromDbValue(String dbValue) {
        for (ExchangeStatus status : values()) {
            if (status.dbValue.equals(dbValue)) {
                return status;
            }
        }
        throw new IllegalArgumentException("알 수 없는 exchange status 값: " + dbValue);
    }
}
