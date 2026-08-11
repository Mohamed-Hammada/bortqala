package com.bemo.hr.finance.application.close;

import java.util.Optional;

public interface ModuleCloseProvider {

    String getModuleName();

    boolean isPeriodCloseReady(String periodId);

    Optional<String> getBlockerReason(String periodId);

    void executeClose(String periodId);
}
