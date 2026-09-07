package com.bemo.hr.serviceops.api;

import com.bemo.hr.serviceops.application.RentalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * No {@code @TenantId}-scoped entity here carries a branchId, so no branch-access check applies
 * to this module — confirmed by inspection of RentalItem/RentalContract during the 2026-09-06
 * security remediation.
 */
@RestController
@RequestMapping("/api/v1/service-ops/rentals")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER', 'INVENTORY_MANAGER', 'GENERAL_MANAGER')")
public class RentalController {

    private final RentalService rentalService;

    public RentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    // Items
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER', 'INVENTORY_MANAGER')")
    public ServiceOpsApi.RentalItemResponse createItem(@Valid @RequestBody ServiceOpsApi.RentalItemCreateRequest request) {
        return rentalService.createItem(request);
    }

    @GetMapping("/items")
    public List<ServiceOpsApi.RentalItemResponse> listItems() {
        return rentalService.listItems();
    }

    @GetMapping("/items/{id}")
    public ServiceOpsApi.RentalItemResponse getItem(@PathVariable String id) {
        return rentalService.getItem(id);
    }

    // Contracts
    @PostMapping("/contracts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER', 'INVENTORY_MANAGER')")
    public ServiceOpsApi.RentalContractResponse createContract(@Valid @RequestBody ServiceOpsApi.RentalContractCreateRequest request) {
        return rentalService.createContract(request);
    }

    @GetMapping("/contracts")
    public List<ServiceOpsApi.RentalContractResponse> listContracts() {
        return rentalService.listContracts();
    }

    @GetMapping("/contracts/{id}")
    public ServiceOpsApi.RentalContractResponse getContract(@PathVariable String id) {
        return rentalService.getContract(id);
    }

    @PostMapping("/contracts/{id}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER', 'INVENTORY_MANAGER')")
    public ServiceOpsApi.RentalContractResponse activateContract(@PathVariable String id) {
        return rentalService.activateContract(id);
    }

    @PostMapping("/contracts/{id}/close")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER', 'INVENTORY_MANAGER')")
    public ServiceOpsApi.RentalContractResponse returnAndCloseContract(
            @PathVariable String id,
            @RequestBody(required = false) ServiceOpsApi.ReturnRentalContractRequest request) {
        return rentalService.returnAndCloseContract(id, request);
    }

    @PostMapping("/contracts/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER', 'INVENTORY_MANAGER')")
    public ServiceOpsApi.RentalContractResponse cancelContract(@PathVariable String id) {
        return rentalService.cancelContract(id);
    }

    // Utilization
    @GetMapping("/utilization")
    public ServiceOpsApi.RentalUtilizationSummary getUtilizationSummary() {
        return rentalService.getUtilizationSummary();
    }
}
