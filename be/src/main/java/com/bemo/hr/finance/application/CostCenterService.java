package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.CostCenter;
import com.bemo.hr.finance.infrastructure.CostCenterRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CostCenterService {

    private final CostCenterRepository costCenterRepository;

    @Transactional(readOnly = true)
    public List<CostCenterResponse> listAll() {
        return costCenterRepository.findAllByOrderByCodeAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CostCenterResponse> listActive() {
        return costCenterRepository.findByActiveTrueOrderByCodeAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CostCenterResponse getById(String id) {
        CostCenter cc = costCenterRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Cost center not found", "COST_CENTER_NOT_FOUND", HttpStatus.NOT_FOUND));
        return toResponse(cc);
    }

    public CostCenterResponse create(CostCenterPayload payload) {
        if (payload.code() == null || payload.code().isBlank()) {
            throw new BusinessRuleException("Cost center code is required", "COST_CENTER_CODE_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        if (payload.name() == null || payload.name().isBlank()) {
            throw new BusinessRuleException("Cost center name is required", "COST_CENTER_NAME_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        if (costCenterRepository.existsByCodeIgnoreCase(payload.code().strip())) {
            throw new BusinessRuleException("Cost center code already exists", "COST_CENTER_CODE_DUPLICATE", HttpStatus.BAD_REQUEST);
        }

        CostCenter cc = new CostCenter(
                payload.code(),
                payload.name(),
                payload.parentId(),
                payload.managerUserId(),
                payload.isHeader(),
                payload.active(),
                payload.effectiveStartDate(),
                payload.effectiveEndDate(),
                payload.glAllocationRule()
        );

        CostCenter saved = costCenterRepository.save(cc);
        log.info("Created cost center {} - {}", saved.getCode(), saved.getName());
        return toResponse(saved);
    }

    public CostCenterResponse update(String id, CostCenterPayload payload) {
        CostCenter cc = costCenterRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Cost center not found", "COST_CENTER_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!cc.getCode().equalsIgnoreCase(payload.code().strip()) && costCenterRepository.existsByCodeIgnoreCase(payload.code().strip())) {
            throw new BusinessRuleException("Cost center code already exists", "COST_CENTER_CODE_DUPLICATE", HttpStatus.BAD_REQUEST);
        }

        cc.update(
                payload.code(),
                payload.name(),
                payload.parentId(),
                payload.managerUserId(),
                payload.isHeader(),
                payload.active(),
                payload.effectiveStartDate(),
                payload.effectiveEndDate(),
                payload.glAllocationRule()
        );

        CostCenter saved = costCenterRepository.save(cc);
        log.info("Updated cost center {} - {}", saved.getCode(), saved.getName());
        return toResponse(saved);
    }

    public void delete(String id) {
        CostCenter cc = costCenterRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Cost center not found", "COST_CENTER_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (costCenterRepository.existsByParentId(id)) {
            throw new BusinessRuleException("Cannot delete a parent cost center that has children", "COST_CENTER_HAS_CHILDREN", HttpStatus.BAD_REQUEST);
        }

        costCenterRepository.delete(cc);
        log.info("Deleted cost center {}", id);
    }

    private CostCenterResponse toResponse(CostCenter cc) {
        return new CostCenterResponse(
                cc.getId(),
                cc.getCode(),
                cc.getName(),
                cc.getParentId(),
                cc.getManagerUserId(),
                cc.isHeader(),
                cc.isActive(),
                cc.getEffectiveStartDate(),
                cc.getEffectiveEndDate(),
                cc.getGlAllocationRule(),
                cc.getCreatedAt(),
                cc.getUpdatedAt(),
                cc.getVersion()
        );
    }

    public record CostCenterPayload(
            String code,
            String name,
            String parentId,
            String managerUserId,
            boolean isHeader,
            boolean active,
            Long effectiveStartDate,
            Long effectiveEndDate,
            String glAllocationRule
    ) {}

    public record CostCenterResponse(
            String id,
            String code,
            String name,
            String parentId,
            String managerUserId,
            boolean isHeader,
            boolean active,
            Long effectiveStartDate,
            Long effectiveEndDate,
            String glAllocationRule,
            long createdAt,
            long updatedAt,
            long version
    ) {}
}
