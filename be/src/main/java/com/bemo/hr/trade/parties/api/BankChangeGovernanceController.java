package com.bemo.hr.trade.parties.api;

import com.bemo.hr.shared.security.Roles;
import com.bemo.hr.trade.parties.application.BankChangeGovernanceService;
import com.bemo.hr.trade.parties.domain.BankChangeRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trade/parties/bank-change-requests")
public class BankChangeGovernanceController {

    private final BankChangeGovernanceService governanceService;

    public BankChangeGovernanceController(BankChangeGovernanceService governanceService) {
        this.governanceService = governanceService;
    }

    @PostMapping
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER_PROCUREMENT_MANAGER_WORKFORCE_MANAGER)
    public BankChangeRequest createRequest(@RequestBody RequestBankChangePayload payload, Authentication authentication) {
        return governanceService.requestBankChange(
                BankChangeRequest.PartyType.valueOf(payload.partyType()),
                payload.partyId(),
                payload.oldIban(),
                payload.newIban(),
                payload.oldBankName(),
                payload.newBankName(),
                payload.reason(),
                authentication.getName()
        );
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER)
    public BankChangeRequest approveRequest(@PathVariable String id, Authentication authentication) {
        return governanceService.approveBankChange(id, authentication.getName());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER)
    public BankChangeRequest rejectRequest(@PathVariable String id, @RequestBody RejectBankChangePayload payload, Authentication authentication) {
        return governanceService.rejectBankChange(id, authentication.getName(), payload.reason());
    }

    @GetMapping
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER_VIEWER)
    public List<BankChangeRequest> getPendingRequests() {
        return governanceService.getPendingRequests();
    }

    public record RequestBankChangePayload(String partyType, String partyId, String oldIban, String newIban,
                                           String oldBankName, String newBankName, String reason) {
    }

    public record RejectBankChangePayload(String reason) {
    }
}
