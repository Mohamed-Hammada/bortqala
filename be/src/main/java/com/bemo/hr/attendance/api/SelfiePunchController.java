package com.bemo.hr.attendance.api;

import com.bemo.hr.attendance.application.SelfiePunchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/** WP-14 AC-3: employee self-service selfie punch endpoint (idempotent by operationId). */
@RestController
@RequestMapping("/api/v1/attendance")
public class SelfiePunchController {

    private final SelfiePunchService selfiePunchService;

    public SelfiePunchController(SelfiePunchService selfiePunchService) {
        this.selfiePunchService = selfiePunchService;
    }

    @PostMapping("/selfie-punch")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public SelfiePunchApi.SelfiePunchResponse punch(@Valid @RequestBody SelfiePunchApi.SelfiePunchRequest request,
                                                    Principal principal) {
        return selfiePunchService.punch(principal == null ? "system" : principal.getName(), request);
    }
}
