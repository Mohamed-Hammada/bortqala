package com.bemo.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Contract for structured application errors: a stable machine-readable code, an HTTP status
 * and a human-readable message that may carry parameter placeholders.
 */
public interface BemoError {

    String code();

    HttpStatus httpStatus();

    String message();

    boolean isLoggable();

    static BemoError of(String code, HttpStatus httpStatus, String message) {
        return of(code, httpStatus, message, false);
    }

    static BemoError of(String code, HttpStatus httpStatus, String message, boolean loggable) {
        return new BemoError() {
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
        };
    }
}
