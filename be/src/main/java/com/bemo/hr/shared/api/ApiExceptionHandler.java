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
        return respond("FORBIDDEN", HttpStatus.FORBIDDEN,
                translated("error.accessDenied", resolveLocale(request)), request);
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> notFound(NotFoundException exception, HttpServletRequest request) {
        String locale = resolveLocale(request);
        String code = exception.getCode() == null ? "NOT_FOUND" : exception.getCode();
        ErrorText errorText = exception.getCode() == null
                ? translated("error.resourceNotFound", locale)
                : translatedOrGeneric(exception.getCode(), locale, "error.resourceNotFound");
        return respond(code, HttpStatus.NOT_FOUND, errorText, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> resourceNotFound(NoResourceFoundException exception, HttpServletRequest request) {
        return respond("NOT_FOUND", HttpStatus.NOT_FOUND,
                translated("error.resourceNotFound", resolveLocale(request)), request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<ApiError> businessConflict(BusinessRuleException exception, HttpServletRequest request) {
        String locale = resolveLocale(request);
        String code = exception.getCode() == null || exception.getCode().isBlank() ? "BUSINESS_CONFLICT" : exception.getCode();
        ErrorText errorText = translatedOrGeneric(code, locale, "error.requestFailed");
        List<ApiError.FieldError> fieldErrors = exception.getFields().stream()
                .map(field -> new ApiError.FieldError(field, code, errorText.localizedMessage()))
                .toList();
        return respond(code, exception.getStatus(), errorText, request, fieldErrors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> dataConflict(DataIntegrityViolationException exception, HttpServletRequest request) {
        return respond("DATA_CONFLICT", HttpStatus.CONFLICT,
                translated("error.dataConflictDetail", resolveLocale(request)), request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiError> optimisticLock(OptimisticLockingFailureException exception, HttpServletRequest request) {
        return respond("CONCURRENT_MODIFICATION", HttpStatus.CONFLICT,
                translated("error.concurrentModification", resolveLocale(request)), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String locale = resolveLocale(request);
        var errorText = translated("error.validationDetail", locale);
        String invalidValue = translated("error.invalidValue", locale).localizedMessage();
        List<ApiError.FieldError> fieldErrors = new ArrayList<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.add(new ApiError.FieldError(error.getField(), "INVALID_VALUE", invalidValue)));
        return respond("VALIDATION_FAILED", HttpStatus.BAD_REQUEST, errorText, request, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> malformed(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return respond("MALFORMED_REQUEST", HttpStatus.BAD_REQUEST,
                translated("error.malformedRequest", resolveLocale(request)), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> methodNotAllowed(HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return respond("METHOD_NOT_ALLOWED", HttpStatus.METHOD_NOT_ALLOWED,
                translated("error.methodNotAllowed", resolveLocale(request)), request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> responseStatus(ResponseStatusException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        HttpStatus effective = status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
        return respond(codeFor(effective), effective,
                translated(translationKeyFor(effective), resolveLocale(request)), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), exception);
        return respond("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR,
                translated("error.unexpectedError", resolveLocale(request)), request);
    }

    private String resolveLocale(HttpServletRequest request) {
        return translationService.resolveLocale(request.getHeader("Accept-Language"));
    }

    private ErrorText translated(String key, String locale) {
        return new ErrorText(
                translationService.translateOrDefault(key, "en-US", key),
                translationService.translateOrDefault(key, locale, key));
    }

    private ErrorText translatedOrGeneric(String key, String locale, String genericKey) {
        ErrorText generic = translated(genericKey, locale);
        if (key == null || key.isBlank()) {
            return generic;
        }
        return new ErrorText(
                translationService.translateOrDefault(key, "en-US", generic.message()),
                translationService.translateOrDefault(key, locale, generic.localizedMessage()));
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
            case METHOD_NOT_ALLOWED -> "METHOD_NOT_ALLOWED";
            case CONFLICT -> "BUSINESS_CONFLICT";
            default -> "REQUEST_FAILED";
        };
    }

    private String translationKeyFor(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "error.malformedRequest";
            case UNAUTHORIZED -> "error.invalidCredentials";
            case FORBIDDEN -> "error.accessDenied";
            case NOT_FOUND -> "error.resourceNotFound";
            case METHOD_NOT_ALLOWED -> "error.methodNotAllowed";
            case CONFLICT -> "error.dataConflictDetail";
            default -> status.is5xxServerError() ? "error.unexpectedError" : "error.requestFailed";
        };
    }

    private record ErrorText(String message, String localizedMessage) { }
}
