package com.bemo.hr.employee.api;

import com.bemo.hr.employee.application.HrConfigurationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {
    private final HrConfigurationService hrConfigurationService;

    public EmployeeController(HrConfigurationService hrConfigurationService) {
        this.hrConfigurationService = hrConfigurationService;
    }

    @GetMapping
    List<EmployeeApi.Response> list() { return hrConfigurationService.listEmployees(); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    EmployeeApi.Response create(@Valid @RequestBody EmployeeApi.UpsertRequest request) {
        return hrConfigurationService.createEmployee(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    EmployeeApi.Response update(@PathVariable String id, @Valid @RequestBody EmployeeApi.UpsertRequest request) {
        return hrConfigurationService.updateEmployee(id, request);
    }

    @GetMapping("/{id}/assignments")
    List<EmployeeApi.AssignmentResponse> assignments(@PathVariable String id) {
        return hrConfigurationService.getEmployeeAssignments(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(@PathVariable String id) { hrConfigurationService.deactivateEmployee(id); }
}
