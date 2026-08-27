package com.bemo.hr.trade.sales.api;

import com.bemo.hr.trade.sales.application.SalesTargetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
