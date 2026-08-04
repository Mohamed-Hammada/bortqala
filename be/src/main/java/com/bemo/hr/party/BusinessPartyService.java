package com.bemo.hr.party;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class BusinessPartyService {
    private final BusinessPartyRepository businessPartyRepository;
    private final com.bemo.hr.audit.application.AuditService auditService;

    List<BusinessPartyApi.Response> list() {
        return businessPartyRepository.findAllByOrderByNameAsc().stream().map(this::response).toList();
    }

    @Transactional
    BusinessPartyApi.Response create(BusinessPartyApi.Request request) {
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

        auditService.record("CREATE", "BUSINESS_PARTY", party.getId(), getCurrentUser(),
                "{\"code\":\"" + party.getCode() + "\",\"name\":\"" + party.getName() + "\"}", null);
        return response(party);
    }

    @Transactional
    BusinessPartyApi.Response update(String id, BusinessPartyApi.Request request) {
        var party = require(id);
        if (request.version() == null || request.version() != party.getVersion()) {
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

        auditService.record("UPDATE", "BUSINESS_PARTY", party.getId(), getCurrentUser(),
                "{\"code\":\"" + party.getCode() + "\",\"name\":\"" + party.getName() + "\"}", null);
        return response(party);
    }

    @Transactional
    void deactivate(String id) {
        BusinessParty p = require(id);
        p.deactivate();
        auditService.record("DEACTIVATE", "BUSINESS_PARTY", p.getId(), getCurrentUser(), "Deactivated", null);
    }

    @Transactional
    int cleanupInvalidPhone() {
        var parties = businessPartyRepository.findAllByOrderByNameAsc();
        int count = 0;
        for (var p : parties) {
            if ("NOT-A-PHONE".equalsIgnoreCase(p.getPhone())) {
                p.clearPhone();
                count++;
            }
        }
        businessPartyRepository.flush();
        return count;
    }

    private String getCurrentUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null && !auth.getName().isBlank()) ? auth.getName() : "system";
    }

    private BusinessParty require(String id) {
        return businessPartyRepository.findById(id).orElseThrow(() -> new NotFoundException("Business party not found.", "PTY_NOT_FOUND"));
    }

    private void validateUniqueCode(String code, String currentId) {
        boolean duplicate = currentId == null ? businessPartyRepository.existsByCodeIgnoreCase(code)
                : businessPartyRepository.existsByCodeIgnoreCaseAndIdNot(code, currentId);
        if (duplicate) throw new BusinessRuleException("Business party code already exists.", "PTY_CODE_EXISTS", HttpStatus.CONFLICT);
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
                party.isActive(), party.getVersion(), party.getCreatedAt(), party.getUpdatedAt());
    }
}
