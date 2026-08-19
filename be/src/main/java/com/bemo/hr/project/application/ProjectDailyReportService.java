package com.bemo.hr.project.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.project.api.DailyReportApi.*;
import com.bemo.hr.project.domain.*;
import com.bemo.hr.project.infrastructure.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ProjectDailyReportService {

    private final ProjectDailyReportRepository dailyReportRepository;
    private final DailyWorkProgressLineRepository progressLineRepository;
    private final DailyLaborSnapshotRepository laborSnapshotRepository;
    private final DailyEquipmentLogRepository equipmentLogRepository;
    private final DailyMaterialConsumptionRepository materialConsumptionRepository;
    private final ProjectRepository projectRepository;
    private final WbsNodeRepository wbsNodeRepository;
    private final AuditService auditService;

    public ProjectDailyReportService(
            ProjectDailyReportRepository dailyReportRepository,
            DailyWorkProgressLineRepository progressLineRepository,
            DailyLaborSnapshotRepository laborSnapshotRepository,
            DailyEquipmentLogRepository equipmentLogRepository,
            DailyMaterialConsumptionRepository materialConsumptionRepository,
            ProjectRepository projectRepository,
            WbsNodeRepository wbsNodeRepository,
            AuditService auditService) {
        this.dailyReportRepository = dailyReportRepository;
        this.progressLineRepository = progressLineRepository;
        this.laborSnapshotRepository = laborSnapshotRepository;
        this.equipmentLogRepository = equipmentLogRepository;
        this.materialConsumptionRepository = materialConsumptionRepository;
        this.projectRepository = projectRepository;
        this.wbsNodeRepository = wbsNodeRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<DailyReportResponse> listDailyReports(String projectId) {
        requireProject(projectId);
        List<ProjectDailyReport> reports = dailyReportRepository.findByProjectIdOrderByReportDateDescShiftDesc(projectId);
        return reports.stream().map(this::mapSummaryResponse).toList();
    }

    @Transactional(readOnly = true)
    public DailyReportResponse getDailyReport(String projectId, String reportId) {
        requireProject(projectId);
        ProjectDailyReport report = requireReport(reportId);
        if (!report.getProjectId().equals(projectId)) {
            throw new NotFoundException("DPR_NOT_FOUND");
        }
        return mapFullResponse(report);
    }

    public DailyReportResponse createDailyReport(String projectId, CreateDailyReportRequest req, String userId) {
        Project project = requireProject(projectId);
        LocalDate reportDate = epochToLocalDate(req.reportDate());
        ReportShift shift = req.shift() != null ? req.shift() : ReportShift.DAY;

        if (dailyReportRepository.findByProjectIdAndReportDateAndShift(projectId, reportDate, shift).isPresent()) {
            throw new BusinessRuleException("DPR_ALREADY_EXISTS_FOR_DATE_SHIFT");
        }

        String reportNumber = String.format("DPR-%s-%s-%s", project.getCode(), reportDate, shift);

        ProjectDailyReport report = new ProjectDailyReport(
                projectId,
                reportNumber,
                reportDate,
                shift,
                req.weatherCondition(),
                req.temperatureCelsius(),
                userId,
                req.generalNotes(),
                req.blockersAndIssues(),
                req.safetyObservations()
        );
        report = dailyReportRepository.save(report);

        saveChildLines(report, req.progressLines(), req.laborSnapshots(), req.equipmentLogs(), req.materialConsumptions());

        auditService.record(
                "PROJECT_DPR_CREATE",
                "PROJECT_DAILY_REPORT",
                report.getId(),
                userId,
                "Created daily report " + report.getReportNumber() + " for project " + project.getCode(),
                null
        );

        return mapFullResponse(report);
    }

    public DailyReportResponse updateDailyReport(String projectId, String reportId, UpdateDailyReportRequest req, String userId) {
        requireProject(projectId);
        ProjectDailyReport report = requireReport(reportId);

        if (report.getStatus() == DailyReportStatus.APPROVED) {
            throw new BusinessRuleException("DPR_CANNOT_EDIT_APPROVED");
        }

        report.updateDraft(
                req.shift(),
                req.weatherCondition(),
                req.temperatureCelsius(),
                req.generalNotes(),
                req.blockersAndIssues(),
                req.safetyObservations()
        );

        // Clear existing child lines
        progressLineRepository.deleteByDailyReportId(report.getId());
        laborSnapshotRepository.deleteByDailyReportId(report.getId());
        equipmentLogRepository.deleteByDailyReportId(report.getId());
        materialConsumptionRepository.deleteByDailyReportId(report.getId());

        // Recreate child lines
        saveChildLines(report, req.progressLines(), req.laborSnapshots(), req.equipmentLogs(), req.materialConsumptions());

        auditService.record(
                "PROJECT_DPR_UPDATE",
                "PROJECT_DAILY_REPORT",
                report.getId(),
                userId,
                "Updated daily report " + report.getReportNumber(),
                null
        );

        return mapFullResponse(report);
    }

    public DailyReportResponse submitDailyReport(String projectId, String reportId, String userId) {
        requireProject(projectId);
        ProjectDailyReport report = requireReport(reportId);
        report.submit(userId);

        auditService.record(
                "PROJECT_DPR_SUBMIT",
                "PROJECT_DAILY_REPORT",
                report.getId(),
                userId,
                "Submitted daily report " + report.getReportNumber() + " for approval",
                null
        );

        return mapFullResponse(report);
    }

    public DailyReportResponse approveDailyReport(String projectId, String reportId, String approverId) {
        requireProject(projectId);
        ProjectDailyReport report = requireReport(reportId);
        report.approve(approverId);

        // Update WBS node progress & lifecycle status
        List<DailyWorkProgressLine> lines = progressLineRepository.findByDailyReportIdOrderByWbsCodeAsc(report.getId());
        for (DailyWorkProgressLine line : lines) {
            wbsNodeRepository.findById(line.getWbsNodeId()).ifPresent(node -> {
                BigDecimal totalCumulative = progressLineRepository.sumApprovedQuantityUpToDate(
                        projectId, node.getId(), report.getReportDate());
                if (node.getStatus() == WbsNodeStatus.PLANNED && totalCumulative.compareTo(BigDecimal.ZERO) > 0) {
                    node.startProgress();
                }
                if (node.getPlannedQuantity() != null &&
                    node.getPlannedQuantity().compareTo(BigDecimal.ZERO) > 0 &&
                    totalCumulative.compareTo(node.getPlannedQuantity()) >= 0) {
                    node.complete();
                }
                wbsNodeRepository.save(node);
            });
        }

        auditService.record(
                "PROJECT_DPR_APPROVE",
                "PROJECT_DAILY_REPORT",
                report.getId(),
                approverId,
                "Approved daily report " + report.getReportNumber() + " and updated WBS progress",
                null
        );

        return mapFullResponse(report);
    }

    public DailyReportResponse reopenDailyReport(String projectId, String reportId, String reason, String userId) {
        requireProject(projectId);
        ProjectDailyReport report = requireReport(reportId);
        report.reopen(userId);

        auditService.record(
                "PROJECT_DPR_REOPEN",
                "PROJECT_DAILY_REPORT",
                report.getId(),
                userId,
                "Reopened daily report " + report.getReportNumber() + ". Reason: " + (reason != null ? reason : "Not specified"),
                null
        );

        return mapFullResponse(report);
    }

    public void deleteDailyReport(String projectId, String reportId, String userId) {
        requireProject(projectId);
        ProjectDailyReport report = requireReport(reportId);
        if (report.getStatus() == DailyReportStatus.APPROVED) {
            throw new BusinessRuleException("DPR_CANNOT_DELETE_APPROVED");
        }

        progressLineRepository.deleteByDailyReportId(report.getId());
        laborSnapshotRepository.deleteByDailyReportId(report.getId());
        equipmentLogRepository.deleteByDailyReportId(report.getId());
        materialConsumptionRepository.deleteByDailyReportId(report.getId());
        dailyReportRepository.delete(report);

        auditService.record(
                "PROJECT_DPR_DELETE",
                "PROJECT_DAILY_REPORT",
                report.getId(),
                userId,
                "Deleted daily report " + report.getReportNumber(),
                null
        );
    }

    public DailyReportResponse copyPreviousDay(String projectId, Long targetDateEpoch, String userId) {
        requireProject(projectId);
        LocalDate targetDate = epochToLocalDate(targetDateEpoch);

        List<ProjectDailyReport> previousReports = dailyReportRepository.findLatestBeforeDate(projectId, targetDate);
        if (previousReports.isEmpty()) {
            throw new NotFoundException("DPR_NO_PREVIOUS_REPORT_FOUND");
        }

        ProjectDailyReport prev = previousReports.get(0);
        List<DailyLaborSnapshot> prevLabor = laborSnapshotRepository.findByDailyReportId(prev.getId());
        List<DailyEquipmentLog> prevEquipment = equipmentLogRepository.findByDailyReportId(prev.getId());
        List<DailyWorkProgressLine> prevProgress = progressLineRepository.findByDailyReportIdOrderByWbsCodeAsc(prev.getId());

        List<CreateLaborSnapshotRequest> laborReqs = prevLabor.stream().map(l -> new CreateLaborSnapshotRequest(
                l.getWbsNodeId(),
                l.getCostCodeId(),
                l.getTradeCategory(),
                l.getSourceType(),
                l.getPartyId(),
                l.getHeadcount(),
                l.getHoursWorked(),
                l.getActivityDescription()
        )).toList();

        List<CreateEquipmentLogRequest> equipReqs = prevEquipment.stream().map(e -> new CreateEquipmentLogRequest(
                e.getWbsNodeId(),
                e.getEquipmentType(),
                e.getEquipmentCode(),
                e.getStatus(),
                e.getHoursOperated(),
                e.getHoursIdle(),
                e.getFuelConsumedLiters(),
                e.getOperatorName(),
                e.getNotes()
        )).toList();

        List<CreateWorkProgressLineRequest> progressReqs = prevProgress.stream().map(p -> new CreateWorkProgressLineRequest(
                p.getWbsNodeId(),
                BigDecimal.ZERO,
                p.getLocationNotes(),
                ""
        )).toList();

        CreateDailyReportRequest req = new CreateDailyReportRequest(
                targetDateEpoch,
                prev.getShift(),
                prev.getWeatherCondition(),
                prev.getTemperatureCelsius(),
                prev.getGeneralNotes(),
                "",
                "",
                progressReqs,
                laborReqs,
                equipReqs,
                List.of()
        );

        return createDailyReport(projectId, req, userId);
    }

    @Transactional(readOnly = true)
    public DprPeriodSummaryResponse getPeriodSummary(String projectId, Long startDateEpoch, Long endDateEpoch) {
        requireProject(projectId);
        LocalDate startDate = epochToLocalDate(startDateEpoch);
        LocalDate endDate = epochToLocalDate(endDateEpoch);

        List<ProjectDailyReport> periodReports = dailyReportRepository
                .findByProjectIdAndReportDateBetweenOrderByReportDateAscShiftAsc(projectId, startDate, endDate);

        long totalReportsCount = periodReports.size();
        long approvedReportsCount = periodReports.stream()
                .filter(r -> r.getStatus() == DailyReportStatus.APPROVED).count();

        List<DailyLaborSnapshot> laborList = laborSnapshotRepository.findInPeriod(projectId, startDate, endDate);
        List<DailyEquipmentLog> equipList = equipmentLogRepository.findInPeriod(projectId, startDate, endDate);
        List<DailyMaterialConsumption> materialList = materialConsumptionRepository.findInPeriod(projectId, startDate, endDate);
        List<DailyWorkProgressLine> progressList = progressLineRepository.findApprovedInPeriod(projectId, startDate, endDate);

        int totalManDays = laborList.stream().mapToInt(DailyLaborSnapshot::getHeadcount).sum();
        BigDecimal totalManHours = laborList.stream()
                .map(DailyLaborSnapshot::getTotalManHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEquipHours = equipList.stream()
                .map(DailyEquipmentLog::getHoursOperated)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFuel = equipList.stream()
                .map(DailyEquipmentLog::getFuelConsumedLiters)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Group WBS Progress
        Map<String, List<DailyWorkProgressLine>> wbsGroups = progressList.stream()
                .collect(Collectors.groupingBy(DailyWorkProgressLine::getWbsNodeId));

        List<DprWbsProgressSummary> wbsSummaries = new ArrayList<>();
        for (Map.Entry<String, List<DailyWorkProgressLine>> entry : wbsGroups.entrySet()) {
            DailyWorkProgressLine first = entry.getValue().get(0);
            BigDecimal totalQty = entry.getValue().stream()
                    .map(DailyWorkProgressLine::getTodayQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal plannedQty = wbsNodeRepository.findById(entry.getKey())
                    .map(WbsNode::getPlannedQuantity).orElse(BigDecimal.ZERO);
            BigDecimal percent = plannedQty != null && plannedQty.compareTo(BigDecimal.ZERO) > 0
                    ? totalQty.multiply(BigDecimal.valueOf(100)).divide(plannedQty, 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            wbsSummaries.add(new DprWbsProgressSummary(
                    entry.getKey(),
                    first.getWbsCode(),
                    first.getWbsName(),
                    first.getUnitOfMeasure(),
                    plannedQty,
                    totalQty,
                    percent
            ));
        }

        // Group Labor Trades
        Map<String, List<DailyLaborSnapshot>> laborGroups = laborList.stream()
                .collect(Collectors.groupingBy(DailyLaborSnapshot::getTradeCategory));

        List<DprLaborTradeSummary> laborSummaries = new ArrayList<>();
        for (Map.Entry<String, List<DailyLaborSnapshot>> entry : laborGroups.entrySet()) {
            int head = entry.getValue().stream().mapToInt(DailyLaborSnapshot::getHeadcount).sum();
            BigDecimal hours = entry.getValue().stream()
                    .map(DailyLaborSnapshot::getTotalManHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            LaborSourceType source = entry.getValue().get(0).getSourceType();
            laborSummaries.add(new DprLaborTradeSummary(entry.getKey(), source, head, hours));
        }

        // Group Materials
        Map<String, List<DailyMaterialConsumption>> materialGroups = materialList.stream()
                .collect(Collectors.groupingBy(DailyMaterialConsumption::getMaterialName));

        List<DprMaterialUsageSummary> materialSummaries = new ArrayList<>();
        for (Map.Entry<String, List<DailyMaterialConsumption>> entry : materialGroups.entrySet()) {
            BigDecimal qty = entry.getValue().stream()
                    .map(DailyMaterialConsumption::getQuantityUsed)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            String uom = entry.getValue().get(0).getUnitOfMeasure();
            materialSummaries.add(new DprMaterialUsageSummary(entry.getKey(), uom, qty));
        }

        return new DprPeriodSummaryResponse(
                projectId,
                startDateEpoch,
                endDateEpoch,
                totalReportsCount,
                approvedReportsCount,
                totalManDays,
                totalManHours,
                totalEquipHours,
                totalFuel,
                wbsSummaries,
                laborSummaries,
                materialSummaries
        );
    }

    // ─── Internal Helper Methods ─────────────────────────────────────

    private void saveChildLines(ProjectDailyReport report,
                                List<CreateWorkProgressLineRequest> progressReqs,
                                List<CreateLaborSnapshotRequest> laborReqs,
                                List<CreateEquipmentLogRequest> equipReqs,
                                List<CreateMaterialConsumptionRequest> materialReqs) {
        int workforceTotal = 0;
        int equipmentTotal = 0;

        if (progressReqs != null) {
            for (CreateWorkProgressLineRequest p : progressReqs) {
                WbsNode node = wbsNodeRepository.findById(p.wbsNodeId()).orElse(null);
                if (node == null) continue;

                BigDecimal prevQty = progressLineRepository.sumApprovedQuantityBeforeDate(
                        report.getProjectId(), node.getId(), report.getReportDate());

                DailyWorkProgressLine line = new DailyWorkProgressLine(
                        report.getId(),
                        node.getId(),
                        node.getWbsCode(),
                        node.getName(),
                        node.getUnitOfMeasure(),
                        prevQty,
                        p.todayQuantity(),
                        node.getPlannedQuantity(),
                        p.locationNotes(),
                        p.remarks()
                );
                progressLineRepository.save(line);
            }
        }

        if (laborReqs != null) {
            for (CreateLaborSnapshotRequest l : laborReqs) {
                DailyLaborSnapshot snapshot = new DailyLaborSnapshot(
                        report.getId(),
                        l.wbsNodeId(),
                        l.costCodeId(),
                        l.tradeCategory(),
                        l.sourceType(),
                        l.partyId(),
                        l.headcount(),
                        l.hoursWorked(),
                        l.activityDescription()
                );
                laborSnapshotRepository.save(snapshot);
                workforceTotal += snapshot.getHeadcount();
            }
        }

        if (equipReqs != null) {
            for (CreateEquipmentLogRequest e : equipReqs) {
                DailyEquipmentLog log = new DailyEquipmentLog(
                        report.getId(),
                        e.wbsNodeId(),
                        e.equipmentType(),
                        e.equipmentCode(),
                        e.status(),
                        e.hoursOperated(),
                        e.hoursIdle(),
                        e.fuelConsumedLiters(),
                        e.operatorName(),
                        e.notes()
                );
                equipmentLogRepository.save(log);
                equipmentTotal++;
            }
        }

        if (materialReqs != null) {
            for (CreateMaterialConsumptionRequest m : materialReqs) {
                DailyMaterialConsumption mc = new DailyMaterialConsumption(
                        report.getId(),
                        m.wbsNodeId(),
                        m.materialName(),
                        m.unitOfMeasure(),
                        m.quantityUsed(),
                        m.deliveryNoteNumber(),
                        m.supplierPartyId(),
                        m.notes()
                );
                materialConsumptionRepository.save(mc);
            }
        }

        report.updateTotals(workforceTotal, equipmentTotal);
        dailyReportRepository.save(report);
    }

    private DailyReportResponse mapSummaryResponse(ProjectDailyReport report) {
        return new DailyReportResponse(
                report.getId(),
                report.getProjectId(),
                report.getReportNumber(),
                localDateToEpoch(report.getReportDate()),
                report.getShift(),
                report.getWeatherCondition(),
                report.getTemperatureCelsius(),
                report.getStatus(),
                report.getSiteEngineerUserId(),
                report.getApproverUserId(),
                instantToEpoch(report.getApprovedAt()),
                instantToEpoch(report.getReopenedAt()),
                report.getGeneralNotes(),
                report.getBlockersAndIssues(),
                report.getSafetyObservations(),
                report.getTotalWorkforceCount(),
                report.getTotalEquipmentCount(),
                BigDecimal.ZERO,
                instantToEpoch(report.getCreatedAt()),
                instantToEpoch(report.getUpdatedAt()),
                report.getVersion(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private DailyReportResponse mapFullResponse(ProjectDailyReport report) {
        List<WorkProgressLineResponse> progress = progressLineRepository
                .findByDailyReportIdOrderByWbsCodeAsc(report.getId())
                .stream()
                .map(p -> {
                    BigDecimal planned = wbsNodeRepository.findById(p.getWbsNodeId())
                            .map(WbsNode::getPlannedQuantity).orElse(BigDecimal.ZERO);
                    return new WorkProgressLineResponse(
                            p.getId(),
                            p.getDailyReportId(),
                            p.getWbsNodeId(),
                            p.getWbsCode(),
                            p.getWbsName(),
                            p.getUnitOfMeasure(),
                            p.getPreviousQuantity(),
                            p.getTodayQuantity(),
                            p.getCumulativeQuantity(),
                            p.getPercentComplete(),
                            planned,
                            p.getLocationNotes(),
                            p.getRemarks()
                    );
                }).toList();

        List<LaborSnapshotResponse> labor = laborSnapshotRepository
                .findByDailyReportId(report.getId())
                .stream()
                .map(l -> new LaborSnapshotResponse(
                        l.getId(),
                        l.getDailyReportId(),
                        l.getWbsNodeId(),
                        l.getCostCodeId(),
                        l.getTradeCategory(),
                        l.getSourceType(),
                        l.getPartyId(),
                        l.getHeadcount(),
                        l.getHoursWorked(),
                        l.getTotalManHours(),
                        l.getActivityDescription()
                )).toList();

        List<EquipmentLogResponse> equipment = equipmentLogRepository
                .findByDailyReportId(report.getId())
                .stream()
                .map(e -> new EquipmentLogResponse(
                        e.getId(),
                        e.getDailyReportId(),
                        e.getWbsNodeId(),
                        e.getEquipmentType(),
                        e.getEquipmentCode(),
                        e.getStatus(),
                        e.getHoursOperated(),
                        e.getHoursIdle(),
                        e.getFuelConsumedLiters(),
                        e.getOperatorName(),
                        e.getNotes()
                )).toList();

        List<MaterialConsumptionResponse> materials = materialConsumptionRepository
                .findByDailyReportId(report.getId())
                .stream()
                .map(m -> new MaterialConsumptionResponse(
                        m.getId(),
                        m.getDailyReportId(),
                        m.getWbsNodeId(),
                        m.getMaterialName(),
                        m.getUnitOfMeasure(),
                        m.getQuantityUsed(),
                        m.getDeliveryNoteNumber(),
                        m.getSupplierPartyId(),
                        m.getNotes()
                )).toList();

        BigDecimal totalManHours = labor.stream()
                .map(LaborSnapshotResponse::totalManHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DailyReportResponse(
                report.getId(),
                report.getProjectId(),
                report.getReportNumber(),
                localDateToEpoch(report.getReportDate()),
                report.getShift(),
                report.getWeatherCondition(),
                report.getTemperatureCelsius(),
                report.getStatus(),
                report.getSiteEngineerUserId(),
                report.getApproverUserId(),
                instantToEpoch(report.getApprovedAt()),
                instantToEpoch(report.getReopenedAt()),
                report.getGeneralNotes(),
                report.getBlockersAndIssues(),
                report.getSafetyObservations(),
                report.getTotalWorkforceCount(),
                report.getTotalEquipmentCount(),
                totalManHours,
                instantToEpoch(report.getCreatedAt()),
                instantToEpoch(report.getUpdatedAt()),
                report.getVersion(),
                progress,
                labor,
                equipment,
                materials
        );
    }

    private Project requireProject(String projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("PROJECT_NOT_FOUND"));
    }

    private ProjectDailyReport requireReport(String reportId) {
        return dailyReportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("DPR_NOT_FOUND"));
    }

    private static LocalDate epochToLocalDate(Long epoch) {
        if (epoch == null) return LocalDate.now(ZoneOffset.UTC);
        return Instant.ofEpochMilli(epoch).atZone(ZoneOffset.UTC).toLocalDate();
    }

    private static Long localDateToEpoch(LocalDate date) {
        if (date == null) return null;
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    private static Long instantToEpoch(Instant instant) {
        if (instant == null) return null;
        return instant.toEpochMilli();
    }
}
