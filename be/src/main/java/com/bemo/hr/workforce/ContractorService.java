package com.bemo.hr.workforce;

import com.bemo.hr.audit.application.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractorService {
    private final ContractorRepository contractorRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<WorkforceApi.ContractorResponse> list() {
        return contractorRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public WorkforceApi.ContractorResponse getById(String id) {
        return mapToResponse(contractorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contractor not found: " + id)));
    }

    @Transactional
    public WorkforceApi.ContractorResponse create(WorkforceApi.ContractorRequest request) {
        Contractor contractor = new Contractor(
                request.code(), request.name(), request.tradeName(), request.phone(),
                request.secondaryPhone(), request.taxId(), request.address(),
                request.accountingModel(), request.paymentRouting(),
                request.settlementCycleDays() != null ? request.settlementCycleDays() : 15,
                request.defaultDailyRate(), request.feeType(), request.feeValue(),
                request.feeBase(), request.fixedPeriodAmount(), request.status(), request.notes()
        );
        var saved = mapToResponse(contractorRepository.save(contractor));
        auditService.record("CREATE", "CONTRACTOR", saved.id(), currentActor(),
                "{\"code\":\"" + safe(saved.code()) + "\",\"name\":\"" + safe(saved.name()) + "\"}", null);
        return saved;
    }

    @Transactional
    public WorkforceApi.ContractorResponse update(String id, WorkforceApi.ContractorRequest request) {
        Contractor contractor = contractorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contractor not found: " + id));
        contractor.update(
                request.code(), request.name(), request.tradeName(), request.phone(),
                request.secondaryPhone(), request.taxId(), request.address(),
                request.accountingModel(), request.paymentRouting(),
                request.settlementCycleDays() != null ? request.settlementCycleDays() : 15,
                request.defaultDailyRate(), request.feeType(), request.feeValue(),
                request.feeBase(), request.fixedPeriodAmount(), request.status(), request.notes()
        );
        var saved = mapToResponse(contractorRepository.save(contractor));
        auditService.record("UPDATE", "CONTRACTOR", saved.id(), currentActor(),
                "{\"code\":\"" + safe(saved.code()) + "\",\"name\":\"" + safe(saved.name()) + "\"}", null);
        return saved;
    }

    private String currentActor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null && !auth.getName().isBlank()) ? auth.getName() : "system";
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }

    private WorkforceApi.ContractorResponse mapToResponse(Contractor c) {
        return new WorkforceApi.ContractorResponse(
                c.getId(), c.getCode(), c.getName(), c.getTradeName(), c.getPhone(),
                c.getSecondaryPhone(), c.getTaxId(), c.getAddress(), c.getAccountingModel(),
                c.getPaymentRouting(), c.getSettlementCycleDays(), c.getDefaultDailyRate(),
                c.getFeeType(), c.getFeeValue(), c.getFeeBase(), c.getFixedPeriodAmount(),
                c.getStatus(), c.getNotes(), c.getCreatedAt().toEpochMilli(), c.getUpdatedAt().toEpochMilli()
        );
    }
}
