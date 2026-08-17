package com.bemo.hr.workforce;

import com.bemo.hr.employee.domain.*;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeCodeSequenceRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkerCategoryService {

    private static final List<CategoryScope> WORKER_SCOPES = List.of(CategoryScope.WORKER, CategoryScope.BOTH);
    private static final int DEFAULT_WORK_DAYS_MASK = workDaysMask(EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY));

    private final WorkerCategoryRepository categoryRepository;
    private final AttendanceCategoryRepository attendanceCategoryRepository;
    private final EmployeeCodeSequenceRepository employeeCodeSequenceRepository;

    private static CategoryScope parseScope(String raw, CategoryScope fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return CategoryScope.valueOf(raw.strip().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private static int expectedDailyMinutes(BigDecimal standardDailyHours) {
        if (standardDailyHours == null || standardDailyHours.signum() <= 0) return 480;
        int minutes = standardDailyHours.multiply(BigDecimal.valueOf(60)).intValue();
        return Math.max(1, Math.min(1_440, minutes));
    }

    private static PayCycle mapPayCycle(String settlementCycle) {
        if (settlementCycle != null) {
            String normalized = settlementCycle.strip().toUpperCase();
            if (normalized.contains("HALF")) return PayCycle.HALF_MONTHLY;
        }
        return PayCycle.MONTHLY;
    }

    private static int workDaysMask(EnumSet<DayOfWeek> days) {
        return days.stream().mapToInt(day -> 1 << (day.getValue() - 1)).reduce(0, (left, right) -> left | right);
    }

    @Transactional(readOnly = true)
    public List<WorkforceApi.CategoryResponse> list() {
        var canonicalById = attendanceCategoryRepository.findByScopeIn(WORKER_SCOPES).stream()
                .collect(Collectors.toMap(AttendanceCategory::getId, Function.identity()));
        return categoryRepository.findByCategoryIdIn(canonicalById.keySet()).stream()
                .filter(config -> canonicalById.containsKey(config.getCategoryId()))
                .sorted(java.util.Comparator.comparing(config -> canonicalById.get(config.getCategoryId()).getName()))
                .map(config -> mapToResponse(config, canonicalById.get(config.getCategoryId())))
                .toList();
    }

    @Transactional
    public WorkforceApi.CategoryResponse create(WorkforceApi.CategoryRequest request) {
        CategoryScope requestedScope = parseScope(request.scope(), CategoryScope.WORKER);
        String code = request.code() == null ? null : request.code().strip().toUpperCase();
        var existing = attendanceCategoryRepository.findByCodeIgnoreCase(code == null ? "" : code).orElse(null);
        if (existing != null && categoryRepository.existsByCategoryId(existing.getId())) {
            throw new BusinessRuleException("Worker category code already exists.",
                    "WORKFORCE_CATEGORY_CODE_EXISTS", HttpStatus.CONFLICT);
        }

        AttendanceCategory canonical = existing;
        if (canonical == null) {
            canonical = new AttendanceCategory(code, request.name() == null ? "" : request.name().strip(),
                    expectedDailyMinutes(request.standardDailyHours()), mapPayCycle(request.defaultSettlementCycle()),
                    AttendanceMode.MANUAL, false, DEFAULT_WORK_DAYS_MASK,
                    "ACTIVE".equalsIgnoreCase(request.status()), requestedScope);
            attendanceCategoryRepository.save(canonical);
            employeeCodeSequenceRepository.save(new EmployeeCodeSequence(canonical.getId()));
        } else {
            canonical.updateScope(CategoryScope.BOTH);
            canonical.update(canonical.getCode(), request.name() == null || request.name().isBlank()
                            ? canonical.getName() : request.name().strip(),
                    canonical.getExpectedDailyMinutes(), canonical.getPayCycle(), canonical.getAttendanceMode(),
                    canonical.isSinglePunchCounts(), canonical.getWorkDaysMask(), true);
        }

        WorkerCategory config = new WorkerCategory(canonical.getCode(), canonical.getName(), request.description(),
                request.defaultDailyRate(), request.standardDailyHours(), request.defaultSettlementCycle(), request.status());
        config.linkToCategory(canonical.getId());
        config = categoryRepository.save(config);
        return mapToResponse(config, canonical);
    }

    @Transactional
    public WorkforceApi.CategoryResponse update(String id, WorkforceApi.CategoryRequest request) {
        WorkerCategory config = categoryRepository.findByCategoryId(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        final String canonicalId = config.getCategoryId();
        AttendanceCategory canonical = attendanceCategoryRepository.findById(canonicalId)
                .orElseThrow(() -> new IllegalArgumentException("Canonical category not found: " + canonicalId));
        CategoryScope requestedScope = parseScope(request.scope(), canonical.getScope());
        config.update(request.code(), request.name(), request.description(),
                request.defaultDailyRate(), request.standardDailyHours(), request.defaultSettlementCycle(), request.status());
        config.linkToCategory(canonical.getId());
        config = categoryRepository.save(config);
        canonical.updateScope(requestedScope);
        canonical.update(request.code() == null ? canonical.getCode() : request.code().strip().toUpperCase(),
                request.name() == null || request.name().isBlank() ? canonical.getName() : request.name().strip(),
                expectedDailyMinutes(request.standardDailyHours()),
                mapPayCycle(request.defaultSettlementCycle()), canonical.getAttendanceMode(),
                canonical.isSinglePunchCounts(), canonical.getWorkDaysMask(),
                "ACTIVE".equalsIgnoreCase(request.status()));
        return mapToResponse(config, canonical);
    }

    private WorkforceApi.CategoryResponse mapToResponse(WorkerCategory config, AttendanceCategory canonical) {
        return new WorkforceApi.CategoryResponse(
                canonical.getId(), canonical.getCode(), canonical.getName(), config.getDescription(),
                config.getDefaultDailyRate(), config.getStandardDailyHours(),
                config.getDefaultSettlementCycle(), config.getStatus(),
                canonical.getScope().name(), canonical.isActive(),
                config.getCreatedAt().toEpochMilli(), config.getUpdatedAt().toEpochMilli()
        );
    }
}
