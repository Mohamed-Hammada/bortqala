package com.bemo.hr.trade.retail.laptop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class LaptopRetailApi {

    private LaptopRetailApi() {
    }

    public record RegisterDeviceRequest(
            @NotBlank(message = "serialNumber is required")
            String serialNumber,
            @NotBlank(message = "brand is required")
            String brand,
            @NotBlank(message = "model is required")
            String model,
            @NotBlank(message = "cpu is required")
            String cpu,
            @Positive(message = "ramGb must be positive")
            int ramGb,
            @Positive(message = "storageGb must be positive")
            int storageGb,
            @NotBlank(message = "storageType is required")
            String storageType,
            @NotNull(message = "purchasePrice is required")
            BigDecimal purchasePrice,
            @NotNull(message = "sellingPrice is required")
            BigDecimal sellingPrice,
            String conditionGrade,
            String supplierId,
            String gpu,
            BigDecimal screenSizeInch
    ) {}

    public record SellDeviceRequest(
            @NotBlank(message = "customerId is required")
            String customerId,
            @NotBlank(message = "customerName is required")
            String customerName,
            int warrantyMonths,
            BigDecimal finalSellingPrice
    ) {}

    public record ReturnDeviceRequest(
            String reason
    ) {}

    public record CreateRepairTicketRequest(
            @NotBlank(message = "serialNumber is required")
            String serialNumber,
            @NotBlank(message = "customerName is required")
            String customerName,
            @NotBlank(message = "customerPhone is required")
            String customerPhone,
            @NotBlank(message = "reportedIssue is required")
            String reportedIssue
    ) {}

    public record UpdateRepairStatusRequest(
            String status,
            String diagnosis,
            String technicianNotes,
            BigDecimal costAmount,
            BigDecimal chargedAmount
    ) {}

    public record SerializedDeviceResponse(
            String id,
            String serialNumber,
            String brand,
            String model,
            String cpu,
            int ramGb,
            int storageGb,
            String storageType,
            String gpu,
            String conditionGrade,
            BigDecimal purchasePrice,
            BigDecimal sellingPrice,
            BigDecimal margin,
            String status,
            String customerName,
            Instant saleDate,
            Instant warrantyEndDate,
            boolean isWarrantyActive
    ) {
        public static SerializedDeviceResponse from(SerializedDevice d) {
            return new SerializedDeviceResponse(
                    d.getId(),
                    d.getSerialNumber(),
                    d.getBrand(),
                    d.getModel(),
                    d.getCpu(),
                    d.getRamGb(),
                    d.getStorageGb(),
                    d.getStorageType(),
                    d.getGpu(),
                    d.getConditionGrade(),
                    d.getPurchasePrice(),
                    d.getSellingPrice(),
                    d.getMargin(),
                    d.getStatus(),
                    d.getCustomerName(),
                    d.getSaleDate(),
                    d.getWarrantyEndDate(),
                    d.isWarrantyActive()
            );
        }
    }

    public record RepairTicketResponse(
            String id,
            String ticketNumber,
            String serialNumber,
            String customerName,
            String customerPhone,
            String reportedIssue,
            String diagnosis,
            String technicianNotes,
            BigDecimal costAmount,
            BigDecimal chargedAmount,
            String status,
            boolean isUnderWarranty,
            Instant createdAt
    ) {
        public static RepairTicketResponse from(DeviceRepairTicket t) {
            return new RepairTicketResponse(
                    t.getId(),
                    t.getTicketNumber(),
                    t.getSerialNumber(),
                    t.getCustomerName(),
                    t.getCustomerPhone(),
                    t.getReportedIssue(),
                    t.getDiagnosis(),
                    t.getTechnicianNotes(),
                    t.getCostAmount(),
                    t.getChargedAmount(),
                    t.getStatus(),
                    t.isUnderWarranty(),
                    t.getCreatedAt()
            );
        }
    }
}
