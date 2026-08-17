package com.bemo.hr.finance.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.finance.domain.EffectiveMasterValue;
import com.bemo.hr.finance.infrastructure.EffectiveMasterValueRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class EffectiveMasterDataService {
    private final EffectiveMasterValueRepository repo;
    private final AuditService audit;

    public EffectiveMasterDataService(EffectiveMasterValueRepository r, AuditService a) {
        repo = r;
        audit = a;
    }

    @Transactional
    public EffectiveMasterValue add(String type, String id, String key, String value, LocalDate from, LocalDate to, String reason, String actor) {
        log.debug("add called with type={}, id={}, key={}, from={}, to={}", type, id, key, from, to);
        if (reason == null || reason.isBlank()) {
            log.warn("Validation failed: reason is required");
            throw err("MASTER_VALUE_REASON_REQUIRED");
        }
        if (from == null || (to != null && to.isBefore(from))) {
            log.warn("Validation failed: effective date range is invalid");
            throw err("MASTER_VALUE_RANGE_INVALID");
        }
        List<EffectiveMasterValue> history = repo.findByMasterTypeAndMasterIdAndValueKeyOrderByEffectiveFromDesc(type, id, key);
        boolean overlap = history.stream().anyMatch(v -> !before(to, v.getEffectiveFrom()) && !before(v.getEffectiveTo(), from));
        if (overlap) {
            log.warn("Validation failed: effective date range overlaps with existing entry");
            throw err("MASTER_VALUE_RANGE_OVERLAP");
        }
        EffectiveMasterValue saved = repo.save(new EffectiveMasterValue(type, id, key, value, from, to, reason.strip(), actor));
        audit.record("EFFECTIVE_VALUE_ADD", type, id, actor, "{\"key\":\"" + key + "\",\"from\":\"" + from + "\"}", null);
        log.info("EffectiveMasterValue {} added successfully for type={}", saved.getId(), type);
        return saved;
    }

    @Transactional(readOnly = true)
    public EffectiveMasterValue resolve(String type, String id, String key, LocalDate date) {
        log.debug("resolve called with type={}, id={}, key={}, date={}", type, id, key, date);
        return repo.findByMasterTypeAndMasterIdAndValueKeyOrderByEffectiveFromDesc(type, id, key).stream().filter(v -> !date.isBefore(v.getEffectiveFrom()) && (v.getEffectiveTo() == null || !date.isAfter(v.getEffectiveTo()))).findFirst().orElseThrow(() -> err("MASTER_VALUE_NOT_FOUND"));
    }

    @Transactional(readOnly = true)
    public List<EffectiveMasterValue> history(String t, String i, String k) {
        log.debug("history called with type={}, id={}, key={}", t, i, k);
        return repo.findByMasterTypeAndMasterIdAndValueKeyOrderByEffectiveFromDesc(t, i, k);
    }

    private boolean before(LocalDate a, LocalDate b) {
        return a != null && a.isBefore(b);
    }

    private BusinessRuleException err(String code) {
        return new BusinessRuleException(code, code, HttpStatus.CONFLICT);
    }
}
