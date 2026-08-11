package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.procurement.domain.ProcurementMatchOverride;
import com.bemo.hr.trade.procurement.infrastructure.ProcurementMatchOverrideRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcurementMatchOverrideService {

    private final ProcurementMatchOverrideRepository repository;

    public ProcurementMatchOverrideService(ProcurementMatchOverrideRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ProcurementMatchOverride approveOverride(String matchId, String overrideReason, String approvedBy) {
        ProcurementMatchOverride override = repository.findByMatchId(matchId)
                .orElseGet(() -> new ProcurementMatchOverride(matchId, overrideReason, approvedBy));
        return repository.save(override);
    }

    @Transactional(readOnly = true)
    public ProcurementMatchOverride getOverride(String matchId) {
        return repository.findByMatchId(matchId)
                .orElseThrow(() -> new BusinessRuleException("Procurement match override not found", "MATCH_OVERRIDE_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
