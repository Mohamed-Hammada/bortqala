package com.bemo.hr.budget.application;

import com.bemo.hr.budget.domain.BudgetVersion;
import com.bemo.hr.budget.infrastructure.BudgetVersionRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BudgetVersionService {

    private final BudgetVersionRepository repository;

    public BudgetVersionService(BudgetVersionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public BudgetVersion createVersion(String versionCode, String name, int fiscalYear) {
        BudgetVersion version = new BudgetVersion(versionCode, name, fiscalYear);
        return repository.save(version);
    }

    @Transactional
    public BudgetVersion activateVersion(String id) {
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
        return repository.save(version);
    }

    @Transactional(readOnly = true)
    public List<BudgetVersion> getVersionsForYear(int fiscalYear) {
        return repository.findByFiscalYear(fiscalYear);
    }
}
