package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayrollComponent;
import com.bemo.hr.payroll.infrastructure.PayrollComponentRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PayrollComponentEvaluatorService {

    private final PayrollComponentRepository repository;

    public PayrollComponentEvaluatorService(PayrollComponentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PayrollComponent createComponent(String code, String name, PayrollComponent.Type type, String calculationFormula) {
        PayrollComponent component = new PayrollComponent(code, name, type, calculationFormula);
        return repository.save(component);
    }

    @Transactional(readOnly = true)
    public EvaluationResult evaluateComponent(String componentId, BigDecimal baseAmount, BigDecimal percentage) {
        PayrollComponent component = repository.findById(componentId)
                .orElseThrow(() -> new BusinessRuleException("Payroll component not found", "PAYROLL_COMPONENT_NOT_FOUND", HttpStatus.NOT_FOUND));

        BigDecimal multiplier = percentage.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal evaluatedAmount = baseAmount.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

        return new EvaluationResult(component.getId(), component.getCode(), component.getType(), baseAmount, percentage, evaluatedAmount);
    }

    @Transactional(readOnly = true)
    public List<PayrollComponent> getAllComponents() {
        return repository.findAll();
    }

    public record EvaluationResult(
            String componentId,
            String code,
            PayrollComponent.Type type,
            BigDecimal baseAmount,
            BigDecimal percentage,
            BigDecimal evaluatedAmount
    ) {
    }
}
