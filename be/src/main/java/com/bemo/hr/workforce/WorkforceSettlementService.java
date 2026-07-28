package com.bemo.hr.workforce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkforceSettlementService {
    private final WorkforceSettlementPeriodRepository periodRepository;
    private final WorkerSettlementRepository workerSettlementRepository;
    private final ContractorSettlementRepository contractorSettlementRepository;
    private final ManualAttendanceEntryRepository attendanceRepository;
    private final WorkerRepository workerRepository;
    private final ContractorRepository contractorRepository;
    private final WorkforceAdvanceRepository advanceRepository;
    private final WorkforceExcelExportService excelExportService;

    @Transactional(readOnly = true)
    public byte[] exportPeriodExcel(String periodId) {
        try {
            return excelExportService.generatePeriodExcel(periodId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate workforce settlement Excel export", e);
        }
    }

    @Transactional(readOnly = true)
    public List<WorkforceApi.SettlementPeriodResponse> listPeriods() {
        return periodRepository.findAll().stream().map(this::mapPeriodToResponse).toList();
    }

    @Transactional
    public WorkforceApi.SettlementPeriodResponse createPeriod(WorkforceApi.SettlementPeriodRequest request) {
        WorkforceSettlementPeriod period = new WorkforceSettlementPeriod(
            request.periodCode(), request.startDate(), request.endDate(),
            request.cycleType(), "DRAFT"
        );
        return mapPeriodToResponse(periodRepository.save(period));
    }

    @Transactional
    public WorkforceApi.SettlementCalculationSummary calculatePeriod(String periodId) {
        WorkforceSettlementPeriod period = periodRepository.findById(periodId)
            .orElseThrow(() -> new IllegalArgumentException("Settlement period not found: " + periodId));

        if ("LOCKED".equalsIgnoreCase(period.getStatus()) || "APPROVED".equalsIgnoreCase(period.getStatus())) {
            throw new IllegalStateException("Cannot recalculate a locked or approved settlement period.");
        }

        // Fetch all attendance entries in range
        List<ManualAttendanceEntry> entries = attendanceRepository.findByWorkDateBetween(period.getStartDate(), period.getEndDate());
        Map<String, List<ManualAttendanceEntry>> workerEntries = entries.stream()
            .collect(Collectors.groupingBy(ManualAttendanceEntry::getWorkerId));

        BigDecimal totalAttendanceUnits = BigDecimal.ZERO;
        BigDecimal grossWorkersAmount = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalAdvanceDeductions = BigDecimal.ZERO;
        BigDecimal netWorkersAmount = BigDecimal.ZERO;

        // Clear previous calculations for this period if drafting
        List<WorkerSettlement> existingWorkerSettlements = workerSettlementRepository.findByPeriodId(periodId);
        workerSettlementRepository.deleteAll(existingWorkerSettlements);
        List<ContractorSettlement> existingContractorSettlements = contractorSettlementRepository.findByPeriodId(periodId);
        contractorSettlementRepository.deleteAll(existingContractorSettlements);

        List<Worker> allWorkers = workerRepository.findAll();
        for (Worker worker : allWorkers) {
            List<ManualAttendanceEntry> list = workerEntries.get(worker.getId());
            if (list == null || list.isEmpty()) continue;

            BigDecimal units = list.stream()
                .map(ManualAttendanceEntry::getAttendanceValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalAttendanceUnits = totalAttendanceUnits.add(units);

            BigDecimal dailyRate = worker.getDefaultDailyRate();
            BigDecimal gross = units.multiply(dailyRate).setScale(2, RoundingMode.HALF_UP);
            grossWorkersAmount = grossWorkersAmount.add(gross);

            // Calculate advance deduction eligibility
            BigDecimal advanceDeduction = BigDecimal.ZERO;
            List<WorkforceAdvance> activeAdvances = advanceRepository.findByWorkerId(worker.getId()).stream()
                .filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus())).toList();

            for (WorkforceAdvance adv : activeAdvances) {
                BigDecimal maxAllowed = gross.multiply(adv.getMaxDeductionPercent()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                BigDecimal wanted = adv.getInstallmentAmount();
                BigDecimal actualDeducted = wanted.min(maxAllowed).min(adv.getRemainingBalance());
                advanceDeduction = advanceDeduction.add(actualDeducted);
            }
            totalAdvanceDeductions = totalAdvanceDeductions.add(advanceDeduction);

            BigDecimal net = gross.subtract(advanceDeduction).setScale(2, RoundingMode.HALF_UP);
            netWorkersAmount = netWorkersAmount.add(net);

            WorkerSettlement ws = new WorkerSettlement(
                periodId, worker.getId(), worker.getContractorId(),
                units, dailyRate, gross, BigDecimal.ZERO, BigDecimal.ZERO, advanceDeduction, net
            );
            workerSettlementRepository.save(ws);
        }

        // Calculate Contractor Settlements based on selected Accounting Model
        List<Contractor> contractors = contractorRepository.findAll();
        BigDecimal netContractorsPayable = BigDecimal.ZERO;

        for (Contractor c : contractors) {
            List<WorkerSettlement> cWorkerSettlements = workerSettlementRepository.findByPeriodIdAndContractorId(periodId, c.getId());
            if (cWorkerSettlements.isEmpty() && !"fixed_period_amount".equalsIgnoreCase(c.getAccountingModel())) {
                continue;
            }

            BigDecimal cWorkersNet = cWorkerSettlements.stream()
                .map(WorkerSettlement::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal cGross = BigDecimal.ZERO;
            BigDecimal cNetPayable = BigDecimal.ZERO;
            BigDecimal cCommission = BigDecimal.ZERO;
            BigDecimal cRatesTotal = BigDecimal.ZERO;

            String model = c.getAccountingModel() != null ? c.getAccountingModel().toLowerCase() : "worker_net_total";

            switch (model) {
                case "contractor_daily_rate" -> {
                    BigDecimal totalUnits = cWorkerSettlements.stream()
                        .map(WorkerSettlement::getTotalAttendanceUnits)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                    cRatesTotal = totalUnits.multiply(c.getDefaultDailyRate()).setScale(2, RoundingMode.HALF_UP);
                    cGross = cRatesTotal;
                    cNetPayable = cGross;
                }
                case "worker_cost_plus_fee" -> {
                    cGross = cWorkersNet;
                    if ("percentage".equalsIgnoreCase(c.getFeeType())) {
                        cCommission = cWorkersNet.multiply(c.getFeeValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    } else {
                        cCommission = c.getFeeValue() != null ? c.getFeeValue() : BigDecimal.ZERO;
                    }
                    cNetPayable = cGross.add(cCommission);
                }
                case "fixed_period_amount" -> {
                    cGross = c.getFixedPeriodAmount() != null ? c.getFixedPeriodAmount() : BigDecimal.ZERO;
                    cNetPayable = cGross;
                }
                case "worker_net_total" -> {
                    cGross = cWorkersNet;
                    cNetPayable = cGross;
                }
                default -> {
                    cGross = cWorkersNet;
                    cNetPayable = cGross;
                }
            }

            netContractorsPayable = netContractorsPayable.add(cNetPayable);

            ContractorSettlement cs = new ContractorSettlement(
                periodId, c.getId(), model, cWorkersNet, cRatesTotal, cCommission,
                c.getFixedPeriodAmount(), BigDecimal.ZERO, BigDecimal.ZERO, cGross, cNetPayable, BigDecimal.ZERO, "REVIEW"
            );
            contractorSettlementRepository.save(cs);
        }

        period.setStatus("REVIEW");
        periodRepository.save(period);

        return new WorkforceApi.SettlementCalculationSummary(
            period.getId(), period.getPeriodCode(),
            workerEntries.size(), contractors.size(),
            totalAttendanceUnits, grossWorkersAmount, totalDeductions,
            totalAdvanceDeductions, netWorkersAmount, netContractorsPayable
        );
    }

    @Transactional
    public WorkforceApi.SettlementPeriodResponse approvePeriod(String periodId) {
        WorkforceSettlementPeriod period = periodRepository.findById(periodId)
            .orElseThrow(() -> new IllegalArgumentException("Period not found: " + periodId));
        period.setStatus("APPROVED");
        return mapPeriodToResponse(periodRepository.save(period));
    }

    private WorkforceApi.SettlementPeriodResponse mapPeriodToResponse(WorkforceSettlementPeriod p) {
        return new WorkforceApi.SettlementPeriodResponse(
            p.getId(), p.getPeriodCode(), p.getStartDate(), p.getEndDate(),
            p.getCycleType(), p.getStatus(),
            p.getCreatedAt().toEpochMilli(), p.getUpdatedAt().toEpochMilli()
        );
    }
}
