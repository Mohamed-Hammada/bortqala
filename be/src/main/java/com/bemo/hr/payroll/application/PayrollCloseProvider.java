package com.bemo.hr.payroll.application;

import com.bemo.hr.finance.application.close.ModuleCloseProvider;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PayrollCloseProvider implements ModuleCloseProvider {

    @Override
    public String getModuleName() {
        return "PAYROLL";
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
        // Payroll period close execution
    }
}
