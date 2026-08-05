package com.bemo.hr.workforce;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.api.TransitionResponse;
import com.bemo.hr.shared.api.WorkflowTransitions;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
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
    private final WorkforceSettlementIssueRepository issueRepository;
    private final ManualAttendanceEntryRepository attendanceRepository;
    private final WorkerRepository workerRepository;
    private final ContractorRepository contractorRepository;
    private final WorkforceAdvanceRepository advanceRepository;
    private final WorkforceAdvancePolicyRepository advancePolicyRepository;
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
        
        // Fix P1-9: Load all ACTIVE workers (or ones with entries)
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
        contractorSettlementRepository.deleteAll(contractorSettlementRepository.findByPeriodId(periodId));

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
            if (workerSettlements.isEmpty() && !"fixed_period_amount".equalsIgnoreCase(contractor.getAccountingModel())) continue;
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
                    gross = rateTotal; payable = gross;
                }
                case "worker_cost_plus_fee" -> {
                    gross = workersNet;
                    commission = "percentage".equalsIgnoreCase(contractor.getFeeType())
                            ? workersNet.multiply(contractor.getFeeValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                            : contractor.getFeeValue();
                    if (commission == null) commission = BigDecimal.ZERO;
                    payable = gross.add(commission);
                }
                case "fixed_period_amount" -> { gross = contractor.getFixedPeriodAmount(); if (gross == null) gross = BigDecimal.ZERO; payable = gross; }
                default -> { gross = workersNet; payable = gross; }
            }
            netContractorsPayable = netContractorsPayable.add(payable);
            contractorSettlementRepository.save(new ContractorSettlement(periodId, contractor.getId(), model,
                    workersNet, rateTotal, commission, contractor.getFixedPeriodAmount(), BigDecimal.ZERO,
                    BigDecimal.ZERO, gross, payable, BigDecimal.ZERO, "CALCULATED"));
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
        if (period.getResultErrorCount() > 0) throw new BusinessRuleException("يجب معالجة أخطاء التسوية قبل الاعتماد.", "SETTL_ERRORS_MUST_BE_RESOLVED", HttpStatus.CONFLICT);
        period.setStatus("APPROVED");
        auditService.record("APPROVE", "WORKFORCE_SETTLEMENT_PERIOD", periodId, actor(),
                "{\"version\":" + period.getCalculationVersion() + "}", null);
        periodRepository.save(period);
        return transition("APPROVED", period);
    }

    @Transactional
    public TransitionResponse lockPeriod(String periodId) {
        WorkforceSettlementPeriod period = requirePeriod(periodId);
        if (!"APPROVED".equals(period.getStatus())) throw new BusinessRuleException("لا يمكن قفل الفترة قبل اعتمادها.", "SETTL_LOCK_BEFORE_APPROVAL", HttpStatus.CONFLICT);
        period.setStatus("LOCKED");
        auditService.record("LOCK", "WORKFORCE_SETTLEMENT_PERIOD", periodId, actor(),
                "{\"version\":" + period.getCalculationVersion() + "}", null);
        periodRepository.save(period);
        return transition("LOCKED", period);
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
        if (needsRecalculation(period)) throw new BusinessRuleException("تغيرت بيانات الحضور أو الأسعار أو السياسات؛ أعد الاحتساب أولاً.", "SETTL_STALE_RECALCULATION_REQUIRED", HttpStatus.CONFLICT);
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

    private WorkforceApi.SettlementIssueResponse mapIssue(WorkforceSettlementIssue issue) {
        return new WorkforceApi.SettlementIssueResponse(issue.getId(), issue.getWorkerId(), issue.getWorkerName(),
                issue.getSeverity(), issue.getCode(), issue.getMessage());
    }

    private boolean needsRecalculation(WorkforceSettlementPeriod period) {
        if (period.getCalculationVersion() == 0 || period.getInputFingerprint() == null) return true;
        var entries = attendanceRepository.findByWorkDateBetween(period.getStartDate(), period.getEndDate());
        var entriesByWorker = entries.stream().collect(Collectors.groupingBy(ManualAttendanceEntry::getWorkerId));
        
        // P1-9: Same worker loading logic for the fingerprint check
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

    private Long epoch(Instant instant) { return instant == null ? null : instant.toEpochMilli(); }
    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? "خطأ غير متوقع أثناء الاحتساب." : current.getMessage();
    }
    private String json(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
