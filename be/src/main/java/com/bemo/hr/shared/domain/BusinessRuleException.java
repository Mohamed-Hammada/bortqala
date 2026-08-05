package com.bemo.hr.shared.domain;

import org.springframework.http.HttpStatus;

import java.util.List;

public class BusinessRuleException extends RuntimeException {
    private final String code;
    private final HttpStatus status;
    private final List<String> fields;

    public BusinessRuleException(String message) {
        this(message, "BUSINESS_CONFLICT", HttpStatus.CONFLICT, List.of());
    }

    public BusinessRuleException(String message, String code, HttpStatus status) {
        this(message, code, status, List.of());
    }

    public BusinessRuleException(String message, String code, HttpStatus status, List<String> fields) {
        super(message);
        this.code = code;
        this.status = status;
        this.fields = fields;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public List<String> getFields() {
        return fields;
    }
}
