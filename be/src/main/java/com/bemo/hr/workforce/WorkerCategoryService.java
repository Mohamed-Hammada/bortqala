package com.bemo.hr.workforce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkerCategoryService {
    private final WorkerCategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<WorkforceApi.CategoryResponse> list() {
        return categoryRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public WorkforceApi.CategoryResponse create(WorkforceApi.CategoryRequest request) {
        WorkerCategory cat = new WorkerCategory(
            request.code(), request.name(), request.description(),
            request.defaultDailyRate(), request.standardDailyHours(),
            request.defaultSettlementCycle(), request.status()
        );
        return mapToResponse(categoryRepository.save(cat));
    }

    @Transactional
    public WorkforceApi.CategoryResponse update(String id, WorkforceApi.CategoryRequest request) {
        WorkerCategory cat = categoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        cat.update(
            request.code(), request.name(), request.description(),
            request.defaultDailyRate(), request.standardDailyHours(),
            request.defaultSettlementCycle(), request.status()
        );
        return mapToResponse(categoryRepository.save(cat));
    }

    private WorkforceApi.CategoryResponse mapToResponse(WorkerCategory c) {
        return new WorkforceApi.CategoryResponse(
            c.getId(), c.getCode(), c.getName(), c.getDescription(),
            c.getDefaultDailyRate(), c.getStandardDailyHours(),
            c.getDefaultSettlementCycle(), c.getStatus(),
            c.getCreatedAt().toEpochMilli(), c.getUpdatedAt().toEpochMilli()
        );
    }
}
