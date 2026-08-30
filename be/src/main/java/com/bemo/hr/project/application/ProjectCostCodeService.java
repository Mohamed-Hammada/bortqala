package com.bemo.hr.project.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.project.api.ProjectApi.*;
import com.bemo.hr.project.domain.ProjectCostCode;
import com.bemo.hr.project.infrastructure.ProjectCostCodeRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ProjectCostCodeService {

    private final ProjectCostCodeRepository projectCostCodeRepository;
    private final AuditService auditService;

    public ProjectCostCodeService(ProjectCostCodeRepository projectCostCodeRepository,
                                  AuditService auditService) {
        this.projectCostCodeRepository = projectCostCodeRepository;
        this.auditService = auditService;
    }

    public List<ProjectCostCodeResponse> listCostCodes(Boolean activeOnly) {
        log.debug("listCostCodes called with activeOnly={}", activeOnly);
        List<ProjectCostCode> list = (activeOnly != null && activeOnly)
                ? projectCostCodeRepository.findByActiveTrueOrderByCodeAsc()
                : projectCostCodeRepository.findAllByOrderByCodeAsc();
        return list.stream().map(this::toResponse).toList();
    }

    public ProjectCostCodeResponse getCostCode(String id) {
        log.debug("getCostCode called with id={}", id);
        return toResponse(requireCostCode(id));
    }

    @Transactional
    public ProjectCostCodeResponse createCostCode(CreateCostCodeRequest request) {
        log.debug("createCostCode called with code={}", request.code());
        String code = request.code().strip();
        if (projectCostCodeRepository.existsByCode(code)) {
            throw new BusinessRuleException("Cost code is already in use.", "COST_CODE_DUPLICATE", HttpStatus.CONFLICT);
        }

        ProjectCostCode costCode = new ProjectCostCode(
                code,
                request.name(),
                request.nameEn(),
                request.category(),
                request.description()
        );

        ProjectCostCode saved = projectCostCodeRepository.save(costCode);
        auditService.record("COST_CODE_CREATE", "PROJECT", saved.getId(), getCurrentUser(),
                "{\"code\":\"" + saved.getCode() + "\",\"name\":\"" + saved.getName() + "\"}", null);
        return toResponse(saved);
    }

    @Transactional
    public ProjectCostCodeResponse updateCostCode(String id, UpdateCostCodeRequest request) {
        log.debug("updateCostCode called with id={}", id);
        ProjectCostCode costCode = requireCostCode(id);
        costCode.update(
                request.name(),
                request.nameEn(),
                request.category(),
                request.description(),
                request.active() == null || request.active()
        );

        ProjectCostCode saved = projectCostCodeRepository.save(costCode);
        auditService.record("COST_CODE_UPDATE", "PROJECT", saved.getId(), getCurrentUser(),
                "{\"code\":\"" + saved.getCode() + "\",\"name\":\"" + saved.getName() + "\"}", null);
        return toResponse(saved);
    }

    @Transactional
    public void deleteCostCode(String id) {
        log.debug("deleteCostCode called with id={}", id);
        ProjectCostCode costCode = requireCostCode(id);
        projectCostCodeRepository.delete(costCode);
        auditService.record("COST_CODE_DELETE", "PROJECT", id, getCurrentUser(),
                "{\"code\":\"" + costCode.getCode() + "\"}", null);
    }

    private ProjectCostCode requireCostCode(String id) {
        return projectCostCodeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cost code not found: " + id, "COST_CODE_NOT_FOUND"));
    }

    private ProjectCostCodeResponse toResponse(ProjectCostCode cc) {
        return new ProjectCostCodeResponse(
                cc.getId(),
                cc.getCode(),
                cc.getName(),
                cc.getNameEn(),
                cc.getCategory(),
                cc.getDescription(),
                cc.isActive(),
                cc.getCreatedAt(),
                cc.getUpdatedAt(),
                cc.getVersion()
        );
    }

    private String getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null && !auth.getName().isBlank()) ? auth.getName() : "system";
    }
}
