package com.bemo.hr.employee.api;

import com.bemo.hr.employee.application.EmployeeCodeDedupService;
import com.bemo.hr.employee.application.HrConfigurationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {
    private final HrConfigurationService hrConfigurationService;
    private final EmployeeCodeDedupService employeeCodeDedupService;

    public EmployeeController(HrConfigurationService hrConfigurationService,
                              EmployeeCodeDedupService employeeCodeDedupService) {
        this.hrConfigurationService = hrConfigurationService;
        this.employeeCodeDedupService = employeeCodeDedupService;
    }

    @GetMapping
    List<EmployeeApi.Response> list() {
        return hrConfigurationService.listEmployees();
    }

    @PostMapping
    @PreAuthorize("@auth.hasPermission('employees.edit')")
    @ResponseStatus(HttpStatus.CREATED)
    EmployeeApi.Response create(@Valid @RequestBody EmployeeApi.UpsertRequest request) {
        return hrConfigurationService.createEmployee(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@auth.hasPermission('employees.edit')")
    EmployeeApi.Response update(@PathVariable String id, @Valid @RequestBody EmployeeApi.UpsertRequest request) {
        return hrConfigurationService.updateEmployee(id, request);
    }

    @GetMapping("/{id}/assignments")
    List<EmployeeApi.AssignmentResponse> assignments(@PathVariable String id) {
        return hrConfigurationService.getEmployeeAssignments(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@auth.hasPermission('employees.deactivate')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(@PathVariable String id) {
        hrConfigurationService.deactivateEmployee(id);
    }

    @PostMapping("/code-corrections")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    EmployeeApi.CodeCorrectionReport correctDuplicateCodes(
            @Valid @RequestBody EmployeeApi.CodeCorrectionRequest request,
            Authentication authentication) {
        return employeeCodeDedupService.correct(request.dryRun(), authentication.getName());
    }
}
