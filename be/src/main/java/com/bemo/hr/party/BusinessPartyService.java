package com.bemo.hr.party;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import lombok.RequiredArgsConstructor;
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
        var party = businessPartyRepository.save(new BusinessParty(request.code(), request.name(), request.partyType(),
                request.contactPerson(), request.phone(), request.notes(), request.active(),
                request.managedType(), request.responsiblePartyId(), request.currencyCode(),
                request.invoicePolicy(), request.paymentTerms(), request.taxId(), request.bankAccount()));

        auditService.record("CREATE", "BUSINESS_PARTY", party.getId(), getCurrentUser(),
                "{\"code\":\"" + party.getCode() + "\",\"name\":\"" + party.getName() + "\"}", null);
        return response(party);
    }

    @Transactional
    BusinessPartyApi.Response update(String id, BusinessPartyApi.Request request) {
        var party = require(id);
        if (request.version() == null || request.version() != party.getVersion()) {
            throw new BusinessRuleException("This business party changed since it was loaded. Refresh and try again.");
        }
        validateUniqueCode(request.code(), id);
        validateManagedType(request.managedType());
        party.update(request.code(), request.name(), request.partyType(), request.contactPerson(), request.phone(),
                request.notes(), request.active(), request.managedType(), request.responsiblePartyId(),
                request.currencyCode(), request.invoicePolicy(), request.paymentTerms(), request.taxId(),
                request.bankAccount());

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

    private String getCurrentUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null && !auth.getName().isBlank()) ? auth.getName() : "system";
    }

    private BusinessParty require(String id) {
        return businessPartyRepository.findById(id).orElseThrow(() -> new NotFoundException("Business party not found."));
    }

    private void validateUniqueCode(String code, String currentId) {
        boolean duplicate = currentId == null ? businessPartyRepository.existsByCodeIgnoreCase(code)
                : businessPartyRepository.existsByCodeIgnoreCaseAndIdNot(code, currentId);
        if (duplicate) throw new BusinessRuleException("Business party code already exists.");
    }

    private void validateManagedType(String managedType) {
        if (managedType != null && !managedType.isBlank()
                && !java.util.Set.of("DIRECT", "AGENT", "BROKER", "CONTRACT", "OTHER").contains(managedType)) {
            throw new BusinessRuleException("Invalid managedType. Allowed: DIRECT, AGENT, BROKER, CONTRACT, OTHER.");
        }
    }

    private BusinessPartyApi.Response response(BusinessParty party) {
        return new BusinessPartyApi.Response(party.getId(), party.getCode(), party.getName(), party.getPartyType(),
                party.getContactPerson(), party.getPhone(), party.getNotes(),
                party.getManagedType(), party.getResponsiblePartyId(), party.getCurrencyCode(),
                party.getInvoicePolicy(), party.getPaymentTerms(), party.getTaxId(), party.getBankAccount(),
                party.isActive(), party.getVersion(), party.getCreatedAt(), party.getUpdatedAt());
    }
}
