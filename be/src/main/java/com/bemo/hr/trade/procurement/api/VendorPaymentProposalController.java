package com.bemo.hr.trade.procurement.api;

import com.bemo.hr.trade.procurement.application.VendorPaymentProposalService;
import com.bemo.hr.trade.procurement.domain.VendorPaymentProposal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/procurement/payment-proposals")
public class VendorPaymentProposalController {

    private final VendorPaymentProposalService proposalService;

    public VendorPaymentProposalController(VendorPaymentProposalService proposalService) {
        this.proposalService = proposalService;
    }

    public record CreateProposalPayload(String supplierId, String invoiceId, BigDecimal proposedAmount, LocalDate dueDate) {}

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER')")
    public VendorPaymentProposal createProposal(@RequestBody CreateProposalPayload payload) {
        return proposalService.createProposal(payload.supplierId(), payload.invoiceId(), payload.proposedAmount(), payload.dueDate());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public VendorPaymentProposal approveProposal(@PathVariable String id) {
        return proposalService.approveProposal(id);
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public VendorPaymentProposal executeProposal(@PathVariable String id) {
        return proposalService.executeProposal(id);
    }

    @GetMapping("/supplier/{supplierId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'VIEWER')")
    public List<VendorPaymentProposal> getProposalsForSupplier(@PathVariable String supplierId) {
        return proposalService.getProposalsForSupplier(supplierId);
    }
}
