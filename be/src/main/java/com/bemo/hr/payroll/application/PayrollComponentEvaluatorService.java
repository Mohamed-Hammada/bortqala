package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayrollComponent;
import com.bemo.hr.payroll.infrastructure.PayrollComponentRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
public class PayrollComponentEvaluatorService {

    private final PayrollComponentRepository repository;

    public PayrollComponentEvaluatorService(PayrollComponentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PayrollComponent createComponent(String code, String name, PayrollComponent.Type type, String calculationFormula) {
        log.debug("createComponent called with code={}, name={}, type={}", code, name, type);
        PayrollComponent component = new PayrollComponent(code, name, type, calculationFormula);
        PayrollComponent saved = repository.save(component);
        log.info("PayrollComponent created id={}", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public EvaluationResult evaluateComponent(String componentId, BigDecimal baseAmount, BigDecimal percentage) {
        log.debug("evaluateComponent called with componentId={}, baseAmount={}, percentage={}", componentId, baseAmount, percentage);
        PayrollComponent component = repository.findById(componentId)
                .orElseThrow(() -> new BusinessRuleException("Payroll component not found", "PAYROLL_COMPONENT_NOT_FOUND", HttpStatus.NOT_FOUND));

        BigDecimal multiplier = percentage.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal evaluatedAmount = baseAmount.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

        log.debug("evaluateComponent completed componentId={} evaluatedAmount={}", componentId, evaluatedAmount);
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
