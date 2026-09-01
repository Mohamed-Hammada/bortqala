package com.bemo.hr.payroll.api;

import com.bemo.hr.payroll.application.EgyptianStatutoryPayrollService;
import com.bemo.hr.payroll.application.PayrollCalculationPolicyService;
import com.bemo.hr.payroll.application.PayrollService;
import com.bemo.hr.payroll.application.PayrollWpsExportService;
import com.bemo.hr.reporting.application.ExcelExportOptions;
import com.bemo.hr.shared.security.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
@PreAuthorize("""
        (hasAnyRole(
            'SUPER_ADMIN',
            'ADMIN',
            'HR_MANAGER',
            'HR_REVIEWER',
            'PAYROLL_MANAGER'
        ) or @auth.hasAnyPermission('hr:payroll:read', 'payroll:run:calculate', 'payroll:run:approve', 'payroll:run:disburse'))
        and @salaryAuthorization.canView(authentication)
        """)
public class PayrollController {

    private final PayrollService payrollService;
    private final AuthService authService;
    private final PayrollCalculationPolicyService payrollCalculationPolicyService;
    private final EgyptianStatutoryPayrollService egyptianStatutoryPayrollService;
    private final PayrollWpsExportService payrollWpsExportService;

    @GetMapping
    public PayrollApi.SheetResponse getSheet(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) String categoryId) {
        return payrollService.getSheet(year, month, categoryId);
    }

    @PostMapping("/calculate-statutory")
    public PayrollApi.StatutoryTaxResponse calculateStatutoryTax(
            @Valid @RequestBody PayrollApi.StatutoryTaxRequest request) {
        var res = egyptianStatutoryPayrollService.calculate(request.grossSalary());
        var brackets = res.taxBracketsBreakdown().stream()
                .map(b -> new PayrollApi.StatutoryTaxBracketResponse(
                        b.bracketNumber(), b.bracketRange(), b.ratePercent(), b.taxableAmountInBracket(), b.computedTax()))
                .toList();
        return new PayrollApi.StatutoryTaxResponse(
                res.monthlyGrossSalary(),
                res.monthlyInsurableWage(),
                res.monthlyEmployeeSocialInsurance(),
                res.monthlyEmployerSocialInsurance(),
                res.monthlyMartyrsFund(),
                res.annualTaxableIncome(),
                res.annualIncomeTax(),
                res.monthlyIncomeTax(),
                res.totalEmployeeStatutoryDeductions(),
                res.monthlyNetSalary(),
                brackets
        );
    }

    @GetMapping("/wps-export")
    public ResponseEntity<byte[]> exportWps(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "EG_WPS") PayrollWpsExportService.WpsFormat format,
            @RequestParam(required = false) String employerId,
            @RequestParam(required = false) String bankCode,
            @RequestParam(required = false) String categoryId) {
        var sheet = payrollService.getSheet(year, month, categoryId);
        byte[] content = payrollWpsExportService.generateWpsFile(sheet, format, employerId, bankCode);

        var headers = new HttpHeaders();
        String ext = format == PayrollWpsExportService.WpsFormat.GCC_SIF ? "sif" : "csv";
        String mimeType = format == PayrollWpsExportService.WpsFormat.GCC_SIF ? "text/plain" : "text/csv; charset=UTF-8";
        headers.setContentType(MediaType.parseMediaType(mimeType));

        String filename = String.format("wps-clearing-%04d%02d-%s.%s", year, month, format.name().toLowerCase(), ext);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());

        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    @PostMapping("/pay")
    @PreAuthorize("(hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PAYROLL_MANAGER') or @auth.hasPermission('payroll:run:disburse')) "
            + "and @salaryAuthorization.canView(authentication)")
    public PayrollApi.SheetResponse recordPayment(
            @Valid @RequestBody PayrollApi.PaymentRequest request,
            Authentication authentication) {
        return payrollService.recordPayment(request, authentication.getName());
    }

    @PostMapping("/pay-bulk")
    @PreAuthorize("(hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PAYROLL_MANAGER') or @auth.hasPermission('payroll:run:disburse')) "
            + "and @salaryAuthorization.canView(authentication)")
    public PayrollApi.SheetResponse payBulk(
            @Valid @RequestBody PayrollApi.BulkPaymentRequest request,
            Authentication authentication) {
        return payrollService.payBulk(request, authentication.getName());
    }

    @PostMapping("/transition")
    @PreAuthorize("(((#request.targetStatus.name() == 'CALCULATED' or #request.targetStatus.name() == 'REVIEWED') "
            + "and (hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER', 'PAYROLL_MANAGER') or @auth.hasPermission('payroll:run:calculate'))) "
            + "or (#request.targetStatus.name() == 'APPROVED' and (hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PAYROLL_MANAGER') or @auth.hasPermission('payroll:run:approve'))) "
            + "or (#request.targetStatus.name() == 'POSTED' and (hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PAYROLL_MANAGER') or @auth.hasPermission('finance:journal:post')))) "
            + "and @salaryAuthorization.canView(authentication)")
    public PayrollApi.SheetResponse transitionStatus(
            @Valid @RequestBody PayrollApi.StatusTransitionRequest request,
            Authentication authentication) {
        return payrollService.transitionStatus(request, authentication.getName());
    }

    @PostMapping("/reverse")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PAYROLL_MANAGER') "
            + "and @salaryAuthorization.canView(authentication)")
    public PayrollApi.SheetResponse reversePayment(
            @Valid @RequestBody PayrollApi.ReversePaymentRequest request,
            Authentication authentication) {
        return payrollService.reversePayment(request, authentication.getName());
    }

    @GetMapping("/export")
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

    @GetMapping("/payments/{id}/explanation")
    public java.util.List<PayrollApi.ExplanationResponse> getExplanation(@PathVariable String id) {
        return payrollService.getPaymentExplanation(id);
    }

    @GetMapping("/calculation-policies")
    public java.util.List<PayrollApi.CalculationPolicyResponse> calculationPolicies() {
        return payrollCalculationPolicyService.list().stream().map(this::policyResponse).toList();
    }

    @PostMapping("/calculation-policies")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PAYROLL_MANAGER') and @salaryAuthorization.canView(authentication)")
    public PayrollApi.CalculationPolicyResponse createCalculationPolicy(
            @Valid @RequestBody PayrollApi.CalculationPolicyRequest request) {
        var from = java.time.Instant.ofEpochMilli(request.effectiveFrom()).atZone(java.time.ZoneOffset.UTC).toLocalDate();
        var to = request.effectiveTo() == null ? null
                : java.time.Instant.ofEpochMilli(request.effectiveTo()).atZone(java.time.ZoneOffset.UTC).toLocalDate();
        return policyResponse(payrollCalculationPolicyService.create(request.name(), from, to,
                request.workingHourDivisor(), request.overtimeMultiplier()));
    }

    private PayrollApi.CalculationPolicyResponse policyResponse(
            com.bemo.hr.payroll.domain.PayrollCalculationPolicy policy) {
        return new PayrollApi.CalculationPolicyResponse(policy.getId(), policy.getName(),
                policy.getEffectiveFrom().atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
                policy.getEffectiveTo() == null ? null
                        : policy.getEffectiveTo().atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
                policy.getWorkingHourDivisor(), policy.getOvertimeMultiplier(), policy.getVersion());
    }
}
