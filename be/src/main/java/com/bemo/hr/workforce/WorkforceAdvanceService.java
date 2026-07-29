package com.bemo.hr.workforce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;

@Service
@RequiredArgsConstructor
public class WorkforceAdvanceService {
    private final WorkforceAdvanceRepository advanceRepository;
    private final WorkforceAdvanceInstallmentRepository installmentRepository;
    private final WorkforceAdvanceLedgerEntryRepository ledgerRepository;
    private final WorkerRepository workerRepository;
    private final WorkerCategoryRepository categoryRepository;
    private final ContractorRepository contractorRepository;
    private final AuditService auditService;
    private final WorkforceAdvancePolicyRepository policyRepository;

    @Transactional(readOnly = true)
    public List<WorkforceApi.AdvanceResponse> list() {
        return advanceRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public WorkforceApi.AdvanceResponse create(WorkforceApi.AdvanceCreateRequest request, String createdBy) {
        LocalDate policyDate;
        try { policyDate = request.firstInstallmentDate() == null || request.firstInstallmentDate().isBlank()
                ? LocalDate.now() : LocalDate.parse(request.firstInstallmentDate()); }
        catch (Exception exception) { throw new BusinessRuleException("تاريخ أول قسط غير صالح."); }
        WorkforceAdvancePolicy policy = effectivePolicy(request.workerId(), policyDate);
        String deductionMode = valueOrDefault(request.deductionMode(), policy != null ? policy.getDeductionMode() : "AUTO");
        String deductionFrequency = valueOrDefault(request.deductionFrequency(), policy != null ? policy.getDeductionFrequency() : "HALF_MONTH");
        BigDecimal maxDeductionPercent = request.maxDeductionPercent() != null ? request.maxDeductionPercent()
                : policy != null ? policy.getMaxDeductionPercent() : new BigDecimal("50");
        BigDecimal instAmount = request.installmentAmount();
        int count = request.totalInstallments() != null ? Math.max(1, request.totalInstallments())
                : policy != null ? policy.getDefaultInstallments() : 1;
        if (instAmount == null || instAmount.compareTo(BigDecimal.ZERO) == 0) {
            instAmount = request.amount().divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
        }

        WorkforceAdvance adv = new WorkforceAdvance(
            request.recipientType(), request.workerId(), request.contractorId(),
            request.amount(), request.termType(), count, instAmount,
            deductionFrequency, maxDeductionPercent, request.reason(),
            request.firstInstallmentDate(), deductionMode,
            request.deferralPeriods() != null ? request.deferralPeriods() : policy != null ? policy.getDeferralPeriods() : 0
        );
        adv.applyPolicySnapshot(policy);
        WorkforceAdvance saved = advanceRepository.save(adv);

        // Generate Installments with valid YYYY-MM-DD due dates
        String startDateStr = (request.firstInstallmentDate() != null && !request.firstInstallmentDate().isBlank())
            ? request.firstInstallmentDate()
            : java.time.LocalDate.now().toString();

        java.time.LocalDate baseDate;
        try {
            baseDate = java.time.LocalDate.parse(startDateStr);
        } catch (Exception e) {
            baseDate = java.time.LocalDate.now();
        }

        boolean isMonthly = "MONTHLY".equalsIgnoreCase(deductionFrequency);
        int deferral = request.deferralPeriods() != null ? request.deferralPeriods() : policy != null ? policy.getDeferralPeriods() : 0;

        for (int i = 1; i <= count; i++) {
            java.time.LocalDate instDate = isMonthly
                ? baseDate.plusMonths((long) (i - 1) + deferral)
                : baseDate.plusDays((long) (i - 1) * 15 + ((long) deferral * 15));

            WorkforceAdvanceInstallment inst = new WorkforceAdvanceInstallment(
                saved.getId(), i, instDate.toString(), instAmount
            );
            installmentRepository.save(inst);
        }

        // Ledger Entry for Issuance
        WorkforceAdvanceLedgerEntry entry = new WorkforceAdvanceLedgerEntry(
            saved.getId(), "ISSUANCE", saved.getAmount(), saved.getRemainingBalance(),
            "Advance issued: " + (request.reason() != null ? request.reason() : ""), createdBy
        );
        ledgerRepository.save(entry);

        auditService.record("CREATE", "ADVANCE", saved.getId(), createdBy,
            "{\"amount\":" + saved.getAmount() + ",\"termType\":\"" + saved.getTermType() + "\"}", null);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<WorkforceApi.AdvancePolicyResponse> listPolicies() {
        return policyRepository.findAllByOrderByScopeTypeAscScopeIdAsc().stream().map(this::mapPolicy).toList();
    }

    @Transactional
    public WorkforceApi.AdvancePolicyResponse savePolicy(WorkforceApi.AdvancePolicyRequest request, String actor) {
        String scopeType = request.scopeType().strip().toUpperCase(java.util.Locale.ROOT);
        if (!List.of("GLOBAL", "CATEGORY", "WORKER").contains(scopeType)) throw new BusinessRuleException("نطاق سياسة السلف غير صالح.");
        if (!"GLOBAL".equals(scopeType) && (request.scopeId() == null || request.scopeId().isBlank())) throw new BusinessRuleException("اختر الفئة أو العامل للاستثناء.");
        if (request.maxDeductionPercent().signum() <= 0 || request.maxDeductionPercent().compareTo(new BigDecimal("100")) > 0) throw new BusinessRuleException("نسبة الخصم يجب أن تكون بين 1 و100.");
        LocalDate effectiveFrom;
        LocalDate effectiveTo = null;
        try {
            effectiveFrom = LocalDate.parse(request.effectiveFrom());
            if (request.effectiveTo() != null && !request.effectiveTo().isBlank()) effectiveTo = LocalDate.parse(request.effectiveTo());
        } catch (Exception exception) { throw new BusinessRuleException("تاريخ بداية أو نهاية السياسة غير صالح."); }
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) throw new BusinessRuleException("نهاية السياسة لا يمكن أن تسبق بدايتها.");
        String scopeId = "GLOBAL".equals(scopeType) ? null : request.scopeId();
        List<WorkforceAdvancePolicy> versions = policyRepository.findAllByOrderByScopeTypeAscScopeIdAsc().stream()
                .filter(item -> item.getScopeType().equals(scopeType) && java.util.Objects.equals(item.getScopeId(), scopeId))
                .toList();
        int version = versions.stream().mapToInt(WorkforceAdvancePolicy::getVersion).max().orElse(0) + 1;
        versions.stream().filter(item -> item.isActive() && item.getEffectiveTo() == null
                && !effectiveFrom.isBefore(item.getEffectiveFrom())).forEach(item -> item.closeBefore(effectiveFrom));
        WorkforceAdvancePolicy policy = new WorkforceAdvancePolicy(scopeType, scopeId, request.deductionMode(),
                request.deductionFrequency(), request.maxDeductionPercent(), request.defaultInstallments(),
                request.deferralPeriods(), request.active(), version, effectiveFrom, effectiveTo);
        WorkforceAdvancePolicy saved = policyRepository.save(policy);
        auditService.record("CREATE_VERSION", "ADVANCE_POLICY", saved.getId(), actor,
                "{\"scopeType\":\"" + scopeType + "\",\"version\":" + version
                        + ",\"effectiveFrom\":\"" + effectiveFrom + "\"}", null);
        return mapPolicy(saved);
    }

    private WorkforceAdvancePolicy effectivePolicy(String workerId, LocalDate date) {
        List<WorkforceAdvancePolicy> policies = policyRepository.findAllByOrderByScopeTypeAscScopeIdAsc().stream()
                .filter(item -> item.isEffectiveOn(date)).toList();
        if (workerId != null && !workerId.isBlank()) {
            var workerPolicy = policies.stream().filter(item -> "WORKER".equals(item.getScopeType()) && workerId.equals(item.getScopeId()))
                    .max(java.util.Comparator.comparingInt(WorkforceAdvancePolicy::getVersion));
            if (workerPolicy.isPresent()) return workerPolicy.get();
            String categoryId = workerRepository.findById(workerId).map(Worker::getCategoryId).orElse(null);
            var categoryPolicy = policies.stream().filter(item -> "CATEGORY".equals(item.getScopeType()) && java.util.Objects.equals(categoryId, item.getScopeId()))
                    .max(java.util.Comparator.comparingInt(WorkforceAdvancePolicy::getVersion));
            if (categoryPolicy.isPresent()) return categoryPolicy.get();
        }
        return policies.stream().filter(item -> "GLOBAL".equals(item.getScopeType()))
                .max(java.util.Comparator.comparingInt(WorkforceAdvancePolicy::getVersion)).orElse(null);
    }

    @Transactional(readOnly = true)
    public WorkforceApi.AdvancePolicyResponse effectivePolicy(String workerId, String date) {
        LocalDate effectiveDate;
        try { effectiveDate = LocalDate.parse(date); }
        catch (Exception exception) { throw new BusinessRuleException("تاريخ السياسة غير صالح."); }
        WorkforceAdvancePolicy policy = effectivePolicy(workerId, effectiveDate);
        if (policy == null) throw new BusinessRuleException("لا توجد سياسة سلف فعالة لهذا العامل في التاريخ المحدد.");
        return mapPolicy(policy);
    }

    private WorkforceApi.AdvancePolicyResponse mapPolicy(WorkforceAdvancePolicy policy) {
        String scopeName = switch (policy.getScopeType()) {
            case "WORKER" -> workerRepository.findById(policy.getScopeId()).map(Worker::getFullName).orElse("—");
            case "CATEGORY" -> categoryRepository.findById(policy.getScopeId()).map(WorkerCategory::getName).orElse("—");
            default -> "الإعداد العام";
        };
        return new WorkforceApi.AdvancePolicyResponse(policy.getId(), policy.getScopeType(), policy.getScopeId(), scopeName,
                policy.getDeductionMode(), policy.getDeductionFrequency(), policy.getMaxDeductionPercent(),
                policy.getDefaultInstallments(), policy.getDeferralPeriods(), policy.getVersion(),
                policy.getEffectiveFrom().toString(), policy.getEffectiveTo() == null ? null : policy.getEffectiveTo().toString(),
                policy.isActive(), policy.getUpdatedAt().toEpochMilli());
    }

    private String valueOrDefault(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }

    @Transactional
    public WorkforceApi.AdvanceResponse pause(String id, String user) {
        WorkforceAdvance adv = advanceRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("السلفة غير موجودة"));
        adv.pause();
        auditService.record("PAUSE", "ADVANCE", adv.getId(), user, "Paused advance deductions", null);
        return mapToResponse(advanceRepository.save(adv));
    }

    @Transactional
    public WorkforceApi.AdvanceResponse resume(String id, String user) {
        WorkforceAdvance adv = advanceRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("السلفة غير موجودة"));
        adv.resume();
        auditService.record("RESUME", "ADVANCE", adv.getId(), user, "Resumed advance deductions", null);
        return mapToResponse(advanceRepository.save(adv));
    }

    @Transactional
    public WorkforceApi.AdvanceResponse repay(String id, WorkforceApi.AdvanceRepayRequest request, String user) {
        WorkforceAdvance adv = advanceRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("السلفة غير موجودة"));

        BigDecimal amount = request.amount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("يجب أن يكون مبلغ السداد أكبر من صفر");
        }
        if (amount.compareTo(adv.getRemainingBalance()) > 0) {
            throw new BusinessRuleException("مبلغ السداد لا يمكن أن يتجاوز الرصيد المتبقي (" + adv.getRemainingBalance() + " ج.م)");
        }
        if ("FULL".equalsIgnoreCase(request.repaymentType()) && amount.compareTo(adv.getRemainingBalance()) != 0) {
            throw new BusinessRuleException("عند السداد الكامل، يجب أن يساوي المبلغ الرصيد المتبقي (" + adv.getRemainingBalance() + " ج.م)");
        }

        BigDecimal oldBalance = adv.getRemainingBalance();
        adv.repay(amount);

        String notes = request.notes() != null ? request.notes() : "";
        if (request.paymentMethod() != null) notes = "طريقة السداد: " + request.paymentMethod() + " | " + notes;
        if (request.receiptRef() != null) notes = "رقم الإيصال: " + request.receiptRef() + " | " + notes;
        if (request.repaymentDate() != null) notes = "تاريخ السداد: " + request.repaymentDate() + " | " + notes;

        WorkforceAdvanceLedgerEntry entry = new WorkforceAdvanceLedgerEntry(
            adv.getId(), "REPAYMENT", amount, adv.getRemainingBalance(),
            notes, user
        );
        ledgerRepository.save(entry);

        String details = "{\"type\":\"" + request.repaymentType()
            + "\",\"amount\":" + amount
            + ",\"oldBalance\":" + oldBalance
            + ",\"newBalance\":" + adv.getRemainingBalance()
            + ",\"paymentMethod\":\"" + (request.paymentMethod() != null ? request.paymentMethod() : "")
            + "\",\"receiptRef\":\"" + (request.receiptRef() != null ? request.receiptRef() : "")
            + "\"}";
        auditService.record("EARLY_REPAYMENT", "ADVANCE", adv.getId(), user, details, null);

        return mapToResponse(advanceRepository.save(adv));
    }

    private WorkforceApi.AdvanceResponse mapToResponse(WorkforceAdvance a) {
        String workerName = a.getWorkerId() != null ?
            workerRepository.findById(a.getWorkerId()).map(Worker::getFullName).orElse("—") : "—";
        String contractorName = a.getContractorId() != null ?
            contractorRepository.findById(a.getContractorId()).map(Contractor::getName).orElse("—") : "—";

        return new WorkforceApi.AdvanceResponse(
            a.getId(), a.getRecipientType(), a.getWorkerId(), workerName,
            a.getContractorId(), contractorName, a.getAmount(), a.getTermType(),
            a.getTotalInstallments(), a.getInstallmentAmount(), a.getRemainingBalance(),
            a.getDeductionFrequency(), a.getMaxDeductionPercent(), a.getStatus(),
            a.getReason(), a.getFirstInstallmentDate(), a.getDeductionMode(), a.getDeferralPeriods(),
            a.getAppliedPolicyId(), a.getAppliedPolicyVersion(), a.getAppliedPolicySnapshot(),
            a.getCreatedAt().toEpochMilli()
        );
    }
}
