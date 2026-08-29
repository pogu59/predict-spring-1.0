package com.predict.controller;

import com.predict.controller.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void illegalArgument_mapsTo400() {
        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(new IllegalArgumentException("존재하지 않는 유저: 1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("존재하지 않는 유저: 1");
    }

    @Test
    void illegalState_mapsTo409() {
        ResponseEntity<ErrorResponse> response = handler.handleConflict(new IllegalStateException("이미 투표한 주제입니다."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo("이미 투표한 주제입니다.");
    }
}
