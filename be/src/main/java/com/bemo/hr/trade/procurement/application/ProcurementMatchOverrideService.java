package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.procurement.domain.ProcurementMatchOverride;
import com.bemo.hr.trade.procurement.infrastructure.ProcurementMatchOverrideRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ProcurementMatchOverrideService {

    private final ProcurementMatchOverrideRepository repository;

    public ProcurementMatchOverrideService(ProcurementMatchOverrideRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ProcurementMatchOverride approveOverride(String matchId, String overrideReason, String approvedBy) {
        log.debug("approveOverride called with matchId={}, approvedBy={}", matchId, approvedBy);
        ProcurementMatchOverride override = repository.findByMatchId(matchId)
                .orElseGet(() -> new ProcurementMatchOverride(matchId, overrideReason, approvedBy));
        ProcurementMatchOverride saved = repository.save(override);
        log.info("MatchOverride {} for match {} approved successfully", saved.getId(), matchId);
        return saved;
    }

    @Transactional(readOnly = true)
    public ProcurementMatchOverride getOverride(String matchId) {
        log.debug("getOverride called with matchId={}", matchId);
        return repository.findByMatchId(matchId)
                .orElseThrow(() -> {
                    log.warn("Match override not found for matchId={}", matchId);
                    return new BusinessRuleException("Procurement match override not found", "MATCH_OVERRIDE_NOT_FOUND", HttpStatus.NOT_FOUND);
                });
    }
}
