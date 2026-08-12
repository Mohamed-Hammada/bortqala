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
    public record ExecuteProposalPayload(String operationId, String paymentMethod) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'VIEWER')")
    public List<VendorPaymentProposal> getProposals() {
        return proposalService.getProposals();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER')")
    public VendorPaymentProposal createProposal(@RequestBody CreateProposalPayload payload, java.security.Principal principal) {
        return proposalService.createProposal(payload.supplierId(), payload.invoiceId(), payload.proposedAmount(), payload.dueDate(), principal.getName());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public VendorPaymentProposal approveProposal(@PathVariable String id, java.security.Principal principal) {
        return proposalService.approveProposal(id, principal.getName());
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public VendorPaymentProposal executeProposal(@PathVariable String id, @RequestBody ExecuteProposalPayload payload,
                                                 java.security.Principal principal) {
        return proposalService.executeProposal(id, payload.operationId(), payload.paymentMethod(), principal.getName());
    }

    @GetMapping("/supplier/{supplierId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'VIEWER')")
    public List<VendorPaymentProposal> getProposalsForSupplier(@PathVariable String supplierId) {
        return proposalService.getProposalsForSupplier(supplierId);
    }
}
