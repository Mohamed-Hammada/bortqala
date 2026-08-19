package com.bemo.hr.trade.sales.api;

import com.bemo.hr.shared.security.Roles;
import com.bemo.hr.trade.sales.application.SalesOrderFullService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trade/sales/orders")
@RequiredArgsConstructor
public class SalesOrderFullController {
    private final SalesOrderFullService salesOrderFullService;

    @PostMapping("/{id}/deliveries")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.SALES_MANAGER)
    public SalesApi.DeliveryResponse deliver(@PathVariable String id, @Valid @RequestBody SalesApi.DeliveryRequest payload,
                                             Authentication authentication) {
        return salesOrderFullService.deliver(id, payload, authentication.getName());
    }

    @PostMapping("/{id}/returns")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.SALES_MANAGER)
    public SalesApi.ReturnResponse receiveReturn(@PathVariable String id, @Valid @RequestBody SalesApi.ReturnRequest payload,
                                                 Authentication authentication) {
        return salesOrderFullService.receiveReturn(id, payload, authentication.getName());
    }

    @GetMapping("/{id}/deliveries")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.SALES_MANAGER + " or " + Roles.VIEWER)
    public List<SalesApi.DeliveryResponse> deliveries(@PathVariable String id) {
        return salesOrderFullService.deliveries(id);
    }

    @GetMapping("/{id}/returns")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.SALES_MANAGER + " or " + Roles.VIEWER)
    public List<SalesApi.ReturnResponse> returns(@PathVariable String id) {
        return salesOrderFullService.returns(id);
    }
}
