package com.bemo.hr.workforce;

import com.bemo.hr.shared.security.Roles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workforce/categories")
@RequiredArgsConstructor
public class WorkerCategoryController {
    private final WorkerCategoryService categoryService;
    private final WorkforceMasterDataExcelExporter excelExporter;

    @GetMapping(value = "/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize(Roles.ADMIN_WORKFORCE_FINANCE_WORKFORCE_MANAGER_WORKFORCE_REVIEWER)
    public ResponseEntity<byte[]> exportExcel() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("worker-categories.xlsx", StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(excelExporter.categories(categoryService.list()));
    }

    @GetMapping
    @PreAuthorize(Roles.ADMIN_WORKFORCE_FINANCE_WORKFORCE_MANAGER_WORKFORCE_REVIEWER)
    public List<WorkforceApi.CategoryResponse> list() {
        return categoryService.list();
    }

    @PostMapping
    @PreAuthorize(Roles.ADMIN_WORKFORCE_MANAGER)
    @ResponseStatus(HttpStatus.CREATED)
    public WorkforceApi.CategoryResponse create(@Valid @RequestBody WorkforceApi.CategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.ADMIN_WORKFORCE_MANAGER)
    public WorkforceApi.CategoryResponse update(@PathVariable String id, @Valid @RequestBody WorkforceApi.CategoryRequest request) {
        return categoryService.update(id, request);
    }
}
