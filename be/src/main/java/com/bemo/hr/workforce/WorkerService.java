package com.bemo.hr.workforce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkerService {
    private final WorkerRepository workerRepository;
    private final WorkerCategoryRepository categoryRepository;
    private final ContractorRepository contractorRepository;

    @Transactional(readOnly = true)
    public List<WorkforceApi.WorkerResponse> list() {
        return workerRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<WorkforceApi.WorkerResponse> listByContractor(String contractorId) {
        return workerRepository.findByContractorId(contractorId).stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public WorkforceApi.WorkerResponse create(WorkforceApi.WorkerRequest request) {
        Worker worker = new Worker(
            request.code(), request.fullName(), request.contractorId(), request.categoryId(),
            request.defaultDailyRate(), request.standardDailyHours(), request.branchId(),
            request.attendanceMode(), request.status(), request.phone(), request.nationalId(), request.notes()
        );
        return mapToResponse(workerRepository.save(worker));
    }

    @Transactional
    public WorkforceApi.WorkerResponse update(String id, WorkforceApi.WorkerRequest request) {
        Worker worker = workerRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Worker not found: " + id));
        worker.update(
            request.code(), request.fullName(), request.contractorId(), request.categoryId(),
            request.defaultDailyRate(), request.standardDailyHours(), request.branchId(),
            request.attendanceMode(), request.status(), request.phone(), request.nationalId(), request.notes()
        );
        return mapToResponse(workerRepository.save(worker));
    }

    private WorkforceApi.WorkerResponse mapToResponse(Worker w) {
        String contractorName = contractorRepository.findById(w.getContractorId())
            .map(Contractor::getName).orElse("—");
        String categoryName = categoryRepository.findById(w.getCategoryId())
            .map(WorkerCategory::getName).orElse("—");
        return new WorkforceApi.WorkerResponse(
            w.getId(), w.getCode(), w.getFullName(), w.getContractorId(), contractorName,
            w.getCategoryId(), categoryName, w.getDefaultDailyRate(), w.getStandardDailyHours(),
            w.getBranchId(), w.getAttendanceMode(), w.getStatus(), w.getPhone(),
            w.getNationalId(), w.getNotes(), w.getCreatedAt().toEpochMilli(), w.getUpdatedAt().toEpochMilli()
        );
    }
}
