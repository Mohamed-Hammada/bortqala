package com.bemo.hr.workforce;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.operations.PartnerLedgerEntry;
import com.bemo.hr.operations.PartnerLedgerEntryRepository;
import com.bemo.hr.project.domain.CostCategory;
import com.bemo.hr.project.domain.CostLedgerEntryType;
import com.bemo.hr.project.domain.ProjectCostLedgerEntry;
import com.bemo.hr.project.infrastructure.ProjectCostLedgerEntryRepository;
import com.bemo.hr.shared.api.TransitionResponse;
import com.bemo.hr.shared.api.WorkflowTransitions;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.idempotency.application.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkforceSettlementService {
    private static final Map<String, List<String>> SETTLEMENT_PERIOD_WORKFLOW = Map.of(
            "DRAFT", List.of("CALCULATE", "DELETE"),
            "CALCULATED", List.of("REVIEW", "RECALCULATE"),
            "REVIEWED", List.of("APPROVE", "RECALCULATE", "EXPORT"),
            "APPROVED", List.of("LOCK", "EXPORT"),
            "LOCKED", List.of("EXPORT"));

    private final WorkforceSettlementPeriodRepository periodRepository;
    private final WorkerSettlementRepository workerSettlementRepository;
    private final ContractorSettlementRepository contractorSettlementRepository;
    private final ContractorSettlementLineRepository contractorSettlementLineRepository;
    private final ContractorSettlementAdjustmentRepository contractorSettlementAdjustmentRepository;
    private final WorkforceSettlementIssueRepository issueRepository;
    private final ManualAttendanceEntryRepository attendanceRepository;
    private final WorkerAssignmentRepository assignmentRepository;
    private final WorkerRepository workerRepository;
    private final ContractorRepository contractorRepository;
    private final WorkforceAdvanceRepository advanceRepository;
    private final WorkforceAdvancePolicyRepository advancePolicyRepository;
    private final PartnerLedgerEntryRepository partnerLedgerEntryRepository;
    private final ProjectCostLedgerEntryRepository projectCostLedgerEntryRepository;
    private final IdempotencyService idempotencyService;
    private final WorkforceExcelExportService excelExportService;
    private final AuditService auditService;
    private final PlatformTransactionManager platformTransactionManager;

    @Transactional(readOnly = true)
    public byte[] exportPeriodExcel(String periodId) {
        try {
            return excelExportService.generatePeriodExcel(periodId);
        } catch (Exception exception) {
            throw new BusinessRuleException("Failed to generate settlement Excel file: " + exception.getMessage(), "SETTLEMENT_EXPORT_FAILED", HttpStatus.CONFLICT);
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
                .map(this::mapContractorSettlementToDetail)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkforceApi.ContractorSettlementDetailResponse getContractorSettlement(String settlementId) {
        ContractorSettlement settlement = contractorSettlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessRuleException("Contractor settlement not found: " + settlementId, "SETTL_NOT_FOUND", HttpStatus.NOT_FOUND));
        return mapContractorSettlementToDetail(settlement);
    }

    @Transactional
    public WorkforceApi.SettlementPeriodResponse createPeriod(WorkforceApi.SettlementPeriodRequest request) {
        if (request.startDate().compareTo(request.endDate()) > 0) {
            throw new BusinessRuleException("Settlement period start date must not be after end date.", "SETTL_START_AFTER_END", HttpStatus.CONFLICT);
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
            throw new BusinessRuleException("Cannot recalculate a locked or approved settlement period.", "SETTL_ALREADY_LOCKED", HttpStatus.CONFLICT);
        }

        var attendanceEntries = attendanceRepository.findByWorkDateBetween(period.getStartDate(), period.getEndDate());
        var attendanceByWorker = attendanceEntries.stream().collect(Collectors.groupingBy(ManualAttendanceEntry::getWorkerId));

        List<Worker> allWorkers = workerRepository.findAll();
        List<Contractor> allContractors = contractorRepository.findAll();
        List<WorkforceAdvance> allAdvances = advanceRepository.findAll();
        WorkforceAdvancePolicy policy = advancePolicyRepository.findAll().stream().findFirst().orElse(null);

        String fingerprint = inputFingerprint(attendanceEntries, allWorkers);

        workerSettlementRepository.deleteAll(workerSettlementRepository.findByPeriodId(periodId));
        List<ContractorSettlement> existingCs = contractorSettlementRepository.findByPeriodId(periodId);
        for (ContractorSettlement cs : existingCs) {
            contractorSettlementLineRepository.deleteBySettlementId(cs.getId());
            contractorSettlementAdjustmentRepository.deleteBySettlementId(cs.getId());
        }
        contractorSettlementRepository.deleteAll(existingCs);

        List<WorkforceSettlementIssue> issues = new ArrayList<>();
        int calculatedWorkers = 0;
        int calculatedContractors = 0;
        BigDecimal totalAttendanceUnits = BigDecimal.ZERO;
        BigDecimal grossWorkersAmount = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalAdvanceDeductions = BigDecimal.ZERO;
        BigDecimal netWorkersAmount = BigDecimal.ZERO;
        BigDecimal netContractorsPayable = BigDecimal.ZERO;

        for (Worker worker : allWorkers) {
            List<ManualAttendanceEntry> workerEntries = attendanceByWorker.getOrDefault(worker.getId(), List.of());
            if (workerEntries.isEmpty() && !"ACTIVE".equalsIgnoreCase(worker.getStatus())) {
                continue;
            }

            BigDecimal units = workerEntries.stream().map(ManualAttendanceEntry::getAttendanceValue).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (units.compareTo(BigDecimal.ZERO) == 0 && !"ACTIVE".equalsIgnoreCase(worker.getStatus())) {
                continue;
            }

            calculatedWorkers++;
            totalAttendanceUnits = totalAttendanceUnits.add(units);

            BigDecimal dailyRate = worker.getDefaultDailyRate() == null ? BigDecimal.ZERO : worker.getDefaultDailyRate();
            if (!workerEntries.isEmpty() && workerEntries.get(0).getEffectiveDailyRate() != null && workerEntries.get(0).getEffectiveDailyRate().compareTo(BigDecimal.ZERO) > 0) {
                dailyRate = workerEntries.get(0).getEffectiveDailyRate();
            }

            BigDecimal gross = units.multiply(dailyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal overtime = workerEntries.stream().map(e -> e.getOvertimeHours() == null ? BigDecimal.ZERO : e.getOvertimeHours())
                    .reduce(BigDecimal.ZERO, BigDecimal::add).multiply(dailyRate.divide(new BigDecimal("8"), 2, RoundingMode.HALF_UP));
            gross = gross.add(overtime);

            BigDecimal deductions = workerEntries.stream().map(e -> e.getDeductionHours() == null ? BigDecimal.ZERO : e.getDeductionHours())
                    .reduce(BigDecimal.ZERO, BigDecimal::add).multiply(dailyRate.divide(new BigDecimal("8"), 2, RoundingMode.HALF_UP));

            BigDecimal advancesDeducted = BigDecimal.ZERO;
            Optional<WorkforceAdvance> activeAdvance = allAdvances.stream()
                    .filter(a -> a.getWorkerId().equals(worker.getId()) && a.getRemainingBalance().compareTo(BigDecimal.ZERO) > 0)
                    .findFirst();

            if (activeAdvance.isPresent()) {
                WorkforceAdvance adv = activeAdvance.get();
                BigDecimal maxDeductible = gross.subtract(deductions);
                if (policy != null && policy.getMaxDeductionPercent() != null) {
                    maxDeductible = maxDeductible.multiply(policy.getMaxDeductionPercent()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                }
                advancesDeducted = adv.getRemainingBalance().min(maxDeductible).max(BigDecimal.ZERO);
            }

            BigDecimal net = gross.subtract(deductions).subtract(advancesDeducted).max(BigDecimal.ZERO);

            grossWorkersAmount = grossWorkersAmount.add(gross);
            totalDeductions = totalDeductions.add(deductions);
            totalAdvanceDeductions = totalAdvanceDeductions.add(advancesDeducted);
            netWorkersAmount = netWorkersAmount.add(net);

            WorkerSettlement ws = new WorkerSettlement(
                    periodId, worker.getId(), worker.getContractorId(),
                    units, dailyRate, gross, overtime, deductions, advancesDeducted, net
            );
            workerSettlementRepository.save(ws);
        }

        // Group by Contractor
        for (Contractor contractor : allContractors) {
            List<WorkerSettlement> workerSettlements = workerSettlementRepository.findByPeriodIdAndContractorId(periodId, contractor.getId());
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

                // Derive project / wbs / costCode from attendance entries or assignments
                String lineProjectId = null;
                String lineWbsNodeId = null;
                String lineCostCodeId = null;
                List<ManualAttendanceEntry> workerAttendance = attendanceByWorker.getOrDefault(ws.getWorkerId(), List.of());
                for (ManualAttendanceEntry mae : workerAttendance) {
                    if (mae.getProjectId() != null) {
                        lineProjectId = mae.getProjectId();
                        lineWbsNodeId = mae.getWbsNodeId();
                        lineCostCodeId = mae.getCostCodeId();
                        break;
                    }
                }
                if (lineProjectId == null) {
                    List<WorkerAssignment> assignments = assignmentRepository.findByWorkerId(ws.getWorkerId());
                    for (WorkerAssignment wa : assignments) {
                        if (wa.getProjectId() != null) {
                            lineProjectId = wa.getProjectId();
                            lineWbsNodeId = wa.getWbsNodeId();
                            lineCostCodeId = wa.getCostCodeId();
                            break;
                        }
                    }
                }

                ContractorSettlementLine line = new ContractorSettlementLine(
                        cs.getId(),
                        ws.getWorkerId(),
                        lineProjectId,
                        lineWbsNodeId,
                        lineCostCodeId,
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
            throw new BusinessRuleException("Settlement errors must be resolved before approval.", "SETTL_ERRORS_MUST_BE_RESOLVED", HttpStatus.CONFLICT);
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
            throw new BusinessRuleException("Cannot lock the period before it is approved.", "SETTL_LOCK_BEFORE_APPROVAL", HttpStatus.CONFLICT);
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
                .orElseThrow(() -> new BusinessRuleException("Contractor settlement not found: " + settlementId, "SETTL_NOT_FOUND", HttpStatus.NOT_FOUND));

        WorkforceSettlementPeriod period = periodRepository.findById(settlement.getPeriodId())
                .orElseThrow(() -> new BusinessRuleException("Settlement period not found", "SETTL_PERIOD_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!"APPROVED".equals(period.getStatus()) && !"LOCKED".equals(period.getStatus())) {
            throw new BusinessRuleException("Contractor settlement must be approved before posting.", "SETTL_NOT_APPROVED", HttpStatus.CONFLICT);
        }

        if ("POSTED".equals(settlement.getStatus()) || settlement.getPostedJournalEntryId() != null) {
            throw new BusinessRuleException("This settlement has already been posted to finance.", "SETTL_ALREADY_POSTED", HttpStatus.CONFLICT);
        }

        String currentActor = actor();
        PartnerLedgerEntry ledgerEntry = new PartnerLedgerEntry(
                settlement.getContractorId(),
                "CREDIT",
                settlement.getNetPayable(),
                "SETTL-" + period.getPeriodCode(),
                "Settlement liability posting for period " + period.getPeriodCode(),
                Instant.now(),
                currentActor
        );
        partnerLedgerEntryRepository.save(ledgerEntry);

        // Record Project Cost Ledger Entries for any project-attributed settlement lines
        List<ContractorSettlementLine> lines = contractorSettlementLineRepository.findBySettlementId(settlementId);
        for (ContractorSettlementLine line : lines) {
            if (line.getProjectId() != null && line.getGrossWage() != null && line.getGrossWage().compareTo(BigDecimal.ZERO) > 0) {
                ProjectCostLedgerEntry costEntry = new ProjectCostLedgerEntry(
                        line.getProjectId(),
                        line.getWbsNodeId(),
                        line.getCostCodeId(),
                        CostCategory.LABOR,
                        CostLedgerEntryType.ACTUAL,
                        "WORKFORCE",
                        settlement.getId(),
                        "SETTL-" + period.getPeriodCode(),
                        LocalDate.now(),
                        "Contractor labor settlement: " + settlement.getAccountingModel() + " - Worker " + line.getWorkerId(),
                        line.getAttendanceDays(),
                        line.getDailyWage(),
                        line.getGrossWage(),
                        "EGP"
                );
                projectCostLedgerEntryRepository.save(costEntry);
            }
        }

        settlement.markPosted("JR-SETTL-" + settlement.getId().substring(0, 8).toUpperCase());
        contractorSettlementRepository.save(settlement);

        auditService.record("POST", "CONTRACTOR_SETTLEMENT", settlementId, currentActor,
                "{\"netPayable\":" + settlement.getNetPayable() + "}", null);

        return getContractorSettlement(settlementId);
    }

    @Transactional
    public WorkforceApi.ContractorSettlementDetailResponse linkInvoice(String settlementId, WorkforceApi.LinkInvoiceRequest request) {
        ContractorSettlement settlement = contractorSettlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessRuleException("Contractor settlement not found: " + settlementId, "SETTL_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (request.invoiceAmount() != null && request.invoiceAmount().subtract(settlement.getNetPayable()).abs().compareTo(new BigDecimal("100.00")) > 0) {
            throw new BusinessRuleException("Invoice amount exceeds approved settlement amount outside tolerance.", "SETTL_INVOICE_VARIANCE_EXCEEDED", HttpStatus.CONFLICT);
        }

        settlement.linkInvoice(request.invoiceNumber(), Instant.ofEpochMilli(request.invoiceDate()));
        contractorSettlementRepository.save(settlement);

        auditService.record("LINK_INVOICE", "CONTRACTOR_SETTLEMENT", settlementId, actor(),
                "{\"invoice\":\"" + request.invoiceNumber() + "\"}", null);

        return getContractorSettlement(settlementId);
    }

    @Transactional
    public WorkforceApi.ContractorSettlementDetailResponse recordPayment(String settlementId, WorkforceApi.RecordSettlementPaymentRequest request) {
        ContractorSettlement settlement = contractorSettlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessRuleException("Contractor settlement not found: " + settlementId, "SETTL_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!"POSTED".equals(settlement.getStatus())) {
            throw new BusinessRuleException("Settlement must be posted to finance before recording payment.", "SETTL_PAYMENT_BEFORE_POST", HttpStatus.CONFLICT);
        }

        BigDecimal newPaid = settlement.getPaidAmount().add(request.amount());
        if (newPaid.compareTo(settlement.getNetPayable()) > 0) {
            throw new BusinessRuleException("Total payments exceed settlement net payable.", "SETTL_PAYMENT_EXCEEDS_NET", HttpStatus.CONFLICT);
        }

        String currentActor = actor();
        PartnerLedgerEntry paymentLedgerEntry = new PartnerLedgerEntry(
                settlement.getContractorId(),
                "DEBIT",
                request.amount(),
                "PMT-SETTL-" + settlement.getId().substring(0, 8).toUpperCase(),
                "Cash payment settlement disbursement",
                request.paymentDate() != null ? Instant.ofEpochMilli(request.paymentDate()) : Instant.now(),
                currentActor
        );
        partnerLedgerEntryRepository.save(paymentLedgerEntry);

        settlement.updatePaidAmount(newPaid);
        contractorSettlementRepository.save(settlement);

        auditService.record("RECORD_PAYMENT", "CONTRACTOR_SETTLEMENT", settlementId, currentActor,
                "{\"amount\":" + request.amount() + ",\"newPaid\":" + newPaid + "}", null);

        return getContractorSettlement(settlementId);
    }

    @Transactional(readOnly = true)
    public WorkforceApi.ProjectLaborCostReportResponse getProjectLaborCostReport(String projectId, String periodId) {
        List<ContractorSettlementLine> lines = contractorSettlementLineRepository.findByProjectId(projectId);
        if (periodId != null && !periodId.isBlank()) {
            List<ContractorSettlement> settlements = contractorSettlementRepository.findByPeriodId(periodId);
            Set<String> settlementIds = settlements.stream().map(ContractorSettlement::getId).collect(Collectors.toSet());
            lines = lines.stream().filter(l -> settlementIds.contains(l.getSettlementId())).toList();
        }

        int totalWorkers = (int) lines.stream().map(ContractorSettlementLine::getWorkerId).distinct().count();
        BigDecimal totalDays = BigDecimal.ZERO;
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalOvertime = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        List<WorkforceApi.ProjectLaborCostItem> items = new ArrayList<>();
        for (ContractorSettlementLine l : lines) {
            if (l.getAttendanceDays() != null) totalDays = totalDays.add(l.getAttendanceDays());
            if (l.getGrossWage() != null) totalGross = totalGross.add(l.getGrossWage());
            if (l.getOvertimeAmount() != null) totalOvertime = totalOvertime.add(l.getOvertimeAmount());
            if (l.getNetWage() != null) totalNet = totalNet.add(l.getNetWage());

            Worker w = workerRepository.findById(l.getWorkerId()).orElse(null);
            String wCode = w != null ? w.getCode() : "";
            String wName = w != null ? w.getFullName() : "Worker " + l.getWorkerId();
            String cId = w != null ? w.getContractorId() : "";
            String cName = contractorRepository.findById(cId).map(Contractor::getName).orElse("—");

            items.add(new WorkforceApi.ProjectLaborCostItem(
                    l.getWorkerId(), wCode, wName, cId, cName,
                    l.getWbsNodeId(), l.getCostCodeId(),
                    l.getAttendanceDays(), l.getDailyWage(),
                    l.getGrossWage(), l.getOvertimeAmount(), l.getNetWage()
            ));
        }

        return new WorkforceApi.ProjectLaborCostReportResponse(
                projectId,
                "Project Labor Report",
                periodId,
                totalWorkers,
                totalDays,
                totalGross,
                totalOvertime,
                totalNet,
                items
        );
    }

    private WorkforceApi.SettlementPeriodResponse mapPeriodToResponse(WorkforceSettlementPeriod p) {
        return new WorkforceApi.SettlementPeriodResponse(
                p.getId(), p.getPeriodCode(), p.getStartDate(), p.getEndDate(), p.getCycleType(), p.getStatus(),
                p.getCalculationVersion(), epoch(p.getLastCalculatedAt()), p.getLastCalculatedBy(),
                epoch(p.getLastCalculationFailedAt()), p.getLastCalculationError(),
                needsRecalculation(p), p.getResultRecordCount(), p.getResultGrossAmount(), p.getResultDeductions(),
                p.getResultAdvances(), p.getResultNetAmount(), p.getResultWarningCount(), p.getResultErrorCount(),
                p.getCreatedAt().toEpochMilli(), p.getUpdatedAt().toEpochMilli()
        );
    }

    private WorkforceApi.ContractorSettlementDetailResponse mapContractorSettlementToDetail(ContractorSettlement cs) {
        String contractorName = contractorRepository.findById(cs.getContractorId())
                .map(Contractor::getName).orElse("—");
        List<WorkforceApi.ContractorSettlementLineResponse> lines = contractorSettlementLineRepository.findBySettlementId(cs.getId())
                .stream().map(line -> {
                    String workerName = workerRepository.findById(line.getWorkerId())
                            .map(Worker::getFullName).orElse("—");
                    return new WorkforceApi.ContractorSettlementLineResponse(
                            line.getId(), line.getSettlementId(), line.getWorkerId(), workerName,
                            line.getProjectId(), line.getWbsNodeId(), line.getCostCodeId(),
                            line.getAttendanceDays(), line.getDailyWage(), line.getGrossWage(),
                            line.getOvertimeAmount(), line.getDeductionsAmount(), line.getAdvanceInstallments(),
                            line.getNetWage()
                    );
                }).toList();

        List<WorkforceApi.ContractorSettlementAdjustmentResponse> adjustments = contractorSettlementAdjustmentRepository.findBySettlementId(cs.getId())
                .stream().map(adj -> new WorkforceApi.ContractorSettlementAdjustmentResponse(
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
            throw new IllegalStateException("Failed to build settlement input fingerprint.", exception);
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
        return current.getMessage() == null ? "Unexpected error during calculation." : current.getMessage();
    }

    private String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private WorkforceSettlementPeriod requirePeriod(String periodId) {
        return periodRepository.findById(periodId)
                .orElseThrow(() -> new BusinessRuleException("Settlement period not found: " + periodId, "SETTL_PERIOD_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private WorkforceSettlementPeriod requireFreshCalculated(String periodId, String expectedStatus) {
        WorkforceSettlementPeriod period = requirePeriod(periodId);
        if (!expectedStatus.equals(period.getStatus())) {
            throw new BusinessRuleException("Settlement period status does not match the required stage: " + period.getStatus(), "SETTL_INVALID_STATUS", HttpStatus.CONFLICT);
        }
        if (needsRecalculation(period)) {
            throw new BusinessRuleException("Attendance or worker data changed since last calculation; recalculate the period first.", "SETTL_RECALCULATION_REQUIRED", HttpStatus.CONFLICT);
        }
        return period;
    }

    private TransitionResponse transition(String targetStatus, WorkforceSettlementPeriod period) {
        List<String> allowed = SETTLEMENT_PERIOD_WORKFLOW.getOrDefault(targetStatus, List.of());
        return new TransitionResponse(targetStatus, period.getCalculationVersion(), allowed);
    }
}
