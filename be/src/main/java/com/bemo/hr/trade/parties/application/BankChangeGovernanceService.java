package com.bemo.hr.trade.parties.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.parties.domain.BankChangeRequest;
import com.bemo.hr.trade.parties.infrastructure.BankChangeRequestRepository;
import com.bemo.hr.approval.SegregationOfDutiesService;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.party.BusinessPartyRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BankChangeGovernanceService {

    private final BankChangeRequestRepository bankChangeRequestRepository;
    private final BusinessPartyRepository businessPartyRepository;
    private final SegregationOfDutiesService segregationOfDutiesService;
    private final AuditService auditService;

    public BankChangeGovernanceService(BankChangeRequestRepository bankChangeRequestRepository,
                                       BusinessPartyRepository businessPartyRepository,
                                       SegregationOfDutiesService segregationOfDutiesService,
                                       AuditService auditService) {
        this.bankChangeRequestRepository = bankChangeRequestRepository;
        this.businessPartyRepository = businessPartyRepository;
        this.segregationOfDutiesService = segregationOfDutiesService;
        this.auditService = auditService;
    }

    @Transactional
    public BankChangeRequest requestBankChange(BankChangeRequest.PartyType partyType, String partyId, String oldIban, String newIban, String oldBankName, String newBankName, String reason, String requestedBy) {
        BankChangeRequest request = new BankChangeRequest(partyType, partyId, oldIban, newIban, oldBankName, newBankName, reason, requestedBy);
        return bankChangeRequestRepository.save(request);
    }

    @Transactional
    public BankChangeRequest approveBankChange(String requestId, String approverUsername) {
        BankChangeRequest request = getRequest(requestId);
        segregationOfDutiesService.validateRequesterNotApprover(request.getRequestedBy(), approverUsername, false);
        var party = businessPartyRepository.findById(request.getPartyId())
                .orElseThrow(() -> new BusinessRuleException("Business party not found", "BUSINESS_PARTY_NOT_FOUND", HttpStatus.NOT_FOUND));
        request.approve(approverUsername);
        party.verifyBank(request.getNewIban(), approverUsername, java.time.Instant.now());
        businessPartyRepository.save(party);
        BankChangeRequest saved = bankChangeRequestRepository.save(request);
        auditService.record("BANK_CHANGE_APPROVED", "BUSINESS_PARTY", party.getId(), approverUsername,
                "{\"requestId\":\"" + request.getId() + "\",\"requestedBy\":\"" + request.getRequestedBy() + "\"}", null);
        return saved;
    }

    @Transactional
    public BankChangeRequest rejectBankChange(String requestId, String approverUsername, String reason) {
        BankChangeRequest request = getRequest(requestId);
        request.reject(approverUsername, reason);
        return bankChangeRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<BankChangeRequest> getPendingRequests() {
        return bankChangeRequestRepository.findByStatus(BankChangeRequest.Status.PENDING);
    }

    private BankChangeRequest getRequest(String id) {
        return bankChangeRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Bank change request not found", "BANK_CHANGE_REQUEST_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
