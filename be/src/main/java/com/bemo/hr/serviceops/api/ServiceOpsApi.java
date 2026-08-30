package com.bemo.hr.serviceops.api;

import com.bemo.hr.serviceops.domain.BookableResource;
import com.bemo.hr.serviceops.domain.RentalContract;
import com.bemo.hr.serviceops.domain.RentalItem;
import com.bemo.hr.serviceops.domain.ResourceBooking;
import com.bemo.hr.serviceops.domain.WorkOrder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class ServiceOpsApi {

    private ServiceOpsApi() {}

    // --- Rentals ---

    public record RentalItemCreateRequest(
            @NotBlank String code,
            @NotBlank String name,
            String nameEn,
            String category,
            BigDecimal rateDaily,
            BigDecimal rateWeekly,
            BigDecimal rateMonthly,
            BigDecimal depositAmount
    ) {}

    public record RentalItemResponse(
            String id,
            String code,
            String name,
            String nameEn,
            String category,
            BigDecimal rateDaily,
            BigDecimal rateWeekly,
            BigDecimal rateMonthly,
            BigDecimal depositAmount,
            RentalItem.Status status,
            long createdAt,
            long updatedAt
    ) {}

    public record RentalContractLineRequest(
            @NotBlank String rentalItemId,
            BigDecimal quantity,
            BigDecimal unitRate
    ) {}

    public record RentalContractCreateRequest(
            @NotBlank String contractNo,
            @NotBlank String customerPartyId,
            @NotBlank String startDate,
            @NotBlank String expectedEndDate,
            RentalContract.RateUnit rateUnit,
            BigDecimal rateAmount,
            BigDecimal depositAmount,
            String notes,
            List<RentalContractLineRequest> lines
    ) {}

    public record RentalContractLineResponse(
            String id,
            String rentalItemId,
            BigDecimal quantity,
            BigDecimal unitRate,
            BigDecimal totalAmount
    ) {}

    public record RentalContractResponse(
            String id,
            String contractNo,
            String customerPartyId,
            String startDate,
            String expectedEndDate,
            String actualEndDate,
            RentalContract.RateUnit rateUnit,
            BigDecimal rateAmount,
            BigDecimal depositAmount,
            BigDecimal damageFee,
            BigDecimal totalAmount,
            RentalContract.Status status,
            String invoiceId,
            String notes,
            List<RentalContractLineResponse> lines,
            long createdAt,
            long updatedAt
    ) {}

    public record ReturnRentalContractRequest(
            String actualEndDate,
            BigDecimal damageFee,
            String notes
    ) {}

    public record RentalUtilizationSummary(
            long totalItems,
            long rentedItems,
            long availableItems,
            double utilizationPercentage
    ) {}

    // --- Work Orders ---

    public record WorkOrderCreateRequest(
            @NotBlank String ticketNo,
            String customerPartyId,
            String customerName,
            @NotBlank String title,
            String description,
            String assignedEmployeeId,
            WorkOrder.Priority priority,
            String promisedAt
    ) {}

    public record AddLaborLineRequest(
            @NotBlank String description,
            BigDecimal hours,
            BigDecimal hourlyRate
    ) {}

    public record AddPartsLineRequest(
            @NotBlank String itemCode,
            @NotBlank String itemName,
            BigDecimal quantity,
            BigDecimal unitPrice
    ) {}

    public record UpdateWorkOrderStatusRequest(
            @NotNull WorkOrder.Status status,
            String overrideNote
    ) {}

    public record WorkOrderLaborLineResponse(
            String id,
            String description,
            BigDecimal hours,
            BigDecimal hourlyRate,
            BigDecimal totalAmount
    ) {}

    public record WorkOrderPartsLineResponse(
            String id,
            String itemCode,
            String itemName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount
    ) {}

    public record WorkOrderResponse(
            String id,
            String ticketNo,
            String customerPartyId,
            String customerName,
            String title,
            String description,
            String assignedEmployeeId,
            WorkOrder.Priority priority,
            WorkOrder.Status status,
            String promisedAt,
            BigDecimal laborTotal,
            BigDecimal partsTotal,
            BigDecimal grandTotal,
            String invoiceId,
            String overrideNote,
            List<WorkOrderLaborLineResponse> laborLines,
            List<WorkOrderPartsLineResponse> partsLines,
            long createdAt,
            long updatedAt
    ) {}

    // --- Bookings ---

    public record BookableResourceCreateRequest(
            @NotBlank String code,
            @NotBlank String name,
            String nameEn,
            BookableResource.Kind kind,
            Integer capacity,
            String location
    ) {}

    public record BookableResourceResponse(
            String id,
            String code,
            String name,
            String nameEn,
            BookableResource.Kind kind,
            Integer capacity,
            String location,
            boolean active,
            long createdAt,
            long updatedAt
    ) {}

    public record ResourceBookingCreateRequest(
            @NotBlank String resourceId,
            @NotBlank String title,
            String customerPartyId,
            String customerName,
            long startTime,
            long endTime,
            String notes
    ) {}

    public record ResourceBookingResponse(
            String id,
            String resourceId,
            String title,
            String customerPartyId,
            String customerName,
            long startTime,
            long endTime,
            ResourceBooking.Status status,
            String notes,
            long createdAt,
            long updatedAt
    ) {}
}
