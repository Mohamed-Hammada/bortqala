package com.bemo.hr.verticals.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public final class SpecializedVerticalsApi {

    private SpecializedVerticalsApi() {}

    // --- School & Education DTOs ---
    public record StudentEnrollmentResponse(
            String id,
            String studentCode,
            String studentName,
            String gradeLevel,
            String academicYear,
            String guardianName,
            String guardianPhone,
            BigDecimal totalTuitionFee,
            BigDecimal transportFee,
            BigDecimal booksFee,
            BigDecimal totalAnnualDue,
            String status,
            long createdAt,
            List<TuitionInvoiceResponse> installmentInvoices
    ) {}

    public record TuitionInvoiceResponse(
            String id,
            String enrollmentId,
            String invoiceNumber,
            String installmentName,
            long dueDate,
            BigDecimal amountDue,
            BigDecimal amountPaid,
            String status,
            long createdAt
    ) {}

    public record RegisterStudentPayload(
            @NotBlank String studentCode,
            @NotBlank String studentName,
            @NotBlank String gradeLevel,
            @NotBlank String academicYear,
            @NotBlank String guardianName,
            String guardianPhone,
            @NotNull BigDecimal totalTuitionFee,
            BigDecimal transportFee,
            BigDecimal booksFee,
            Integer installmentsCount
    ) {}

    // --- Tourism & Travel DTOs ---
    public record TourismBookingResponse(
            String id,
            String bookingCode,
            String customerName,
            String packageName,
            String destination,
            long travelDate,
            long returnDate,
            int travelersCount,
            BigDecimal sellingPrice,
            BigDecimal hotelCost,
            BigDecimal flightCost,
            BigDecimal excursionCost,
            BigDecimal totalDirectCost,
            BigDecimal grossMargin,
            BigDecimal grossMarginPercentage,
            String status,
            long createdAt
    ) {}

    public record CreateBookingPayload(
            @NotBlank String bookingCode,
            @NotBlank String customerName,
            @NotBlank String packageName,
            @NotBlank String destination,
            @NotNull Long travelDate,
            @NotNull Long returnDate,
            @NotNull @Positive Integer travelersCount,
            @NotNull BigDecimal sellingPrice,
            BigDecimal hotelCost,
            BigDecimal flightCost,
            BigDecimal excursionCost
    ) {}

    // --- Customs Clearance DTOs ---
    public record CustomsDeclarationResponse(
            String id,
            String fileNumber,
            String importerName,
            String portOfEntry,
            String billOfLadingNumber,
            String customsCertificateNumber,
            BigDecimal dutyDisbursementAmount,
            BigDecimal portHandlingAmount,
            BigDecimal clearanceServiceFee,
            BigDecimal totalInvoiceAmount,
            String status,
            long createdAt
    ) {}

    public record OpenDeclarationPayload(
            @NotBlank String fileNumber,
            @NotBlank String importerName,
            @NotBlank String portOfEntry,
            @NotBlank String billOfLadingNumber,
            String customsCertificateNumber,
            BigDecimal dutyDisbursementAmount,
            BigDecimal portHandlingAmount,
            @NotNull BigDecimal clearanceServiceFee
    ) {}

    // --- 3PL Logistics DTOs ---
    public record ThreePlContractResponse(
            String id,
            String contractCode,
            String clientName,
            String warehouseName,
            int palletCapacity,
            BigDecimal ratePerPalletMonthly,
            BigDecimal handlingInRatePerPallet,
            BigDecimal handlingOutRatePerPallet,
            BigDecimal estimatedMonthlyRevenue,
            String billingFrequency,
            String status,
            long createdAt
    ) {}

    public record Create3plContractPayload(
            @NotBlank String contractCode,
            @NotBlank String clientName,
            @NotBlank String warehouseName,
            @NotNull @Positive Integer palletCapacity,
            @NotNull BigDecimal ratePerPalletMonthly,
            BigDecimal handlingInRatePerPallet,
            BigDecimal handlingOutRatePerPallet,
            String billingFrequency
    ) {}

    // --- Overview Summary DTO ---
    public record VerticalsSummaryResponse(
            int totalActiveStudents,
            BigDecimal totalTuitionBilled,
            int totalActiveBookings,
            BigDecimal totalTourismRevenue,
            BigDecimal averageTourismMarginPct,
            int totalOpenCustomsFiles,
            BigDecimal totalDutyDisbursements,
            int totalActive3plContracts,
            int total3plPalletCapacity
    ) {}
}
