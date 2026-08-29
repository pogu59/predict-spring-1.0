package com.predict.enums;

/**
 * votes.choice, topics.correct_answer, score_settlements.choice.
 * MySQL ENUM('yes','no')과 1:1 대응.
 */
public enum Choice {
    YES("yes"),
    NO("no");

    private final String dbValue;

    Choice(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static Choice fromDbValue(String dbValue) {
        for (Choice choice : values()) {
            if (choice.dbValue.equals(dbValue)) {
                return choice;
            }
        }
        throw new IllegalArgumentException("알 수 없는 choice 값: " + dbValue);
    }
}
