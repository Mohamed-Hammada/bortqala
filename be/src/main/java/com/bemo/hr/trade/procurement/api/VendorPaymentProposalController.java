package com.bemo.hr.trade.procurement.api;

import com.bemo.hr.trade.procurement.application.VendorPaymentProposalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'VIEWER')")
    public List<VendorPaymentProposalService.ProposalResult> getProposals() {
        return proposalService.getProposals();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROCUREMENT_MANAGER', 'PROCUREMENT_USER', 'FINANCE_MANAGER')")
    public VendorPaymentProposalService.ProposalResult createProposal(@Valid @RequestBody CreateProposalPayload payload,
                                                                      java.security.Principal principal) {
        List<VendorPaymentProposalService.AllocationInput> allocations = payload.allocations() == null || payload.allocations().isEmpty()
                ? List.of(new VendorPaymentProposalService.AllocationInput(payload.invoiceId(), payload.proposedAmount()))
                : payload.allocations().stream()
                  .map(allocation -> new VendorPaymentProposalService.AllocationInput(allocation.invoiceId(), allocation.amount()))
                  .toList();
        return proposalService.createProposal(payload.supplierId(), allocations, payload.dueDate(), principal.getName());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public VendorPaymentProposalService.ProposalResult approveProposal(@PathVariable String id, java.security.Principal principal) {
        return proposalService.approveProposal(id, principal.getName());
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public VendorPaymentProposalService.ProposalResult executeProposal(@PathVariable String id,
                                                                       @Valid @RequestBody ExecuteProposalPayload payload,
                                                                       java.security.Principal principal) {
        return proposalService.executeProposal(id, payload.operationId(), payload.paymentMethod(), principal.getName());
    }

    @GetMapping("/supplier/{supplierId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'VIEWER')")
    public List<VendorPaymentProposalService.ProposalResult> getProposalsForSupplier(@PathVariable String supplierId) {
        return proposalService.getProposalsForSupplier(supplierId);
    }

    public record AllocationPayload(@NotBlank String invoiceId,
                                    @NotNull @DecimalMin(value = "0.01") BigDecimal amount) {
    }

    public record CreateProposalPayload(@NotBlank String supplierId, String invoiceId, BigDecimal proposedAmount,
                                        @NotNull LocalDate dueDate, List<@Valid AllocationPayload> allocations) {
    }

    public record ExecuteProposalPayload(@NotBlank String operationId, @NotBlank String paymentMethod) {
    }
}
