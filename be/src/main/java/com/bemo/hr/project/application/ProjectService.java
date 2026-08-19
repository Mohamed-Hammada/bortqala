package com.bemo.hr.project.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.project.api.ProjectApi.*;
import com.bemo.hr.project.domain.Project;
import com.bemo.hr.project.domain.ProjectPartyRole;
import com.bemo.hr.project.domain.ProjectStatus;
import com.bemo.hr.project.domain.WbsNode;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final WbsNodeRepository wbsNodeRepository;
    private final ProjectPartyRoleRepository projectPartyRoleRepository;
    private final AuditService auditService;

    public ProjectService(ProjectRepository projectRepository,
                          WbsNodeRepository wbsNodeRepository,
                          ProjectPartyRoleRepository projectPartyRoleRepository,
                          AuditService auditService) {
        this.projectRepository = projectRepository;
        this.wbsNodeRepository = wbsNodeRepository;
        this.projectPartyRoleRepository = projectPartyRoleRepository;
        this.auditService = auditService;
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
        return projects.stream().map(this::toProjectResponse).toList();
    }

    public ProjectResponse getProject(String id) {
        log.debug("getProject called with id={}", id);
        return toProjectResponse(requireProject(id));
    }

    public ProjectSummaryResponse getProjectSummary() {
        log.debug("getProjectSummary called");
        List<Project> all = projectRepository.findAll();
        long total = all.size();
        long active = all.stream().filter(p -> p.getStatus() == ProjectStatus.ACTIVE).count();
        long onHold = all.stream().filter(p -> p.getStatus() == ProjectStatus.ON_HOLD).count();
        long completed = all.stream().filter(p -> p.getStatus() == ProjectStatus.COMPLETED).count();
        long closed = all.stream().filter(p -> p.getStatus() == ProjectStatus.CLOSED).count();
        BigDecimal totalContractValue = all.stream()
                .map(Project::getContractValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<WbsNode> allWbs = wbsNodeRepository.findAll();
        BigDecimal totalPlannedAmount = allWbs.stream()
                .map(WbsNode::getPlannedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ProjectSummaryResponse(total, active, onHold, completed, closed, totalContractValue, totalPlannedAmount);
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

        Project project = new Project(
                code,
                request.name(),
                request.nameEn(),
                request.description(),
                request.companyId(),
                request.branchId(),
                request.ownerPartyId(),
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
                "{\"code\":\"" + saved.getCode() + "\",\"name\":\"" + saved.getName() + "\"}", null);
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

        project.update(
                request.name(),
                request.nameEn(),
                request.description(),
                request.companyId(),
                request.branchId(),
                request.ownerPartyId(),
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
                "{\"code\":\"" + saved.getCode() + "\",\"name\":\"" + saved.getName() + "\"}", null);
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
                "{\"status\":\"ACTIVE\"}", null);
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
                "{\"status\":\"ON_HOLD\"}", null);
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
                "{\"status\":\"COMPLETED\"}", null);
        return toProjectResponse(saved);
    }

    @Transactional
    public ProjectResponse closeProject(String id) {
        log.debug("closeProject called with id={}", id);
        Project project = requireProject(id);
        project.close();
        Project saved = projectRepository.save(project);
        auditService.record("PROJECT_CLOSE", "PROJECT", saved.getId(), getCurrentUser(),
                "{\"status\":\"CLOSED\"}", null);
        return toProjectResponse(saved);
    }

    @Transactional
    public ProjectResponse reopenProject(String id) {
        log.debug("reopenProject called with id={}", id);
        Project project = requireProject(id);
        project.reopen();
        Project saved = projectRepository.save(project);
        auditService.record("PROJECT_REOPEN", "PROJECT", saved.getId(), getCurrentUser(),
                "{\"status\":\"ACTIVE\"}", null);
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
                "{\"code\":\"" + project.getCode() + "\"}", null);
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
        if (projectPartyRoleRepository.existsByProjectIdAndPartyIdAndRoleType(projectId, partyId, request.roleType())) {
            throw new BusinessRuleException("Party role already assigned to project.", "PROJECT_ROLE_DUPLICATE", HttpStatus.CONFLICT);
        }
        ProjectPartyRole role = new ProjectPartyRole(projectId, partyId, request.roleType(), request.notes());
        ProjectPartyRole saved = projectPartyRoleRepository.save(role);
        auditService.record("PROJECT_ROLE_ASSIGN", "PROJECT", projectId, getCurrentUser(),
                "{\"partyId\":\"" + partyId + "\",\"role\":\"" + request.roleType().name() + "\"}", null);
        return toProjectPartyRoleResponse(saved);
    }

    @Transactional
    public void removeProjectRole(String roleId) {
        ProjectPartyRole role = projectPartyRoleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Project party role not found: " + roleId));
        projectPartyRoleRepository.delete(role);
        auditService.record("PROJECT_ROLE_REMOVE", "PROJECT", role.getProjectId(), getCurrentUser(),
                "{\"roleId\":\"" + roleId + "\"}", null);
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    public Project requireProject(String id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found: " + id, "PROJECT_NOT_FOUND"));
    }

    private ProjectResponse toProjectResponse(Project project) {
        List<WbsNode> wbsList = wbsNodeRepository.findByProjectIdOrderBySortOrderAsc(project.getId());
        BigDecimal totalPlanned = wbsList.stream()
                .map(WbsNode::getPlannedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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
                totalPlanned,
                wbsList.size()
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
}
