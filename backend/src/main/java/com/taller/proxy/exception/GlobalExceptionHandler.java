package com.taller.proxy.exception;

import com.taller.proxy.dto.Dtos.ErrorBody;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorBody> rateLimit(RateLimitExceededException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getRetryAfterSeconds()))
                .body(new ErrorBody("RATE_LIMIT_EXCEEDED", e.getMessage(), e.getRetryAfterSeconds()));
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ErrorBody> quota(QuotaExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(new ErrorBody("QUOTA_EXCEEDED", e.getMessage(), null));
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorBody> badRequest(Exception e) {
        String msg = e instanceof MethodArgumentNotValidException ? "Solicitud inválida: el prompt es obligatorio (máx. 4000 caracteres)." : e.getMessage();
        return ResponseEntity.badRequest().body(new ErrorBody("BAD_REQUEST", msg, null));
    }
}
