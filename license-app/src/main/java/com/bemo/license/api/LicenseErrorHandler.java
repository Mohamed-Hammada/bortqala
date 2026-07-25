package com.bemo.license.api;

import com.bemo.license.application.LicenseException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class LicenseErrorHandler {
    @ExceptionHandler(LicenseException.class)
    ResponseEntity<Map<String,Object>> license(LicenseException error){return ResponseEntity.badRequest().body(body(error.getCode(),error.getMessage()));}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException error){return ResponseEntity.badRequest().body(body("VALIDATION_ERROR",error.getBindingResult().getFieldErrors().stream().findFirst().map(item->item.getField()+" "+item.getDefaultMessage()).orElse("Invalid request.")));}
    private Map<String,Object> body(String code,String message){return Map.of("code",code,"message",message,"timestamp",Instant.now().toEpochMilli());}
}
