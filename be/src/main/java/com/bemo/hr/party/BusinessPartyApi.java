package com.bemo.hr.party;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

final class BusinessPartyApi {
    private BusinessPartyApi() { }

    record Response(String id, String code, String name, String nameEn, String partyType,
                    String contactPerson, String phone, String email, String address,
                    String notes, String managedType, String responsiblePartyId,
                    String relationshipStartDate, String relationshipEndDate,
                    String currencyCode, String invoicePolicy, String paymentTerms, String taxId,
                    String bankAccount, boolean active, long version,
                    Instant createdAt, Instant updatedAt) { }

    record Request(@NotBlank @Size(max = 50) String code,
                   @NotBlank @Size(max = 160) String name,
                   @Size(max = 160) String nameEn,
                   @NotBlank @Size(max = 80) String partyType,
                   @Size(max = 160) String contactPerson,
                   @Size(max = 50) @jakarta.validation.constraints.Pattern(regexp = "^$|^[+0-9\\s\\-().]{7,30}$", message = "Invalid phone number format.") String phone,
                   @Size(max = 100) @jakarta.validation.constraints.Email(regexp = "^$|^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$", message = "Invalid email format.") String email,
                   @Size(max = 500) String address,
                   @Size(max = 1000) String notes,
                   @NotBlank @Size(max = 30) String managedType,
                   @Size(max = 36) String responsiblePartyId,
                   @Size(max = 10) String relationshipStartDate,
                   @Size(max = 10) String relationshipEndDate,
                   @NotBlank @Size(max = 10) String currencyCode,
                   @NotBlank @Size(max = 30) String invoicePolicy,
                   @NotBlank @Size(max = 30) String paymentTerms,
                   @Size(max = 50) String taxId,
                   @Size(max = 100) String bankAccount,
                   @NotNull Boolean active,
                   Long version) { }
}
