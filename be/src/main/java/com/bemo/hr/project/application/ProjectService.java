package com.bemo.hr.project.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.organization.infrastructure.BranchRepository;
import com.bemo.hr.organization.infrastructure.CompanyRepository;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.project.api.ProjectApi.*;
import com.bemo.hr.project.domain.Project;
import com.bemo.hr.project.domain.ProjectPartyRole;
import com.bemo.hr.project.domain.ProjectStatus;
import com.bemo.hr.project.domain.WbsNode;
import com.bemo.hr.project.domain.WbsNodeStatus;
import com.bemo.hr.project.infrastructure.ProjectPartyRoleRepository;
import com.bemo.hr.project.infrastructure.ProjectRepository;
import com.bemo.hr.project.infrastructure.WbsNodeRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final WbsNodeRepository wbsNodeRepository;
    private final ProjectPartyRoleRepository projectPartyRoleRepository;
    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;
    private final BusinessPartyRepository businessPartyRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public ProjectService(ProjectRepository projectRepository,
                          WbsNodeRepository wbsNodeRepository,
                          ProjectPartyRoleRepository projectPartyRoleRepository,
                          CompanyRepository companyRepository,
                          BranchRepository branchRepository,
                          BusinessPartyRepository businessPartyRepository,
                          AuditService auditService,
                          ObjectMapper objectMapper) {
        this.projectRepository = projectRepository;
        this.wbsNodeRepository = wbsNodeRepository;
        this.projectPartyRoleRepository = projectPartyRoleRepository;
        this.companyRepository = companyRepository;
        this.branchRepository = branchRepository;
        this.businessPartyRepository = businessPartyRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    public List<ProjectResponse> listProjects(String companyId, ProjectStatus status) {
        log.debug("listProjects called with companyId={}, status={}", companyId, status);
        List<Project> projects;
        if (companyId != null && !companyId.isBlank()) {
            projects = projectRepository.findByCompanyIdOrderByCreatedAtDesc(companyId.strip());
        } else if (status != null) {
            projects = projectRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            projects = projectRepository.findAllByOrderByCreatedAtDesc();
        }
        if (projects.isEmpty()) {
            return List.of();
        }

        List<String> projectIds = projects.stream().map(Project::getId).toList();
        List<Object[]> summaries = wbsNodeRepository.summarizeWbsByProjectIds(projectIds);
        Map<String, BigDecimal> plannedAmounts = new HashMap<>();
        Map<String, Integer> wbsCounts = new HashMap<>();
        for (Object[] row : summaries) {
            String pId = (String) row[0];
            BigDecimal sum = (BigDecimal) row[1];
            long count = row[2] instanceof Number ? ((Number) row[2]).longValue() : 0L;
            plannedAmounts.put(pId, sum != null ? sum : BigDecimal.ZERO);
            wbsCounts.put(pId, (int) count);
        }

        return projects.stream()
                .map(p -> toProjectResponse(
                        p,
                        plannedAmounts.getOrDefault(p.getId(), BigDecimal.ZERO),
                        wbsCounts.getOrDefault(p.getId(), 0)
                ))
                .toList();
    }

    public ProjectResponse getProject(String id) {
        log.debug("getProject called with id={}", id);
        return toProjectResponse(requireProject(id));
    }

    public ProjectSummaryResponse getProjectSummary() {
        log.debug("getProjectSummary called");
        long total = projectRepository.count();
        long active = projectRepository.countByStatus(ProjectStatus.ACTIVE);
        long onHold = projectRepository.countByStatus(ProjectStatus.ON_HOLD);
        long completed = projectRepository.countByStatus(ProjectStatus.COMPLETED);
        long closed = projectRepository.countByStatus(ProjectStatus.CLOSED);
        BigDecimal totalContractValue = projectRepository.sumTotalContractValue();
        BigDecimal totalPlannedAmount = wbsNodeRepository.sumTotalPlannedAmount();

        return new ProjectSummaryResponse(total, active, onHold, completed, closed,
                totalContractValue != null ? totalContractValue : BigDecimal.ZERO,
                totalPlannedAmount != null ? totalPlannedAmount : BigDecimal.ZERO);
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        log.debug("createProject called with code={}, name={}", request.code(), request.name());
        String code = request.code().strip();
        if (projectRepository.existsByCode(code)) {
            throw new BusinessRuleException("Project code is already in use.", "PROJECT_CODE_DUPLICATE", HttpStatus.CONFLICT);
        }
        LocalDate startDate = fromEpoch(request.startDate());
        LocalDate endDate = fromEpoch(request.endDate());
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessRuleException("Project end date must be after start date.", "PROJECT_INVALID_DATES", HttpStatus.BAD_REQUEST);
        }

        validateOrganizationAndParty(request.companyId(), request.branchId(), request.ownerPartyId());

        Project project = new Project(
                code,
                request.name(),
                request.nameEn(),
                request.description(),
                request.companyId() != null ? request.companyId().strip() : null,
                request.branchId() != null ? request.branchId().strip() : null,
                request.ownerPartyId() != null ? request.ownerPartyId().strip() : null,
                request.projectManagerId(),
                request.siteAddress(),
                request.contractNumber(),
                request.contractValue(),
                request.currencyCode(),
                startDate,
                endDate,
                request.budgetBlocking() == null || request.budgetBlocking()
        );

        Project saved = projectRepository.save(project);
        auditService.record("PROJECT_CREATE", "PROJECT", saved.getId(), getCurrentUser(),
                toAuditJson(Map.of("code", saved.getCode(), "name", saved.getName())), null);
        log.info("Project {} created with code {}", saved.getId(), saved.getCode());
        return toProjectResponse(saved);
    }

    @Transactional
    public ProjectResponse updateProject(String id, UpdateProjectRequest request) {
        log.debug("updateProject called with id={}", id);
        Project project = requireProject(id);
        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new BusinessRuleException("Project is closed and cannot be modified.", "PROJECT_CLOSED", HttpStatus.CONFLICT);
        }

        LocalDate startDate = fromEpoch(request.startDate());
        LocalDate endDate = fromEpoch(request.endDate());
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessRuleException("Project end date must be after start date.", "PROJECT_INVALID_DATES", HttpStatus.BAD_REQUEST);
        }

        validateOrganizationAndParty(request.companyId(), request.branchId(), request.ownerPartyId());

        project.update(
                request.name(),
                request.nameEn(),
                request.description(),
                request.companyId() != null ? request.companyId().strip() : null,
                request.branchId() != null ? request.branchId().strip() : null,
                request.ownerPartyId() != null ? request.ownerPartyId().strip() : null,
                request.projectManagerId(),
                request.siteAddress(),
                request.contractNumber(),
                request.contractValue(),
                request.currencyCode(),
                startDate,
                endDate,
                request.budgetBlocking() == null || request.budgetBlocking()
        );

        Project saved = projectRepository.save(project);
        auditService.record("PROJECT_UPDATE", "PROJECT", saved.getId(), getCurrentUser(),
                toAuditJson(Map.of("code", saved.getCode(), "name", saved.getName())), null);
        log.info("Project {} updated successfully", saved.getId());
        return toProjectResponse(saved);
    }

    @Transactional
    public ProjectResponse activateProject(String id) {
        log.debug("activateProject called with id={}", id);
        Project project = requireProject(id);
        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new BusinessRuleException("Project is closed.", "PROJECT_CLOSED", HttpStatus.CONFLICT);
        }
        project.activate();
        Project saved = projectRepository.save(project);
        auditService.record("PROJECT_ACTIVATE", "PROJECT", saved.getId(), getCurrentUser(),
                toAuditJson(Map.of("status", "ACTIVE")), null);
        return toProjectResponse(saved);
    }

    @Transactional
    public ProjectResponse holdProject(String id) {
        log.debug("holdProject called with id={}", id);
        Project project = requireProject(id);
        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new BusinessRuleException("Project is closed.", "PROJECT_CLOSED", HttpStatus.CONFLICT);
        }
        project.hold();
        Project saved = projectRepository.save(project);
        auditService.record("PROJECT_HOLD", "PROJECT", saved.getId(), getCurrentUser(),
                toAuditJson(Map.of("status", "ON_HOLD")), null);
        return toProjectResponse(saved);
    }

    @Transactional
    public ProjectResponse completeProject(String id) {
        log.debug("completeProject called with id={}", id);
        Project project = requireProject(id);
        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new BusinessRuleException("Project is closed.", "PROJECT_CLOSED", HttpStatus.CONFLICT);
        }
        project.complete();
        Project saved = projectRepository.save(project);
        auditService.record("PROJECT_COMPLETE", "PROJECT", saved.getId(), getCurrentUser(),
                toAuditJson(Map.of("status", "COMPLETED")), null);
        return toProjectResponse(saved);
    }

    @Transactional
    public ProjectResponse closeProject(String id) {
        log.debug("closeProject called with id={}", id);
        Project project = requireProject(id);
        if (wbsNodeRepository.existsByProjectIdAndStatus(id, WbsNodeStatus.IN_PROGRESS)) {
            throw new BusinessRuleException("Cannot close project with work in progress.", "PROJECT_CLOSE_BLOCKED_IN_PROGRESS", HttpStatus.CONFLICT);
        }
        project.close();
        Project saved = projectRepository.save(project);
        auditService.record("PROJECT_CLOSE", "PROJECT", saved.getId(), getCurrentUser(),
                toAuditJson(Map.of("status", "CLOSED")), null);
        return toProjectResponse(saved);
    }

    @Transactional
    public ProjectResponse reopenProject(String id) {
        log.debug("reopenProject called with id={}", id);
        Project project = requireProject(id);
        project.reopen();
        Project saved = projectRepository.save(project);
        auditService.record("PROJECT_REOPEN", "PROJECT", saved.getId(), getCurrentUser(),
                toAuditJson(Map.of("status", "ACTIVE")), null);
        return toProjectResponse(saved);
    }

    @Transactional
    public void deleteProject(String id) {
        log.debug("deleteProject called with id={}", id);
        Project project = requireProject(id);
        if (project.getStatus() != ProjectStatus.DRAFT) {
            throw new BusinessRuleException("Only draft projects can be deleted.", "PROJECT_DELETE_NOT_ALLOWED", HttpStatus.CONFLICT);
        }
        wbsNodeRepository.deleteByProjectId(id);
        projectPartyRoleRepository.deleteByProjectId(id);
        projectRepository.delete(project);
        auditService.record("PROJECT_DELETE", "PROJECT", id, getCurrentUser(),
                toAuditJson(Map.of("code", project.getCode())), null);
        log.info("Project {} deleted successfully", id);
    }

    // ─── Project Stakeholder Roles ───────────────────────────────────

    public List<ProjectPartyRoleResponse> getProjectRoles(String projectId) {
        requireProject(projectId);
        return projectPartyRoleRepository.findByProjectId(projectId).stream()
                .map(this::toProjectPartyRoleResponse).toList();
    }

    @Transactional
    public ProjectPartyRoleResponse assignProjectRole(String projectId, AssignPartyRoleRequest request) {
        requireProject(projectId);
        String partyId = request.partyId().strip();
        if (businessPartyRepository != null && !businessPartyRepository.existsById(partyId)) {
            throw new BusinessRuleException("Business party not found.", "PARTY_NOT_FOUND", HttpStatus.BAD_REQUEST);
        }
        if (projectPartyRoleRepository.existsByProjectIdAndPartyIdAndRoleType(projectId, partyId, request.roleType())) {
            throw new BusinessRuleException("Party role already assigned to project.", "PROJECT_ROLE_DUPLICATE", HttpStatus.CONFLICT);
        }
        ProjectPartyRole role = new ProjectPartyRole(projectId, partyId, request.roleType(), request.notes());
        ProjectPartyRole saved = projectPartyRoleRepository.save(role);
        auditService.record("PROJECT_ROLE_ASSIGN", "PROJECT", projectId, getCurrentUser(),
                toAuditJson(Map.of("partyId", partyId, "role", request.roleType().name())), null);
        return toProjectPartyRoleResponse(saved);
    }

    @Transactional
    public void removeProjectRole(String roleId) {
        ProjectPartyRole role = projectPartyRoleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Project party role not found: " + roleId));
        projectPartyRoleRepository.delete(role);
        auditService.record("PROJECT_ROLE_REMOVE", "PROJECT", role.getProjectId(), getCurrentUser(),
                toAuditJson(Map.of("roleId", roleId)), null);
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private void validateOrganizationAndParty(String companyId, String branchId, String ownerPartyId) {
        if (companyId != null && !companyId.isBlank()) {
            if (companyRepository != null && !companyRepository.existsById(companyId.strip())) {
                throw new BusinessRuleException("Company not found.", "COMPANY_NOT_FOUND", HttpStatus.BAD_REQUEST);
            }
        }
        if (branchId != null && !branchId.isBlank()) {
            if (branchRepository != null && !branchRepository.existsById(branchId.strip())) {
                throw new BusinessRuleException("Branch not found.", "BRANCH_NOT_FOUND", HttpStatus.BAD_REQUEST);
            }
        }
        if (ownerPartyId != null && !ownerPartyId.isBlank()) {
            if (businessPartyRepository != null && !businessPartyRepository.existsById(ownerPartyId.strip())) {
                throw new BusinessRuleException("Business party not found.", "PARTY_NOT_FOUND", HttpStatus.BAD_REQUEST);
            }
        }
    }

    public Project requireProject(String id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found: " + id, "PROJECT_NOT_FOUND"));
    }

    private ProjectResponse toProjectResponse(Project project) {
        List<WbsNode> wbsList = wbsNodeRepository.findByProjectIdOrderBySortOrderAsc(project.getId());
        BigDecimal totalPlanned = wbsList.stream()
                .map(WbsNode::getPlannedAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return toProjectResponse(project, totalPlanned, wbsList.size());
    }

    private ProjectResponse toProjectResponse(Project project, BigDecimal totalPlanned, int wbsCount) {
        return new ProjectResponse(
                project.getId(),
                project.getCode(),
                project.getName(),
                project.getNameEn(),
                project.getDescription(),
                project.getCompanyId(),
                project.getBranchId(),
                project.getOwnerPartyId(),
                project.getProjectManagerId(),
                project.getSiteAddress(),
                project.getContractNumber(),
                project.getContractValue(),
                project.getCurrencyCode(),
                toEpoch(project.getStartDate()),
                toEpoch(project.getEndDate()),
                project.getStatus(),
                project.isBudgetBlocking(),
                project.isActive(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                project.getVersion(),
                totalPlanned != null ? totalPlanned : BigDecimal.ZERO,
                wbsCount
        );
    }

    private ProjectPartyRoleResponse toProjectPartyRoleResponse(ProjectPartyRole role) {
        return new ProjectPartyRoleResponse(
                role.getId(),
                role.getProjectId(),
                role.getPartyId(),
                role.getRoleType(),
                role.getNotes(),
                role.getCreatedAt()
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

    private String toAuditJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.warn("Failed to serialize audit detail payload", ex);
            return "{}";
        }
    }
}
