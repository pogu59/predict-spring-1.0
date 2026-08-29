package com.predict.controller.dto;

import com.predict.enums.Choice;

/** correctAnswer가 null이면 무효(void) 처리로 취급한다. */
public record TopicConfirmRequest(Choice correctAnswer) {
}
