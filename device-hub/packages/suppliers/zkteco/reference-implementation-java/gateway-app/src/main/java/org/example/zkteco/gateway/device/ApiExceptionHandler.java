package org.example.zkteco.gateway.device;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(DeviceNotFoundException.class)
    ProblemDetail notFound(DeviceNotFoundException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        detail.setTitle("Device not found");
        return detail;
    }

    @ExceptionHandler({IllegalArgumentException.class, UnsupportedOperationException.class})
    ProblemDetail badRequest(RuntimeException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        detail.setTitle("Invalid device operation");
        return detail;
    }
}
