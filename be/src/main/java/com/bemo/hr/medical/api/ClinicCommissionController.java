package com.bemo.hr.medical.api;

import com.bemo.hr.medical.api.MedicalClinicApi.DoctorCommissionStatementResponse;
import com.bemo.hr.medical.application.ClinicCommissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/clinic/commissions")
@RequiredArgsConstructor
public class ClinicCommissionController {

    private final ClinicCommissionService commissionService;

    @GetMapping("/statement")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DoctorCommissionStatementResponse> getStatement(
            @RequestParam String doctorId,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) BigDecimal rate
    ) {
        return ResponseEntity.ok(commissionService.getMonthlyStatement(doctorId, year, month, rate));
    }
}
