package com.bemo.hr.shared.domain;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}
