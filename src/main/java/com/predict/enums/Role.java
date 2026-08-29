package com.predict.enums;

/**
 * users.role. 관리자 페이지 접근 권한 체크에 사용된다.
 * MySQL ENUM('user','admin')과 1:1 대응.
 */
public enum Role {
    USER("user"),
    ADMIN("admin");

    private final String dbValue;

    Role(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static Role fromDbValue(String dbValue) {
        for (Role role : values()) {
            if (role.dbValue.equals(dbValue)) {
                return role;
            }
        }
        throw new IllegalArgumentException("알 수 없는 role 값: " + dbValue);
    }
}
