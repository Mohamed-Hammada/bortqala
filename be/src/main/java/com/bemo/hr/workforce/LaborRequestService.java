package com.bemo.hr.workforce;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.employee.domain.AttendanceCategory;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LaborRequestService {
    private final LaborRequestRepository requestRepository;
    private final LaborRequestItemRepository itemRepository;
    private final ContractorRepository contractorRepository;
    private final AttendanceCategoryRepository categoryRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<WorkforceApi.LaborRequestResponse> list() {
        log.debug("list called");
        return requestRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public WorkforceApi.LaborRequestResponse create(WorkforceApi.LaborRequestCreate dto, String createdBy) {
        log.debug("create called with requestNumber={}", dto.requestNumber());
        LaborRequest req = new LaborRequest(
                dto.requestNumber(), Instant.now(), dto.branchId(), dto.shiftName(),
                dto.contractorId(), dto.projectId(), dto.wbsNodeId(), dto.costCodeId(),
                dto.siteLocation(), "DRAFT", dto.notes(), createdBy
        );
        LaborRequest saved = requestRepository.save(req);
        if (dto.items() != null) {
            for (WorkforceApi.LaborRequestItemDto itemDto : dto.items()) {
                LaborRequestItem item = new LaborRequestItem(
                        saved.getId(), itemDto.categoryId(), itemDto.requestedCount(),
                        itemDto.sentCount(), itemDto.acceptedCount()
                );
                itemRepository.save(item);
            }
        }

        log.info("LaborRequest {} created successfully", saved.getId());
        auditService.record("CREATE", "LABOR_REQUEST", saved.getId(), createdBy,
                "{\"requestNumber\":\"" + dto.requestNumber() + "\"}", null);

        return mapToResponse(saved);
    }

    @Transactional
    public WorkforceApi.LaborRequestResponse updateStatus(String id, String status, String user) {
        log.debug("updateStatus called with id={}, status={}", id, status);
        LaborRequest req = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Labor request not found: " + id));
        req.updateStatus(status, user);
        log.info("LaborRequest {} status updated to {}", id, status);
        return mapToResponse(requestRepository.save(req));
    }

    private WorkforceApi.LaborRequestResponse mapToResponse(LaborRequest req) {
        String contractorName = contractorRepository.findById(req.getContractorId())
                .map(Contractor::getName).orElse("—");
        List<WorkforceApi.LaborRequestItemDto> items = itemRepository.findByRequestId(req.getId())
                .stream().map(i -> new WorkforceApi.LaborRequestItemDto(
                        i.getId(), i.getCategoryId(),
                        categoryRepository.findById(i.getCategoryId()).map(AttendanceCategory::getName).orElse("—"),
                        i.getRequestedCount(), i.getSentCount(), i.getAcceptedCount(), i.getVarianceCount()
                )).toList();

        return new WorkforceApi.LaborRequestResponse(
                req.getId(), req.getRequestNumber(), req.getRequestDate().toEpochMilli(),
                req.getBranchId(), req.getShiftName(), req.getContractorId(), contractorName,
                req.getProjectId(), req.getWbsNodeId(), req.getCostCodeId(), req.getSiteLocation(),
                req.getStatus(), req.getNotes(), req.getCreatedBy(), req.getApprovedBy(),
                items, req.getCreatedAt().toEpochMilli(), req.getUpdatedAt().toEpochMilli()
        );
    }
}
