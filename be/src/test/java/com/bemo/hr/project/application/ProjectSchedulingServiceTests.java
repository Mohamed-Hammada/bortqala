package com.bemo.hr.project.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.project.api.ScheduleApi.*;
import com.bemo.hr.project.domain.*;
import com.bemo.hr.project.infrastructure.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectSchedulingServiceTests {

    @Mock
    private ProjectScheduleRepository scheduleRepository;

    @Mock
    private ProjectScheduleTaskRepository taskRepository;

    @Mock
    private TaskDependencyRepository dependencyRepository;

    @Mock
    private ScheduleBaselineRepository baselineRepository;

    @Mock
    private ScheduleBaselineTaskRepository baselineTaskRepository;

    @Mock
    private TaskResourceAssignmentRepository resourceAssignmentRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WbsNodeRepository wbsNodeRepository;

    @Mock
    private DailyWorkProgressLineRepository progressLineRepository;

    @Mock
    private AuditService auditService;

    private ProjectSchedulingService service;

    private Project project;
    private ProjectSchedule schedule;
    private ProjectScheduleTask taskA;
    private ProjectScheduleTask taskB;
    private ProjectScheduleTask taskC;

    @BeforeEach
    void setUp() {
        service = new ProjectSchedulingService(
                scheduleRepository,
                taskRepository,
                dependencyRepository,
                baselineRepository,
                baselineTaskRepository,
                resourceAssignmentRepository,
                projectRepository,
                wbsNodeRepository,
                progressLineRepository,
                auditService
        );

        project = new Project(
                "PRJ-001",
                "برج النخيل",
                "Palm Tower",
                "وصف المشروع",
                "c-1",
                "b-1",
                "party-1",
                "pm-1",
                "القاهرة",
                "CNT-101",
                BigDecimal.valueOf(50000000),
                "EGP",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 12, 31),
                true
        );

        schedule = new ProjectSchedule(
                project.getId(),
                "Palm Tower Schedule",
                "STANDARD_6DAY",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 12, 31)
        );

        taskA = new ProjectScheduleTask(
                schedule.getId(), null, null, "TSK-01", "حفر الموقع", "Excavation",
                5, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5),
                false, TaskConstraintType.ASAP, null, 1
        );

        taskB = new ProjectScheduleTask(
                schedule.getId(), null, null, "TSK-02", "صب الخرسانة العادية", "Plain Concrete",
                3, LocalDate.of(2026, 3, 6), LocalDate.of(2026, 3, 8),
                false, TaskConstraintType.ASAP, null, 2
        );

        taskC = new ProjectScheduleTask(
                schedule.getId(), null, null, "TSK-03", "حدادة القواعد المسلحة", "Rebar Footings",
                4, LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 12),
                false, TaskConstraintType.ASAP, null, 3
        );
    }

    @Test
    void recalculateCpm_linearDependencies_calculatesCriticalPath() {
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(scheduleRepository.findByProjectId(project.getId())).thenReturn(Optional.of(schedule));
        when(taskRepository.findByScheduleIdOrderBySortOrderAsc(schedule.getId())).thenReturn(List.of(taskA, taskB, taskC));

        TaskDependency dep1 = new TaskDependency(schedule.getId(), taskA.getId(), taskB.getId(), TaskDependencyType.FS, 0);
        TaskDependency dep2 = new TaskDependency(schedule.getId(), taskB.getId(), taskC.getId(), TaskDependencyType.FS, 0);
        when(dependencyRepository.findByScheduleId(schedule.getId())).thenReturn(List.of(dep1, dep2));

        ProjectScheduleResponse response = service.recalculateCpm(project.getId());

        assertThat(response).isNotNull();
        // Since it's a linear chain with no slack, all tasks must be critical with 0 total float
        assertThat(taskA.isCritical()).isTrue();
        assertThat(taskA.getTotalFloatDays()).isEqualTo(0);
        assertThat(taskB.isCritical()).isTrue();
        assertThat(taskB.getTotalFloatDays()).isEqualTo(0);
        assertThat(taskC.isCritical()).isTrue();
        assertThat(taskC.getTotalFloatDays()).isEqualTo(0);

        // Verify forward pass dates
        assertThat(taskA.getEarlyStartDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(taskA.getEarlyEndDate()).isEqualTo(LocalDate.of(2026, 3, 5));
        assertThat(taskB.getEarlyStartDate()).isEqualTo(LocalDate.of(2026, 3, 6));
        assertThat(taskB.getEarlyEndDate()).isEqualTo(LocalDate.of(2026, 3, 8));
        assertThat(taskC.getEarlyStartDate()).isEqualTo(LocalDate.of(2026, 3, 9));
        assertThat(taskC.getEarlyEndDate()).isEqualTo(LocalDate.of(2026, 3, 12));
    }

    @Test
    void recalculateCpm_circularDependency_throwsException() {
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(scheduleRepository.findByProjectId(project.getId())).thenReturn(Optional.of(schedule));
        when(taskRepository.findByScheduleIdOrderBySortOrderAsc(schedule.getId())).thenReturn(List.of(taskA, taskB, taskC));

        // Create circular cycle: A -> B -> C -> A
        TaskDependency dep1 = new TaskDependency(schedule.getId(), taskA.getId(), taskB.getId(), TaskDependencyType.FS, 0);
        TaskDependency dep2 = new TaskDependency(schedule.getId(), taskB.getId(), taskC.getId(), TaskDependencyType.FS, 0);
        TaskDependency dep3 = new TaskDependency(schedule.getId(), taskC.getId(), taskA.getId(), TaskDependencyType.FS, 0);
        when(dependencyRepository.findByScheduleId(schedule.getId())).thenReturn(List.of(dep1, dep2, dep3));

        assertThatThrownBy(() -> service.recalculateCpm(project.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("SCHEDULE_CYCLE_DETECTED");
    }

    @Test
    void createBaseline_createsSnapshotAndIncrementsVersion() {
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(scheduleRepository.findByProjectId(project.getId())).thenReturn(Optional.of(schedule));
        when(taskRepository.findByScheduleIdOrderBySortOrderAsc(schedule.getId())).thenReturn(List.of(taskA, taskB));
        when(dependencyRepository.findByScheduleId(schedule.getId())).thenReturn(List.of());
        when(baselineRepository.save(any(ScheduleBaseline.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateBaselineRequest req = new CreateBaselineRequest("Contract Baseline v1", "Original target schedule");
        ScheduleBaselineResponse res = service.createBaseline(project.getId(), req, "pm-1");

        assertThat(res).isNotNull();
        assertThat(res.versionNumber()).isEqualTo(1);
        assertThat(res.name()).isEqualTo("Contract Baseline v1");
        assertThat(schedule.getCurrentBaselineVersion()).isEqualTo(1);
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.BASELINE_LOCKED);

        verify(baselineTaskRepository, times(2)).save(any(ScheduleBaselineTask.class));
    }

    @Test
    void detectOverAllocations_flagsTasksExceedingDailyCapacity() {
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(scheduleRepository.findByProjectId(project.getId())).thenReturn(Optional.of(schedule));
        when(taskRepository.findByScheduleIdOrderBySortOrderAsc(schedule.getId())).thenReturn(List.of(taskA, taskB));

        // Assign same excavator to taskA (6 hours) and taskB (6 hours) on overlapping dates -> 12 hours > 8 capacity limit
        TaskResourceAssignment r1 = new TaskResourceAssignment(taskA.getId(), TaskResourceType.EQUIPMENT,
                "حفار 20 طن", null, null, BigDecimal.valueOf(6.0),
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5), null);
        TaskResourceAssignment r2 = new TaskResourceAssignment(taskB.getId(), TaskResourceType.EQUIPMENT,
                "حفار 20 طن", null, null, BigDecimal.valueOf(6.0),
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 3), null);

        when(resourceAssignmentRepository.findByTaskId(taskA.getId())).thenReturn(List.of(r1));
        when(resourceAssignmentRepository.findByTaskId(taskB.getId())).thenReturn(List.of(r2));

        List<ResourceOverAllocationResponse> overList = service.detectOverAllocations(project.getId());

        assertThat(overList).isNotEmpty();
        assertThat(overList.get(0).resourceName()).isEqualTo("حفار 20 طن");
        assertThat(overList.get(0).allocatedQuantity()).isEqualTo(BigDecimal.valueOf(12.0));
        assertThat(overList.get(0).overAllocatedAmount()).isEqualTo(BigDecimal.valueOf(4.0));
    }
}
