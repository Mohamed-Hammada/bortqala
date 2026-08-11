package com.bemo.hr.workforce.application;

import com.bemo.hr.finance.application.close.ModuleCloseProvider;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WorkforceCloseProvider implements ModuleCloseProvider {

    @Override
    public String getModuleName() {
        return "WORKFORCE";
    }

    @Override
    public boolean isPeriodCloseReady(String periodId) {
        return true;
    }

    @Override
    public Optional<String> getBlockerReason(String periodId) {
        return Optional.empty();
    }

    @Override
    public void executeClose(String periodId) {
        // Workforce period close execution
    }
}
