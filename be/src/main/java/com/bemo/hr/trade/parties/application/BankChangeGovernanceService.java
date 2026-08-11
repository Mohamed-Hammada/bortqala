package com.bemo.hr.trade.parties.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.parties.domain.BankChangeRequest;
import com.bemo.hr.trade.parties.infrastructure.BankChangeRequestRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BankChangeGovernanceService {

    private final BankChangeRequestRepository bankChangeRequestRepository;

    public BankChangeGovernanceService(BankChangeRequestRepository bankChangeRequestRepository) {
        this.bankChangeRequestRepository = bankChangeRequestRepository;
    }

    @Transactional
    public BankChangeRequest requestBankChange(BankChangeRequest.PartyType partyType, String partyId, String oldIban, String newIban, String oldBankName, String newBankName, String reason, String requestedBy) {
        BankChangeRequest request = new BankChangeRequest(partyType, partyId, oldIban, newIban, oldBankName, newBankName, reason, requestedBy);
        return bankChangeRequestRepository.save(request);
    }

    @Transactional
    public BankChangeRequest approveBankChange(String requestId, String approverUsername) {
        BankChangeRequest request = getRequest(requestId);
        request.approve(approverUsername);
        return bankChangeRequestRepository.save(request);
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
