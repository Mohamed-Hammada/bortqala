package com.bemo.hr.workforce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LaborRequestService {
    private final LaborRequestRepository requestRepository;
    private final LaborRequestItemRepository itemRepository;
    private final ContractorRepository contractorRepository;
    private final WorkerCategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<WorkforceApi.LaborRequestResponse> list() {
        return requestRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public WorkforceApi.LaborRequestResponse create(WorkforceApi.LaborRequestCreate dto, String createdBy) {
        LaborRequest req = new LaborRequest(
            dto.requestNumber(), Instant.now(), dto.branchId(), dto.shiftName(),
            dto.contractorId(), "DRAFT", dto.notes(), createdBy
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
        return mapToResponse(saved);
    }

    @Transactional
    public WorkforceApi.LaborRequestResponse updateStatus(String id, String status, String user) {
        LaborRequest req = requestRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Labor request not found: " + id));
        req.updateStatus(status, user);
        return mapToResponse(requestRepository.save(req));
    }

    private WorkforceApi.LaborRequestResponse mapToResponse(LaborRequest req) {
        String contractorName = contractorRepository.findById(req.getContractorId())
            .map(Contractor::getName).orElse("—");
        List<WorkforceApi.LaborRequestItemDto> items = itemRepository.findByRequestId(req.getId())
            .stream().map(i -> new WorkforceApi.LaborRequestItemDto(
                i.getId(), i.getCategoryId(),
                categoryRepository.findById(i.getCategoryId()).map(WorkerCategory::getName).orElse("—"),
                i.getRequestedCount(), i.getSentCount(), i.getAcceptedCount(), i.getVarianceCount()
            )).toList();

        return new WorkforceApi.LaborRequestResponse(
            req.getId(), req.getRequestNumber(), req.getRequestDate().toEpochMilli(),
            req.getBranchId(), req.getShiftName(), req.getContractorId(), contractorName,
            req.getStatus(), req.getNotes(), req.getCreatedBy(), req.getApprovedBy(),
            items, req.getCreatedAt().toEpochMilli(), req.getUpdatedAt().toEpochMilli()
        );
    }
}
