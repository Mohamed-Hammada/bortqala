package com.bemo.shared.exception;

import java.util.Map;

/**
 * Thrown when a request is syntactically well-formed but semantically invalid (bad values,
 * malformed fields, failed business validations). Maps to HTTP 400.
 */
public class BadInputException extends BemoException {

    public BadInputException(String message) {
        super(BemoErrorType.BAD_INPUT, message);
    }

    public BadInputException(String message, Map<String, Object> metadata) {
        super(BemoErrorType.BAD_INPUT, message, metadata);
    }

    public BadInputException(String message, Throwable cause) {
        super(BemoErrorType.BAD_INPUT, message, cause);
    }
}
