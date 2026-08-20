package com.bemo.hr.payroll.api;

import com.bemo.hr.payroll.application.PayrollComponentEvaluatorService;
import com.bemo.hr.payroll.domain.PayrollComponent;
import com.bemo.hr.shared.security.Roles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payroll/components")
public class PayrollComponentController {

    private final PayrollComponentEvaluatorService evaluatorService;

    public PayrollComponentController(PayrollComponentEvaluatorService evaluatorService) {
        this.evaluatorService = evaluatorService;
    }

    @PostMapping
    @PreAuthorize(Roles.ADMIN_HR_MANAGER_PAYROLL_MANAGER)
    public PayrollComponent createComponent(@RequestBody CreateComponentPayload payload) {
        return evaluatorService.createComponent(payload.code(), payload.name(), PayrollComponent.Type.valueOf(payload.type()), payload.calculationFormula());
    }

    @PostMapping("/evaluate")
    @PreAuthorize(Roles.ADMIN_HR_MANAGER_PAYROLL_MANAGER_VIEWER)
    public PayrollComponentEvaluatorService.EvaluationResult evaluate(@RequestBody EvaluatePayload payload) {
        return evaluatorService.evaluateComponent(payload.componentId(), payload.baseAmount(), payload.percentage());
    }

    @GetMapping
    @PreAuthorize(Roles.ADMIN_HR_MANAGER_PAYROLL_MANAGER_VIEWER)
    public List<PayrollComponent> getAllComponents() {
        return evaluatorService.getAllComponents();
    }

    public record CreateComponentPayload(String code, String name, String type, String calculationFormula) {
    }

    public record EvaluatePayload(String componentId, BigDecimal baseAmount, BigDecimal percentage) {
    }
}
