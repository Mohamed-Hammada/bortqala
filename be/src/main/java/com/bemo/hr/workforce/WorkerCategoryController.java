package com.bemo.hr.workforce;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/workforce/categories")
@RequiredArgsConstructor
public class WorkerCategoryController {
    private final WorkerCategoryService categoryService;

    @GetMapping
    public List<WorkforceApi.CategoryResponse> list() {
        return categoryService.list();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkforceApi.CategoryResponse create(@Valid @RequestBody WorkforceApi.CategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public WorkforceApi.CategoryResponse update(@PathVariable String id, @Valid @RequestBody WorkforceApi.CategoryRequest request) {
        return categoryService.update(id, request);
    }
}
