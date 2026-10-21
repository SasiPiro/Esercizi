package com.ecommerce.monolith.shared.internal;

import com.ecommerce.monolith.shared.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
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

import static com.ecommerce.monolith.shared.AppConstants.*;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Value("${spring.application.name:ecommerce-monolith}")
    private String serviceName;

    // --- 1. BUSINESS RULES ---
    // Every module's exceptions extend BusinessException and carry status, title and type,
    // so this handler does not depend on the modules.
    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex) {
        return createProblemDetail(ex, ex.getStatus(), ex.getTitle(), ex.getType());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return createProblemDetail(ex, HttpStatus.BAD_REQUEST, "Invalid request", TYPE_VALIDATION_ERROR);
    }

    // --- SECURITY ---
    // Without these handlers, exceptions thrown inside controllers/services (e.g. the
    // AuthenticationManager in AuthService.login, or @PreAuthorize) fall into the
    // catch-all below and become HTTP 500.

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        // Already logged with the attempted username by AuthService (login) or not
        // interesting (expired/invalid guest token): no second line here.
        // Generic message on purpose: do not reveal whether the username exists
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ERROR_INVALID_CREDENTIALS);
        problemDetail.setTitle("Unauthorized");
        problemDetail.setType(TYPE_UNAUTHORIZED);
        problemDetail.setProperty("service", serviceName);
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return createProblemDetail(ex, HttpStatus.FORBIDDEN, "Forbidden", TYPE_FORBIDDEN);
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
    public ProblemDetail handleAllUncaughtException(Exception ex, HttpServletRequest request) {
        // The only ERROR with stack trace: a real bug or an infrastructure failure
        log.error("[HTTP_SERVER_ERROR] status=500 method={} path={} exception={}",
                request.getMethod(), request.getRequestURI(), ex.getClass().getName(), ex);

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
        // 4xx = the client sent something the business rules reject: expected, not an
        // application error. INFO without stack trace; WARN/ERROR stay meaningful.
        log.info("[HTTP_CLIENT_ERROR] status={} title=\"{}\" detail=\"{}\"", status.value(), title, ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle(title);
        problemDetail.setType(type);
        problemDetail.setProperty("service", serviceName);
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}
