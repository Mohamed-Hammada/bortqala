package com.bemo.hr.attendance.application;

import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.reporting.api.ReportingApi;
import com.bemo.hr.reporting.application.ReportingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Generates attendance reports for months affected by a biometric import.
 * Runs after the import transaction commits so the punch evidence is visible
 * and the import response is not blocked by report computation.
 */
@Component
@RequiredArgsConstructor
public class BiometricImportReportListener {
    private static final Logger log = LoggerFactory.getLogger(BiometricImportReportListener.class);

    private final ReportingService reportingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onImportCompleted(BiometricImportCompletedEvent event) {
        if (event.affectedMonths().isEmpty()) return;
        for (var period : event.affectedMonths()) {
            try {
                reportingService.create(
                        new ReportingApi.CreateRequest(
                                period.atDay(1),
                                period.atEndOfMonth(),
                                PayCycle.MONTHLY),
                        event.actor());
            } catch (Exception ex) {
                // Log but do not propagate — the import itself succeeded; report
                // generation failures should not surface as import errors.
                log.warn("Post-import report generation failed for {}: {}", period, ex.getMessage());
            }
        }
    }
}
