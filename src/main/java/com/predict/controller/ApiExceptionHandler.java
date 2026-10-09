package com.predict.controller;

import com.predict.controller.dto.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * 서비스 레이어의 IllegalArgumentException(존재하지 않는 리소스/잘못된 입력)은 400,
 * IllegalStateException(상태 위반: 이미 투표함, 마감 전/후 상태 위반 등)은 409로 매핑한다.
 * ResponseStatusException(CurrentUserService의 401/403 등)은 이 핸들러가 없으면 Spring의
 * 기본 에러 바디({timestamp,status,error,path})로 나가 메시지가 프론트에 전달되지 않으므로,
 * 여기서도 동일하게 {message} 형태로 통일해 내려준다.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(new ErrorResponse(ex.getReason()));
    }

    /**
     * 같은 요청이 거의 동시에 두 번 들어와 UNIQUE 제약(미션 하루 1회 제출, 원장 idempotency_key 등)에
     * 걸린 경우. 먼저 들어온 요청은 정상 처리됐으므로 409로 알린다.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("이미 처리된 요청이에요"));
    }

    /** 리워드 지갑(@Version)을 동시에 바꾸려다 한쪽이 진 경우 — 다시 시도하면 된다. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("잠시 후 다시 시도해 주세요"));
    }
}
