package com.skillstorm.auth_service.Exceptions;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DuplicateEmailException.class)
    public ProblemDetail handleDuplicateEmail(
            DuplicateEmailException exception) {

        return createProblem(
                HttpStatus.CONFLICT,
                "Email already registered",
                exception.getMessage(),
                "DUPLICATE_EMAIL");
    }

    /*
     * This protects against a race condition where two registration
     * requests pass the Java duplicate check at nearly the same time,
     * but the database unique constraint rejects one of them.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(
            DataIntegrityViolationException exception) {

        return createProblem(
                HttpStatus.CONFLICT,
                "Data conflict",
                "The requested operation conflicts with existing data.",
                "DATA_CONFLICT");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(
            BadCredentialsException exception) {
        return createProblem(
                HttpStatus.UNAUTHORIZED,
                "Authentication failed",
                "The email or password is incorrect.",
                "INVALID_CREDENTIALS");
    }

    @ExceptionHandler(DisabledException.class)
    public ProblemDetail handleDisabledAccount(
            DisabledException exception) {

        return createProblem(
                HttpStatus.FORBIDDEN,
                "Account disabled",
                "This account is currently disabled.",
                "ACCOUNT_DISABLED");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationFailure(
            AuthenticationException exception) {

        return createProblem(
                HttpStatus.UNAUTHORIZED,
                "Authentication failed",
                "Authentication could not be completed.",
                "AUTHENTICATION_FAILED");
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ProblemDetail handleUserNotFound(
            UsernameNotFoundException exception) {

        return createProblem(
                HttpStatus.NOT_FOUND,
                "User not found",
                exception.getMessage(),
                "USER_NOT_FOUND");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationFailure(
            MethodArgumentNotValidException exception) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError fieldError
                : exception.getBindingResult().getFieldErrors()) {

            String message = fieldError.getDefaultMessage();

            fieldErrors.putIfAbsent(
                    fieldError.getField(),
                    message == null ? "Invalid value." : message);
        }

        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "One or more request fields are invalid.",
                "VALIDATION_FAILED");

        problem.setProperty("fieldErrors", fieldErrors);

        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(
            IllegalArgumentException exception) {

        return createProblem(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                exception.getMessage(),
                "INVALID_ARGUMENT");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(
            Exception exception) {

        LOGGER.error("Unexpected authentication-service error", exception);

        return createProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "An unexpected error occurred.",
                "INTERNAL_SERVER_ERROR");
    }

    private ProblemDetail createProblem(
            HttpStatus status,
            String title,
            String detail,
            String errorCode) {

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(status, detail);

        problem.setTitle(title);

        problem.setType(
                URI.create(
                        "urn:auth-service:error:"
                                + errorCode.toLowerCase()
                        ));

        problem.setProperty("errorCode", errorCode);
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }
}