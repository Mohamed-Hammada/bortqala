package com.bemo.hr.trade.sales.application;

import com.bemo.hr.finance.application.close.ModuleCloseProvider;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SalesCloseProvider implements ModuleCloseProvider {

    @Override
    public String getModuleName() {
        return "SALES";
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
        // Sales/O2C period close execution
    }
}
