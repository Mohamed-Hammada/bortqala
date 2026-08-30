package com.bemo.hr.serviceops.api;

import com.bemo.hr.serviceops.application.RentalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/service-ops/rentals")
public class RentalController {

    private final RentalService rentalService;

    public RentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    // Items
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
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
    public ServiceOpsApi.RentalContractResponse activateContract(@PathVariable String id) {
        return rentalService.activateContract(id);
    }

    @PostMapping("/contracts/{id}/close")
    public ServiceOpsApi.RentalContractResponse returnAndCloseContract(
            @PathVariable String id,
            @RequestBody(required = false) ServiceOpsApi.ReturnRentalContractRequest request) {
        return rentalService.returnAndCloseContract(id, request);
    }

    @PostMapping("/contracts/{id}/cancel")
    public ServiceOpsApi.RentalContractResponse cancelContract(@PathVariable String id) {
        return rentalService.cancelContract(id);
    }

    // Utilization
    @GetMapping("/utilization")
    public ServiceOpsApi.RentalUtilizationSummary getUtilizationSummary() {
        return rentalService.getUtilizationSummary();
    }
}
