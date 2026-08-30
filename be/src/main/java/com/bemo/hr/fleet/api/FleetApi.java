package com.bemo.hr.fleet.api;

import com.bemo.hr.fleet.domain.MaintenanceSchedule;
import com.bemo.hr.fleet.domain.Vehicle;
import com.bemo.hr.fleet.domain.VehicleDocument;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public final class FleetApi {

    private FleetApi() {
    }

    // --- Vehicles ---
    public record VehicleCreateRequest(
            @NotBlank String plateNumber,
            @NotBlank String make,
            @NotBlank String model,
            int year,
            Vehicle.VehicleType vehicleType,
            String assetId,
            String defaultDriverId,
            String defaultDriverName,
            BigDecimal initialOdometer,
            String notes
    ) {}

    public record VehicleResponse(
            String id,
            String plateNumber,
            String make,
            String model,
            int year,
            Vehicle.VehicleType vehicleType,
            String assetId,
            BigDecimal assetNetBookValue,
            String defaultDriverId,
            String defaultDriverName,
            BigDecimal currentOdometer,
            Vehicle.Status status,
            String notes,
            long createdAt,
            long updatedAt
    ) {}

    // --- Fuel Logs ---
    public record FuelLogCreateRequest(
            @NotBlank String vehicleId,
            @NotBlank String logDate,
            @NotNull @Positive BigDecimal liters,
            @NotNull BigDecimal odometer,
            @NotNull BigDecimal totalCost,
            String stationName,
            String driverName,
            String notes
    ) {}

    public record FuelLogResponse(
            String id,
            String vehicleId,
            String logDate,
            BigDecimal liters,
            BigDecimal odometer,
            BigDecimal totalCost,
            BigDecimal efficiencyKmPerLiter,
            String stationName,
            String driverName,
            String notes,
            long createdAt
    ) {}

    // --- Maintenance Schedules ---
    public record MaintenanceScheduleCreateRequest(
            @NotBlank String vehicleId,
            @NotBlank String title,
            MaintenanceSchedule.MaintenanceKind maintenanceKind,
            BigDecimal intervalKm,
            Integer intervalDays,
            BigDecimal lastDoneOdometer,
            String lastDoneDate
    ) {}

    public record MaintenanceScheduleResponse(
            String id,
            String vehicleId,
            String title,
            MaintenanceSchedule.MaintenanceKind maintenanceKind,
            BigDecimal intervalKm,
            Integer intervalDays,
            BigDecimal lastDoneOdometer,
            String lastDoneDate,
            boolean isDue,
            String dueReason,
            boolean active,
            long createdAt
    ) {}

    // --- Maintenance Records ---
    public record MaintenanceRecordCreateRequest(
            @NotBlank String vehicleId,
            String scheduleId,
            @NotBlank String title,
            @NotBlank String performedDate,
            @NotNull BigDecimal odometer,
            @NotNull BigDecimal cost,
            String vendorPartyId,
            String vendorName,
            String description
    ) {}

    public record MaintenanceRecordResponse(
            String id,
            String vehicleId,
            String scheduleId,
            String title,
            String performedDate,
            BigDecimal odometer,
            BigDecimal cost,
            String vendorPartyId,
            String vendorName,
            String description,
            long createdAt
    ) {}

    // --- Vehicle Documents ---
    public record VehicleDocumentCreateRequest(
            @NotBlank String vehicleId,
            VehicleDocument.DocumentType documentType,
            @NotBlank String documentNumber,
            String issueDate,
            @NotBlank String expiryDate,
            String issuer,
            String notes
    ) {}

    public record VehicleDocumentResponse(
            String id,
            String vehicleId,
            VehicleDocument.DocumentType documentType,
            String documentNumber,
            String issueDate,
            String expiryDate,
            String issuer,
            boolean isExpired,
            boolean isDueSoon,
            String notes,
            long createdAt
    ) {}

    // --- Fleet Cost Report ---
    public record FleetCostSummary(
            int totalVehicles,
            BigDecimal totalFuelCost,
            BigDecimal totalMaintenanceCost,
            BigDecimal grandTotalCost,
            BigDecimal totalKilometers,
            BigDecimal costPerKilometer
    ) {}
}
