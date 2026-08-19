package com.bemo.hr.project.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.project.api.ProjectApi.*;
import com.bemo.hr.project.domain.*;
import com.bemo.hr.project.infrastructure.ProjectPartyRoleRepository;
import com.bemo.hr.project.infrastructure.ProjectRepository;
import com.bemo.hr.project.infrastructure.WbsNodeRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProjectServiceTests {

    private ProjectRepository projectRepository;
    private WbsNodeRepository wbsNodeRepository;
    private ProjectPartyRoleRepository projectPartyRoleRepository;
    private AuditService auditService;
    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        wbsNodeRepository = mock(WbsNodeRepository.class);
        projectPartyRoleRepository = mock(ProjectPartyRoleRepository.class);
        auditService = mock(AuditService.class);
        projectService = new ProjectService(projectRepository, wbsNodeRepository, projectPartyRoleRepository, auditService);
    }

    @Test
    void createProject_succeeds_whenValid() {
        when(projectRepository.existsByCode("PRJ-001")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        long startMs = LocalDate.of(2026, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        long endMs = LocalDate.of(2026, 12, 31).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        CreateProjectRequest request = new CreateProjectRequest(
                "PRJ-001", "برج النخيل", "Palm Tower", "مشروع سكني تجاري",
                "comp-1", "br-1", "party-client-1", "mgr-1",
                "القاهرة الجديدة", "CNT-2026-001", new BigDecimal("50000000.00"),
                "EGP", startMs, endMs, true
        );

        ProjectResponse response = projectService.createProject(request);

        assertThat(response).isNotNull();
        assertThat(response.code()).isEqualTo("PRJ-001");
        assertThat(response.name()).isEqualTo("برج النخيل");
        assertThat(response.nameEn()).isEqualTo("Palm Tower");
        assertThat(response.contractValue()).isEqualTo(new BigDecimal("50000000.00"));
        assertThat(response.status()).isEqualTo(ProjectStatus.DRAFT);
        assertThat(response.budgetBlocking()).isTrue();

        verify(auditService).record(eq("PROJECT_CREATE"), eq("PROJECT"), anyString(), anyString(), anyString(), isNull());
    }

    @Test
    void createProject_throwsConflict_whenCodeAlreadyExists() {
        when(projectRepository.existsByCode("PRJ-001")).thenReturn(true);

        CreateProjectRequest request = new CreateProjectRequest(
                "PRJ-001", "برج النخيل", null, null, null, null, null,
                null, null, null, null, null, null, null, null
        );

        assertThatThrownBy(() -> projectService.createProject(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Project code is already in use.");
    }

    @Test
    void createProject_throwsBadRequest_whenEndDateBeforeStartDate() {
        when(projectRepository.existsByCode("PRJ-001")).thenReturn(false);

        long startMs = LocalDate.of(2026, 12, 31).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        long endMs = LocalDate.of(2026, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        CreateProjectRequest request = new CreateProjectRequest(
                "PRJ-001", "برج النخيل", null, null, null, null, null,
                null, null, null, null, null, startMs, endMs, null
        );

        assertThatThrownBy(() -> projectService.createProject(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Project end date must be after start date.");
    }

    @Test
    void updateProject_succeeds_whenActive() {
        Project project = new Project(
                "PRJ-001", "برج النخيل", null, null, null, null, null,
                null, null, null, new BigDecimal("1000000"), "EGP", null, null, true
        );
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProjectRequest updateRequest = new UpdateProjectRequest(
                "برج النخيل المطور", "Palm Tower Updated", "وصف جديد",
                "comp-1", "br-1", "party-1", "mgr-2", "التجمع الخامس",
                "CNT-2026-999", new BigDecimal("75000000.00"), "EGP",
                null, null, true
        );

        ProjectResponse response = projectService.updateProject(project.getId(), updateRequest);

        assertThat(response.name()).isEqualTo("برج النخيل المطور");
        assertThat(response.nameEn()).isEqualTo("Palm Tower Updated");
        assertThat(response.contractValue()).isEqualTo(new BigDecimal("75000000.00"));
    }

    @Test
    void updateProject_throwsConflict_whenProjectIsClosed() {
        Project project = new Project(
                "PRJ-001", "برج النخيل", null, null, null, null, null,
                null, null, null, BigDecimal.ZERO, "EGP", null, null, true
        );
        project.close();
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        UpdateProjectRequest updateRequest = new UpdateProjectRequest(
                "اسم معدل", null, null, null, null, null, null,
                null, null, BigDecimal.ZERO, "EGP", null, null, null
        );

        assertThatThrownBy(() -> projectService.updateProject(project.getId(), updateRequest))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Project is closed");
    }

    @Test
    void projectLifecycle_transitionsStateCorrectly() {
        Project project = new Project(
                "PRJ-001", "برج النخيل", null, null, null, null, null,
                null, null, null, BigDecimal.ZERO, "EGP", null, null, true
        );
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 1. Activate
        ProjectResponse activeRes = projectService.activateProject(project.getId());
        assertThat(activeRes.status()).isEqualTo(ProjectStatus.ACTIVE);

        // 2. Hold
        ProjectResponse holdRes = projectService.holdProject(project.getId());
        assertThat(holdRes.status()).isEqualTo(ProjectStatus.ON_HOLD);

        // 3. Complete
        ProjectResponse compRes = projectService.completeProject(project.getId());
        assertThat(compRes.status()).isEqualTo(ProjectStatus.COMPLETED);

        // 4. Close
        ProjectResponse closeRes = projectService.closeProject(project.getId());
        assertThat(closeRes.status()).isEqualTo(ProjectStatus.CLOSED);

        // 5. Reopen
        ProjectResponse reopenRes = projectService.reopenProject(project.getId());
        assertThat(reopenRes.status()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    void deleteProject_succeeds_onlyForDraft() {
        Project draftProject = new Project(
                "PRJ-DRAFT", "مشروع مسودة", null, null, null, null, null,
                null, null, null, BigDecimal.ZERO, "EGP", null, null, true
        );
        when(projectRepository.findById(draftProject.getId())).thenReturn(Optional.of(draftProject));

        projectService.deleteProject(draftProject.getId());

        verify(wbsNodeRepository).deleteByProjectId(draftProject.getId());
        verify(projectPartyRoleRepository).deleteByProjectId(draftProject.getId());
        verify(projectRepository).delete(draftProject);
    }

    @Test
    void assignProjectRole_succeeds_andRejectsDuplicate() {
        Project project = new Project(
                "PRJ-001", "برج النخيل", null, null, null, null, null,
                null, null, null, BigDecimal.ZERO, "EGP", null, null, true
        );
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectPartyRoleRepository.existsByProjectIdAndPartyIdAndRoleType(project.getId(), "party-1", ProjectPartyRoleType.CLIENT_OWNER))
                .thenReturn(false);
        when(projectPartyRoleRepository.save(any(ProjectPartyRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssignPartyRoleRequest request = new AssignPartyRoleRequest("party-1", ProjectPartyRoleType.CLIENT_OWNER, "المالك الأساسي");
        ProjectPartyRoleResponse response = projectService.assignProjectRole(project.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.partyId()).isEqualTo("party-1");
        assertThat(response.roleType()).isEqualTo(ProjectPartyRoleType.CLIENT_OWNER);

        // Duplicate rejection
        when(projectPartyRoleRepository.existsByProjectIdAndPartyIdAndRoleType(project.getId(), "party-1", ProjectPartyRoleType.CLIENT_OWNER))
                .thenReturn(true);
        assertThatThrownBy(() -> projectService.assignProjectRole(project.getId(), request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Party role already assigned");
    }
}
