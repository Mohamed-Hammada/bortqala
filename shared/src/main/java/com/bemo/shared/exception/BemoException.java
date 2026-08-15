package com.bemo.shared.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Base runtime exception for all structured application errors. Carries a {@link BemoError}
 * and an optional metadata map that is serialized into the error response.
 */
public class BemoException extends RuntimeException {

    private final BemoError error;
    private final Map<String, Object> metadata;

    public BemoException(BemoError error) {
        this(error, null, (Map<String, Object>) null);
    }

    public BemoException(BemoError error, String message) {
        this(error, message, (Map<String, Object>) null);
    }

    public BemoException(BemoError error, String message, Map<String, Object> metadata) {
        super(message != null ? message : error.message());
        this.error = error;
        this.metadata = metadata;
    }

    public BemoException(BemoError error, String message, Throwable cause) {
        super(message != null ? message : error.message(), cause);
        this.error = error;
        this.metadata = null;
    }

    public BemoError getError() {
        return error;
    }

    public String getCode() {
        return error.code();
    }

    public HttpStatus getHttpStatus() {
        return error.httpStatus();
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public boolean isLoggable() {
        return error.isLoggable();
    }
}
