package com.bemo.hr.shared.api;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail authentication(AuthenticationException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "Authentication failed",
                "The username or password is incorrect.", "authentication-failed");
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage(), "not-found");
    }

    @ExceptionHandler({BusinessRuleException.class, DataIntegrityViolationException.class})
    ProblemDetail conflict(RuntimeException exception) {
        String detail = exception instanceof BusinessRuleException
                ? exception.getMessage() : "The operation conflicts with existing data.";
        return problem(HttpStatus.CONFLICT, "Business rule conflict", detail, "business-conflict");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception) {
        var errors = new LinkedHashMap<String, String>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        var problem = problem(HttpStatus.BAD_REQUEST, "Validation failed", "One or more fields are invalid.", "validation-failed");
        problem.setProperty("errors", errors);
        return problem;
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String type) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://hr.bemo.local/problems/" + type));
        return problem;
    }
}
