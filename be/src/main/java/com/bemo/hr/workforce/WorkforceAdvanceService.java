package com.bemo.hr.workforce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final ContractorRepository contractorRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<WorkforceApi.AdvanceResponse> list() {
        return advanceRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public WorkforceApi.AdvanceResponse create(WorkforceApi.AdvanceCreateRequest request, String createdBy) {
        BigDecimal instAmount = request.installmentAmount();
        int count = request.totalInstallments() != null ? Math.max(1, request.totalInstallments()) : 1;
        if (instAmount == null || instAmount.compareTo(BigDecimal.ZERO) == 0) {
            instAmount = request.amount().divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
        }

        WorkforceAdvance adv = new WorkforceAdvance(
            request.recipientType(), request.workerId(), request.contractorId(),
            request.amount(), request.termType(), count, instAmount,
            request.deductionFrequency(), request.maxDeductionPercent(), request.reason(),
            request.firstInstallmentDate(), request.deductionMode(), request.deferralPeriods()
        );
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

        boolean isMonthly = "MONTHLY".equalsIgnoreCase(request.deductionFrequency());
        int deferral = request.deferralPeriods() != null ? request.deferralPeriods() : 0;

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
    public WorkforceApi.AdvanceResponse repay(String id, BigDecimal amount, String user) {
        WorkforceAdvance adv = advanceRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("السلفة غير موجودة"));
        adv.repay(amount);
        auditService.record("EARLY_REPAYMENT", "ADVANCE", adv.getId(), user, "Repaid " + amount, null);
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
            a.getCreatedAt().toEpochMilli()
        );
    }
}
