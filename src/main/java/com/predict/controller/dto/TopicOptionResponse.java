package com.predict.controller.dto;

/** 선택지 1개 + (공개 가능한 시점의) 득표수. voteCount는 아직 집계 전이면 null. */
public record TopicOptionResponse(Long id, String text, Integer voteCount) {
}
