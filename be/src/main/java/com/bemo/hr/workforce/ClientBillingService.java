package com.bemo.hr.workforce;

import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.reporting.infrastructure.ExcelExportSupport;
import com.bemo.hr.reporting.application.ExcelExportOptions;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.i18n.TranslationService;
import com.bemo.hr.trade.sales.application.SalesReceivablesService;
import com.bemo.hr.trade.sales.domain.CustomerInvoice;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Manpower client billing: generate a monthly draft billing from approved
 * workforce attendance, price it with effective-dated client rates, confirm it
 * into a single AR delivery invoice and report the gross margin against the
 * settled wage cost of the same approved period windows.
 */
@Service
public class ClientBillingService {

    private static final BigDecimal RATE_ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final List<String> APPROVED_STATUSES = List.of("APPROVED", "LOCKED");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ClientWorkerRateRepository rateRepository;
    private final ClientBillingPeriodRepository periodRepository;
    private final ClientBillingDraftLineRepository lineRepository;
    private final WorkerRepository workerRepository;
    private final WorkerCategoryRepository categoryRepository;
    private final ManualAttendanceEntryRepository attendanceRepository;
    private final WorkforceSettlementPeriodRepository settlementPeriodRepository;
    private final ContractorSettlementRepository contractorSettlementRepository;
    private final ContractorSettlementLineRepository settlementLineRepository;
    private final BusinessPartyRepository partyRepository;
    private final SalesReceivablesService salesReceivablesService;
    private final TranslationService translationService;

    public ClientBillingService(ClientWorkerRateRepository rateRepository,
                                ClientBillingPeriodRepository periodRepository,
                                ClientBillingDraftLineRepository lineRepository,
                                WorkerRepository workerRepository,
                                WorkerCategoryRepository categoryRepository,
                                ManualAttendanceEntryRepository attendanceRepository,
                                WorkforceSettlementPeriodRepository settlementPeriodRepository,
                                ContractorSettlementRepository contractorSettlementRepository,
                                ContractorSettlementLineRepository settlementLineRepository,
                                BusinessPartyRepository partyRepository,
                                SalesReceivablesService salesReceivablesService,
                                TranslationService translationService) {
        this.rateRepository = rateRepository;
        this.periodRepository = periodRepository;
        this.lineRepository = lineRepository;
        this.workerRepository = workerRepository;
        this.categoryRepository = categoryRepository;
        this.attendanceRepository = attendanceRepository;
        this.settlementPeriodRepository = settlementPeriodRepository;
        this.contractorSettlementRepository = contractorSettlementRepository;
        this.settlementLineRepository = settlementLineRepository;
        this.partyRepository = partyRepository;
        this.salesReceivablesService = salesReceivablesService;
        this.translationService = translationService;
    }

    // ------------------------------------------------------------------
    // Client rates
    // ------------------------------------------------------------------

    @Transactional
    public ClientBillingApi.RateResponse addRate(ClientBillingApi.CreateRateRequest request) {
        requireClient(request.clientPartyId());
        requireCategory(request.workerCategoryId());
        if (request.effectiveTo() != null && request.effectiveTo().compareTo(request.effectiveFrom()) < 0) {
            throw error("CLIENT_RATE_INVALID", HttpStatus.BAD_REQUEST);
        }
        List<ClientWorkerRate> existing = rateRepository
                .findByClientPartyIdAndWorkerCategoryId(request.clientPartyId(), request.workerCategoryId());
        for (ClientWorkerRate rate : existing) {
            if (overlaps(request.effectiveFrom(), request.effectiveTo(), rate.getEffectiveFrom(), rate.getEffectiveTo())) {
                throw error("CLIENT_RATE_OVERLAP", HttpStatus.CONFLICT);
            }
        }
        ClientWorkerRate saved = rateRepository.save(new ClientWorkerRate(
                UUID.randomUUID().toString(), request.clientPartyId(), request.workerCategoryId(),
                request.dayRate(), request.effectiveFrom(), request.effectiveTo(),
                actor() == null ? "system" : actor()));
        return toRateResponse(saved);
    }

    @Transactional
    public void deleteRate(String rateId) {
        ClientWorkerRate rate = rateRepository.findById(rateId)
                .orElseThrow(() -> error("CLIENT_RATE_NOT_FOUND", HttpStatus.NOT_FOUND));
        rateRepository.delete(rate);
    }

    @Transactional(readOnly = true)
    public List<ClientBillingApi.RateResponse> listRates(String clientPartyId) {
        List<ClientWorkerRate> rates = rateRepository.findAll().stream()
                .filter(rate -> clientPartyId == null || clientPartyId.isBlank()
                        || clientPartyId.equals(rate.getClientPartyId()))
                .toList();
        return rates.stream().map(this::toRateResponse).toList();
    }

    // ------------------------------------------------------------------
    // Draft generation & review
    // ------------------------------------------------------------------

    @Transactional
    public ClientBillingApi.BillingReviewResponse generate(ClientBillingApi.GenerateBillingRequest request) {
        requireClient(request.clientPartyId());
        YearMonth month = parseMonth(request.period());
        String windowStart = month.atDay(1).toString();
        String windowEnd = month.atEndOfMonth().toString();

        ClientBillingPeriod period = periodRepository.findByClientPartyIdAndPeriod(request.clientPartyId(), request.period())
                .map(existing -> {
                    if (ClientBillingPeriod.STATUS_INVOICED.equals(existing.getStatus())) {
                        throw error("CLIENT_BILLING_PERIOD_EXISTS", HttpStatus.CONFLICT);
                    }
                    lineRepository.deleteByBillingPeriodId(existing.getId());
                    return existing;
                })
                .orElseGet(() -> periodRepository.save(new ClientBillingPeriod(
                        UUID.randomUUID().toString(), request.clientPartyId(), request.period(),
                        actor() == null ? "system" : actor())));

        List<ClientBillingDraftLine> lines = buildLines(request.clientPartyId(), period.getId(), windowStart, windowEnd);
        lineRepository.saveAll(lines);
        return review(period, lines);
    }

    @Transactional(readOnly = true)
    public ClientBillingApi.BillingReviewResponse review(String clientPartyId, String period) {
        ClientBillingPeriod billing = requirePeriod(clientPartyId, period);
        return review(billing, lineRepository.findByBillingPeriodId(billing.getId()));
    }

    // ------------------------------------------------------------------
    // Confirmation into invoice
    // ------------------------------------------------------------------

    @Transactional
    public ClientBillingApi.ConfirmResponse confirm(String clientPartyId, String period) {
        ClientBillingPeriod billing = requirePeriod(clientPartyId, period);
        if (!ClientBillingPeriod.STATUS_OPEN.equals(billing.getStatus())) {
            throw error("CLIENT_BILLING_PERIOD_NOT_OPEN", HttpStatus.CONFLICT);
        }
        List<ClientBillingDraftLine> lines = lineRepository.findByBillingPeriodId(billing.getId());
        if (lines.isEmpty()) {
            throw error("CLIENT_BILLING_EMPTY", HttpStatus.CONFLICT);
        }
        for (ClientBillingDraftLine line : lines) {
            if (ClientBillingDraftLine.LINE_MISSING_RATE.equals(line.getLineStatus())) {
                throw error("CLIENT_BILLING_UNRESOLVED_LINES", HttpStatus.CONFLICT);
            }
        }
        BigDecimal total = lines.stream()
                .map(ClientBillingDraftLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        LocalDate billingDate = parseMonth(period).atEndOfMonth();
        CustomerInvoice invoice = salesReceivablesService.createAndIssueDeliveryInvoice(
                invoiceNumber(clientPartyId, period), clientPartyId, null, billingDate, "EGP", total, actor());
        billing.markInvoiced(invoice.getId(), invoice.getInvoiceNumber(), total);
        periodRepository.save(billing);
        return new ClientBillingApi.ConfirmResponse(
                billing.getId(), clientPartyId, period, billing.getStatus(),
                invoice.getId(), invoice.getInvoiceNumber(), total);
    }

    // ------------------------------------------------------------------
    // Margin reporting
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ClientBillingApi.MarginReportResponse marginReport(String clientPartyId, String period) {
        ClientBillingPeriod billing = requirePeriod(clientPartyId, period);
        List<ClientBillingDraftLine> lines = lineRepository.findByBillingPeriodId(billing.getId());
        return buildMarginReport(clientPartyId, period, lines);
    }

    @Transactional(readOnly = true)
    public byte[] marginExport(String clientPartyId, String period, String locale) {
        ClientBillingPeriod billing = requirePeriod(clientPartyId, period);
        List<ClientBillingDraftLine> lines = lineRepository.findByBillingPeriodId(billing.getId());
        ClientBillingApi.MarginReportResponse report = buildMarginReport(clientPartyId, period, lines);

        var options = new ExcelExportOptions(locale, null);
        var messages = ExcelExportSupport.messages(translationService, options);
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = ExcelExportSupport.sheet(workbook,
                    ExcelExportSupport.text(messages, "export.sheet.margin"), options.rightToLeft());
            var headers = List.of("worker", "category", "approvedDays", "dayRate", "billed", "wageCost", "margin")
                    .stream().map(key -> ExcelExportSupport.text(messages, "export.column." + key)).toList();
            ExcelExportSupport.writeHeader(sheet, headers);
            var styles = ExcelExportSupport.styles(workbook);
            int rowIndex = 1;
            for (var row : report.rows()) {
                ExcelExportSupport.writeRow(sheet, rowIndex++, List.of(
                        row.fullName(), row.categoryName(), row.approvedDays(), row.dayRate(),
                        row.billedAmount(), row.wageCost(), row.marginAmount()), styles);
            }
            if (report.rows().isEmpty()) {
                ExcelExportSupport.writeRow(sheet, rowIndex++, List.of(
                        "", "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO), styles);
                rowIndex++;
            } else {
                ExcelExportSupport.writeRow(sheet, rowIndex++, List.of(
                        ExcelExportSupport.text(messages, "export.total"), "",
                        BigDecimal.ZERO, BigDecimal.ZERO,
                        report.totalBilled(), report.totalWageCost(), report.totalMargin()), styles);
            }
            ExcelExportSupport.finishTable(sheet, rowIndex - 1, headers.size(), "ClientBillingMarginTable", options);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create client billing margin workbook.", exception);
        }
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private List<ClientBillingDraftLine> buildLines(String clientPartyId, String periodId,
                                                    String windowStart, String windowEnd) {
        List<Worker> workers = workerRepository.findAll();
        List<ClientBillingDraftLine> lines = new ArrayList<>();
        String previousMonthStart = parseMonth(windowStart.substring(0, 7)).minusMonths(1).atDay(1).toString();
        String previousMonthEnd = parseMonth(windowStart.substring(0, 7)).minusMonths(1).atEndOfMonth().toString();
        for (Worker worker : workers) {
            BillingAccumulator current = accumulateForWorker(clientPartyId, worker, windowStart, windowEnd);
            if (!current.hasDays()) continue;
            BillingAccumulator previous = accumulateForWorker(clientPartyId, worker, previousMonthStart, previousMonthEnd);
            BigDecimal wageCost = wageCostForWorker(worker.getId(), windowStart, windowEnd);
            String categoryName = categoryRepository.findById(worker.getCategoryId())
                    .map(WorkerCategory::getName).orElse("");
            String lineStatus = current.missing() ? ClientBillingDraftLine.LINE_MISSING_RATE : ClientBillingDraftLine.LINE_BILLABLE;
            String reason = current.missing()
                    ? "No effective client rate for category '" + categoryName + "' in " + windowStart.substring(0, 7) + "."
                    : null;
            BigDecimal variance = current.missing()
                    ? RATE_ZERO
                    : current.amount().subtract(previous.missing() ? RATE_ZERO : previous.amount())
                            .setScale(2, RoundingMode.HALF_UP);
            lines.add(new ClientBillingDraftLine(
                    UUID.randomUUID().toString(), periodId, worker.getId(), worker.getCode(), worker.getFullName(),
                    worker.getCategoryId(), categoryName,
                    current.days(), rateFor(clientPartyId, worker, windowEnd), current.missing() ? RATE_ZERO : current.amount(),
                    wageCost, variance, lineStatus, reason));
        }
        return lines;
    }

    private BillingAccumulator accumulateForWorker(String clientPartyId, Worker worker,
                                                   String windowStart, String windowEnd) {
        BigDecimal days = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal amount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        boolean missing = false;
        for (WorkforceSettlementPeriod period : approvedPeriods(windowStart, windowEnd)) {
            String from = max(period.getStartDate(), windowStart);
            String to = min(period.getEndDate(), windowEnd);
            for (ManualAttendanceEntry entry : attendanceRepository.findByWorkerIdAndWorkDateBetween(worker.getId(), from, to)) {
                BigDecimal value = entry.getAttendanceValue() == null ? BigDecimal.ZERO : entry.getAttendanceValue();
                days = days.add(value);
                ClientWorkerRate rate = effectiveRate(clientPartyId, worker.getCategoryId(), entry.getWorkDate());
                if (rate == null) {
                    missing = true;
                } else {
                    amount = amount.add(value.multiply(rate.getDayRate()));
                }
            }
        }
        return new BillingAccumulator(days, amount.setScale(2, RoundingMode.HALF_UP), missing);
    }

    private ClientWorkerRate effectiveRate(String clientPartyId, String categoryId, String date) {
        return rateRepository.findByClientPartyIdAndWorkerCategoryId(clientPartyId, categoryId).stream()
                .filter(rate -> date.compareTo(rate.getEffectiveFrom()) >= 0)
                .filter(rate -> rate.getEffectiveTo() == null || date.compareTo(rate.getEffectiveTo()) <= 0)
                .max(Comparator.comparing(ClientWorkerRate::getEffectiveFrom))
                .orElse(null);
    }

    private BigDecimal rateFor(String clientPartyId, Worker worker, String date) {
        ClientWorkerRate rate = effectiveRate(clientPartyId, worker.getCategoryId(), date);
        return rate == null ? null : rate.getDayRate();
    }

    private BigDecimal wageCostForWorker(String workerId, String windowStart, String windowEnd) {
        BigDecimal cost = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (WorkforceSettlementPeriod period : approvedPeriods(windowStart, windowEnd)) {
            for (ContractorSettlement settlement : contractorSettlementRepository.findByPeriodId(period.getId())) {
                for (ContractorSettlementLine line : settlementLineRepository.findBySettlementId(settlement.getId())) {
                    if (workerId.equals(line.getWorkerId()) && line.getGrossWage() != null) {
                        cost = cost.add(line.getGrossWage());
                    }
                }
            }
        }
        return cost.setScale(2, RoundingMode.HALF_UP);
    }

    private List<WorkforceSettlementPeriod> approvedPeriods(String windowStart, String windowEnd) {
        return settlementPeriodRepository.findAll().stream()
                .filter(period -> APPROVED_STATUSES.contains(period.getStatus()))
                .filter(period -> period.getEndDate().compareTo(windowStart) >= 0)
                .filter(period -> period.getStartDate().compareTo(windowEnd) <= 0)
                .toList();
    }

    private ClientBillingApi.BillingReviewResponse review(ClientBillingPeriod billing, List<ClientBillingDraftLine> lines) {
        BigDecimal totalDays = lines.stream()
                .map(ClientBillingDraftLine::getApprovedDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = lines.stream()
                .filter(line -> ClientBillingDraftLine.LINE_BILLABLE.equals(line.getLineStatus()))
                .map(line -> line.getAmount() == null ? BigDecimal.ZERO : line.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalWageCost = lines.stream()
                .map(line -> line.getWageCost() == null ? BigDecimal.ZERO : line.getWageCost())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        return new ClientBillingApi.BillingReviewResponse(
                new ClientBillingApi.BillingPeriodResponse(
                        billing.getId(), billing.getClientPartyId(), billing.getPeriod(), billing.getStatus(),
                        billing.getInvoiceId(), billing.getInvoiceNumber(), billing.getTotalAmount()),
                lines.stream().map(this::toLineResponse).toList(),
                totalDays, totalAmount, totalWageCost);
    }

    private ClientBillingApi.MarginReportResponse buildMarginReport(String clientPartyId, String period,
                                                                    List<ClientBillingDraftLine> lines) {
        List<ClientBillingApi.MarginRowResponse> rows = new ArrayList<>();
        for (ClientBillingDraftLine line : lines) {
            if (!ClientBillingDraftLine.LINE_BILLABLE.equals(line.getLineStatus())) continue;
            BigDecimal billed = line.getAmount() == null ? RATE_ZERO : line.getAmount();
            BigDecimal cost = line.getWageCost() == null ? RATE_ZERO : line.getWageCost();
            rows.add(new ClientBillingApi.MarginRowResponse(
                    line.getWorkerId(), line.getWorkerCode(), line.getFullName(), line.getCategoryName(),
                    line.getApprovedDays(), line.getDayRate(), billed, cost,
                    billed.subtract(cost).setScale(2, RoundingMode.HALF_UP)));
        }
        BigDecimal totalBilled = rows.stream().map(ClientBillingApi.MarginRowResponse::billedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCost = rows.stream().map(ClientBillingApi.MarginRowResponse::wageCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        return new ClientBillingApi.MarginReportResponse(
                clientPartyId, period, totalBilled, totalCost,
                totalBilled.subtract(totalCost).setScale(2, RoundingMode.HALF_UP), rows);
    }

    private String invoiceNumber(String clientPartyId, String period) {
        return "CLB-" + period.replace("-", "") + "-" + clientPartyId.substring(0, 6).toUpperCase();
    }

    private ClientBillingPeriod requirePeriod(String clientPartyId, String period) {
        return periodRepository.findByClientPartyIdAndPeriod(clientPartyId, period)
                .orElseThrow(() -> error("CLIENT_BILLING_PERIOD_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private void requireClient(String clientPartyId) {
        partyRepository.findById(clientPartyId)
                .orElseThrow(() -> error("AR_CUSTOMER_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private void requireCategory(String categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw error("CLIENT_CATEGORY_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
    }

    private static boolean overlaps(String fromA, String toA, String fromB, String toB) {
        if (toA != null && toA.compareTo(fromB) < 0) return false;
        return toB == null || toB.compareTo(fromA) >= 0;
    }

    private static String max(String a, String b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    private static String min(String a, String b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    private YearMonth parseMonth(String period) {
        try {
            return YearMonth.parse(period, MONTH);
        } catch (java.time.format.DateTimeParseException exception) {
            throw error("CLIENT_BILLING_INVALID_PERIOD", HttpStatus.BAD_REQUEST);
        }
    }

    private ClientBillingApi.RateResponse toRateResponse(ClientWorkerRate rate) {
        String categoryName = categoryRepository.findById(rate.getWorkerCategoryId())
                .map(WorkerCategory::getName).orElse(null);
        return new ClientBillingApi.RateResponse(
                rate.getId(), rate.getClientPartyId(), rate.getWorkerCategoryId(), categoryName,
                rate.getDayRate(), rate.getEffectiveFrom(), rate.getEffectiveTo(), rate.getVersion());
    }

    private ClientBillingApi.BillingLineResponse toLineResponse(ClientBillingDraftLine line) {
        return new ClientBillingApi.BillingLineResponse(
                line.getId(), line.getWorkerId(), line.getWorkerCode(), line.getFullName(),
                line.getCategoryId(), line.getCategoryName(), line.getApprovedDays(), line.getDayRate(),
                line.getAmount(), line.getWageCost(), line.getVarianceAmount(), line.getLineStatus(), line.getReason());
    }

    private String actor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }

    private static BusinessRuleException error(String code, HttpStatus status) {
        return new BusinessRuleException(code, code, status);
    }

    private record BillingAccumulator(BigDecimal days, BigDecimal amount, boolean missing) {
        boolean hasDays() {
            return days.compareTo(BigDecimal.ZERO) > 0;
        }
    }
}