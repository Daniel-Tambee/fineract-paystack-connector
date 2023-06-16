package com.paystackfineract.connector.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConnectorExceptions.PaymentNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(ConnectorExceptions.PaymentNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConnectorExceptions.InvalidWebhookSignatureException.class)
    public ResponseEntity<Object> handleBadSignature(ConnectorExceptions.InvalidWebhookSignatureException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(ConnectorExceptions.AmountMismatchException.class)
    public ResponseEntity<Object> handleAmountMismatch(ConnectorExceptions.AmountMismatchException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ConnectorExceptions.PaystackApiException.class)
    public ResponseEntity<Object> handlePaystack(ConnectorExceptions.PaystackApiException ex) {
        return build(HttpStatus.BAD_GATEWAY, "Paystack error: " + ex.getMessage());
    }

    @ExceptionHandler(ConnectorExceptions.FineractApiException.class)
    public ResponseEntity<Object> handleFineract(ConnectorExceptions.FineractApiException ex) {
        return build(HttpStatus.BAD_GATEWAY, "Fineract error: " + ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneric(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: " + ex.getMessage());
    }

    private ResponseEntity<Object> build(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
