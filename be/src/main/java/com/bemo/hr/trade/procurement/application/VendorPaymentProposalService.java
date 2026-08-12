package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.procurement.domain.VendorPaymentProposal;
import com.bemo.hr.trade.procurement.infrastructure.VendorPaymentProposalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class VendorPaymentProposalService {

    private final VendorPaymentProposalRepository repository;

    public VendorPaymentProposalService(VendorPaymentProposalRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public VendorPaymentProposal createProposal(String supplierId, String invoiceId, BigDecimal proposedAmount, LocalDate dueDate) {
        VendorPaymentProposal proposal = new VendorPaymentProposal(supplierId, invoiceId, proposedAmount, dueDate);
        return repository.save(proposal);
    }

    @Transactional
    public VendorPaymentProposal approveProposal(String id) {
        VendorPaymentProposal proposal = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Payment proposal not found", "PAYMENT_PROPOSAL_NOT_FOUND", HttpStatus.NOT_FOUND));
        proposal.approve();
        return repository.save(proposal);
    }

    @Transactional
    public VendorPaymentProposal executeProposal(String id) {
        VendorPaymentProposal proposal = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Payment proposal not found", "PAYMENT_PROPOSAL_NOT_FOUND", HttpStatus.NOT_FOUND));
        proposal.execute();
        return repository.save(proposal);
    }

    @Transactional(readOnly = true)
    public List<VendorPaymentProposal> getProposalsForSupplier(String supplierId) {
        return repository.findBySupplierId(supplierId);
    }
}
