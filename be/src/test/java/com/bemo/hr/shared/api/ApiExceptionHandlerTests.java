package com.bemo.hr.shared.api;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.i18n.TranslationService;
import com.bemo.hr.shared.observability.RequestAuditFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiExceptionHandlerTests {
    @Mock
    private TranslationService translationService;

    private ApiExceptionHandler handler() {
        return new ApiExceptionHandler(translationService);
    }

    private MockHttpServletRequest request() {
        var request = new MockHttpServletRequest("POST", "/api/v1/employees");
        request.setAttribute(RequestAuditFilter.REQUEST_ATTRIBUTE_CORRELATION_ID, "correlation-123");
        return request;
    }

    @Test
    void businessConflictReturnsStandardShapeWithCorrelationId() {
        when(translationService.resolveLocale(any())).thenReturn("ar-EG");
        when(translationService.translateOrDefault(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        var response = handler().businessConflict(new BusinessRuleException("Employee code already exists."), request());

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        ApiError body = response.getBody();
        assertThat(body.code()).isEqualTo("BUSINESS_CONFLICT");
        assertThat(body.message()).isEqualTo("error.requestFailed");
        assertThat(body.localizedMessage()).isEqualTo("error.requestFailed");
        assertThat(body.status()).isEqualTo(409);
        assertThat(body.path()).isEqualTo("/api/v1/employees");
        assertThat(body.correlationId()).isEqualTo("correlation-123");
        assertThat(body.timestamp()).isNotNull();
        assertThat(body.fieldErrors()).isNull();
    }

    @Test
    void notFoundReturnsNotFoundShape() {
        when(translationService.resolveLocale(any())).thenReturn("en-US");
        when(translationService.translateOrDefault(eq("error.resourceNotFound"), eq("en-US"), anyString()))
                .thenReturn("Resource not found");
        var response = handler().notFound(new NotFoundException("Employee not found."), request());

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("Resource not found");
    }

    @Test
    void businessConflictWithCodeAndStatusReturnsMetadata() {
        when(translationService.resolveLocale(any())).thenReturn("ar-EG");
        var exception = new BusinessRuleException("Schedule effective date ranges cannot overlap.",
                "SCHEDULE_RULE_OVERLAP", org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                List.of("schedules[0]", "schedules[2]"));

        var response = handler().businessConflict(exception, request());

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        ApiError body = response.getBody();
        assertThat(body.code()).isEqualTo("SCHEDULE_RULE_OVERLAP");
        assertThat(body.status()).isEqualTo(422);
        assertThat(body.fieldErrors()).hasSize(2);
        assertThat(body.fieldErrors().get(0).field()).isEqualTo("schedules[0]");
        assertThat(body.fieldErrors().get(0).code()).isEqualTo("SCHEDULE_RULE_OVERLAP");
        assertThat(body.fieldErrors().get(1).field()).isEqualTo("schedules[2]");
    }

    @Test
    void authenticationReturnsLocalizedMessages() {
        when(translationService.resolveLocale(any())).thenReturn("ar-EG");
        when(translationService.translateOrDefault(eq("error.invalidCredentials"), eq("en-US"), anyString()))
                .thenReturn("The username or password is incorrect.");
        when(translationService.translateOrDefault(eq("error.invalidCredentials"), eq("ar-EG"), anyString()))
                .thenReturn("اسم المستخدم أو كلمة المرور غير صحيحة.");

        var response = handler().authentication(new BadCredentialsException("bad"), request());

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody().code()).isEqualTo("AUTHENTICATION_FAILED");
        assertThat(response.getBody().message()).isEqualTo("The username or password is incorrect.");
        assertThat(response.getBody().localizedMessage()).isEqualTo("اسم المستخدم أو كلمة المرور غير صحيحة.");
    }

    @Test
    void validationReturnsFieldErrorsArray() {
        when(translationService.resolveLocale(any())).thenReturn("ar-EG");
        when(translationService.translateOrDefault(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    if ("error.invalidValue".equals(invocation.getArgument(0)) && "ar-EG".equals(invocation.getArgument(1))) {
                        return "قيمة غير صالحة";
                    }
                    return invocation.getArgument(2);
                });
        var target = new Object();
        var bindingResult = new BeanPropertyBindingResult(target, "target");
        bindingResult.addError(new FieldError("target", "employeeCode", "must not be blank"));
        var exception = new MethodArgumentNotValidException(null, bindingResult);

        var response = handler().validation(exception, request());

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().fieldErrors()).hasSize(1);
        assertThat(response.getBody().fieldErrors().get(0).field()).isEqualTo("employeeCode");
        assertThat(response.getBody().fieldErrors().get(0).code()).isEqualTo("INVALID_VALUE");
        assertThat(response.getBody().fieldErrors().get(0).message()).isEqualTo("قيمة غير صالحة");
    }

    @Test
    void acceptLanguageHeaderIsDelegatedToLocaleResolver() {
        when(translationService.resolveLocale("en-US, en;q=0.9")).thenReturn("en-US");
        when(translationService.translateOrDefault(eq("error.invalidCredentials"), eq("en-US"), anyString()))
                .thenReturn("Invalid credentials.");
        var request = request();
        request.addHeader("Accept-Language", "en-US, en;q=0.9");

        var response = handler().authentication(new BadCredentialsException("bad"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody().localizedMessage()).isEqualTo("Invalid credentials.");
        verify(translationService).resolveLocale("en-US, en;q=0.9");
    }
}
