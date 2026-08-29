package com.bemo.hr.trade.sales.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.sales.api.SalesTargetApi;
import com.bemo.hr.trade.sales.domain.CommissionRule;
import com.bemo.hr.trade.sales.domain.SalesCommissionPayout;
import com.bemo.hr.trade.sales.domain.SalesTarget;
import com.bemo.hr.trade.sales.infrastructure.CommissionRuleRepository;
import com.bemo.hr.trade.sales.infrastructure.SalesTargetRepository;
import com.bemo.hr.trade.sales.infrastructure.SalesCommissionPayoutRepository;
import com.bemo.hr.trade.sales.infrastructure.CustomerInvoiceRepository;
import com.bemo.hr.trade.sales.infrastructure.CustomerReceiptRepository;
import com.bemo.hr.shared.i18n.TranslationService;
import com.bemo.hr.reporting.application.ExcelExportOptions;
import com.bemo.hr.reporting.infrastructure.ExcelExportSupport;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SalesTargetService {

    private final SalesTargetRepository targetRepository;
    private final CommissionRuleRepository ruleRepository;
    private final CustomerInvoiceRepository invoiceRepository;
    private final CustomerReceiptRepository receiptRepository;
    private final SalesCommissionPayoutRepository payoutRepository;
    private final TranslationService translationService;

    @Transactional
    public SalesTargetApi.TargetResponse createTarget(SalesTargetApi.TargetRequest req, String appId) {
        if (targetRepository.existsByScopeAndTargetRefIdAndPeriod(
                SalesTarget.Scope.valueOf(req.scope()), req.targetRefId(), req.period())) {
            throw new BusinessRuleException("TARGET_DUPLICATE");
        }
        SalesTarget target = new SalesTarget(
                UUID.randomUUID().toString(), appId,
                SalesTarget.Scope.valueOf(req.scope()),
                req.targetRefId(), req.period(),
                SalesTarget.Metric.valueOf(req.metric()),
                req.targetValue());
        target = targetRepository.save(target);
        return toTargetResponse(target, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<SalesTargetApi.TargetResponse> listTargets(String period, String appId) {
        List<SalesTarget> targets = period != null
                ? targetRepository.findByPeriod(period)
                : targetRepository.findAll();
        List<SalesTargetApi.TargetResponse> result = new ArrayList<>();
        for (SalesTarget t : targets) {
            BigDecimal achieved = computeAchieved(t);
            result.add(toTargetResponse(t, achieved));
        }
        return result;
    }

    @Transactional
    public void deleteTarget(String id) {
        targetRepository.deleteById(id);
    }

    @Transactional
    public SalesTargetApi.CommissionRuleResponse createRule(SalesTargetApi.CommissionRuleRequest req, String appId) {
        if (ruleRepository.existsByNameIgnoreCaseAndActiveTrue(req.name())) {
            throw new BusinessRuleException("RULE_OVERLAP");
        }
        CommissionRule rule = new CommissionRule(
                UUID.randomUUID().toString(), appId, req.name(),
                CommissionRule.Basis.valueOf(req.basis()),
                req.percent(), req.minAmount(), req.active(),
                req.validFrom(), req.validTo());
        rule = ruleRepository.save(rule);
        return toRuleResponse(rule);
    }

    @Transactional(readOnly = true)
    public List<SalesTargetApi.CommissionRuleResponse> listRules() {
        return ruleRepository.findByActiveTrue().stream().map(this::toRuleResponse).toList();
    }

    @Transactional
    public void deleteRule(String id) {
        ruleRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public SalesTargetApi.CommissionStatementResponse computeStatement(String repId, String period) {
        List<CommissionRule> rules = ruleRepository.findByActiveTrue();
        long periodStart = LocalDate.parse(period + "-01").atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        long periodEnd = LocalDate.parse(period + "-01").plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        List<SalesTargetApi.CommissionStatementEntry> entries = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CommissionRule rule : rules) {
            if (rule.getValidFrom() != null && rule.getValidFrom() > periodEnd) continue;
            if (rule.getValidTo() != null && rule.getValidTo() < periodStart) continue;

            BigDecimal basisAmount = computeBasisAmount(rule, repId, periodStart, periodEnd);
            if (basisAmount.compareTo(rule.getMinAmount()) < 0) continue;

            BigDecimal commission = basisAmount.multiply(rule.getPercent())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            entries.add(new SalesTargetApi.CommissionStatementEntry(
                    rule.getId(), rule.getName(), basisAmount,
                    rule.getPercent(), commission));
            total = total.add(commission);
        }

        SalesCommissionPayout existing = payoutRepository.findByRepIdAndPeriod(repId, period).orElse(null);
        return new SalesTargetApi.CommissionStatementResponse(
                repId, period, entries, total,
                existing != null, existing != null ? existing.getSentAt().toEpochMilli() : null);
    }

    @Transactional
    public SalesTargetApi.PayrollSendResponse sendToPayroll(String repId, String period, String appId, String username) {
        var existing = payoutRepository.findByRepIdAndPeriod(repId, period);
        if (existing.isPresent()) {
            return new SalesTargetApi.PayrollSendResponse(
                    repId, period, existing.get().getTotalCommission(), true,
                    existing.get().getSentAt().toEpochMilli());
        }
        SalesTargetApi.CommissionStatementResponse statement = computeStatement(repId, period);
        SalesCommissionPayout payout = payoutRepository.save(new SalesCommissionPayout(
                UUID.randomUUID().toString(), appId, repId, period, statement.totalCommission(),
                username == null || username.isBlank() ? "system" : username));
        return new SalesTargetApi.PayrollSendResponse(
                repId, period, payout.getTotalCommission(), false,
                payout.getSentAt().toEpochMilli());
    }

    public byte[] exportStatement(String repId, String period, String locale) {
        var options = new ExcelExportOptions(locale, null);
        var messages = ExcelExportSupport.messages(translationService, options);
        SalesTargetApi.CommissionStatementResponse statement = computeStatement(repId, period);
        var rows = statement.entries().stream().<List<?>>map(e -> List.of(
                e.ruleName(), e.basisAmount(), e.percent() + "%", e.commissionAmount())).toList();
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = ExcelExportSupport.sheet(workbook,
                    ExcelExportSupport.text(messages, "export.sheet.commissions"), options.rightToLeft());
            var headers = List.of("rule", "basisAmount", "percent", "commission")
                    .stream().map(key -> ExcelExportSupport.text(messages, "export.column." + key)).toList();
            ExcelExportSupport.writeHeader(sheet, headers);
            var styles = ExcelExportSupport.styles(workbook);
            int rowIndex = 1;
            if (rows.isEmpty()) {
                ExcelExportSupport.writeRow(sheet, rowIndex, List.of("", BigDecimal.ZERO, "", BigDecimal.ZERO), styles);
                rowIndex++;
            }
            for (var values : rows) ExcelExportSupport.writeRow(sheet, rowIndex++, values, styles);
            ExcelExportSupport.finishTable(sheet, rowIndex - 1, headers.size(), "CommissionStatementTable", options);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create commission statement workbook.", exception);
        }
    }

    private BigDecimal computeBasisAmount(CommissionRule rule, String repId,
                                         long periodStart, long periodEnd) {
        if (rule.getBasis() == CommissionRule.Basis.INVOICE_TOTAL) {
            return invoiceRepository.findAllByOrderByInvoiceDateDescCreatedAtDesc().stream()
                    .filter(inv -> inv.getStatus() != com.bemo.hr.trade.sales.domain.CustomerInvoice.Status.DRAFT)
                    .map(com.bemo.hr.trade.sales.domain.CustomerInvoice::getInvoicedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            return receiptRepository.findAllByOrderByReceiptDateDescCreatedAtDesc().stream()
                    .map(com.bemo.hr.trade.sales.domain.CustomerReceipt::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    private BigDecimal computeAchieved(SalesTarget target) {
        if (target.getMetric() == SalesTarget.Metric.REVENUE) {
            return invoiceRepository.findAllByOrderByInvoiceDateDescCreatedAtDesc().stream()
                    .filter(inv -> inv.getStatus() != com.bemo.hr.trade.sales.domain.CustomerInvoice.Status.DRAFT)
                    .map(com.bemo.hr.trade.sales.domain.CustomerInvoice::getInvoicedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return BigDecimal.ZERO;
    }

    private SalesTargetApi.TargetResponse toTargetResponse(SalesTarget t, BigDecimal achieved) {
        return new SalesTargetApi.TargetResponse(
                t.getId(), t.getScope().name(), t.getTargetRefId(),
                t.getPeriod(), t.getMetric().name(), t.getTargetValue(),
                achieved, t.getVersion());
    }

    private SalesTargetApi.CommissionRuleResponse toRuleResponse(CommissionRule r) {
        return new SalesTargetApi.CommissionRuleResponse(
                r.getId(), r.getName(), r.getBasis().name(),
                r.getPercent(), r.getMinAmount(), r.isActive(),
                r.getValidFrom(), r.getValidTo(), r.getVersion());
    }
}
