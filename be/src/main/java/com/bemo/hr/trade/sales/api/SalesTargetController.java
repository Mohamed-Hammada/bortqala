package com.bemo.hr.trade.sales.api;

import com.bemo.hr.shared.security.AuthService;
import com.bemo.hr.trade.sales.application.SalesTargetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sales/targets")
@PreAuthorize("@auth.hasPermission('sales.manage')")
@RequiredArgsConstructor
public class SalesTargetController {

    private final SalesTargetService service;
    private final AuthService authService;

    @PostMapping
    public SalesTargetApi.TargetResponse createTarget(
            @Valid @RequestBody SalesTargetApi.TargetRequest req, Authentication auth) {
        return service.createTarget(req, auth.getName());
    }

    @GetMapping
    public List<SalesTargetApi.TargetResponse> listTargets(
            @RequestParam(required = false) String period) {
        return service.listTargets(period, null);
    }

    @DeleteMapping("/{id}")
    public void deleteTarget(@PathVariable String id) {
        service.deleteTarget(id);
    }

    @PostMapping("/commission-rules")
    public SalesTargetApi.CommissionRuleResponse createRule(
            @Valid @RequestBody SalesTargetApi.CommissionRuleRequest req, Authentication auth) {
        return service.createRule(req, auth.getName());
    }

    @GetMapping("/commission-rules")
    public List<SalesTargetApi.CommissionRuleResponse> listRules() {
        return service.listRules();
    }

    @DeleteMapping("/commission-rules/{id}")
    public void deleteRule(@PathVariable String id) {
        service.deleteRule(id);
    }

    @GetMapping("/commissions")
    public SalesTargetApi.CommissionStatementResponse statement(
            @RequestParam String repId, @RequestParam String period) {
        return service.computeStatement(repId, period);
    }

    @PostMapping("/commissions/send-to-payroll")
    public SalesTargetApi.PayrollSendResponse sendToPayroll(
            @Valid @RequestBody SalesTargetApi.SendToPayrollRequest req, Authentication auth) {
        return service.sendToPayroll(req.repId(), req.period(), auth.getName(), auth.getName());
    }

    @GetMapping("/commissions/export.xlsx")
    public ResponseEntity<byte[]> exportStatement(
            @RequestParam String repId, @RequestParam String period, Authentication auth) {
        String locale = authService.currentPreferences(auth.getName()).locale();
        boolean arabic = locale != null && locale.startsWith("ar");
        String filename = arabic ? "كشف-عمولات-المبيعات.xlsx" : "sales-commission-statement.xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(service.exportStatement(repId, period, locale));
    }
}
