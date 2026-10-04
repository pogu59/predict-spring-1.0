package com.predict.enums;

/**
 * 게시글 말머리. posts.topic 문자열과 1:1 대응. 말머리가 없던 시절 글은 null.
 */
public enum PostTopic {
    INFO("info"),
    ANALYSIS("analysis"),
    QUESTION("question"),
    CHAT("chat");

    private final String dbValue;

    PostTopic(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static PostTopic fromDbValue(String dbValue) {
        for (PostTopic topic : values()) {
            if (topic.dbValue.equals(dbValue)) {
                return topic;
            }
        }
        throw new IllegalArgumentException("알 수 없는 말머리 값: " + dbValue);
    }
}
