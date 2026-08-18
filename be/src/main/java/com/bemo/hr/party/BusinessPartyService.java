package com.bemo.hr.party;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class BusinessPartyService {
    private final BusinessPartyRepository businessPartyRepository;
    private final com.bemo.hr.audit.application.AuditService auditService;

    List<BusinessPartyApi.Response> list() {
        log.debug("list called");
        return businessPartyRepository.findAllByOrderByNameAsc().stream().map(this::response).toList();
    }

    @Transactional
    BusinessPartyApi.Response create(BusinessPartyApi.Request request) {
        log.debug("create called with code={}, name={}", request.code(), request.name());
        validateUniqueCode(request.code(), null);
        validateManagedType(request.managedType());
        validateFields(request);
        var party = businessPartyRepository.save(new BusinessParty(
                request.code(), request.name(), request.nameEn(), request.partyType(),
                request.contactPerson(), request.phone(), request.email(), request.address(),
                request.notes(), request.active(),
                request.managedType(), request.responsiblePartyId(),
                request.relationshipStartDate(), request.relationshipEndDate(),
                request.currencyCode(), request.invoicePolicy(), request.paymentTerms(),
                request.taxId(), request.bankAccount()));
        party.updateSupplierProfile(request.supplierCategory(), request.riskLevel(), request.ownerUserId());

        auditService.record("CREATE", "BUSINESS_PARTY", party.getId(), getCurrentUser(),
                "{\"code\":\"" + party.getCode() + "\",\"name\":\"" + party.getName() + "\"}", null);
        log.info("BusinessParty {} created successfully", party.getId());
        return response(party);
    }

    @Transactional
    BusinessPartyApi.Response update(String id, BusinessPartyApi.Request request) {
        log.debug("update called with id={}", id);
        var party = require(id);
        if (request.version() == null || request.version() != party.getVersion()) {
            log.warn("Validation failed: version conflict for party {}", id);
            throw new BusinessRuleException("This business party changed since it was loaded. Refresh and try again.", "PTY_VERSION_CONFLICT", HttpStatus.CONFLICT);
        }
        validateUniqueCode(request.code(), id);
        validateManagedType(request.managedType());
        validateFields(request);
        party.update(request.code(), request.name(), request.nameEn(), request.partyType(),
                request.contactPerson(), request.phone(), request.email(), request.address(),
                request.notes(), request.active(),
                request.managedType(), request.responsiblePartyId(),
                request.relationshipStartDate(), request.relationshipEndDate(),
                request.currencyCode(), request.invoicePolicy(), request.paymentTerms(),
                request.taxId(), request.bankAccount());
        party.updateSupplierProfile(request.supplierCategory(), request.riskLevel(), request.ownerUserId());

        auditService.record("UPDATE", "BUSINESS_PARTY", party.getId(), getCurrentUser(),
                "{\"code\":\"" + party.getCode() + "\",\"name\":\"" + party.getName() + "\"}", null);
        log.info("BusinessParty {} updated successfully", party.getId());
        return response(party);
    }

    @Transactional
    void deactivate(String id) {
        log.debug("deactivate called with id={}", id);
        BusinessParty p = require(id);
        p.deactivate();
        auditService.record("DEACTIVATE", "BUSINESS_PARTY", p.getId(), getCurrentUser(), "Deactivated", null);
        log.info("BusinessParty {} deactivated successfully", id);
    }

    @Transactional
    int cleanupInvalidPhone() {
        log.debug("cleanupInvalidPhone called");
        var parties = businessPartyRepository.findAllByOrderByNameAsc();
        int count = 0;
        for (var p : parties) {
            if ("NOT-A-PHONE".equalsIgnoreCase(p.getPhone())) {
                p.clearPhone();
                count++;
            }
        }
        businessPartyRepository.flush();
        log.info("cleanupInvalidPhone completed, cleaned {} records", count);
        return count;
    }

    private String getCurrentUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null && !auth.getName().isBlank()) ? auth.getName() : "system";
    }

    private BusinessParty require(String id) {
        return businessPartyRepository.findById(id).orElseThrow(() -> new NotFoundException("Business party not found.", "PTY_NOT_FOUND"));
    }

    @Transactional
    BusinessPartyApi.Response createSupplierRequest(SupplierOnboardingApi.SupplierRequest request) {
        log.debug("createSupplierRequest called with code={}, taxId={}", request.code(), request.taxId());
        validateUniqueCode(request.code(), null);
        if (!request.taxId().matches("^[A-Za-z0-9\\-]{6,50}$")) {
            log.warn("Validation failed: invalid tax ID format: {}", request.taxId());
            throw new BusinessRuleException("Invalid tax ID format.", "PTY_INVALID_TAX_ID", HttpStatus.CONFLICT);
        }
        if (!businessPartyRepository.findByTaxIdIgnoreCase(request.taxId()).isEmpty()) {
            log.warn("Validation failed: duplicate tax ID: {}", request.taxId());
            throw new BusinessRuleException("A supplier with this tax ID already exists.", "SUPPLIER_DUPLICATE_TAX_ID", HttpStatus.CONFLICT);
        }
        BusinessParty party = new BusinessParty(request.code(), request.name(), request.nameEn(), "SUPPLIER",
                request.contactPerson(), request.phone(), request.email(), request.address(), request.notes(), false,
                "DIRECT", null, null, null, request.currencyCode(), "E_INVOICE", request.paymentTerms(),
                request.taxId(), null);
        party.updateSupplierProfile(request.supplierCategory(), request.riskLevel(), request.ownerUserId());
        party.beginSupplierRequest();
        businessPartyRepository.save(party);
        auditService.record("CREATE_REQUEST", "SUPPLIER_ONBOARDING", party.getId(), getCurrentUser(),
                "{\"code\":\"" + party.getCode() + "\",\"taxId\":\"" + party.getTaxId() + "\"}", null);
        log.info("SupplierOnboarding request {} created successfully", party.getId());
        return response(party);
    }

    BusinessParty requireParty(String id) {
        return require(id);
    }

    BusinessPartyApi.Response toResponse(BusinessParty party) {
        return response(party);
    }

    String currentUser() {
        return getCurrentUser();
    }

    private void validateUniqueCode(String code, String currentId) {
        boolean duplicate = currentId == null ? businessPartyRepository.existsByCodeIgnoreCase(code)
                : businessPartyRepository.existsByCodeIgnoreCaseAndIdNot(code, currentId);
        if (duplicate)
            throw new BusinessRuleException("Business party code already exists.", "PTY_CODE_EXISTS", HttpStatus.CONFLICT);
    }

    private void validateManagedType(String managedType) {
        if (managedType != null && !managedType.isBlank()
                && !java.util.Set.of("DIRECT", "MANAGED").contains(managedType)) {
            throw new BusinessRuleException("Invalid relationship type. Allowed: DIRECT, MANAGED.", "PTY_INVALID_RELATIONSHIP_TYPE", HttpStatus.CONFLICT);
        }
    }

    private void validateFields(BusinessPartyApi.Request r) {
        if (r.email() != null && !r.email().isBlank()
                && !r.email().matches("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new BusinessRuleException("Invalid email format.", "PTY_INVALID_EMAIL", HttpStatus.CONFLICT);
        }
        if (r.taxId() != null && !r.taxId().isBlank()
                && !r.taxId().matches("^[A-Za-z0-9\\-]{6,50}$")) {
            throw new BusinessRuleException("Invalid tax ID format.", "PTY_INVALID_TAX_ID", HttpStatus.CONFLICT);
        }
        if (r.managedType() != null && r.managedType().equals("MANAGED")
                && (r.responsiblePartyId() == null || r.responsiblePartyId().isBlank())) {
            throw new BusinessRuleException("Responsible partner is required for managed suppliers.", "PTY_RESPONSIBLE_PARTY_REQUIRED", HttpStatus.CONFLICT);
        }
    }

    private BusinessPartyApi.Response response(BusinessParty party) {
        return new BusinessPartyApi.Response(party.getId(), party.getCode(), party.getName(), party.getNameEn(),
                party.getPartyType(), party.getContactPerson(), party.getPhone(), party.getEmail(), party.getAddress(),
                party.getNotes(), party.getManagedType(), party.getResponsiblePartyId(),
                party.getRelationshipStartDate(), party.getRelationshipEndDate(),
                party.getCurrencyCode(), party.getInvoicePolicy(), party.getPaymentTerms(),
                party.getTaxId(), party.getBankAccount(),
                party.getOnboardingStatus(), party.getSupplierCategory(), party.getRiskLevel(),
                party.getOwnerUserId(), party.getApprovalInstanceId(), party.isBankVerified(),
                party.getBankVerifiedAt() == null ? null : party.getBankVerifiedAt().toEpochMilli(), party.getBankVerifiedBy(),
                party.isActive(), party.getVersion(), party.getCreatedAt(), party.getUpdatedAt());
    }
}
