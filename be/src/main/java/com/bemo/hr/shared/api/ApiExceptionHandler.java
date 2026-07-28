package com.bemo.hr.shared.api;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.i18n.TranslationService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final TranslationService translationService;

    public ApiExceptionHandler(TranslationService translationService) {
        this.translationService = translationService;
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail authentication(AuthenticationException exception, HttpServletRequest request) {
        String locale = resolveLocale(request);
        return problem(HttpStatus.UNAUTHORIZED,
                translationService.translate("error.authenticationTitle", locale),
                translationService.translate("error.invalidCredentials", locale), "authentication-failed");
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException exception, HttpServletRequest request) {
        String locale = resolveLocale(request);
        return problem(HttpStatus.NOT_FOUND,
                translationService.translate("error.notFoundTitle", locale),
                exception.getMessage(), "not-found");
    }

    @ExceptionHandler({BusinessRuleException.class, DataIntegrityViolationException.class})
    ProblemDetail conflict(RuntimeException exception, HttpServletRequest request) {
        String locale = resolveLocale(request);
        String detail = exception instanceof BusinessRuleException
                ? exception.getMessage() : translationService.translate("error.dataConflictDetail", locale);
        return problem(HttpStatus.CONFLICT,
                translationService.translate("error.conflictTitle", locale), detail, "business-conflict");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String locale = resolveLocale(request);
        var errors = new LinkedHashMap<String, String>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        var problem = problem(HttpStatus.BAD_REQUEST,
                translationService.translate("error.validationTitle", locale),
                translationService.translate("error.validationDetail", locale), "validation-failed");
        problem.setProperty("errors", errors);
        return problem;
    }

    private String resolveLocale(HttpServletRequest request) {
        String acceptLang = request.getHeader("Accept-Language");
        if (acceptLang != null && translationService.isSupported(acceptLang)) {
            return acceptLang;
        }
        return "ar-EG";
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String type) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://hr.bemo.local/problems/" + type));
        return problem;
    }
}
