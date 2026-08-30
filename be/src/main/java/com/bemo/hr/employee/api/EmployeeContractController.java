package com.bemo.hr.employee.api;

import com.bemo.hr.employee.application.EmployeeContractService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeContractController {

    private final EmployeeContractService contractService;

    public EmployeeContractController(EmployeeContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping("/{employeeId}/contracts")
    @PreAuthorize("@auth.hasPermission('employees.read')")
    public List<EmployeeContractApi.ContractResponse> listContracts(@PathVariable String employeeId) {
        return contractService.listContractsForEmployee(employeeId);
    }

    @GetMapping("/contracts/{id}")
    @PreAuthorize("@auth.hasPermission('employees.read')")
    public EmployeeContractApi.ContractResponse getContract(@PathVariable String id) {
        return contractService.getContract(id);
    }

    @PostMapping("/{employeeId}/contracts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@auth.hasPermission('employees.edit')")
    public EmployeeContractApi.ContractResponse createContract(
            @PathVariable String employeeId,
            @Valid @RequestBody EmployeeContractApi.CreateContractRequest request) {
        return contractService.createContract(employeeId, request);
    }

    @PostMapping("/contracts/{id}/amend")
    @PreAuthorize("@auth.hasPermission('employees.edit')")
    public EmployeeContractApi.ContractResponse amendContract(
            @PathVariable String id,
            @Valid @RequestBody EmployeeContractApi.AmendContractRequest request) {
        return contractService.amendContract(id, request);
    }

    @PostMapping("/contracts/{id}/terminate")
    @PreAuthorize("@auth.hasPermission('employees.edit')")
    public EmployeeContractApi.ContractResponse terminateContract(
            @PathVariable String id,
            @Valid @RequestBody EmployeeContractApi.TerminateContractRequest request) {
        return contractService.terminateContract(id, request);
    }

    @GetMapping("/contracts/expiring")
    @PreAuthorize("@auth.hasPermission('employees.read')")
    public List<EmployeeContractApi.ContractResponse> listExpiringContracts(
            @RequestParam(defaultValue = "30") int withinDays) {
        return contractService.listExpiringContracts(withinDays);
    }
}
