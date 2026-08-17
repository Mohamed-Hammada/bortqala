package com.bemo.hr.workforce;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.operations.PartnerLedgerEntry;
import com.bemo.hr.operations.PartnerLedgerEntryRepository;
import com.bemo.hr.shared.api.TransitionResponse;
import com.bemo.hr.shared.api.WorkflowTransitions;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.idempotency.application.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkforceSettlementService {
    private static final Map<String, List<String>> SETTLEMENT_PERIOD_WORKFLOW = Map.of(
            "DRAFT", List.of("CALCULATE", "DELETE"),
            "CALCULATED", List.of("REVIEW", "RECALCULATE"),
            "REVIEWED", List.of("APPROVE", "RECALCULATE"),
            "APPROVED", List.of("LOCK", "EXPORT"),
            "LOCKED", List.of("EXPORT"));

    private final WorkforceSettlementPeriodRepository periodRepository;
    private final WorkerSettlementRepository workerSettlementRepository;
    private final ContractorSettlementRepository contractorSettlementRepository;
    private final ContractorSettlementLineRepository contractorSettlementLineRepository;
    private final ContractorSettlementAdjustmentRepository contractorSettlementAdjustmentRepository;
    private final WorkforceSettlementIssueRepository issueRepository;
    private final ManualAttendanceEntryRepository attendanceRepository;
    private final WorkerRepository workerRepository;
    private final ContractorRepository contractorRepository;
    private final WorkforceAdvanceRepository advanceRepository;
    private final WorkforceAdvancePolicyRepository advancePolicyRepository;
    private final PartnerLedgerEntryRepository partnerLedgerEntryRepository;
    private final IdempotencyService idempotencyService;
    private final WorkforceExcelExportService excelExportService;
    private final AuditService auditService;
    private final PlatformTransactionManager platformTransactionManager;

    @Transactional(readOnly = true)
    public byte[] exportPeriodExcel(String periodId) {
        try {
            return excelExportService.generatePeriodExcel(periodId);
        } catch (Exception exception) {
            throw new BusinessRuleException("تعذر إنشاء ملف Excel لفترة التسوية: " + exception.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<WorkforceApi.SettlementPeriodResponse> listPeriods() {
        return periodRepository.findAll().stream()
                .sorted(Comparator.comparing(WorkforceSettlementPeriod::getStartDate).reversed())
                .map(this::mapPeriodToResponse).toList();
    }

    @Transactional(readOnly = true)
    public WorkforceApi.SettlementPeriodResponse getPeriod(String periodId) {
        return mapPeriodToResponse(requirePeriod(periodId));
    }

    @Transactional(readOnly = true)
    public List<WorkforceApi.ContractorSettlementDetailResponse> listContractorSettlementsForPeriod(String periodId) {
        return contractorSettlementRepository.findByPeriodId(periodId).stream()
                .map(cs -> mapContractorSettlementToDetail(cs))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkforceApi.ContractorSettlementDetailResponse getContractorSettlement(String settlementId) {
        ContractorSettlement settlement = contractorSettlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessRuleException("تسوية المقاول غير موجودة: " + settlementId, "SETTL_NOT_FOUND", HttpStatus.NOT_FOUND));
        return mapContractorSettlementToDetail(settlement);
    }

    @Transactional
    public WorkforceApi.SettlementPeriodResponse createPeriod(WorkforceApi.SettlementPeriodRequest request) {
        if (request.startDate().compareTo(request.endDate()) > 0) {
            throw new BusinessRuleException("تاريخ بداية فترة التسوية يجب ألا يتجاوز تاريخ النهاية.", "SETTL_START_AFTER_END", HttpStatus.CONFLICT);
        }
        WorkforceSettlementPeriod period = new WorkforceSettlementPeriod(
                request.periodCode(), request.startDate(), request.endDate(), request.cycleType(), "DRAFT");
        WorkforceSettlementPeriod saved = periodRepository.save(period);
        auditService.record("CREATE", "WORKFORCE_SETTLEMENT_PERIOD", saved.getId(), actor(),
                "{\"periodCode\":\"" + saved.getPeriodCode() + "\"}", null);
        return mapPeriodToResponse(saved);
    }

    public WorkforceApi.SettlementCalculationSummary calculatePeriod(String periodId) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        try {
            return transactionTemplate.execute(status -> calculateInTransaction(periodId));
        } catch (RuntimeException exception) {
            transactionTemplate.executeWithoutResult(status -> {
                WorkforceSettlementPeriod period = requirePeriod(periodId);
                period.markCalculationFailed(rootMessage(exception));
                auditService.record("CALCULATE_FAILED", "WORKFORCE_SETTLEMENT_PERIOD", period.getId(), actor(),
                        "{\"reason\":\"" + json(rootMessage(exception)) + "\"}", null);
            });
            throw exception;
        }
    }

    private WorkforceApi.SettlementCalculationSummary calculateInTransaction(String periodId) {
        WorkforceSettlementPeriod period = requirePeriod(periodId);
        if ("LOCKED".equals(period.getStatus()) || "APPROVED".equals(period.getStatus())) {
            throw new BusinessRuleException("لا يمكن إعادة احتساب فترة معتمدة أو مقفلة.", "SETTL_RECALCULATE_APPROVED_LOCKED", HttpStatus.CONFLICT);
        }

        List<ManualAttendanceEntry> entries = attendanceRepository.findByWorkDateBetween(period.getStartDate(), period.getEndDate());
        Map<String, List<ManualAttendanceEntry>> workerEntries = entries.stream()
                .collect(Collectors.groupingBy(ManualAttendanceEntry::getWorkerId));

        List<Worker> allWorkers = workerRepository.findAll().stream()
                .filter(w -> "ACTIVE".equalsIgnoreCase(w.getStatus()) || workerEntries.containsKey(w.getId()))
                .toList();

        var contractorIds = allWorkers.stream().map(Worker::getContractorId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        List<Contractor> contractors = contractorIds.isEmpty() ? java.util.Collections.emptyList() : contractorRepository.findAllById(contractorIds);
        String fingerprint = inputFingerprint(entries, allWorkers);
        int nextVersion = period.getCalculationVersion() + 1;
        List<WorkforceSettlementIssue> issues = new ArrayList<>();

        BigDecimal totalAttendanceUnits = BigDecimal.ZERO;
        BigDecimal grossWorkersAmount = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalAdvanceDeductions = BigDecimal.ZERO;
        BigDecimal netWorkersAmount = BigDecimal.ZERO;

        workerSettlementRepository.deleteAll(workerSettlementRepository.findByPeriodId(periodId));
        List<ContractorSettlement> existingContractorSettlements = contractorSettlementRepository.findByPeriodId(periodId);
        for (ContractorSettlement cs : existingContractorSettlements) {
            contractorSettlementLineRepository.deleteBySettlementId(cs.getId());
            contractorSettlementAdjustmentRepository.deleteBySettlementId(cs.getId());
        }
        contractorSettlementRepository.deleteAll(existingContractorSettlements);

        int calculatedWorkers = 0;
        for (Worker worker : allWorkers) {
            List<ManualAttendanceEntry> list = workerEntries.get(worker.getId());
            if (list == null || list.isEmpty()) {
                if ("ACTIVE".equalsIgnoreCase(worker.getStatus())) {
                    issues.add(new WorkforceSettlementIssue(periodId, nextVersion, worker.getId(), worker.getFullName(),
                            "WARNING", "NO_ATTENDANCE", "لا توجد سجلات حضور للعامل داخل الفترة."));
                }
                continue;
            }
            if (worker.getDefaultDailyRate() == null || worker.getDefaultDailyRate().signum() <= 0) {
                issues.add(new WorkforceSettlementIssue(periodId, nextVersion, worker.getId(), worker.getFullName(),
                        "ERROR", "MISSING_DAILY_RATE", "سعر اليومية غير صالح ويجب تصحيحه قبل الاعتماد."));
            }
            calculatedWorkers++;
            BigDecimal units = list.stream().map(ManualAttendanceEntry::getAttendanceValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalAttendanceUnits = totalAttendanceUnits.add(units);
            BigDecimal dailyRate = worker.getDefaultDailyRate() == null ? BigDecimal.ZERO : worker.getDefaultDailyRate();
            BigDecimal gross = units.multiply(dailyRate).setScale(2, RoundingMode.HALF_UP);
            grossWorkersAmount = grossWorkersAmount.add(gross);

            BigDecimal advanceDeduction = BigDecimal.ZERO;
            List<WorkforceAdvance> activeAdvances = advanceRepository.findByWorkerId(worker.getId()).stream()
                    .filter(item -> "ACTIVE".equalsIgnoreCase(item.getStatus())).toList();
            for (WorkforceAdvance advance : activeAdvances) {
                BigDecimal maxAllowed = gross.multiply(advance.getMaxDeductionPercent())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                BigDecimal actual = advance.getInstallmentAmount().min(maxAllowed).min(advance.getRemainingBalance());
                advanceDeduction = advanceDeduction.add(actual);
            }
            totalAdvanceDeductions = totalAdvanceDeductions.add(advanceDeduction);
            BigDecimal net = gross.subtract(advanceDeduction).setScale(2, RoundingMode.HALF_UP);
            netWorkersAmount = netWorkersAmount.add(net);
            WorkerSettlement settlement = new WorkerSettlement(periodId, worker.getId(), worker.getContractorId(),
                    units, dailyRate, gross, BigDecimal.ZERO, BigDecimal.ZERO, advanceDeduction, net);
            settlement.applyAdvancePolicySnapshot(activeAdvances.stream().map(advance -> advance.getId() + ":"
                    + (advance.getAppliedPolicyId() == null ? "DIRECT" : advance.getAppliedPolicyId()) + ":v"
                    + (advance.getAppliedPolicyVersion() == null ? 0 : advance.getAppliedPolicyVersion()) + ":"
                    + advance.getMaxDeductionPercent()).collect(Collectors.joining(";")));
            workerSettlementRepository.save(settlement);
        }

        BigDecimal netContractorsPayable = BigDecimal.ZERO;
        int calculatedContractors = 0;
        for (Contractor contractor : contractors) {
            List<WorkerSettlement> workerSettlements = workerSettlementRepository
                    .findByPeriodIdAndContractorId(periodId, contractor.getId());
            if (workerSettlements.isEmpty() && !"fixed_period_amount".equalsIgnoreCase(contractor.getAccountingModel()))
                continue;
            calculatedContractors++;
            BigDecimal workersNet = workerSettlements.stream().map(WorkerSettlement::getNetAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal gross;
            BigDecimal payable;
            BigDecimal commission = BigDecimal.ZERO;
            BigDecimal rateTotal = BigDecimal.ZERO;
            String model = contractor.getAccountingModel() == null ? "worker_net_total" : contractor.getAccountingModel().toLowerCase();
            switch (model) {
                case "contractor_daily_rate" -> {
                    BigDecimal units = workerSettlements.stream().map(WorkerSettlement::getTotalAttendanceUnits)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    rateTotal = units.multiply(contractor.getDefaultDailyRate()).setScale(2, RoundingMode.HALF_UP);
                    gross = rateTotal;
                    payable = gross;
                }
                case "worker_cost_plus_fee" -> {
                    gross = workersNet;
                    commission = "percentage".equalsIgnoreCase(contractor.getFeeType())
                            ? workersNet.multiply(contractor.getFeeValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                            : contractor.getFeeValue();
                    if (commission == null) commission = BigDecimal.ZERO;
                    payable = gross.add(commission);
                }
                case "fixed_period_amount" -> {
                    gross = contractor.getFixedPeriodAmount();
                    if (gross == null) gross = BigDecimal.ZERO;
                    payable = gross;
                }
                default -> {
                    gross = workersNet;
                    payable = gross;
                }
            }
            netContractorsPayable = netContractorsPayable.add(payable);
            ContractorSettlement cs = new ContractorSettlement(periodId, contractor.getId(), model,
                    workersNet, rateTotal, commission, contractor.getFixedPeriodAmount(), BigDecimal.ZERO,
                    BigDecimal.ZERO, gross, payable, BigDecimal.ZERO, "CALCULATED");
            cs = contractorSettlementRepository.save(cs);

            for (WorkerSettlement ws : workerSettlements) {
                Worker workerObj = allWorkers.stream().filter(w -> w.getId().equals(ws.getWorkerId())).findFirst().orElse(null);
                ContractorSettlementLine line = new ContractorSettlementLine(
                        cs.getId(),
                        ws.getWorkerId(),
                        ws.getTotalAttendanceUnits(),
                        ws.getDailyRate(),
                        ws.getGrossAmount(),
                        ws.getOvertimeAmount(),
                        ws.getDeductionsAmount(),
                        ws.getAdvanceDeductions(),
                        ws.getNetAmount(),
                        "{\"workerCode\":\"" + (workerObj != null ? workerObj.getCode() : "") + "\"}"
                );
                contractorSettlementLineRepository.save(line);
            }
        }

        issueRepository.saveAll(issues);
        int warningCount = (int) issues.stream().filter(issue -> "WARNING".equals(issue.getSeverity())).count();
        int errorCount = (int) issues.stream().filter(issue -> "ERROR".equals(issue.getSeverity())).count();
        String currentActor = actor();
        period.markCalculated(currentActor, fingerprint, calculatedWorkers, grossWorkersAmount,
                totalDeductions, totalAdvanceDeductions, netWorkersAmount, warningCount, errorCount);
        periodRepository.save(period);
        auditService.record("CALCULATE", "WORKFORCE_SETTLEMENT_PERIOD", period.getId(), currentActor,
                "{\"version\":" + period.getCalculationVersion() + ",\"records\":" + calculatedWorkers
                        + ",\"warnings\":" + warningCount + ",\"errors\":" + errorCount + "}", null);

        return new WorkforceApi.SettlementCalculationSummary(period.getId(), period.getPeriodCode(), calculatedWorkers,
                calculatedContractors, totalAttendanceUnits, grossWorkersAmount, totalDeductions,
                totalAdvanceDeductions, netWorkersAmount, netContractorsPayable, period.getStatus(),
                period.getCalculationVersion(), period.getLastCalculatedAt().toEpochMilli(), currentActor,
                warningCount, errorCount, issues.stream().map(this::mapIssue).toList());
    }

    @Transactional(readOnly = true)
    public List<WorkforceApi.SettlementIssueResponse> listIssues(String periodId) {
        WorkforceSettlementPeriod period = requirePeriod(periodId);
        return issueRepository.findByPeriodIdAndCalculationVersionOrderBySeverityDescWorkerNameAsc(
                periodId, period.getCalculationVersion()).stream().map(this::mapIssue).toList();
    }

    @Transactional
    public TransitionResponse reviewPeriod(String periodId) {
        WorkforceSettlementPeriod period = requireFreshCalculated(periodId, "CALCULATED");
        period.setStatus("REVIEWED");
        auditService.record("REVIEW", "WORKFORCE_SETTLEMENT_PERIOD", periodId, actor(),
                "{\"version\":" + period.getCalculationVersion() + "}", null);
        periodRepository.save(period);
        return transition("REVIEWED", period);
    }

    @Transactional
    public TransitionResponse approvePeriod(String periodId) {
        WorkforceSettlementPeriod period = requireFreshCalculated(periodId, "REVIEWED");
        if (period.getResultErrorCount() > 0)
            throw new BusinessRuleException("يجب معالجة أخطاء التسوية قبل الاعتماد.", "SETTL_ERRORS_MUST_BE_RESOLVED", HttpStatus.CONFLICT);
        period.setStatus("APPROVED");
        auditService.record("APPROVE", "WORKFORCE_SETTLEMENT_PERIOD", periodId, actor(),
                "{\"version\":" + period.getCalculationVersion() + "}", null);
        periodRepository.save(period);

        for (ContractorSettlement cs : contractorSettlementRepository.findByPeriodId(periodId)) {
            cs.updatePaidAmount(BigDecimal.ZERO);
        }
        return transition("APPROVED", period);
    }

    @Transactional
    public TransitionResponse lockPeriod(String periodId) {
        WorkforceSettlementPeriod period = requirePeriod(periodId);
        if (!"APPROVED".equals(period.getStatus()))
            throw new BusinessRuleException("لا يمكن قفل الفترة قبل اعتمادها.", "SETTL_LOCK_BEFORE_APPROVAL", HttpStatus.CONFLICT);
        period.setStatus("LOCKED");
        auditService.record("LOCK", "WORKFORCE_SETTLEMENT_PERIOD", periodId, actor(),
                "{\"version\":" + period.getCalculationVersion() + "}", null);
        periodRepository.save(period);
        return transition("LOCKED", period);
    }

    @Transactional
    public WorkforceApi.ContractorSettlementDetailResponse postSettlement(String settlementId, WorkforceApi.SettlementPostingRequest request) {
        String requestHash = IdempotencyService.hash(settlementId + "|POST_SETTLEMENT|" + request.expectedVersion());
        return idempotencyService.execute("SETTLEMENT_POST", request.operationId(), requestHash,
                () -> postSettlementTransaction(settlementId, request),
                response -> response.id(),
                id -> getContractorSettlement(settlementId));
    }

    private WorkforceApi.ContractorSettlementDetailResponse postSettlementTransaction(String settlementId, WorkforceApi.SettlementPostingRequest request) {
        ContractorSettlement settlement = contractorSettlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessRuleException("تسوية المقاول غير موجودة: " + settlementId, "SETTL_NOT_FOUND", HttpStatus.NOT_FOUND));

        WorkforceSettlementPeriod period = periodRepository.findById(settlement.getPeriodId())
                .orElseThrow(() -> new BusinessRuleException("فترة التسوية غير موجودة", "SETTL_PERIOD_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!"APPROVED".equals(period.getStatus()) && !"LOCKED".equals(period.getStatus())) {
            throw new BusinessRuleException("يجب اعتماد تسوية المقاول قبل الترحيل.", "SETTL_NOT_APPROVED", HttpStatus.CONFLICT);
        }

        if ("POSTED".equals(settlement.getStatus()) || settlement.getPostedJournalEntryId() != null) {
            throw new BusinessRuleException("تم ترحيل هذه التسوية بالفعل للمالية.", "SETTL_ALREADY_POSTED", HttpStatus.CONFLICT);
        }

        String currentActor = actor();
        PartnerLedgerEntry ledgerEntry = new PartnerLedgerEntry(
                settlement.getContractorId(),
                "CREDIT",
                settlement.getNetPayable(),
                "SETTL-" + period.getPeriodCode(),
                "ترحيل مستحقات تسوية المقاول عن الفترة " + period.getPeriodCode(),
                Instant.now(),
                currentActor
        );
        partnerLedgerEntryRepository.save(ledgerEntry);

        settlement.markPosted("JR-SETTL-" + settlement.getId().substring(0, 8).toUpperCase());
        contractorSettlementRepository.save(settlement);

        auditService.record("POST", "CONTRACTOR_SETTLEMENT", settlementId, currentActor,
                "{\"netPayable\":" + settlement.getNetPayable() + "}", null);

        return getContractorSettlement(settlementId);
    }

    @Transactional
    public WorkforceApi.ContractorSettlementDetailResponse linkInvoice(String settlementId, WorkforceApi.LinkInvoiceRequest request) {
        ContractorSettlement settlement = contractorSettlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessRuleException("تسوية المقاول غير موجودة: " + settlementId, "SETTL_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (request.invoiceAmount() != null && request.invoiceAmount().subtract(settlement.getNetPayable()).abs().compareTo(new BigDecimal("100.00")) > 0) {
            throw new BusinessRuleException("قيمة الفاتورة تتجاوز المبلغ المعتمد بالتسوية خارج حد التسامح.", "SETTL_INVOICE_VARIANCE_EXCEEDED", HttpStatus.CONFLICT);
        }

        settlement.linkInvoice(request.invoiceNumber(), Instant.ofEpochMilli(request.invoiceDate()));
        contractorSettlementRepository.save(settlement);

        auditService.record("LINK_INVOICE", "CONTRACTOR_SETTLEMENT", settlementId, actor(),
                "{\"invoiceNumber\":\"" + request.invoiceNumber() + "\"}", null);

        return getContractorSettlement(settlementId);
    }

    @Transactional
    public WorkforceApi.ContractorSettlementDetailResponse markPaid(String settlementId, WorkforceApi.RecordSettlementPaymentRequest request) {
        String requestHash = IdempotencyService.hash(settlementId + "|MARK_PAID|" + request.amount());
        return idempotencyService.execute("SETTLEMENT_PAYMENT", request.operationId(), requestHash,
                () -> markPaidTransaction(settlementId, request),
                response -> response.id(),
                id -> getContractorSettlement(settlementId));
    }

    private WorkforceApi.ContractorSettlementDetailResponse markPaidTransaction(String settlementId, WorkforceApi.RecordSettlementPaymentRequest request) {
        ContractorSettlement settlement = contractorSettlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessRuleException("تسوية المقاول غير موجودة: " + settlementId, "SETTL_NOT_FOUND", HttpStatus.NOT_FOUND));

        BigDecimal remainingPayable = settlement.getNetPayable().subtract(settlement.getPaidAmount());
        if (request.amount().compareTo(remainingPayable) > 0) {
            throw new BusinessRuleException("مبلغ الصرف يجاوز رصيد التسوية المستحق.", "PROC_PAYMENT_AMOUNT_EXCEEDED", HttpStatus.CONFLICT);
        }

        String currentActor = actor();
        settlement.updatePaidAmount(request.amount());
        contractorSettlementRepository.save(settlement);

        PartnerLedgerEntry ledgerEntry = new PartnerLedgerEntry(
                settlement.getContractorId(),
                "DEBIT",
                request.amount(),
                request.paymentReference() != null ? request.paymentReference() : "PAY-" + settlementId.substring(0, 8),
                "صرف دفعة من مستحقات تسوية المقاول",
                request.paymentDate() != null ? Instant.ofEpochMilli(request.paymentDate()) : Instant.now(),
                currentActor
        );
        partnerLedgerEntryRepository.save(ledgerEntry);

        auditService.record("PAYMENT", "CONTRACTOR_SETTLEMENT", settlementId, currentActor,
                "{\"amount\":" + request.amount() + "}", null);

        return getContractorSettlement(settlementId);
    }

    private TransitionResponse transition(String targetStatus, WorkforceSettlementPeriod period) {
        List<String> actions = new ArrayList<>(WorkflowTransitions.allowedActions(targetStatus, SETTLEMENT_PERIOD_WORKFLOW));
        if ("REVIEWED".equals(targetStatus)) {
            actions.add("EXPORT");
        }
        if ("APPROVED".equals(targetStatus) && period.getResultErrorCount() > 0) {
            actions.remove("LOCK");
        }
        return new TransitionResponse(targetStatus, period.getCalculationVersion(), actions);
    }

    private WorkforceSettlementPeriod requireFreshCalculated(String periodId, String requiredStatus) {
        WorkforceSettlementPeriod period = requirePeriod(periodId);
        if (!requiredStatus.equals(period.getStatus())) {
            throw new BusinessRuleException("الحالة الحالية لا تسمح بهذا الإجراء. الحالة المطلوبة: " + requiredStatus);
        }
        if (needsRecalculation(period))
            throw new BusinessRuleException("تغيرت بيانات الحضور أو الأسعار أو السياسات؛ أعد الاحتساب أولاً.", "SETTL_STALE_RECALCULATION_REQUIRED", HttpStatus.CONFLICT);
        return period;
    }

    private WorkforceSettlementPeriod requirePeriod(String periodId) {
        return periodRepository.findById(periodId)
                .orElseThrow(() -> new BusinessRuleException("فترة التسوية غير موجودة: " + periodId));
    }

    private WorkforceApi.SettlementPeriodResponse mapPeriodToResponse(WorkforceSettlementPeriod period) {
        return new WorkforceApi.SettlementPeriodResponse(period.getId(), period.getPeriodCode(), period.getStartDate(),
                period.getEndDate(), period.getCycleType(), period.getStatus(), period.getCalculationVersion(),
                epoch(period.getLastCalculatedAt()), period.getLastCalculatedBy(), epoch(period.getLastCalculationFailedAt()),
                period.getLastCalculationError(), needsRecalculation(period), period.getResultRecordCount(),
                period.getResultGrossAmount(), period.getResultDeductions(), period.getResultAdvances(),
                period.getResultNetAmount(), period.getResultWarningCount(), period.getResultErrorCount(),
                period.getCreatedAt().toEpochMilli(), period.getUpdatedAt().toEpochMilli());
    }

    private WorkforceApi.ContractorSettlementDetailResponse mapContractorSettlementToDetail(ContractorSettlement cs) {
        Contractor contractor = contractorRepository.findById(cs.getContractorId()).orElse(null);
        String contractorName = contractor != null ? contractor.getName() : cs.getContractorId();

        List<WorkforceApi.ContractorSettlementLineResponse> lines = contractorSettlementLineRepository.findBySettlementId(cs.getId()).stream()
                .map(line -> {
                    Worker worker = workerRepository.findById(line.getWorkerId()).orElse(null);
                    return new WorkforceApi.ContractorSettlementLineResponse(
                            line.getId(), line.getSettlementId(), line.getWorkerId(),
                            worker != null ? worker.getFullName() : line.getWorkerId(),
                            line.getAttendanceDays(), line.getDailyWage(), line.getGrossWage(),
                            line.getOvertimeAmount(), line.getDeductionsAmount(), line.getAdvanceInstallments(),
                            line.getNetWage()
                    );
                }).toList();

        List<WorkforceApi.ContractorSettlementAdjustmentResponse> adjustments = contractorSettlementAdjustmentRepository.findBySettlementId(cs.getId()).stream()
                .map(adj -> new WorkforceApi.ContractorSettlementAdjustmentResponse(
                        adj.getId(), adj.getSettlementId(), adj.getAdjustmentType(), adj.getDescription(),
                        adj.getAmount(), adj.getReason(), adj.getCreatedBy(), adj.getCreatedAt().toEpochMilli()
                )).toList();

        return new WorkforceApi.ContractorSettlementDetailResponse(
                cs.getId(), cs.getPeriodId(), cs.getContractorId(), contractorName, cs.getAccountingModel(),
                cs.getWorkersNetTotal(), cs.getContractorRatesTotal(), cs.getCommissionAmount(), cs.getFixedAmount(),
                cs.getAdditionsAmount(), cs.getDeductionsAmount(), cs.getGrossAmount(), cs.getNetPayable(), cs.getPaidAmount(),
                cs.getInvoiceNumber(), cs.getInvoiceDate() != null ? cs.getInvoiceDate().toEpochMilli() : null,
                cs.getPostedJournalEntryId(), cs.getStatus(), cs.getVersion(), lines, adjustments,
                cs.getCreatedAt().toEpochMilli(), cs.getUpdatedAt().toEpochMilli()
        );
    }

    private WorkforceApi.SettlementIssueResponse mapIssue(WorkforceSettlementIssue issue) {
        return new WorkforceApi.SettlementIssueResponse(issue.getId(), issue.getWorkerId(), issue.getWorkerName(),
                issue.getSeverity(), issue.getCode(), issue.getMessage());
    }

    private boolean needsRecalculation(WorkforceSettlementPeriod period) {
        if (period.getCalculationVersion() == 0 || period.getInputFingerprint() == null) return true;
        var entries = attendanceRepository.findByWorkDateBetween(period.getStartDate(), period.getEndDate());
        var entriesByWorker = entries.stream().collect(Collectors.groupingBy(ManualAttendanceEntry::getWorkerId));

        List<Worker> workers = workerRepository.findAll().stream()
                .filter(w -> "ACTIVE".equalsIgnoreCase(w.getStatus()) || entriesByWorker.containsKey(w.getId()))
                .toList();

        return !period.getInputFingerprint().equals(inputFingerprint(entries, workers));
    }

    private String inputFingerprint(List<ManualAttendanceEntry> entries, List<Worker> workers) {
        try {
            StringBuilder source = new StringBuilder();
            entries.stream().sorted(Comparator.comparing(ManualAttendanceEntry::getId)).forEach(entry -> source
                    .append(entry.getId()).append('|').append(entry.getWorkDate()).append('|')
                    .append(entry.getAttendanceValue()).append('|').append(entry.getUpdatedAt()).append(';'));
            workers.stream().sorted(Comparator.comparing(Worker::getId)).forEach(worker -> source
                    .append(worker.getId()).append('|').append(worker.getDefaultDailyRate()).append('|')
                    .append(worker.getContractorId()).append('|').append(worker.getUpdatedAt()).append(';'));
            advanceRepository.findAll().stream().sorted(Comparator.comparing(WorkforceAdvance::getId)).forEach(advance -> source
                    .append(advance.getId()).append('|').append(advance.getRemainingBalance()).append('|')
                    .append(advance.getUpdatedAt()).append(';'));
            advancePolicyRepository.findAll().stream().sorted(Comparator.comparing(WorkforceAdvancePolicy::getId)).forEach(policy -> source
                    .append(policy.getId()).append('|').append(policy.getMaxDeductionPercent()).append('|')
                    .append(policy.getUpdatedAt()).append(';'));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(source.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("تعذر تكوين بصمة مدخلات التسوية.", exception);
        }
    }

    private String actor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }

    private Long epoch(Instant instant) {
        return instant == null ? null : instant.toEpochMilli();
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? "خطأ غير متوقع أثناء الاحتساب." : current.getMessage();
    }

    private String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
