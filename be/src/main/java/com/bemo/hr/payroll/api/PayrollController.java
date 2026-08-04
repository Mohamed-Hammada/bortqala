package com.bemo.hr.payroll.api;

import com.bemo.hr.payroll.application.PayrollService;
import com.bemo.hr.reporting.application.ExcelExportOptions;
import com.bemo.hr.shared.security.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER', 'PAYROLL_MANAGER')")
public class PayrollController {

    private final PayrollService payrollService;
    private final AuthService authService;

    @GetMapping
    @PreAuthorize("@salaryAuthorization.canView(authentication)")
    public PayrollApi.SheetResponse getSheet(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) String categoryId) {
        return payrollService.getSheet(year, month, categoryId);
    }

    @PostMapping("/pay")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'PAYROLL_MANAGER')")
    public PayrollApi.SheetResponse recordPayment(
            @Valid @RequestBody PayrollApi.PaymentRequest request,
            Authentication authentication) {
        return payrollService.recordPayment(request, authentication.getName());
    }

    @PostMapping("/pay-bulk")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'PAYROLL_MANAGER')")
    public PayrollApi.SheetResponse payBulk(
            @Valid @RequestBody PayrollApi.BulkPaymentRequest request,
            Authentication authentication) {
        return payrollService.payBulk(request, authentication.getName());
    }

    @PostMapping("/transition")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'PAYROLL_MANAGER')")
    public PayrollApi.SheetResponse transitionStatus(
            @Valid @RequestBody PayrollApi.StatusTransitionRequest request,
            Authentication authentication) {
        return payrollService.transitionStatus(request, authentication.getName());
    }

    @PostMapping("/reverse")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'PAYROLL_MANAGER')")
    public PayrollApi.SheetResponse reversePayment(
            @Valid @RequestBody PayrollApi.ReversePaymentRequest request,
            Authentication authentication) {
        return payrollService.reversePayment(request, authentication.getName());
    }

    @GetMapping("/export")
    @PreAuthorize("@salaryAuthorization.canView(authentication)")
    public ResponseEntity<byte[]> export(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) String categoryId,
            Authentication authentication) {
        var preference = authService.currentPreferences(authentication.getName());
        var options = new ExcelExportOptions(preference.locale(), preference.excelTableStyle());
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        String baseName = preference.locale().startsWith("ar") ? "كشف-صرف-المرتبات" : "payroll-register";
        headers.setContentDisposition(ContentDisposition.attachment().filename(baseName + "-"
                        + year + "-" + String.format("%02d", month) + "-"
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".xlsx",
                StandardCharsets.UTF_8).build());
        return new ResponseEntity<>(payrollService.export(year, month, categoryId, options), headers, HttpStatus.OK);
    }
}
