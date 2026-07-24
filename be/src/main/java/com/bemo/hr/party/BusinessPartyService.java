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

    List<BusinessPartyApi.Response> list() {
        return businessPartyRepository.findAllByOrderByNameAsc().stream().map(this::response).toList();
    }

    @Transactional
    BusinessPartyApi.Response create(BusinessPartyApi.Request request) {
        validateUniqueCode(request.code(), null);
        var party = businessPartyRepository.save(new BusinessParty(request.code(), request.name(), request.partyType(),
                request.contactPerson(), request.phone(), request.notes(), request.active()));
        return response(party);
    }

    @Transactional
    BusinessPartyApi.Response update(String id, BusinessPartyApi.Request request) {
        var party = require(id);
        if (request.version() == null || request.version() != party.getVersion()) {
            throw new BusinessRuleException("This business party changed since it was loaded. Refresh and try again.");
        }
        validateUniqueCode(request.code(), id);
        party.update(request.code(), request.name(), request.partyType(), request.contactPerson(), request.phone(),
                request.notes(), request.active());
        return response(party);
    }

    @Transactional
    void deactivate(String id) { require(id).deactivate(); }

    private BusinessParty require(String id) {
        return businessPartyRepository.findById(id).orElseThrow(() -> new NotFoundException("Business party not found."));
    }

    private void validateUniqueCode(String code, String currentId) {
        boolean duplicate = currentId == null ? businessPartyRepository.existsByCodeIgnoreCase(code)
                : businessPartyRepository.existsByCodeIgnoreCaseAndIdNot(code, currentId);
        if (duplicate) throw new BusinessRuleException("Business party code already exists.");
    }

    private BusinessPartyApi.Response response(BusinessParty party) {
        return new BusinessPartyApi.Response(party.getId(), party.getCode(), party.getName(), party.getPartyType(),
                party.getContactPerson(), party.getPhone(), party.getNotes(), party.isActive(), party.getVersion(),
                party.getCreatedAt(), party.getUpdatedAt());
    }
}
