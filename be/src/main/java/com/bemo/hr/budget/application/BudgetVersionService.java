package com.bemo.hr.budget.application;

import com.bemo.hr.budget.domain.BudgetVersion;
import com.bemo.hr.budget.infrastructure.BudgetVersionRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class BudgetVersionService {

    private final BudgetVersionRepository repository;

    public BudgetVersionService(BudgetVersionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public BudgetVersion createVersion(String versionCode, String name, int fiscalYear) {
        log.debug("createVersion called with versionCode={}, name={}, fiscalYear={}", versionCode, name, fiscalYear);
        BudgetVersion version = new BudgetVersion(versionCode, name, fiscalYear);
        BudgetVersion saved = repository.save(version);
        log.info("Budget version {} created successfully", saved.getId());
        return saved;
    }

    @Transactional
    public BudgetVersion activateVersion(String id) {
        log.debug("activateVersion called with id={}", id);
        BudgetVersion version = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Budget version not found", "BUDGET_VERSION_NOT_FOUND", HttpStatus.NOT_FOUND));

        List<BudgetVersion> existing = repository.findByFiscalYear(version.getFiscalYear());
        for (BudgetVersion v : existing) {
            if (v.getStatus() == BudgetVersion.Status.ACTIVE) {
                v.supersede();
                repository.save(v);
            }
        }

        version.activate();
        BudgetVersion saved = repository.save(version);
        log.info("Budget version {} activated successfully", id);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<BudgetVersion> getVersionsForYear(int fiscalYear) {
        log.debug("getVersionsForYear called with fiscalYear={}", fiscalYear);
        return repository.findByFiscalYear(fiscalYear);
    }
}
