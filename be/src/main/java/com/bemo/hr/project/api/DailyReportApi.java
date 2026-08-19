package com.bemo.hr.project.api;

import com.bemo.hr.project.domain.DailyReportStatus;
import com.bemo.hr.project.domain.EquipmentSiteStatus;
import com.bemo.hr.project.domain.LaborSourceType;
import com.bemo.hr.project.domain.ReportShift;
import com.bemo.hr.project.domain.WeatherCondition;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class DailyReportApi {

    private DailyReportApi() {
    }

    public record CreateDailyReportRequest(
            @NotNull Long reportDate,
            ReportShift shift,
            WeatherCondition weatherCondition,
            BigDecimal temperatureCelsius,
            String generalNotes,
            String blockersAndIssues,
            String safetyObservations,
            List<CreateWorkProgressLineRequest> progressLines,
            List<CreateLaborSnapshotRequest> laborSnapshots,
            List<CreateEquipmentLogRequest> equipmentLogs,
            List<CreateMaterialConsumptionRequest> materialConsumptions
    ) {}

    public record UpdateDailyReportRequest(
            ReportShift shift,
            WeatherCondition weatherCondition,
            BigDecimal temperatureCelsius,
            String generalNotes,
            String blockersAndIssues,
            String safetyObservations,
            List<CreateWorkProgressLineRequest> progressLines,
            List<CreateLaborSnapshotRequest> laborSnapshots,
            List<CreateEquipmentLogRequest> equipmentLogs,
            List<CreateMaterialConsumptionRequest> materialConsumptions
    ) {}

    public record CreateWorkProgressLineRequest(
            @NotNull String wbsNodeId,
            BigDecimal todayQuantity,
            String locationNotes,
            String remarks
    ) {}

    public record CreateLaborSnapshotRequest(
            String wbsNodeId,
            String costCodeId,
            @NotNull String tradeCategory,
            LaborSourceType sourceType,
            String partyId,
            int headcount,
            BigDecimal hoursWorked,
            String activityDescription
    ) {}

    public record CreateEquipmentLogRequest(
            String wbsNodeId,
            @NotNull String equipmentType,
            String equipmentCode,
            EquipmentSiteStatus status,
            BigDecimal hoursOperated,
            BigDecimal hoursIdle,
            BigDecimal fuelConsumedLiters,
            String operatorName,
            String notes
    ) {}

    public record CreateMaterialConsumptionRequest(
            String wbsNodeId,
            @NotNull String materialName,
            @NotNull String unitOfMeasure,
            @NotNull BigDecimal quantityUsed,
            String deliveryNoteNumber,
            String supplierPartyId,
            String notes
    ) {}

    public record ReopenReportRequest(
            String reason
    ) {}

    public record DailyReportResponse(
            String id,
            String projectId,
            String reportNumber,
            Long reportDate,
            ReportShift shift,
            WeatherCondition weatherCondition,
            BigDecimal temperatureCelsius,
            DailyReportStatus status,
            String siteEngineerUserId,
            String approverUserId,
            Long approvedAt,
            Long reopenedAt,
            String generalNotes,
            String blockersAndIssues,
            String safetyObservations,
            int totalWorkforceCount,
            int totalEquipmentCount,
            BigDecimal totalManHours,
            Long createdAt,
            Long updatedAt,
            long version,
            List<WorkProgressLineResponse> progressLines,
            List<LaborSnapshotResponse> laborSnapshots,
            List<EquipmentLogResponse> equipmentLogs,
            List<MaterialConsumptionResponse> materialConsumptions
    ) {}

    public record WorkProgressLineResponse(
            String id,
            String dailyReportId,
            String wbsNodeId,
            String wbsCode,
            String wbsName,
            String unitOfMeasure,
            BigDecimal previousQuantity,
            BigDecimal todayQuantity,
            BigDecimal cumulativeQuantity,
            BigDecimal percentComplete,
            BigDecimal plannedQuantity,
            String locationNotes,
            String remarks
    ) {}

    public record LaborSnapshotResponse(
            String id,
            String dailyReportId,
            String wbsNodeId,
            String costCodeId,
            String tradeCategory,
            LaborSourceType sourceType,
            String partyId,
            int headcount,
            BigDecimal hoursWorked,
            BigDecimal totalManHours,
            String activityDescription
    ) {}

    public record EquipmentLogResponse(
            String id,
            String dailyReportId,
            String wbsNodeId,
            String equipmentType,
            String equipmentCode,
            EquipmentSiteStatus status,
            BigDecimal hoursOperated,
            BigDecimal hoursIdle,
            BigDecimal fuelConsumedLiters,
            String operatorName,
            String notes
    ) {}

    public record MaterialConsumptionResponse(
            String id,
            String dailyReportId,
            String wbsNodeId,
            String materialName,
            String unitOfMeasure,
            BigDecimal quantityUsed,
            String deliveryNoteNumber,
            String supplierPartyId,
            String notes
    ) {}

    public record DprPeriodSummaryResponse(
            String projectId,
            Long startDate,
            Long endDate,
            long totalReportsCount,
            long approvedReportsCount,
            int totalManDays,
            BigDecimal totalManHours,
            BigDecimal totalEquipmentOperatingHours,
            BigDecimal totalFuelLiters,
            List<DprWbsProgressSummary> wbsProgress,
            List<DprLaborTradeSummary> laborBreakdown,
            List<DprMaterialUsageSummary> materialUsage
    ) {}

    public record DprWbsProgressSummary(
            String wbsNodeId,
            String wbsCode,
            String wbsName,
            String unitOfMeasure,
            BigDecimal plannedQuantity,
            BigDecimal totalQuantityExecuted,
            BigDecimal percentComplete
    ) {}

    public record DprLaborTradeSummary(
            String tradeCategory,
            LaborSourceType sourceType,
            int totalHeadcount,
            BigDecimal totalManHours
    ) {}

    public record DprMaterialUsageSummary(
            String materialName,
            String unitOfMeasure,
            BigDecimal totalQuantityUsed
    ) {}
}
