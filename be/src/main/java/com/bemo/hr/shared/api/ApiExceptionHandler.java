package com.bemo.hr.shared.api;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.i18n.TranslationService;
import com.bemo.hr.shared.observability.RequestAuditFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private final TranslationService translationService;

    public ApiExceptionHandler(TranslationService translationService) {
        this.translationService = translationService;
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> authentication(AuthenticationException exception, HttpServletRequest request) {
        return respond("AUTHENTICATION_FAILED", HttpStatus.UNAUTHORIZED,
                translated("error.invalidCredentials", resolveLocale(request)), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> forbidden(AccessDeniedException exception, HttpServletRequest request) {
        return respond("FORBIDDEN", HttpStatus.FORBIDDEN, raw("Access denied."), request);
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> notFound(NotFoundException exception, HttpServletRequest request) {
        return respond("NOT_FOUND", HttpStatus.NOT_FOUND, raw(exception.getMessage()), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> resourceNotFound(NoResourceFoundException exception, HttpServletRequest request) {
        return respond("NOT_FOUND", HttpStatus.NOT_FOUND, raw("Resource not found."), request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<ApiError> businessConflict(BusinessRuleException exception, HttpServletRequest request) {
        List<ApiError.FieldError> fieldErrors = exception.getFields().stream()
                .map(field -> new ApiError.FieldError(field, exception.getCode(), exception.getMessage()))
                .toList();
        return respond(exception.getCode(), exception.getStatus(), raw(exception.getMessage()), request, fieldErrors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> dataConflict(DataIntegrityViolationException exception, HttpServletRequest request) {
        return respond("DATA_CONFLICT", HttpStatus.CONFLICT,
                translated("error.dataConflictDetail", resolveLocale(request)), request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiError> optimisticLock(OptimisticLockingFailureException exception, HttpServletRequest request) {
        return respond("CONCURRENT_MODIFICATION", HttpStatus.CONFLICT,
                raw("The record was modified by another reviewer. Reload and retry."), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        var errorText = translated("error.validationDetail", resolveLocale(request));
        List<ApiError.FieldError> fieldErrors = new ArrayList<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.add(new ApiError.FieldError(error.getField(), "INVALID_VALUE", error.getDefaultMessage())));
        return respond("VALIDATION_FAILED", HttpStatus.BAD_REQUEST, errorText, request, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> malformed(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return respond("MALFORMED_REQUEST", HttpStatus.BAD_REQUEST, raw("Malformed request body."), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> methodNotAllowed(HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return respond("METHOD_NOT_ALLOWED", HttpStatus.METHOD_NOT_ALLOWED, raw("HTTP method not supported."), request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> responseStatus(ResponseStatusException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        HttpStatus effective = status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
        String message = exception.getReason() != null ? exception.getReason() : effective.getReasonPhrase();
        return respond(codeFor(effective), effective, raw(message), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), exception);
        return respond("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, raw("An unexpected error occurred."), request);
    }

    private String resolveLocale(HttpServletRequest request) {
        String acceptLang = request.getHeader("Accept-Language");
        if (acceptLang != null && translationService.isSupported(acceptLang)) {
            return acceptLang;
        }
        return "ar-EG";
    }

    private ErrorText translated(String key, String locale) {
        return new ErrorText(
                translationService.translateOrDefault(key, "en-US", key),
                translationService.translateOrDefault(key, locale, key));
    }

    private ErrorText raw(String message) {
        return new ErrorText(message, message);
    }

    private ResponseEntity<ApiError> respond(String code, HttpStatus status, ErrorText errorText,
                                             HttpServletRequest request) {
        return respond(code, status, errorText, request, List.of());
    }

    private ResponseEntity<ApiError> respond(String code, HttpStatus status, ErrorText errorText,
                                             HttpServletRequest request, List<ApiError.FieldError> fieldErrors) {
        Object correlationId = request.getAttribute(RequestAuditFilter.REQUEST_ATTRIBUTE_CORRELATION_ID);
        var error = new ApiError(
                code,
                errorText.message(),
                errorText.localizedMessage(),
                status.value(),
                request.getRequestURI(),
                correlationId != null ? correlationId.toString() : null,
                Instant.now(),
                fieldErrors.isEmpty() ? null : fieldErrors);
        return ResponseEntity.status(status).body(error);
    }

    private String codeFor(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "MALFORMED_REQUEST";
            case UNAUTHORIZED -> "AUTHENTICATION_FAILED";
            case FORBIDDEN -> "FORBIDDEN";
            case NOT_FOUND -> "NOT_FOUND";
            case CONFLICT -> "BUSINESS_CONFLICT";
            default -> "REQUEST_FAILED";
        };
    }

    private record ErrorText(String message, String localizedMessage) { }
}
