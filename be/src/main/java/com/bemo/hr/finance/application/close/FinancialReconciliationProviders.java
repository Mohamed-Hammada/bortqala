package com.bemo.hr.finance.application.close;

import com.bemo.hr.finance.domain.reconciliation.SubledgerReconciliationReport;
import com.bemo.hr.finance.infrastructure.FiscalPeriodRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class FinancialReconciliationProviders {
    @Bean SubledgerReconciliationProvider apReconciliation(JdbcTemplate jdbc, FiscalPeriodRepository periods) {
        return provider(SubledgerReconciliationReport.SubledgerType.AP, jdbc, periods,
                "select id, invoice_number, (base_net_amount-paid_amount) from supplier_invoices where app_id=? and invoice_date<=? and status<>'CANCELLED'",
                "SUPPLIER_INVOICE", -1);
    }
    @Bean SubledgerReconciliationProvider arReconciliation(JdbcTemplate jdbc, FiscalPeriodRepository periods) {
        return provider(SubledgerReconciliationReport.SubledgerType.AR, jdbc, periods,
                "select id, invoice_number, outstanding_amount from customer_invoices where app_id=? and invoice_date<=? and status<>'DRAFT'",
                "CUSTOMER_INVOICE", 1);
    }
    @Bean SubledgerReconciliationProvider inventoryReconciliation(JdbcTemplate jdbc, FiscalPeriodRepository periods) {
        return new SubledgerReconciliationProvider() {
            public SubledgerReconciliationReport.SubledgerType type() {
                return SubledgerReconciliationReport.SubledgerType.INVENTORY;
            }

            public ReconciliationCalculation calculate(String periodId, LocalDate ignored) {
                LocalDate end = periods.findById(periodId).orElseThrow().getEndDate();
                LocalDateTime cutoff = end.plusDays(1).atStartOfDay();
                String tenant = TenantContext.require();
                String inventoryAccountId = jdbc.query(
                        "select inventory_account_id from inventory_valuation_policies where app_id=?",
                        (rs, n) -> rs.getString(1), tenant).stream().findFirst().orElse(null);
                List<InventorySource> sourceRows = new ArrayList<>();
                sourceRows.addAll(jdbc.query(
                        "select id,movement_id,value_effect,journal_entry_id from inventory_movement_costs where app_id=? and occurred_at<?",
                        (rs, n) -> new InventorySource(rs.getString(1), rs.getString(2), rs.getBigDecimal(3), rs.getString(4)),
                        tenant, cutoff));
                sourceRows.addAll(jdbc.query(
                        "select id,operation_id,value_difference,journal_entry_id from inventory_revaluations where app_id=? and occurred_at<?",
                        (rs, n) -> new InventorySource(rs.getString(1), rs.getString(2), rs.getBigDecimal(3), rs.getString(4)),
                        tenant, cutoff));
                BigDecimal subledger = sourceRows.stream().map(InventorySource::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                List<SourceDifference> differences = sourceRows.stream().map(source -> {
                    BigDecimal sourceGl = inventoryJournalAmount(jdbc, tenant, source.journalId(), inventoryAccountId);
                    return new SourceDifference(source.id(), source.reference(), source.amount(), sourceGl);
                }).filter(source -> source.subledgerAmount().compareTo(source.glAmount()) != 0).toList();
                BigDecimal gl = sourceRows.stream()
                        .map(source -> inventoryJournalAmount(jdbc, tenant, source.journalId(), inventoryAccountId))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal difference = gl.subtract(subledger);
                return new ReconciliationCalculation(type(), gl, subledger, difference,
                        difference.signum() == 0, differences);
            }
        };
    }
    @Bean SubledgerReconciliationProvider treasuryReconciliation(JdbcTemplate jdbc, FiscalPeriodRepository periods) {
        return provider(SubledgerReconciliationReport.SubledgerType.TREASURY, jdbc, periods,
                "select id, statement_reference, closing_balance from bank_statements where app_id=? and period_end<=?",
                "BANK_STATEMENT", 1);
    }

    private SubledgerReconciliationProvider provider(SubledgerReconciliationReport.SubledgerType type, JdbcTemplate jdbc,
            FiscalPeriodRepository periods, String sourceSql, String sourceType, int sign) {
        return provider(type, jdbc, periods, sourceSql, sourceType, sign, false);
    }
    private SubledgerReconciliationProvider provider(SubledgerReconciliationReport.SubledgerType type, JdbcTemplate jdbc,
            FiscalPeriodRepository periods, String sourceSql, String sourceType, int sign, boolean repeatedTenant) {
        return new SubledgerReconciliationProvider() {
            public SubledgerReconciliationReport.SubledgerType type() { return type; }
            public ReconciliationCalculation calculate(String periodId, LocalDate ignored) {
                LocalDate end = periods.findById(periodId).orElseThrow().getEndDate();
                String tenant = TenantContext.require();
                Object[] args = repeatedTenant ? new Object[]{tenant, tenant, end} : new Object[]{tenant, end};
                List<SourceDifference> sources = jdbc.query(sourceSql, (rs, n) -> new SourceDifference(
                        rs.getString(1), rs.getString(2), rs.getBigDecimal(3), BigDecimal.ZERO), args);
                BigDecimal sub = sources.stream().map(SourceDifference::subledgerAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal gl = jdbc.queryForObject("select coalesce(sum((l.debit-l.credit)*?),0) from journal_source_metadata m join journal_entries j on j.id=m.journal_id join journal_entry_lines l on l.journal_entry_id=j.id where m.app_id=? and m.source_document_type=? and j.status='POSTED' and j.entry_date<=?",
                        BigDecimal.class, sign, tenant, sourceType, end);
                if (gl == null) gl = BigDecimal.ZERO;
                BigDecimal difference = gl.subtract(sub);
                BigDecimal finalGl = gl;
                List<SourceDifference> differences = sources.stream().filter(s -> s.subledgerAmount().signum() != 0)
                        .map(s -> new SourceDifference(s.documentId(), s.documentNumber(), s.subledgerAmount(),
                                sourceGl(jdbc, tenant, sourceType, s.documentId(), end, sign))).filter(s -> s.subledgerAmount().compareTo(s.glAmount()) != 0).toList();
                return new ReconciliationCalculation(type, finalGl, sub, difference, difference.signum() == 0, differences);
            }
        };
    }
    private BigDecimal sourceGl(JdbcTemplate jdbc, String tenant, String type, String id, LocalDate end, int sign) {
        BigDecimal value = jdbc.queryForObject("select coalesce(sum((l.debit-l.credit)*?),0) from journal_source_metadata m join journal_entries j on j.id=m.journal_id join journal_entry_lines l on l.journal_entry_id=j.id where m.app_id=? and m.source_document_type=? and m.source_document_id=? and j.status='POSTED' and j.entry_date<=?",
                BigDecimal.class, sign, tenant, type, id, end);
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal inventoryJournalAmount(JdbcTemplate jdbc, String tenant, String journalId, String accountId) {
        if (journalId == null || accountId == null) return BigDecimal.ZERO;
        BigDecimal value = jdbc.queryForObject(
                "select coalesce(sum(l.debit-l.credit),0) from journal_entries j join journal_entry_lines l on l.journal_entry_id=j.id where j.app_id=? and j.id=? and j.status='POSTED' and l.account_id=?",
                BigDecimal.class, tenant, journalId, accountId);
        return value == null ? BigDecimal.ZERO : value;
    }

    private record InventorySource(String id, String reference, BigDecimal amount, String journalId) {}
}
