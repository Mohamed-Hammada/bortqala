package com.bemo.hr.trade.procurement.api;

import com.bemo.hr.shared.security.Roles;
import com.bemo.hr.trade.procurement.application.SourcingService;
import com.bemo.hr.trade.procurement.domain.RfqHeader;
import com.bemo.hr.trade.procurement.domain.SourcingAward;
import com.bemo.hr.trade.procurement.domain.SupplierQuoteHeader;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/procurement/rfqs")
public class SourcingController {

    private final SourcingService sourcingService;

    public SourcingController(SourcingService sourcingService) {
        this.sourcingService = sourcingService;
    }

    @PostMapping
    @PreAuthorize(Roles.ADMIN_PROCUREMENT_MANAGER_PROCUREMENT_USER)
    public RfqHeader createRfq(@RequestBody CreateRfqPayload payload) {
        return sourcingService.createRfq(payload.rfqNumber(), payload.requisitionId(), LocalDate.parse(payload.issueDate()), LocalDate.parse(payload.dueDate()));
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize(Roles.ADMIN_PROCUREMENT_MANAGER)
    public RfqHeader issueRfq(@PathVariable String id) {
        return sourcingService.issueRfq(id);
    }

    @PostMapping("/{id}/quotes")
    @PreAuthorize(Roles.ADMIN_PROCUREMENT_MANAGER_PROCUREMENT_USER)
    public SupplierQuoteHeader submitQuote(@PathVariable String id, @RequestBody SubmitQuotePayload payload) {
        return sourcingService.submitQuote(
                id,
                payload.supplierId(),
                payload.quoteNumber(),
                LocalDate.parse(payload.quoteDate()),
                LocalDate.parse(payload.validUntil()),
                payload.totalAmount()
        );
    }

    @PostMapping("/{id}/award")
    @PreAuthorize(Roles.ADMIN_PROCUREMENT_MANAGER)
    public SourcingAward awardQuote(@PathVariable String id, @RequestBody AwardQuotePayload payload, Authentication auth) {
        return sourcingService.awardQuote(id, payload.quoteId(), auth.getName());
    }

    @GetMapping("/{id}/quotes")
    @PreAuthorize(Roles.ADMIN_PROCUREMENT_MANAGER_PROCUREMENT_USER_VIEWER)
    public List<SupplierQuoteHeader> getQuotes(@PathVariable String id) {
        return sourcingService.getQuotesForRfq(id);
    }

    public record CreateRfqPayload(String rfqNumber, String requisitionId, String issueDate, String dueDate) {
    }

    public record SubmitQuotePayload(String supplierId, String quoteNumber, String quoteDate, String validUntil,
                                     BigDecimal totalAmount) {
    }

    public record AwardQuotePayload(String quoteId) {
    }
}
