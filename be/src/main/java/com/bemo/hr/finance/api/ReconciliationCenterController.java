package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.SubledgerReconciliationService;
import com.bemo.hr.finance.application.close.SubledgerReconciliationProvider;
import com.bemo.hr.finance.domain.reconciliation.SubledgerReconciliationReport;
import com.bemo.hr.finance.infrastructure.FiscalPeriodRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/reconciliation-center")
public class ReconciliationCenterController {

    private final SubledgerReconciliationService reconciliationService;
    private final List<SubledgerReconciliationProvider> providers;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final ObjectMapper objectMapper;

    public ReconciliationCenterController(SubledgerReconciliationService reconciliationService,
                                          List<SubledgerReconciliationProvider> providers,
                                          FiscalPeriodRepository fiscalPeriodRepository,
                                          ObjectMapper objectMapper) {
        this.reconciliationService = reconciliationService;
        this.providers = providers;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.objectMapper = objectMapper;
    }

    public record ReconciliationDomainSummary(
            String domainKey,
            String subledgerType,
            BigDecimal subledgerBalance,
            BigDecimal glBalance,
            BigDecimal varianceAmount,
            boolean isBalanced,
            int discrepancyCount,
            String status
    ) {}

    public record DiscrepancyDetailItem(
            String documentId,
            String documentNumber,
            BigDecimal subledgerAmount,
            BigDecimal glAmount,
            BigDecimal variance,
            String discrepancyReason
    ) {}

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT') or @auth.hasPermission('finance:reconciliation:read')")
    public ResponseEntity<List<ReconciliationDomainSummary>> getOverview(
            @RequestParam(required = false) String periodId) {

        String activePeriodId = resolvePeriodId(periodId);
        List<ReconciliationDomainSummary> summaries = new ArrayList<>();

        SubledgerReconciliationReport.SubledgerType[] types = SubledgerReconciliationReport.SubledgerType.values();
        for (SubledgerReconciliationReport.SubledgerType type : types) {
            SubledgerReconciliationProvider provider = providers.stream()
                    .filter(p -> p.type() == type)
                    .findFirst()
                    .orElse(null);

            if (provider != null && activePeriodId != null) {
                try {
                    var calc = provider.calculate(activePeriodId, LocalDate.now());
                    int discrepancies = calc.sourceDifferences() != null ? calc.sourceDifferences().size() : 0;
                    summaries.add(new ReconciliationDomainSummary(
                            type.name().toLowerCase(),
                            type.name(),
                            calc.subledgerBalance() != null ? calc.subledgerBalance() : BigDecimal.ZERO,
                            calc.glBalance() != null ? calc.glBalance() : BigDecimal.ZERO,
                            calc.difference() != null ? calc.difference().abs() : BigDecimal.ZERO,
                            calc.isBalanced(),
                            discrepancies,
                            calc.isBalanced() ? "BALANCED" : "VARIANCE"
                    ));
                } catch (Exception ex) {
                    summaries.add(new ReconciliationDomainSummary(
                            type.name().toLowerCase(),
                            type.name(),
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            true,
                            0,
                            "NOT_CONFIGURED"
                    ));
                }
            } else {
                summaries.add(new ReconciliationDomainSummary(
                        type.name().toLowerCase(),
                        type.name(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        true,
                        0,
                        "READY"
                ));
            }
        }

        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/drilldown")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT') or @auth.hasPermission('finance:reconciliation:read')")
    public ResponseEntity<List<DiscrepancyDetailItem>> getDrilldown(
            @RequestParam String subledgerType,
            @RequestParam(required = false) String periodId) {

        String activePeriodId = resolvePeriodId(periodId);
        SubledgerReconciliationReport.SubledgerType type = SubledgerReconciliationReport.SubledgerType.valueOf(subledgerType.toUpperCase());

        SubledgerReconciliationProvider provider = providers.stream()
                .filter(p -> p.type() == type)
                .findFirst()
                .orElse(null);

        if (provider == null || activePeriodId == null) {
            return ResponseEntity.ok(List.of());
        }

        var calc = provider.calculate(activePeriodId, LocalDate.now());
        if (calc == null || calc.sourceDifferences() == null) {
            return ResponseEntity.ok(List.of());
        }

        List<DiscrepancyDetailItem> items = calc.sourceDifferences().stream()
                .map(d -> new DiscrepancyDetailItem(
                        d.documentId(),
                        d.documentNumber() != null ? d.documentNumber() : d.documentId(),
                        d.subledgerAmount() != null ? d.subledgerAmount() : BigDecimal.ZERO,
                        d.glAmount() != null ? d.glAmount() : BigDecimal.ZERO,
                        (d.subledgerAmount() != null ? d.subledgerAmount() : BigDecimal.ZERO)
                                .subtract(d.glAmount() != null ? d.glAmount() : BigDecimal.ZERO),
                        d.glAmount() == null || d.glAmount().signum() == 0
                                ? "UNPOSTED_SUBLEDGER_DOCUMENT"
                                : "AMOUNT_MISMATCH_WITH_GL"
                ))
                .toList();

        return ResponseEntity.ok(items);
    }

    private String resolvePeriodId(String periodId) {
        if (periodId != null && !periodId.isBlank()) {
            return periodId;
        }
        var periods = fiscalPeriodRepository.findAllByOrderByFiscalYearDescPeriodNumberAsc();
        return periods.isEmpty() ? null : periods.get(0).getId();
    }
}
