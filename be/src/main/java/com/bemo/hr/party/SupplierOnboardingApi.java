package com.bemo.hr.party;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public final class SupplierOnboardingApi {
    private SupplierOnboardingApi() {
    }

    public record SupplierRequest(
            @NotBlank @Size(max = 50) String code,
            @NotBlank @Size(max = 160) String name,
            @Size(max = 160) String nameEn,
            @Size(max = 160) String contactPerson,
            @Size(max = 50) String phone,
            @Size(max = 100) String email,
            @Size(max = 500) String address,
            @NotBlank @Size(max = 50) String taxId,
            @NotBlank @Size(max = 10) String currencyCode,
            @NotBlank @Size(max = 30) String paymentTerms,
            @Size(max = 50) String supplierCategory,
            @Size(max = 20) String riskLevel,
            @Size(max = 100) String ownerUserId,
            @Size(max = 1000) String notes) {
    }

    public record DocumentRequest(
            @NotBlank @Size(max = 50) String documentType,
            @Size(max = 100) String documentNumber,
            LocalDate issueDate,
            LocalDate expiryDate,
            @NotNull Boolean mandatory) {
    }

    public record DocumentResponse(String id, String documentType, String documentNumber, String fileName,
                                   String contentType, long fileSize,
                                   LocalDate issueDate, LocalDate expiryDate, boolean mandatory, boolean verified,
                                   String verifiedBy, Long verifiedAt, long createdAt, boolean expired) {
    }

    public record BankAccountRequest(
            @NotBlank @Size(max = 160) String accountName,
            @NotBlank @Size(max = 100) String iban,
            @NotBlank @Size(max = 160) String bankName,
            @NotBlank @Size(max = 10) String currencyCode,
            boolean primary) {
    }

    public record BankAccountResponse(String id, String accountName, String iban, String bankName,
                                      String currencyCode, boolean primary, String verificationStatus,
                                      String verifiedBy, Long verifiedAt, long createdAt) {
    }

    public record DuplicateResponse(List<DuplicateMatch> taxIdMatches, List<DuplicateMatch> bankMatches,
                                    boolean duplicateFound) {
        public DuplicateResponse(List<DuplicateMatch> taxIdMatches, List<DuplicateMatch> bankMatches) {
            this(taxIdMatches, bankMatches, !taxIdMatches.isEmpty() || !bankMatches.isEmpty());
        }
    }

    public record DuplicateMatch(String supplierId, String code, String name, String reason) {
    }

    public record ComplianceItem(String code, boolean passed, String explanation) {
    }

    public record Supplier360(
            BusinessPartyApi.Response supplier,
            List<DocumentResponse> documents,
            List<BankAccountResponse> bankAccounts,
            List<ComplianceItem> compliance,
            long documentCount,
            long expiredDocumentCount,
            long verifiedBankCount,
            boolean procurementAllowed,
            boolean paymentAllowed) {
    }

    public record TransitionRequest(@Size(max = 1000) String reason) {
    }
}
