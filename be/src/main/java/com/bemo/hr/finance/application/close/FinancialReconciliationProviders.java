package com.bemo.hr.finance.application.close;

import com.bemo.hr.finance.application.close.SubledgerReconciliationProvider.SourceDifference;
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

    @Bean
    SubledgerReconciliationProvider apReconciliation(
            JdbcTemplate jdbc,
            FiscalPeriodRepository periods,
            FinancialControlAccountResolver accounts) {
        return new SubledgerReconciliationProvider() {
            public SubledgerReconciliationReport.SubledgerType type() {
                return SubledgerReconciliationReport.SubledgerType.AP;
            }

            public ReconciliationCalculation calculate(String periodId, LocalDate ignored) {
                LocalDate end = periods.findById(periodId).orElseThrow().getEndDate();
                String tenant = TenantContext.require();
                String control = accounts.fixedControlAccount("SUPPLIER_INVOICE_RECORDED", end, "CREDIT");

                List<SourceDifference> sources = jdbc.query("""
                                select i.id,
                                       coalesce(i.invoice_number, i.internal_reference),
                                       i.base_net_amount - coalesce((
                                           select sum(p.amount * i.exchange_rate)
                                           from supplier_payments p
                                           where p.app_id=i.app_id
                                             and p.supplier_invoice_id=i.id
                                             and p.payment_date<=?
                                       ),0)
                                from supplier_invoices i
                                where i.app_id=? and i.invoice_date<=? and i.status<>'CANCELLED'
                                """,
                        (rs, n) -> new SourceDifference(
                                rs.getString(1), rs.getString(2), rs.getBigDecimal(3), BigDecimal.ZERO),
                        end, tenant, end);

                BigDecimal subledger = totalSources(sources);
                BigDecimal gl = accountBalance(jdbc, tenant, control, end, -1);
                List<SourceDifference> differences = sources.stream()
                        .map(source -> new SourceDifference(
                                source.documentId(),
                                source.documentNumber(),
                                source.subledgerAmount(),
                                apSourceGl(jdbc, tenant, source.documentId(), control, end)))
                        .filter(source -> source.subledgerAmount().compareTo(source.glAmount()) != 0)
                        .toList();

                BigDecimal difference = gl.subtract(subledger);
                return new ReconciliationCalculation(type(), gl, subledger, difference,
                        difference.signum() == 0, differences);
            }
        };
    }

    @Bean
    SubledgerReconciliationProvider arReconciliation(
            JdbcTemplate jdbc,
            FiscalPeriodRepository periods,
            FinancialControlAccountResolver accounts) {
        return new SubledgerReconciliationProvider() {
            public SubledgerReconciliationReport.SubledgerType type() {
                return SubledgerReconciliationReport.SubledgerType.AR;
            }

            public ReconciliationCalculation calculate(String periodId, LocalDate ignored) {
                LocalDate end = periods.findById(periodId).orElseThrow().getEndDate();
                String tenant = TenantContext.require();
                String control = accounts.fixedControlAccount("CUSTOMER_INVOICE_ISSUED", end, "DEBIT");

                List<SourceDifference> sources = jdbc.query("""
                                select i.id,
                                       i.invoice_number,
                                       i.amount
                                         + coalesce((select sum(ca.amount)
                                                     from customer_credit_notes ca
                                                     where ca.app_id=i.app_id and ca.invoice_id=i.id),0)
                                         - coalesce((select sum(c.amount)
                                                     from customer_credit_notes c
                                                     where c.app_id=i.app_id and c.invoice_id=i.id
                                                       and c.credit_date<=?),0)
                                         - coalesce((select sum(a.amount)
                                                     from customer_receipt_allocations a
                                                     join customer_receipts r on r.id=a.receipt_id
                                                     where a.app_id=i.app_id and a.invoice_id=i.id
                                                       and r.app_id=i.app_id and r.receipt_date<=?),0)
                                from customer_invoices i
                                where i.app_id=? and i.invoice_date<=? and i.status<>'DRAFT'
                                """,
                        (rs, n) -> new SourceDifference(
                                rs.getString(1), rs.getString(2), rs.getBigDecimal(3), BigDecimal.ZERO),
                        end, end, tenant, end);

                BigDecimal subledger = totalSources(sources);
                BigDecimal gl = accountBalance(jdbc, tenant, control, end, 1);
                List<SourceDifference> differences = sources.stream()
                        .map(source -> new SourceDifference(
                                source.documentId(),
                                source.documentNumber(),
                                source.subledgerAmount(),
                                arSourceGl(jdbc, tenant, source.documentId(), control, end)))
                        .filter(source -> source.subledgerAmount().compareTo(source.glAmount()) != 0)
                        .toList();

                BigDecimal difference = gl.subtract(subledger);
                return new ReconciliationCalculation(type(), gl, subledger, difference,
                        difference.signum() == 0, differences);
            }
        };
    }

    @Bean
    SubledgerReconciliationProvider inventoryReconciliation(
            JdbcTemplate jdbc,
            FiscalPeriodRepository periods) {
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
                        (rs, n) -> new InventorySource(
                                rs.getString(1), rs.getString(2), rs.getBigDecimal(3), rs.getString(4)),
                        tenant, cutoff));
                sourceRows.addAll(jdbc.query(
                        "select id,operation_id,value_difference,journal_entry_id from inventory_revaluations where app_id=? and occurred_at<?",
                        (rs, n) -> new InventorySource(
                                rs.getString(1), rs.getString(2), rs.getBigDecimal(3), rs.getString(4)),
                        tenant, cutoff));

                BigDecimal subledger = sourceRows.stream()
                        .map(InventorySource::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal gl = inventoryAccountId == null
                        ? BigDecimal.ZERO
                        : accountBalance(jdbc, tenant, inventoryAccountId, end, 1);

                List<SourceDifference> differences = sourceRows.stream()
                        .map(source -> new SourceDifference(
                                source.id(),
                                source.reference(),
                                source.amount(),
                                inventoryJournalAmount(jdbc, tenant, source.journalId(), inventoryAccountId)))
                        .filter(source -> source.subledgerAmount().compareTo(source.glAmount()) != 0)
                        .toList();

                BigDecimal difference = gl.subtract(subledger);
                return new ReconciliationCalculation(type(), gl, subledger, difference,
                        difference.signum() == 0, differences);
            }
        };
    }

    @Bean
    SubledgerReconciliationProvider treasuryReconciliation(
            JdbcTemplate jdbc,
            FiscalPeriodRepository periods) {
        // Intentionally left on the legacy source-linked behavior until a real
        // bank-statement -> GL bank-account mapping is defined.
        return genericSourceProvider(
                SubledgerReconciliationReport.SubledgerType.TREASURY,
                jdbc,
                periods,
                "select id, statement_reference, closing_balance from bank_statements where app_id=? and period_end<=?",
                "BANK_STATEMENT",
                1);
    }

    private SubledgerReconciliationProvider genericSourceProvider(
            SubledgerReconciliationReport.SubledgerType type,
            JdbcTemplate jdbc,
            FiscalPeriodRepository periods,
            String sourceSql,
            String sourceType,
            int sign) {
        return new SubledgerReconciliationProvider() {
            public SubledgerReconciliationReport.SubledgerType type() {
                return type;
            }

            public ReconciliationCalculation calculate(String periodId, LocalDate ignored) {
                LocalDate end = periods.findById(periodId).orElseThrow().getEndDate();
                String tenant = TenantContext.require();
                List<SourceDifference> sources = jdbc.query(sourceSql,
                        (rs, n) -> new SourceDifference(
                                rs.getString(1), rs.getString(2), rs.getBigDecimal(3), BigDecimal.ZERO),
                        tenant, end);
                BigDecimal subledger = totalSources(sources);
                BigDecimal gl = jdbc.queryForObject("""
                                select coalesce(sum((l.debit-l.credit)*?),0)
                                from journal_source_metadata m
                                join journal_entries j on j.id=m.journal_id
                                join journal_entry_lines l on l.journal_entry_id=j.id
                                where m.app_id=? and m.source_document_type=?
                                  and j.status in ('POSTED','REVERSED') and j.entry_date<=?
                                """,
                        BigDecimal.class, sign, tenant, sourceType, end);
                if (gl == null) gl = BigDecimal.ZERO;
                BigDecimal difference = gl.subtract(subledger);
                return new ReconciliationCalculation(type, gl, subledger, difference,
                        difference.signum() == 0, List.of());
            }
        };
    }

    private BigDecimal totalSources(List<SourceDifference> sources) {
        return sources.stream()
                .map(SourceDifference::subledgerAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal accountBalance(
            JdbcTemplate jdbc, String tenant, String accountId, LocalDate end, int sign) {
        BigDecimal value = jdbc.queryForObject("""
                        select coalesce(sum((l.debit-l.credit)*?),0)
                        from journal_entries j
                        join journal_entry_lines l on l.journal_entry_id=j.id
                        where j.app_id=? and l.account_id=?
                          and j.status in ('POSTED','REVERSED')
                          and j.entry_date<=?
                        """,
                BigDecimal.class, sign, tenant, accountId, end);
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal sourceDocumentControlAmount(
            JdbcTemplate jdbc, String tenant, String sourceType, String sourceId,
            String accountId, LocalDate end, int sign) {
        BigDecimal value = jdbc.queryForObject("""
                        select coalesce(sum((l.debit-l.credit)*?),0)
                        from journal_source_metadata m
                        join journal_entries j on j.id=m.journal_id
                        join journal_entry_lines l on l.journal_entry_id=j.id
                        where m.app_id=? and m.source_document_type=? and m.source_document_id=?
                          and j.status in ('POSTED','REVERSED') and j.entry_date<=?
                          and l.account_id=?
                        """,
                BigDecimal.class, sign, tenant, sourceType, sourceId, end, accountId);
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal apSourceGl(
            JdbcTemplate jdbc, String tenant, String invoiceId, String accountId, LocalDate end) {
        BigDecimal invoice = sourceDocumentControlAmount(
                jdbc, tenant, "SUPPLIER_INVOICE", invoiceId, accountId, end, -1);
        BigDecimal payments = jdbc.queryForObject("""
                        select coalesce(sum((l.debit-l.credit)*-1),0)
                        from supplier_payments p
                        join journal_source_metadata m
                          on m.source_document_type='SUPPLIER_PAYMENT' and m.source_document_id=p.id
                        join journal_entries j on j.id=m.journal_id
                        join journal_entry_lines l on l.journal_entry_id=j.id
                        where p.app_id=? and p.supplier_invoice_id=?
                          and m.app_id=? and j.status in ('POSTED','REVERSED')
                          and j.entry_date<=? and l.account_id=?
                        """,
                BigDecimal.class, tenant, invoiceId, tenant, end, accountId);
        return invoice.add(payments == null ? BigDecimal.ZERO : payments);
    }

    private BigDecimal arSourceGl(
            JdbcTemplate jdbc, String tenant, String invoiceId, String accountId, LocalDate end) {
        BigDecimal invoice = sourceDocumentControlAmount(
                jdbc, tenant, "CUSTOMER_INVOICE", invoiceId, accountId, end, 1);

        BigDecimal receipts = jdbc.queryForObject("""
                        select coalesce(sum(l.debit-l.credit),0)
                        from customer_receipt_allocations a
                        join customer_receipts r on r.id=a.receipt_id
                        join journal_source_metadata m
                          on m.source_document_type='CUSTOMER_RECEIPT' and m.source_document_id=r.id
                        join journal_entries j on j.id=m.journal_id
                        join journal_entry_lines l on l.journal_entry_id=j.id
                        where a.app_id=? and a.invoice_id=? and r.app_id=?
                          and m.app_id=? and j.status in ('POSTED','REVERSED')
                          and j.entry_date<=? and l.account_id=?
                        """,
                BigDecimal.class, tenant, invoiceId, tenant, tenant, end, accountId);

        BigDecimal credits = jdbc.queryForObject("""
                        select coalesce(sum(l.debit-l.credit),0)
                        from customer_credit_notes c
                        join journal_source_metadata m
                          on m.source_document_type='CUSTOMER_CREDIT_NOTE' and m.source_document_id=c.id
                        join journal_entries j on j.id=m.journal_id
                        join journal_entry_lines l on l.journal_entry_id=j.id
                        where c.app_id=? and c.invoice_id=? and m.app_id=?
                          and j.status in ('POSTED','REVERSED')
                          and j.entry_date<=? and l.account_id=?
                        """,
                BigDecimal.class, tenant, invoiceId, tenant, end, accountId);

        return invoice
                .add(receipts == null ? BigDecimal.ZERO : receipts)
                .add(credits == null ? BigDecimal.ZERO : credits);
    }

    private BigDecimal inventoryJournalAmount(
            JdbcTemplate jdbc, String tenant, String journalId, String accountId) {
        if (journalId == null || accountId == null) return BigDecimal.ZERO;
        BigDecimal value = jdbc.queryForObject("""
                        select coalesce(sum(l.debit-l.credit),0)
                        from journal_entries j
                        join journal_entry_lines l on l.journal_entry_id=j.id
                        where j.app_id=? and j.id=?
                          and j.status in ('POSTED','REVERSED')
                          and l.account_id=?
                        """,
                BigDecimal.class, tenant, journalId, accountId);
        return value == null ? BigDecimal.ZERO : value;
    }

    private record InventorySource(String id, String reference, BigDecimal amount, String journalId) {
    }
}
