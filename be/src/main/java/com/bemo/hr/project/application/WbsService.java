package com.bemo.hr.project.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.project.api.ProjectApi.*;
import com.bemo.hr.project.domain.Project;
import com.bemo.hr.project.domain.ProjectStatus;
import com.bemo.hr.project.domain.WbsNode;
import com.bemo.hr.project.infrastructure.ProjectCostCodeRepository;
import com.bemo.hr.project.infrastructure.ProjectRepository;
import com.bemo.hr.project.infrastructure.WbsNodeRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class WbsService {

    private final WbsNodeRepository wbsNodeRepository;
    private final ProjectRepository projectRepository;
    private final ProjectCostCodeRepository projectCostCodeRepository;
    private final AuditService auditService;

    public WbsService(WbsNodeRepository wbsNodeRepository,
                      ProjectRepository projectRepository,
                      ProjectCostCodeRepository projectCostCodeRepository,
                      AuditService auditService) {
        this.wbsNodeRepository = wbsNodeRepository;
        this.projectRepository = projectRepository;
        this.projectCostCodeRepository = projectCostCodeRepository;
        this.auditService = auditService;
    }

    public List<WbsNodeResponse> getWbsTree(String projectId) {
        log.debug("getWbsTree called for projectId={}", projectId);
        requireProject(projectId);
        List<WbsNode> allNodes = wbsNodeRepository.findByProjectIdOrderBySortOrderAsc(projectId);

        // Group by parentId
        Map<String, List<WbsNode>> byParent = new HashMap<>();
        List<WbsNode> roots = new ArrayList<>();
        for (WbsNode node : allNodes) {
            if (node.getParentId() == null || node.getParentId().isBlank()) {
                roots.add(node);
            } else {
                byParent.computeIfAbsent(node.getParentId(), k -> new ArrayList<>()).add(node);
            }
        }

        return roots.stream().map(root -> buildNodeResponse(root, byParent)).toList();
    }

    public List<WbsNodeResponse> getFlatWbsList(String projectId) {
        log.debug("getFlatWbsList called for projectId={}", projectId);
        requireProject(projectId);
        return wbsNodeRepository.findByProjectIdOrderBySortOrderAsc(projectId).stream()
                .map(this::toSimpleResponse).toList();
    }

    public WbsNodeResponse getWbsNode(String id) {
        log.debug("getWbsNode called with id={}", id);
        return toSimpleResponse(requireWbsNode(id));
    }

    @Transactional
    public WbsNodeResponse createWbsNode(String projectId, CreateWbsNodeRequest request) {
        log.debug("createWbsNode called for projectId={}, wbsCode={}", projectId, request.wbsCode());
        Project project = requireProject(projectId);
        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new BusinessRuleException("Project is closed.", "PROJECT_CLOSED", HttpStatus.CONFLICT);
        }

        String wbsCode = request.wbsCode().strip();
        if (wbsNodeRepository.existsByProjectIdAndWbsCode(projectId, wbsCode)) {
            throw new BusinessRuleException("WBS node code already exists in this project.", "WBS_CODE_DUPLICATE", HttpStatus.CONFLICT);
        }

        if (request.costCodeId() != null && !request.costCodeId().isBlank()) {
            projectCostCodeRepository.findById(request.costCodeId().strip())
                    .orElseThrow(() -> new BusinessRuleException("Cost code not found.", "COST_CODE_NOT_FOUND", HttpStatus.BAD_REQUEST));
        }

        LocalDate startDate = fromEpoch(request.startDate());
        LocalDate endDate = fromEpoch(request.endDate());

        int level = 1;
        String wbsPath = "/" + wbsCode;
        String parentId = null;

        if (request.parentId() != null && !request.parentId().isBlank()) {
            WbsNode parent = requireWbsNode(request.parentId().strip());
            if (!parent.getProjectId().equals(projectId)) {
                throw new BusinessRuleException("Parent node does not belong to the same project.", "WBS_PARENT_MISMATCH", HttpStatus.BAD_REQUEST);
            }
            parentId = parent.getId();
            level = parent.getLevel() + 1;
            wbsPath = parent.getWbsPath() + "/" + wbsCode;
        }

        int sortOrder = request.sortOrder() != null ? request.sortOrder() : 0;

        WbsNode node = new WbsNode(
                projectId,
                parentId,
                wbsCode,
                wbsPath,
                request.name(),
                request.nameEn(),
                request.description(),
                request.nodeType(),
                level,
                sortOrder,
                request.unitOfMeasure(),
                request.plannedQuantity(),
                request.unitRate(),
                request.costCodeId(),
                startDate,
                endDate,
                request.status()
        );

        WbsNode saved = wbsNodeRepository.save(node);
        auditService.record("WBS_NODE_CREATE", "PROJECT", projectId, getCurrentUser(),
                "{\"nodeId\":\"" + saved.getId() + "\",\"wbsCode\":\"" + saved.getWbsCode() + "\",\"name\":\"" + saved.getName() + "\"}", null);
        log.info("WbsNode {} created with code {} for project {}", saved.getId(), saved.getWbsCode(), projectId);
        return toSimpleResponse(saved);
    }

    @Transactional
    public WbsNodeResponse updateWbsNode(String id, UpdateWbsNodeRequest request) {
        log.debug("updateWbsNode called with id={}", id);
        WbsNode node = requireWbsNode(id);
        Project project = requireProject(node.getProjectId());
        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new BusinessRuleException("Project is closed.", "PROJECT_CLOSED", HttpStatus.CONFLICT);
        }

        if (request.costCodeId() != null && !request.costCodeId().isBlank()) {
            projectCostCodeRepository.findById(request.costCodeId().strip())
                    .orElseThrow(() -> new BusinessRuleException("Cost code not found.", "COST_CODE_NOT_FOUND", HttpStatus.BAD_REQUEST));
        }

        LocalDate startDate = fromEpoch(request.startDate());
        LocalDate endDate = fromEpoch(request.endDate());

        node.update(
                request.name(),
                request.nameEn(),
                request.description(),
                request.nodeType(),
                request.unitOfMeasure(),
                request.plannedQuantity(),
                request.unitRate(),
                request.costCodeId(),
                startDate,
                endDate,
                request.status()
        );

        WbsNode saved = wbsNodeRepository.save(node);
        auditService.record("WBS_NODE_UPDATE", "PROJECT", node.getProjectId(), getCurrentUser(),
                "{\"nodeId\":\"" + saved.getId() + "\",\"name\":\"" + saved.getName() + "\"}", null);
        return toSimpleResponse(saved);
    }

    @Transactional
    public WbsNodeResponse repositionWbsNode(String id, RepositionWbsNodeRequest request) {
        log.debug("repositionWbsNode called with id={}, targetParentId={}", id, request.parentId());
        WbsNode node = requireWbsNode(id);
        Project project = requireProject(node.getProjectId());
        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new BusinessRuleException("Project is closed.", "PROJECT_CLOSED", HttpStatus.CONFLICT);
        }

        String targetParentId = request.parentId() != null && !request.parentId().isBlank() ? request.parentId().strip() : null;

        // Check self parent
        if (id.equals(targetParentId)) {
            throw new BusinessRuleException("A node cannot be its own parent.", "WBS_CYCLE_DETECTED", HttpStatus.CONFLICT);
        }

        // Cycle detection: ensure targetParentId is not a descendant of id
        if (targetParentId != null) {
            WbsNode currentParent = requireWbsNode(targetParentId);
            if (!currentParent.getProjectId().equals(node.getProjectId())) {
                throw new BusinessRuleException("Target parent node belongs to a different project.", "WBS_PARENT_MISMATCH", HttpStatus.BAD_REQUEST);
            }
            while (currentParent != null) {
                if (currentParent.getId().equals(id)) {
                    throw new BusinessRuleException("A node cannot be moved under one of its own descendants.", "WBS_CYCLE_DETECTED", HttpStatus.CONFLICT);
                }
                if (currentParent.getParentId() == null) {
                    break;
                }
                currentParent = wbsNodeRepository.findById(currentParent.getParentId()).orElse(null);
            }
        }

        int newLevel;
        String newPath;
        if (targetParentId == null) {
            newLevel = 1;
            newPath = "/" + node.getWbsCode();
        } else {
            WbsNode targetParent = requireWbsNode(targetParentId);
            newLevel = targetParent.getLevel() + 1;
            newPath = targetParent.getWbsPath() + "/" + node.getWbsCode();
        }

        String oldPath = node.getWbsPath();
        node.reposition(targetParentId, node.getWbsCode(), newPath, newLevel, request.sortOrder());
        WbsNode saved = wbsNodeRepository.save(node);

        // Update descendant paths and levels
        updateDescendantPaths(node.getProjectId(), oldPath, newPath, newLevel - node.getLevel());

        auditService.record("WBS_NODE_REPOSITION", "PROJECT", node.getProjectId(), getCurrentUser(),
                "{\"nodeId\":\"" + saved.getId() + "\",\"newParentId\":\"" + targetParentId + "\"}", null);
        return toSimpleResponse(saved);
    }

    @Transactional
    public void deleteWbsNode(String id) {
        log.debug("deleteWbsNode called with id={}", id);
        WbsNode node = requireWbsNode(id);
        Project project = requireProject(node.getProjectId());
        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new BusinessRuleException("Project is closed.", "PROJECT_CLOSED", HttpStatus.CONFLICT);
        }

        if (wbsNodeRepository.existsByProjectIdAndParentId(node.getProjectId(), id)) {
            throw new BusinessRuleException("Cannot delete WBS node that has child nodes.", "WBS_HAS_CHILDREN", HttpStatus.CONFLICT);
        }

        wbsNodeRepository.delete(node);
        auditService.record("WBS_NODE_DELETE", "PROJECT", node.getProjectId(), getCurrentUser(),
                "{\"nodeId\":\"" + id + "\",\"wbsCode\":\"" + node.getWbsCode() + "\"}", null);
        log.info("WbsNode {} deleted successfully", id);
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private void updateDescendantPaths(String projectId, String oldPrefix, String newPrefix, int levelDelta) {
        List<WbsNode> all = wbsNodeRepository.findByProjectIdOrderBySortOrderAsc(projectId);
        for (WbsNode item : all) {
            if (item.getWbsPath().startsWith(oldPrefix + "/")) {
                String updatedPath = newPrefix + item.getWbsPath().substring(oldPrefix.length());
                item.reposition(item.getParentId(), item.getWbsCode(), updatedPath, item.getLevel() + levelDelta, item.getSortOrder());
                wbsNodeRepository.save(item);
            }
        }
    }

    private Project requireProject(String id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found: " + id, "PROJECT_NOT_FOUND"));
    }

    private WbsNode requireWbsNode(String id) {
        return wbsNodeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("WBS node not found: " + id, "WBS_NODE_NOT_FOUND"));
    }

    private WbsNodeResponse buildNodeResponse(WbsNode node, Map<String, List<WbsNode>> byParent) {
        List<WbsNode> childNodes = byParent.getOrDefault(node.getId(), Collections.emptyList());
        List<WbsNodeResponse> children = childNodes.stream()
                .map(child -> buildNodeResponse(child, byParent))
                .toList();

        return new WbsNodeResponse(
                node.getId(),
                node.getProjectId(),
                node.getParentId(),
                node.getWbsCode(),
                node.getWbsPath(),
                node.getName(),
                node.getNameEn(),
                node.getDescription(),
                node.getNodeType(),
                node.getLevel(),
                node.getSortOrder(),
                node.getUnitOfMeasure(),
                node.getPlannedQuantity(),
                node.getUnitRate(),
                node.getPlannedAmount(),
                node.getCostCodeId(),
                toEpoch(node.getStartDate()),
                toEpoch(node.getEndDate()),
                node.getStatus(),
                node.getCreatedAt(),
                node.getUpdatedAt(),
                node.getVersion(),
                children
        );
    }

    private WbsNodeResponse toSimpleResponse(WbsNode node) {
        return new WbsNodeResponse(
                node.getId(),
                node.getProjectId(),
                node.getParentId(),
                node.getWbsCode(),
                node.getWbsPath(),
                node.getName(),
                node.getNameEn(),
                node.getDescription(),
                node.getNodeType(),
                node.getLevel(),
                node.getSortOrder(),
                node.getUnitOfMeasure(),
                node.getPlannedQuantity(),
                node.getUnitRate(),
                node.getPlannedAmount(),
                node.getCostCodeId(),
                toEpoch(node.getStartDate()),
                toEpoch(node.getEndDate()),
                node.getStatus(),
                node.getCreatedAt(),
                node.getUpdatedAt(),
                node.getVersion(),
                Collections.emptyList()
        );
    }

    private Long toEpoch(LocalDate date) {
        return date == null ? null : date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    private LocalDate fromEpoch(Long epochMillis) {
        return epochMillis == null ? null : Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate();
    }

    private String getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null && !auth.getName().isBlank()) ? auth.getName() : "system";
    }
}
