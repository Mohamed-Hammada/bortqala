package com.bemo.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Canonical framework error codes mapped to HTTP statuses. Applications should define their
 * own domain error enums implementing {@link BemoError}; these cover the shared surface.
 */
public enum BemoErrorType implements BemoError {

    VALIDATION_ERROR("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Validation failed.", false),
    BAD_INPUT("BAD_INPUT", HttpStatus.BAD_REQUEST, "Bad input.", false),
    UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Authentication required.", false),
    FORBIDDEN("FORBIDDEN", HttpStatus.FORBIDDEN, "Access denied.", false),
    NOT_FOUND("NOT_FOUND", HttpStatus.NOT_FOUND, "Resource not found.", false),
    CONFLICT("CONFLICT", HttpStatus.CONFLICT, "Operation conflicts with current state.", false),
    UNPROCESSABLE_ENTITY("UNPROCESSABLE_ENTITY", HttpStatus.UNPROCESSABLE_ENTITY, "Request could not be processed.", false),
    RATE_LIMITED("RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS, "Too many requests.", false),
    INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error.", true),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable.", true);

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
    private final boolean loggable;

    BemoErrorType(String code, HttpStatus httpStatus, String message, boolean loggable) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
        this.loggable = loggable;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public boolean isLoggable() {
        return loggable;
    }
}
