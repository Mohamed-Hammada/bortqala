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
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final HrConfigurationService hrConfigurationService;

    public CategoryController(HrConfigurationService hrConfigurationService) {
        this.hrConfigurationService = hrConfigurationService;
    }

    @GetMapping
    List<CategoryApi.Response> list() { return hrConfigurationService.listCategories(); }

    @GetMapping("/{id}")
    CategoryApi.Response get(@PathVariable String id) { return hrConfigurationService.getCategory(id); }

    @GetMapping("/{id}/schedule-history")
    List<CategoryApi.ScheduleResponse> scheduleHistory(@PathVariable String id) {
        return hrConfigurationService.getScheduleHistory(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    CategoryApi.Response create(@Valid @RequestBody CategoryApi.UpsertRequest request) {
        return hrConfigurationService.createCategory(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    CategoryApi.Response update(@PathVariable String id, @Valid @RequestBody CategoryApi.UpsertRequest request) {
        return hrConfigurationService.updateCategory(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(@PathVariable String id) { hrConfigurationService.deactivateCategory(id); }
}
