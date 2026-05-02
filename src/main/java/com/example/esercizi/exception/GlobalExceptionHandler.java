package com.example.esercizi.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.esercizi.constants.AppConstants.*;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Value("${spring.application.name:esercizi}")
    private String serviceName;

    // --- 1. CUSTOM EXCEPTION HANDLING ---
    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        return createProblemDetail(ex, HttpStatus.NOT_FOUND, ERROR_USER_NOT_FOUND, TYPE_USER_NOT_FOUND);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException ex) {
        return createProblemDetail(ex, HttpStatus.NOT_FOUND, ERROR_PRODUCT_NOT_FOUND, TYPE_PRODUCT_NOT_FOUND);
    }

    @ExceptionHandler(CartEmptyException.class)
    public ProblemDetail handleCartEmpty(CartEmptyException ex) {
        return createProblemDetail(ex, HttpStatus.BAD_REQUEST, ERROR_CART_EMPTY, TYPE_CART_EMPTY);
    }

    @ExceptionHandler(PromotionNotFoundException.class)
    public ProblemDetail handlePromotionNotFound(PromotionNotFoundException ex) {
        return createProblemDetail(ex, HttpStatus.NOT_FOUND, ERROR_PROMOTION_NOT_FOUND, TYPE_PROMOTION_NOT_FOUND);
    }

    @ExceptionHandler(PromotionExpiredException.class)
    public ProblemDetail handlePromotionExpired(PromotionExpiredException ex) {
        return createProblemDetail(ex, HttpStatus.GONE, ERROR_PROMOTION_EXPIRED, TYPE_PROMOTION_EXPIRED);
    }

    @ExceptionHandler(ConcurrentPromotionAccessException.class)
    public ProblemDetail handleConcurrentPromotion(ConcurrentPromotionAccessException ex) {
        return createProblemDetail(ex, HttpStatus.CONFLICT, ERROR_CONCURRENT_PROMOTION, TYPE_CONCURRENT_PROMOTION);
    }

    @ExceptionHandler(CouponNotCumulativeException.class)
    public ProblemDetail handleCouponNotCumulative(CouponNotCumulativeException ex) {
        return createProblemDetail(ex, HttpStatus.CONFLICT, ERROR_COUPON_NOT_CUMULATIVE, TYPE_COUPON_NOT_CUMULATIVE);
    }

    // --- 2. OVERRIDE STANDARD METHOD (DTO VALIDATION) ---
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Map<String, String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Generic Error",
                        (existing, replacement) -> existing));

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problemDetail.setTitle("Invalid input data");
        problemDetail.setType(TYPE_VALIDATION_ERROR);
        problemDetail.setProperty("errors", errors);
        problemDetail.setProperty("service", serviceName);
        problemDetail.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    // --- 3. CATCH-ALL ---
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAllUncaughtException(Exception ex) {
        log.error("UNHANDLED ERROR: ", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Contact support if the problem persists");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(TYPE_GENERIC_ERROR);
        problemDetail.setProperty("service", serviceName);
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    // HELPER METHOD
    private ProblemDetail createProblemDetail(Exception ex, HttpStatus status, String title, URI type) {
        log.warn("{}: {}", title, ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle(title);
        problemDetail.setType(type);
        problemDetail.setProperty("service", serviceName);
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}
