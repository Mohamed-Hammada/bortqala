package com.bemo.hr.medical.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class MedicalClinicApi {

    private MedicalClinicApi() {}

    public record RegisterPatientRequest(
            String nationalId,
            @NotBlank String fullName,
            @NotBlank String phone,
            String gender,
            String birthDate,
            String bloodGroup,
            String allergiesText,
            String notes,
            String emergencyContactName,
            String emergencyContactPhone
    ) {}

    public record UpdatePatientRequest(
            String nationalId,
            @NotBlank String fullName,
            @NotBlank String phone,
            String gender,
            String birthDate,
            String bloodGroup,
            String allergiesText,
            String notes,
            String emergencyContactName,
            String emergencyContactPhone
    ) {}

    public record PatientResponse(
            String id,
            String mrn,
            String nationalId,
            String fullName,
            String phone,
            String gender,
            String birthDate,
            String bloodGroup,
            String allergiesText,
            String notes,
            String emergencyContactName,
            String emergencyContactPhone,
            long createdAt,
            long updatedAt
    ) {}

    public record DuplicateCheckResponse(
            boolean duplicateFound,
            List<PatientResponse> matchingPatients
    ) {}

    public record QueueVisitRequest(
            @NotBlank String patientId,
            @NotBlank String doctorEmployeeId,
            String visitDate,
            BigDecimal feeCharged,
            BigDecimal insuranceCovered,
            String paymentMethod
    ) {}

    public record CompleteVisitRequest(
            String chiefComplaint,
            String diagnosisIcd,
            String diagnosisNotes,
            BigDecimal feeCharged,
            BigDecimal insuranceCovered,
            String paymentMethod,
            List<PrescriptionLineRequest> prescriptionLines
    ) {}

    public record PrescriptionLineRequest(
            @NotBlank String drugName,
            @NotBlank String dose,
            @NotBlank String frequency,
            @NotBlank String duration,
            String instructions
    ) {}

    public record PrescriptionLineResponse(
            String id,
            String drugName,
            String dose,
            String frequency,
            String duration,
            String instructions,
            long createdAt
    ) {}

    public record ClinicVisitResponse(
            String id,
            String patientId,
            String patientName,
            String patientMrn,
            String patientPhone,
            String doctorEmployeeId,
            String doctorName,
            String visitDate,
            long visitTime,
            int token,
            String status,
            String chiefComplaint,
            String diagnosisIcd,
            String diagnosisNotes,
            BigDecimal feeCharged,
            BigDecimal insuranceCovered,
            BigDecimal patientShare,
            String paymentMethod,
            List<PrescriptionLineResponse> prescriptionLines,
            long createdAt,
            long updatedAt
    ) {}

    public record DoctorCommissionStatementResponse(
            String doctorEmployeeId,
            String doctorName,
            String period,
            int completedVisitsCount,
            BigDecimal totalRevenue,
            BigDecimal commissionRatePercent,
            BigDecimal commissionAmount,
            List<ClinicVisitResponse> visits
    ) {}

    public record NationalIdParseResponse(
            boolean valid,
            String nationalId,
            String birthDate,
            String gender,
            String governorateCode,
            String governorateName,
            String errorMessage
    ) {}

    // WP-21 EMR Depth DTOs
    public record AddAllergyRequest(
            @NotBlank String substance,
            String severity,
            String reactionNotes
    ) {}

    public record PatientAllergyDto(
            String id,
            String patientId,
            String substance,
            String severity,
            String reactionNotes,
            long notedAt
    ) {}

    public record AddConditionRequest(
            String icdCode,
            @NotBlank String label,
            boolean chronic,
            String onsetDate,
            String status,
            String notes
    ) {}

    public record UpdateConditionRequest(
            String icdCode,
            @NotBlank String label,
            boolean chronic,
            String onsetDate,
            String status,
            String notes
    ) {}

    public record PatientConditionDto(
            String id,
            String patientId,
            String icdCode,
            String label,
            boolean chronic,
            String onsetDate,
            String status,
            String notes,
            long createdAt
    ) {}

    public record RecordVitalsRequest(
            Integer systolicBp,
            Integer diastolicBp,
            Integer pulse,
            BigDecimal tempC,
            Integer spo2,
            BigDecimal weightKg,
            BigDecimal heightCm,
            String notes
    ) {}

    public record VisitVitalsDto(
            String id,
            String visitId,
            String patientId,
            Integer systolicBp,
            Integer diastolicBp,
            Integer pulse,
            BigDecimal tempC,
            Integer spo2,
            BigDecimal weightKg,
            BigDecimal heightCm,
            BigDecimal bmi,
            String notes,
            long recordedAt
    ) {}

    public record UploadDocumentRequest(
            String visitId,
            @NotBlank String documentKind,
            @NotBlank String fileName,
            String contentType,
            Long fileSize,
            String storagePath,
            String notes
    ) {}

    public record PatientDocumentDto(
            String id,
            String patientId,
            String visitId,
            String documentKind,
            String fileName,
            String contentType,
            Long fileSize,
            String storagePath,
            String notes,
            long uploadedAt
    ) {}

    public record SignConsentRequest(
            String visitId,
            @NotBlank String templateKey,
            @NotBlank String title,
            @NotBlank String bodyText,
            @NotBlank String signedByName,
            String signedByRelation,
            String ipAddress
    ) {}

    public record ConsentFormDto(
            String id,
            String patientId,
            String visitId,
            String templateKey,
            String title,
            String bodyText,
            String signedByName,
            String signedByRelation,
            long signedAt,
            String ipAddress
    ) {}

    public record PatientChartResponse(
            PatientResponse patient,
            List<PatientAllergyDto> allergies,
            boolean hasSevereAllergies,
            List<PatientConditionDto> conditions,
            List<VisitVitalsDto> vitalsHistory,
            List<ClinicVisitResponse> recentVisits,
            List<PatientDocumentDto> documents,
            List<ConsentFormDto> consents
    ) {}

    // WP-22 Appointments & Rosters DTOs
    public record DoctorRosterDto(
            String id,
            String doctorEmployeeId,
            String doctorName,
            int weekday,
            String startTime,
            String endTime,
            int slotMinutes,
            int maxPatientsPerSlot,
            String validFrom,
            String validTo,
            boolean active
    ) {}

    public record SaveDoctorRosterRequest(
            @NotBlank String doctorEmployeeId,
            int weekday,
            @NotBlank String startTime,
            @NotBlank String endTime,
            int slotMinutes,
            int maxPatientsPerSlot,
            String validFrom,
            String validTo
    ) {}

    public record AvailableSlotDto(
            String startTime,
            long startsAt,
            int durationMinutes,
            boolean available,
            String bookedAppointmentId
    ) {}

    public record BookAppointmentRequest(
            @NotBlank String patientId,
            @NotBlank String doctorEmployeeId,
            @NotBlank String visitDate,
            @NotBlank String startTime,
            int durationMinutes,
            String source,
            String reason
    ) {}

    public record ClinicAppointmentResponse(
            String id,
            String patientId,
            String patientName,
            String patientMrn,
            String patientPhone,
            String doctorEmployeeId,
            String doctorName,
            String visitDate,
            String startTime,
            long startsAt,
            int durationMinutes,
            String status,
            String source,
            String reason,
            String clinicVisitId,
            Long reminderSentAt,
            long createdAt,
            long updatedAt
    ) {}

    public record AppointmentMetricsResponse(
            String period,
            int totalAppointments,
            int bookedCount,
            int confirmedCount,
            int checkedInCount,
            int completedCount,
            int noShowCount,
            int cancelledCount,
            BigDecimal noShowRatePercent
    ) {}

    // WP-23 Pharmacy & Narcotics DTOs
    public record PharmacyItemDto(
            String id,
            String itemId,
            String tradeName,
            String genericName,
            String dosageForm,
            String strengthText,
            boolean controlled,
            String controlSchedule
    ) {}

    public record SavePharmacyItemRequest(
            @NotBlank String itemId,
            @NotBlank String tradeName,
            String genericName,
            String dosageForm,
            String strengthText,
            boolean controlled,
            String controlSchedule
    ) {}

    public record BatchFefoSuggestionDto(
            String batchNumber,
            String expiryDate,
            BigDecimal availableQuantity,
            boolean expired,
            boolean nearExpiry
    ) {}

    public record DispenseLineRequest(
            String prescriptionLineId,
            @NotBlank String pharmacyItemId,
            String batchNumber,
            String expiryDate,
            @NotNull BigDecimal quantity
    ) {}

    public record DispensePrescriptionRequest(
            String secondSignerId,
            String notes,
            List<DispenseLineRequest> lines
    ) {}

    public record PharmacyDispenseRecordDto(
            String id,
            String prescriptionId,
            String patientId,
            String patientName,
            String patientMrn,
            String prescriberDoctorId,
            String prescriberDoctorName,
            String dispenserUserId,
            String dispenserUserName,
            String secondSignerId,
            String secondSignerName,
            String status,
            boolean controlled,
            String notes,
            List<PharmacyDispenseLineDto> lines,
            long createdAt
    ) {}

    public record PharmacyDispenseLineDto(
            String id,
            String prescriptionLineId,
            String pharmacyItemId,
            String tradeName,
            String batchNumber,
            String expiryDate,
            BigDecimal quantityDispensed,
            long createdAt
    ) {}

    public record NarcoticsRegisterEntryDto(
            String id,
            String dispenseRecordId,
            String pharmacyItemId,
            String tradeName,
            String patientMrn,
            String patientName,
            String prescriberDoctorName,
            String dispenserUserName,
            String secondSignerName,
            String batchNumber,
            BigDecimal quantity,
            String reason,
            long signedAt
    ) {}

    // WP-24 Lab & Imaging DTOs
    public record LabTestItemDto(
            String id,
            String code,
            String category,
            String name,
            String sampleType,
            String normalRangeText,
            BigDecimal price
    ) {}

    public record SaveLabTestItemRequest(
            @NotBlank String code,
            String category,
            @NotBlank String name,
            String sampleType,
            String normalRangeText,
            BigDecimal price
    ) {}

    public record CreateLabOrderRequest(
            @NotBlank String patientId,
            String visitId,
            @NotBlank String doctorEmployeeId,
            @NotBlank String testId,
            String externalLabPartyId,
            String externalLabName
    ) {}

    public record EnterLabResultRequest(
            @NotBlank String resultValueText,
            String resultFlag,
            String resultNotes,
            String attachmentId,
            String attachmentFilename
    ) {}

    public record SendOutLabOrderRequest(
            String externalLabPartyId,
            String externalLabName
    ) {}

    public record LabOrderDto(
            String id,
            String patientId,
            String patientName,
            String patientMrn,
            String visitId,
            String doctorEmployeeId,
            String doctorName,
            String testId,
            String category,
            String testCode,
            String testName,
            String status,
            long orderedAt,
            Long collectedAt,
            Long sentOutAt,
            Long resultedAt,
            Long validatedAt,
            String resultValueText,
            String resultFlag,
            String resultNotes,
            String externalLabPartyId,
            String externalLabName,
            String attachmentId,
            String attachmentFilename,
            boolean isCriticalAcknowledged,
            Long criticalAcknowledgedAt
    ) {}

    // WP-25 Insurance & Claims DTOs
    public record InsurancePayerDto(
            String id,
            String name,
            String type,
            String contactPhone,
            String contactEmail,
            boolean active
    ) {}

    public record SaveInsurancePayerRequest(
            @NotBlank String name,
            @NotBlank String type,
            String contactPhone,
            String contactEmail,
            Boolean active
    ) {}

    public record InsurancePlanDto(
            String id,
            String payerId,
            String name,
            BigDecimal coveragePercent,
            BigDecimal copayFlat,
            BigDecimal annualLimit,
            String exclusionsText,
            boolean active
    ) {}

    public record SaveInsurancePlanRequest(
            @NotBlank String payerId,
            @NotBlank String name,
            BigDecimal coveragePercent,
            BigDecimal copayFlat,
            BigDecimal annualLimit,
            String exclusionsText,
            Boolean active
    ) {}

    public record PatientInsurancePolicyDto(
            String id,
            String patientId,
            String planId,
            String planName,
            String payerId,
            String payerName,
            String memberNumber,
            String validFrom,
            String validTo,
            boolean isPrimary
    ) {}

    public record AttachInsurancePolicyRequest(
            @NotBlank String patientId,
            @NotBlank String planId,
            @NotBlank String memberNumber,
            @NotBlank String validFrom,
            @NotBlank String validTo,
            Boolean isPrimary
    ) {}

    public record InsurancePreAuthorizationDto(
            String id,
            String payerId,
            String payerName,
            String patientId,
            String patientMrn,
            String patientName,
            String visitId,
            String procedureText,
            String approvalCode,
            BigDecimal requestedAmount,
            BigDecimal approvedAmount,
            String status,
            Long decidedAt
    ) {}

    public record RequestPreAuthorizationRequest(
            @NotBlank String payerId,
            @NotBlank String patientId,
            String visitId,
            @NotBlank String procedureText,
            @NotBlank String approvalCode,
            @NotNull BigDecimal requestedAmount
    ) {}

    public record DecidePreAuthorizationRequest(
            @NotBlank String status,
            BigDecimal approvedAmount
    ) {}

    public record InsuranceSplitCalculationResult(
            BigDecimal totalFee,
            BigDecimal coveragePercent,
            BigDecimal copayFlat,
            BigDecimal insurerShare,
            BigDecimal patientShare,
            boolean isPolicyValid
    ) {}

    public record CalculateInsuranceSplitRequest(
            @NotBlank String patientId,
            @NotNull BigDecimal feeCharged,
            String visitDate
    ) {}

    public record InsuranceClaimBatchDto(
            String id,
            String batchNumber,
            String payerId,
            String payerName,
            String period,
            String status,
            BigDecimal totalClaimedAmount,
            BigDecimal totalApprovedAmount,
            BigDecimal totalRejectedAmount,
            Long submittedAt,
            Long settledAt,
            String notes,
            List<InsuranceClaimLineDto> lines
    ) {}

    public record CreateClaimBatchRequest(
            @NotBlank String payerId,
            @NotBlank String period,
            String notes
    ) {}

    public record InsuranceClaimLineDto(
            String id,
            String batchId,
            String visitId,
            String patientId,
            String patientMrn,
            String patientName,
            String memberNumber,
            String procedureText,
            BigDecimal totalFee,
            BigDecimal insurerShare,
            BigDecimal patientShare,
            String status,
            String rejectionReason,
            String resubmittedLineId
    ) {}

    public record SettleClaimBatchRequest(
            List<SettleLineDecision> lineDecisions,
            String notes
    ) {}

    public record SettleLineDecision(
            @NotBlank String lineId,
            @NotBlank String decision,
            String rejectionReason
    ) {}

    public record ResubmitClaimLineRequest(
            @NotBlank String originalLineId,
            @NotBlank String newBatchId,
            BigDecimal adjustedInsurerShare,
            String notes
    ) {}

    // WP-26 Hospital Ops DTOs
    public record SaveHospitalWardRequest(
            @NotBlank String code,
            @NotBlank String name,
            String departmentId,
            boolean active
    ) {}

    public record HospitalWardDto(
            String id,
            String code,
            String name,
            String departmentId,
            boolean active,
            long createdAt,
            long updatedAt
    ) {}

    public record SaveHospitalRoomRequest(
            @NotBlank String wardId,
            @NotBlank String roomNumber,
            String roomType,
            boolean active
    ) {}

    public record HospitalRoomDto(
            String id,
            String wardId,
            String roomNumber,
            String roomType,
            boolean active,
            long createdAt,
            long updatedAt
    ) {}

    public record SaveHospitalBedRequest(
            @NotBlank String roomId,
            @NotBlank String bedNumber,
            String status,
            boolean active
    ) {}

    public record HospitalBedDto(
            String id,
            String roomId,
            String roomNumber,
            String wardId,
            String wardName,
            String bedNumber,
            String status,
            String currentAdmissionId,
            String currentPatientName,
            String currentPatientMrn,
            boolean active,
            long createdAt,
            long updatedAt
    ) {}

    public record AdmitPatientRequest(
            @NotBlank String patientId,
            @NotBlank String admittingDoctorId,
            @NotBlank String bedId,
            String chiefComplaint
    ) {}

    public record TransferPatientBedRequest(
            @NotBlank String targetBedId,
            String transferReason
    ) {}

    public record DischargePatientRequest(
            @NotBlank String dischargeSummary
    ) {}

    public record HospitalAdmissionDto(
            String id,
            String patientId,
            String patientMrn,
            String patientName,
            String admittingDoctorId,
            String admittingDoctorName,
            String currentBedId,
            String currentBedNumber,
            String currentRoomNumber,
            String currentWardName,
            String status,
            String chiefComplaint,
            long admittedAt,
            Long dischargedAt,
            String dischargeSummary,
            List<HospitalBedStayDto> bedStays,
            long createdAt,
            long updatedAt
    ) {}

    public record HospitalBedStayDto(
            String id,
            String admissionId,
            String bedId,
            String bedNumber,
            String roomNumber,
            String wardName,
            long startedAt,
            Long endedAt,
            String transferReason
    ) {}

    public record HospitalOccupancyMetricsDto(
            long totalBeds,
            long occupiedBeds,
            double occupancyRatePercent,
            double averageLengthOfStayDays
    ) {}

    public record ScheduleOtRequest(
            @NotBlank String theaterName,
            @NotBlank String patientId,
            @NotBlank String surgeonDoctorId,
            @NotBlank String surgeryType,
            long plannedStart,
            int durationMinutes
    ) {}

    public record CompleteOtSurgeryRequest(
            String anesthesiaNotes,
            String surgicalNotes
    ) {}

    public record HospitalOtScheduleDto(
            String id,
            String theaterName,
            String patientId,
            String patientMrn,
            String patientName,
            String surgeonDoctorId,
            String surgeonDoctorName,
            String surgeryType,
            String status,
            long plannedStart,
            int durationMinutes,
            Long actualStart,
            Long actualEnd,
            String anesthesiaNotes,
            String surgicalNotes,
            List<HospitalOtChargeDto> charges,
            long createdAt,
            long updatedAt
    ) {}

    public record AddOtChargeRequest(
            @NotBlank String itemName,
            @NotNull BigDecimal quantity,
            @NotNull BigDecimal unitPrice
    ) {}

    public record HospitalOtChargeDto(
            String id,
            String otScheduleId,
            String itemName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            long chargedAt
    ) {}

    public record CreateMarEntryRequest(
            @NotBlank String admissionId,
            @NotBlank String medicationName,
            @NotBlank String dose,
            String route,
            long dueAt
    ) {}

    public record AdministerMarEntryRequest(
            @NotBlank String status,
            String nurseId,
            String nurseName,
            String notes
    ) {}

    public record HospitalMarEntryDto(
            String id,
            String admissionId,
            String medicationName,
            String dose,
            String route,
            long dueAt,
            String status,
            Long administeredAt,
            String nurseId,
            String nurseName,
            String notes,
            long createdAt
    ) {}

    public record RecordFluidIoRequest(
            @NotBlank String admissionId,
            @NotBlank String type,
            @NotBlank String routeOrFluid,
            int amountMl,
            String recordedBy
    ) {}

    public record HospitalFluidIoEntryDto(
            String id,
            String admissionId,
            long entryTime,
            String type,
            String routeOrFluid,
            int amountMl,
            String recordedBy
    ) {}

    public record AddNursingNoteRequest(
            @NotBlank String admissionId,
            @NotBlank String nurseName,
            @NotBlank String noteText
    ) {}

    public record HospitalNursingNoteDto(
            String id,
            String admissionId,
            long recordedAt,
            String nurseName,
            String noteText
    ) {}

    // --- WP-27 Dental & Specialty Charting DTOs ---

    public record DentalRecordDto(
            String id,
            String patientId,
            String visitId,
            int toothNumber,
            String condition,
            String surface,
            String notes,
            long notedOn
    ) {}

    public record RecordToothConditionRequest(
            String visitId,
            int toothNumber,
            @NotBlank String condition,
            String surface,
            String notes,
            Long notedOn
    ) {}

    public record ToothStatusSummaryDto(
            int toothNumber,
            String condition,
            String surface,
            String notes,
            long notedOn
    ) {}

    public record PatientOdontogramDto(
            String patientId,
            String patientName,
            String patientMrn,
            List<ToothStatusSummaryDto> teeth,
            List<DentalRecordDto> history
    ) {}

    public record CreateDentalPlanRequest(
            @NotBlank String title
    ) {}

    public record AddDentalPlanItemRequest(
            int toothNumber,
            @NotBlank String procedureText,
            BigDecimal estimatedCost
    ) {}

    public record DentalTreatmentPlanItemDto(
            String id,
            String planId,
            int toothNumber,
            String procedureText,
            BigDecimal estimatedCost,
            String status,
            Long completedAt,
            String visitId
    ) {}

    public record DentalTreatmentPlanDto(
            String id,
            String patientId,
            String title,
            String status,
            long createdAt,
            long updatedAt,
            List<DentalTreatmentPlanItemDto> items
    ) {}

    public record MarkDentalItemDoneRequest(
            String visitId
    ) {}

    public record SaveExamTemplateRequest(
            @NotBlank String specialty,
            @NotBlank String name,
            @NotBlank String schemaJson
    ) {}

    public record ExamTemplateDto(
            String id,
            String specialty,
            String name,
            String schemaJson,
            boolean active,
            long createdAt,
            long updatedAt
    ) {}

    public record SubmitExamAnswerRequest(
            @NotBlank String visitId,
            @NotBlank String templateId,
            @NotBlank String answersJson,
            String recordedBy
    ) {}

    public record ExamAnswerDto(
            String id,
            String visitId,
            String templateId,
            String answersJson,
            long recordedAt,
            String recordedBy
    ) {}

    // Medical Vertical Depth DTOs
    public record PatientFamilyLinkDto(
            String id,
            String patientId,
            String guardianPatientId,
            String relationshipType,
            boolean isPrimaryPayer,
            String notes,
            long createdAt
    ) {}

    public record LinkFamilyMemberRequest(
            @NotBlank String guardianPatientId,
            @NotBlank String relationshipType,
            boolean isPrimaryPayer,
            String notes
    ) {}

    public record PediatricDoseCalculationRequest(
            @NotNull BigDecimal weightKg,
            @NotNull BigDecimal doseMgPerKgPerDay,
            @NotNull Integer frequencyPerDay,
            BigDecimal drugConcentrationMgPerMl
    ) {}

    public record PediatricDoseCalculationResponse(
            BigDecimal weightKg,
            BigDecimal dailyDoseMg,
            BigDecimal singleDoseMg,
            BigDecimal singleDoseMl,
            Integer frequencyPerDay,
            String administrationInstructions
    ) {}

    public record TelemedicineSessionDto(
            String id,
            String patientId,
            String doctorId,
            String doctorName,
            long scheduledTime,
            String meetingLink,
            String roomToken,
            String status,
            String clinicalNotes,
            long createdAt
    ) {}

    public record ScheduleTelemedicineSessionRequest(
            @NotBlank String patientId,
            @NotBlank String doctorId,
            @NotBlank String doctorName,
            long scheduledTime,
            String roomName
    ) {}

    public record MedicalLicenseRecordDto(
            String id,
            String practitionerId,
            String practitionerName,
            String licenseType,
            String licenseNumber,
            String issuingAuthority,
            long issueDate,
            long expiryDate,
            String status,
            long createdAt
    ) {}

    public record RegisterMedicalLicenseRequest(
            @NotBlank String practitionerId,
            @NotBlank String practitionerName,
            @NotBlank String licenseType,
            @NotBlank String licenseNumber,
            @NotBlank String issuingAuthority,
            long issueDate,
            long expiryDate
    ) {}
}
