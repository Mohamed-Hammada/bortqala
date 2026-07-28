package com.bemo.hr.workforce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkforceAdvanceService {
    private final WorkforceAdvanceRepository advanceRepository;
    private final WorkforceAdvanceInstallmentRepository installmentRepository;
    private final WorkforceAdvanceLedgerEntryRepository ledgerRepository;
    private final WorkerRepository workerRepository;
    private final ContractorRepository contractorRepository;

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
            request.deductionFrequency(), request.maxDeductionPercent(), request.reason()
        );
        WorkforceAdvance saved = advanceRepository.save(adv);

        // Generate Installments
        for (int i = 1; i <= count; i++) {
            WorkforceAdvanceInstallment inst = new WorkforceAdvanceInstallment(
                saved.getId(), i, "INSTALLMENT-" + i, instAmount
            );
            installmentRepository.save(inst);
        }

        // Ledger Entry for Issuance
        WorkforceAdvanceLedgerEntry entry = new WorkforceAdvanceLedgerEntry(
            saved.getId(), "ISSUANCE", saved.getAmount(), saved.getRemainingBalance(),
            "Advance issued: " + (request.reason() != null ? request.reason() : ""), createdBy
        );
        ledgerRepository.save(entry);

        return mapToResponse(saved);
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
            a.getReason(), a.getCreatedAt().toEpochMilli()
        );
    }
}
