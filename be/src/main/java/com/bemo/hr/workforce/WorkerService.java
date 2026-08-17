package com.bemo.hr.workforce;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.employee.domain.AttendanceCategory;
import com.bemo.hr.employee.domain.CategoryScope;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerService {
    private final WorkerRepository workerRepository;
    private final AttendanceCategoryRepository categoryRepository;
    private final ContractorRepository contractorRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<WorkforceApi.WorkerResponse> list() {
        log.debug("list called");
        return workerRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<WorkforceApi.WorkerResponse> listByContractor(String contractorId) {
        log.debug("listByContractor called with contractorId={}", contractorId);
        return workerRepository.findByContractorId(contractorId).stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public WorkforceApi.WorkerResponse create(WorkforceApi.WorkerRequest request) {
        log.debug("create called with code={}, fullName={}", request.code(), request.fullName());
        requireWorkerCategory(request.categoryId());
        Worker worker = new Worker(
                request.code(), request.fullName(), request.contractorId(), request.categoryId(),
                request.defaultDailyRate(), request.standardDailyHours(), request.branchId(),
                request.attendanceMode(), request.status(), request.phone(), request.nationalId(), request.notes()
        );
        var saved = mapToResponse(workerRepository.save(worker));
        log.info("Worker {} created successfully", saved.id());
        auditService.record("CREATE", "WORKER", saved.id(), currentActor(),
                "{\"code\":\"" + safe(saved.code()) + "\",\"fullName\":\"" + safe(saved.fullName()) + "\"}", null);
        return saved;
    }

    @Transactional
    public WorkforceApi.WorkerResponse update(String id, WorkforceApi.WorkerRequest request) {
        log.debug("update called with id={}", id);
        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found: " + id));
        requireWorkerCategory(request.categoryId());
        worker.update(
                request.code(), request.fullName(), request.contractorId(), request.categoryId(),
                request.defaultDailyRate(), request.standardDailyHours(), request.branchId(),
                request.attendanceMode(), request.status(), request.phone(), request.nationalId(), request.notes()
        );
        var saved = mapToResponse(workerRepository.save(worker));
        log.info("Worker {} updated successfully", saved.id());
        auditService.record("UPDATE", "WORKER", saved.id(), currentActor(),
                "{\"code\":\"" + safe(saved.code()) + "\",\"fullName\":\"" + safe(saved.fullName()) + "\"}", null);
        return saved;
    }

    private void requireWorkerCategory(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            throw new BusinessRuleException("A worker category is required.", "WORKER_CATEGORY_REQUIRED", HttpStatus.CONFLICT);
        }
        AttendanceCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessRuleException("Category not found.", "WORKFORCE_CATEGORY_NOT_FOUND", HttpStatus.CONFLICT));
        if (!category.isActive()) {
            throw new BusinessRuleException("Select an active worker category.", "WORKFORCE_CATEGORY_INACTIVE", HttpStatus.CONFLICT);
        }
        if (category.getScope() == CategoryScope.EMPLOYEE) {
            throw new BusinessRuleException("This category is reserved for employees.", "WORKFORCE_CATEGORY_EMPLOYEE_ONLY", HttpStatus.CONFLICT);
        }
    }

    private String currentActor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null && !auth.getName().isBlank()) ? auth.getName() : "system";
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }

    private WorkforceApi.WorkerResponse mapToResponse(Worker w) {
        String contractorName = contractorRepository.findById(w.getContractorId())
                .map(Contractor::getName).orElse("—");
        String categoryName = categoryRepository.findById(w.getCategoryId())
                .map(AttendanceCategory::getName).orElse("—");
        return new WorkforceApi.WorkerResponse(
                w.getId(), w.getCode(), w.getFullName(), w.getContractorId(), contractorName,
                w.getCategoryId(), categoryName, w.getDefaultDailyRate(), w.getStandardDailyHours(),
                w.getBranchId(), w.getAttendanceMode(), w.getStatus(), w.getPhone(),
                w.getNationalId(), w.getNotes(), w.getCreatedAt().toEpochMilli(), w.getUpdatedAt().toEpochMilli()
        );
    }
}
