package com.bemo.hr.employee.api;

import com.bemo.hr.employee.application.HrConfigurationService;
import com.bemo.hr.shared.security.Roles;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final HrConfigurationService hrConfigurationService;

    public CategoryController(HrConfigurationService hrConfigurationService) {
        this.hrConfigurationService = hrConfigurationService;
    }

    @GetMapping
    List<CategoryApi.Response> list() {
        return hrConfigurationService.listCategories();
    }

    @GetMapping("/{id}")
    CategoryApi.Response get(@PathVariable String id) {
        return hrConfigurationService.getCategory(id);
    }

    @GetMapping("/{id}/schedule-history")
    List<CategoryApi.ScheduleResponse> scheduleHistory(@PathVariable String id) {
        return hrConfigurationService.getScheduleHistory(id);
    }

    @PostMapping
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_MANAGER)
    @ResponseStatus(HttpStatus.CREATED)
    CategoryApi.Response create(@Valid @RequestBody CategoryApi.UpsertRequest request) {
        return hrConfigurationService.createCategory(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_MANAGER)
    CategoryApi.Response update(@PathVariable String id, @Valid @RequestBody CategoryApi.UpsertRequest request) {
        return hrConfigurationService.updateCategory(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_MANAGER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(@PathVariable String id) {
        hrConfigurationService.deactivateCategory(id);
    }
}
