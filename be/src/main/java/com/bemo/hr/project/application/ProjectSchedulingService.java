package com.bemo.hr.project.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.project.api.ScheduleApi.*;
import com.bemo.hr.project.domain.*;
import com.bemo.hr.project.infrastructure.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ProjectSchedulingService {

    private final ProjectScheduleRepository scheduleRepository;
    private final ProjectScheduleTaskRepository taskRepository;
    private final TaskDependencyRepository dependencyRepository;
    private final ScheduleBaselineRepository baselineRepository;
    private final ScheduleBaselineTaskRepository baselineTaskRepository;
    private final TaskResourceAssignmentRepository resourceAssignmentRepository;
    private final ProjectRepository projectRepository;
    private final WbsNodeRepository wbsNodeRepository;
    private final DailyWorkProgressLineRepository progressLineRepository;
    private final AuditService auditService;

    public ProjectSchedulingService(
            ProjectScheduleRepository scheduleRepository,
            ProjectScheduleTaskRepository taskRepository,
            TaskDependencyRepository dependencyRepository,
            ScheduleBaselineRepository baselineRepository,
            ScheduleBaselineTaskRepository baselineTaskRepository,
            TaskResourceAssignmentRepository resourceAssignmentRepository,
            ProjectRepository projectRepository,
            WbsNodeRepository wbsNodeRepository,
            DailyWorkProgressLineRepository progressLineRepository,
            AuditService auditService) {
        this.scheduleRepository = scheduleRepository;
        this.taskRepository = taskRepository;
        this.dependencyRepository = dependencyRepository;
        this.baselineRepository = baselineRepository;
        this.baselineTaskRepository = baselineTaskRepository;
        this.resourceAssignmentRepository = resourceAssignmentRepository;
        this.projectRepository = projectRepository;
        this.wbsNodeRepository = wbsNodeRepository;
        this.progressLineRepository = progressLineRepository;
        this.auditService = auditService;
    }

    public ProjectSchedule getOrCreateSchedule(String projectId) {
        Project project = requireProject(projectId);
        return scheduleRepository.findByProjectId(projectId).orElseGet(() -> {
            ProjectSchedule schedule = new ProjectSchedule(
                    projectId,
                    project.getName() + " - Schedule",
                    "STANDARD_6DAY",
                    project.getStartDate(),
                    project.getEndDate()
            );
            return scheduleRepository.save(schedule);
        });
    }

    @Transactional(readOnly = true)
    public ProjectScheduleResponse getSchedule(String projectId) {
        ProjectSchedule schedule = getOrCreateSchedule(projectId);
        List<ProjectScheduleTask> tasks = taskRepository.findByScheduleIdOrderBySortOrderAsc(schedule.getId());
        List<TaskDependency> dependencies = dependencyRepository.findByScheduleId(schedule.getId());
        List<ScheduleBaseline> baselines = baselineRepository.findByScheduleIdOrderByVersionNumberDesc(schedule.getId());

        int criticalCount = (int) tasks.stream().filter(ProjectScheduleTask::isCritical).count();
        BigDecimal avgProgress = tasks.isEmpty() ? BigDecimal.ZERO :
                tasks.stream()
                        .map(ProjectScheduleTask::getPercentComplete)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(tasks.size()), 2, RoundingMode.HALF_UP);

        List<ProjectScheduleTaskResponse> taskResponses = tasks.stream().map(t -> {
            List<TaskResourceAssignmentResponse> resources = resourceAssignmentRepository.findByTaskId(t.getId())
                    .stream().map(this::mapResourceResponse).toList();
            return mapTaskResponse(t, resources);
        }).toList();

        List<TaskDependencyResponse> depResponses = dependencies.stream().map(this::mapDependencyResponse).toList();
        List<ScheduleBaselineResponse> baseResponses = baselines.stream().map(b -> {
            int count = baselineTaskRepository.findByBaselineId(b.getId()).size();
            return mapBaselineResponse(b, count);
        }).toList();

        return new ProjectScheduleResponse(
                schedule.getId(),
                schedule.getProjectId(),
                schedule.getName(),
                schedule.getCalendarCode(),
                localDateToEpoch(schedule.getStartDate()),
                localDateToEpoch(schedule.getEndDate()),
                schedule.getStatus(),
                schedule.getCurrentBaselineVersion(),
                tasks.size(),
                criticalCount,
                avgProgress,
                schedule.getCreatedAt(),
                schedule.getUpdatedAt(),
                schedule.getVersion(),
                taskResponses,
                depResponses,
                baseResponses
        );
    }

    public ProjectScheduleTaskResponse createTask(String projectId, CreateScheduleTaskRequest req, String userId) {
        ProjectSchedule schedule = getOrCreateSchedule(projectId);
        LocalDate plannedStart = epochToLocalDate(req.plannedStartDate());
        LocalDate plannedEnd = epochToLocalDate(req.plannedEndDate());
        LocalDate constraintDate = epochToLocalDate(req.constraintDate());

        ProjectScheduleTask task = new ProjectScheduleTask(
                schedule.getId(),
                req.wbsNodeId(),
                req.parentTaskId(),
                req.taskCode(),
                req.name(),
                req.nameEn(),
                req.durationDays(),
                plannedStart,
                plannedEnd,
                req.isMilestone(),
                req.constraintType(),
                constraintDate,
                req.sortOrder()
        );
        task = taskRepository.save(task);

        recalculateCpmInternal(schedule);

        auditService.record("SCHEDULE_TASK_CREATE", "PROJECT_SCHEDULE_TASK", task.getId(), userId,
                "Created task " + task.getTaskCode() + " - " + task.getName(), null);

        return mapTaskResponse(task, List.of());
    }

    public ProjectScheduleTaskResponse updateTask(String projectId, String taskId, UpdateScheduleTaskRequest req, String userId) {
        ProjectSchedule schedule = getOrCreateSchedule(projectId);
        ProjectScheduleTask task = requireTask(taskId);

        task.updateDetails(
                req.wbsNodeId(),
                req.parentTaskId(),
                req.taskCode(),
                req.name(),
                req.nameEn(),
                req.durationDays(),
                epochToLocalDate(req.plannedStartDate()),
                epochToLocalDate(req.plannedEndDate()),
                req.isMilestone(),
                req.constraintType(),
                epochToLocalDate(req.constraintDate()),
                req.sortOrder()
        );
        task = taskRepository.save(task);

        recalculateCpmInternal(schedule);

        auditService.record("SCHEDULE_TASK_UPDATE", "PROJECT_SCHEDULE_TASK", task.getId(), userId,
                "Updated task " + task.getTaskCode(), null);

        List<TaskResourceAssignmentResponse> resources = resourceAssignmentRepository.findByTaskId(task.getId())
                .stream().map(this::mapResourceResponse).toList();
        return mapTaskResponse(task, resources);
    }

    public void deleteTask(String projectId, String taskId, String userId) {
        ProjectSchedule schedule = getOrCreateSchedule(projectId);
        ProjectScheduleTask task = requireTask(taskId);

        dependencyRepository.deleteByPredecessorTaskIdOrSuccessorTaskId(taskId, taskId);
        resourceAssignmentRepository.deleteByTaskId(taskId);
        taskRepository.delete(task);

        recalculateCpmInternal(schedule);

        auditService.record("SCHEDULE_TASK_DELETE", "PROJECT_SCHEDULE_TASK", taskId, userId,
                "Deleted task " + task.getTaskCode(), null);
    }

    public TaskDependencyResponse addDependency(String projectId, CreateDependencyRequest req, String userId) {
        ProjectSchedule schedule = getOrCreateSchedule(projectId);
        requireTask(req.predecessorTaskId());
        requireTask(req.successorTaskId());

        if (req.predecessorTaskId().equals(req.successorTaskId())) {
            throw new BusinessRuleException("CANNOT_DEPEND_ON_SELF");
        }

        if (dependencyRepository.findByScheduleIdAndPredecessorTaskIdAndSuccessorTaskId(
                schedule.getId(), req.predecessorTaskId(), req.successorTaskId()).isPresent()) {
            throw new BusinessRuleException("DEPENDENCY_ALREADY_EXISTS");
        }

        TaskDependency dep = new TaskDependency(
                schedule.getId(),
                req.predecessorTaskId(),
                req.successorTaskId(),
                req.dependencyType(),
                req.lagDays()
        );
        dep = dependencyRepository.save(dep);

        // Recalculate CPM (will detect cycles and throw BusinessRuleException if invalid)
        try {
            recalculateCpmInternal(schedule);
        } catch (BusinessRuleException ex) {
            dependencyRepository.delete(dep);
            throw ex;
        }

        auditService.record("SCHEDULE_DEP_ADD", "TASK_DEPENDENCY", dep.getId(), userId,
                "Added dependency " + req.predecessorTaskId() + " -> " + req.successorTaskId(), null);

        return mapDependencyResponse(dep);
    }

    public void removeDependency(String projectId, String dependencyId, String userId) {
        ProjectSchedule schedule = getOrCreateSchedule(projectId);
        TaskDependency dep = dependencyRepository.findById(dependencyId)
                .orElseThrow(() -> new NotFoundException("DEPENDENCY_NOT_FOUND"));

        dependencyRepository.delete(dep);
        recalculateCpmInternal(schedule);

        auditService.record("SCHEDULE_DEP_REMOVE", "TASK_DEPENDENCY", dependencyId, userId,
                "Removed dependency", null);
    }

    public ProjectScheduleResponse recalculateCpm(String projectId) {
        ProjectSchedule schedule = getOrCreateSchedule(projectId);
        recalculateCpmInternal(schedule);
        return getSchedule(projectId);
    }

    public ScheduleBaselineResponse createBaseline(String projectId, CreateBaselineRequest req, String userId) {
        ProjectSchedule schedule = getOrCreateSchedule(projectId);
        recalculateCpmInternal(schedule);

        List<ProjectScheduleTask> tasks = taskRepository.findByScheduleIdOrderBySortOrderAsc(schedule.getId());
        if (tasks.isEmpty()) {
            throw new BusinessRuleException("CANNOT_BASELINE_EMPTY_SCHEDULE");
        }

        int newVersion = schedule.getCurrentBaselineVersion() + 1;
        ScheduleBaseline baseline = new ScheduleBaseline(
                schedule.getId(),
                newVersion,
                req.name(),
                userId,
                req.notes()
        );
        baseline = baselineRepository.save(baseline);

        for (ProjectScheduleTask t : tasks) {
            LocalDate start = t.getEarlyStartDate() != null ? t.getEarlyStartDate() :
                    (t.getPlannedStartDate() != null ? t.getPlannedStartDate() : LocalDate.now());
            LocalDate end = t.getEarlyEndDate() != null ? t.getEarlyEndDate() :
                    (t.getPlannedEndDate() != null ? t.getPlannedEndDate() : start);

            ScheduleBaselineTask bt = new ScheduleBaselineTask(
                    baseline.getId(),
                    t.getId(),
                    start,
                    end,
                    t.getDurationDays(),
                    BigDecimal.ZERO
            );
            baselineTaskRepository.save(bt);
        }

        schedule.incrementBaselineVersion();
        scheduleRepository.save(schedule);

        auditService.record("SCHEDULE_BASELINE_CREATE", "SCHEDULE_BASELINE", baseline.getId(), userId,
                "Created baseline v" + newVersion + " (" + baseline.getName() + ")", null);

        return mapBaselineResponse(baseline, tasks.size());
    }

    @Transactional(readOnly = true)
    public List<ScheduleBaselineComparisonResponse> getBaselineComparison(String projectId, String baselineId) {
        ProjectSchedule schedule = getOrCreateSchedule(projectId);
        List<ProjectScheduleTask> currentTasks = taskRepository.findByScheduleIdOrderBySortOrderAsc(schedule.getId());
        List<ScheduleBaselineTask> baselineTasks = baselineTaskRepository.findByBaselineId(baselineId);

        Map<String, ScheduleBaselineTask> baseMap = baselineTasks.stream()
                .collect(Collectors.toMap(ScheduleBaselineTask::getTaskId, bt -> bt, (a, b) -> a));

        List<ScheduleBaselineComparisonResponse> comparisons = new ArrayList<>();
        for (ProjectScheduleTask t : currentTasks) {
            ScheduleBaselineTask bt = baseMap.get(t.getId());
            if (bt != null) {
                LocalDate curStart = t.getEarlyStartDate() != null ? t.getEarlyStartDate() : t.getPlannedStartDate();
                LocalDate curEnd = t.getEarlyEndDate() != null ? t.getEarlyEndDate() : t.getPlannedEndDate();
                int variance = (curEnd != null && bt.getBaselineEndDate() != null)
                        ? (int) ChronoUnit.DAYS.between(bt.getBaselineEndDate(), curEnd)
                        : 0;

                comparisons.add(new ScheduleBaselineComparisonResponse(
                        t.getId(),
                        t.getTaskCode(),
                        t.getName(),
                        localDateToEpoch(bt.getBaselineStartDate()),
                        localDateToEpoch(bt.getBaselineEndDate()),
                        bt.getDurationDays(),
                        localDateToEpoch(curStart),
                        localDateToEpoch(curEnd),
                        t.getDurationDays(),
                        variance,
                        t.isCritical()
                ));
            }
        }
        return comparisons;
    }

    public TaskResourceAssignmentResponse assignResource(String projectId, String taskId, AssignResourceRequest req, String userId) {
        requireProject(projectId);
        requireTask(taskId);

        TaskResourceAssignment assignment = new TaskResourceAssignment(
                taskId,
                req.resourceType(),
                req.resourceName(),
                req.partyId(),
                req.employeeId(),
                req.quantityAllocated(),
                epochToLocalDate(req.startDate()),
                epochToLocalDate(req.endDate()),
                req.notes()
        );
        assignment = resourceAssignmentRepository.save(assignment);

        auditService.record("SCHEDULE_RESOURCE_ASSIGN", "TASK_RESOURCE_ASSIGNMENT", assignment.getId(), userId,
                "Assigned resource " + req.resourceName() + " to task " + taskId, null);

        return mapResourceResponse(assignment);
    }

    public void removeResourceAssignment(String projectId, String assignmentId, String userId) {
        requireProject(projectId);
        TaskResourceAssignment assignment = resourceAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("RESOURCE_ASSIGNMENT_NOT_FOUND"));

        resourceAssignmentRepository.delete(assignment);
        auditService.record("SCHEDULE_RESOURCE_REMOVE", "TASK_RESOURCE_ASSIGNMENT", assignmentId, userId,
                "Removed resource assignment", null);
    }

    public void importFromWbs(String projectId, String userId) {
        ProjectSchedule schedule = getOrCreateSchedule(projectId);
        List<WbsNode> nodes = wbsNodeRepository.findByProjectIdOrderBySortOrderAsc(projectId);

        int count = 0;
        for (WbsNode node : nodes) {
            if (taskRepository.findByScheduleIdAndWbsNodeId(schedule.getId(), node.getId()).isEmpty()) {
                int duration = 5;
                if (node.getStartDate() != null && node.getEndDate() != null) {
                    duration = (int) Math.max(1, ChronoUnit.DAYS.between(node.getStartDate(), node.getEndDate()) + 1);
                }
                ProjectScheduleTask task = new ProjectScheduleTask(
                        schedule.getId(),
                        node.getId(),
                        null,
                        node.getWbsCode(),
                        node.getName(),
                        node.getNameEn(),
                        duration,
                        node.getStartDate(),
                        node.getEndDate(),
                        node.getNodeType() == WbsNodeType.MILESTONE,
                        TaskConstraintType.ASAP,
                        null,
                        node.getSortOrder()
                );
                taskRepository.save(task);
                count++;
            }
        }

        recalculateCpmInternal(schedule);
        auditService.record("SCHEDULE_WBS_IMPORT", "PROJECT_SCHEDULE", schedule.getId(), userId,
                "Imported " + count + " tasks from WBS hierarchy", null);
    }

    public void syncProgressFromDpr(String projectId, String userId) {
        ProjectSchedule schedule = getOrCreateSchedule(projectId);
        List<ProjectScheduleTask> tasks = taskRepository.findByScheduleIdOrderBySortOrderAsc(schedule.getId());

        for (ProjectScheduleTask task : tasks) {
            if (task.getWbsNodeId() != null) {
                wbsNodeRepository.findById(task.getWbsNodeId()).ifPresent(node -> {
                    BigDecimal planned = node.getPlannedQuantity();
                    if (planned != null && planned.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal approvedQty = progressLineRepository.sumApprovedQuantityUpToDate(
                                projectId, node.getId(), LocalDate.now());
                        BigDecimal percent = approvedQty.multiply(BigDecimal.valueOf(100))
                                .divide(planned, 2, RoundingMode.HALF_UP)
                                .min(BigDecimal.valueOf(100));
                        task.updatePercentComplete(percent);
                        taskRepository.save(task);
                    }
                });
            }
        }

        auditService.record("SCHEDULE_DPR_SYNC", "PROJECT_SCHEDULE", schedule.getId(), userId,
                "Synced task progress from approved DPR reports", null);
    }

    @Transactional(readOnly = true)
    public List<ResourceOverAllocationResponse> detectOverAllocations(String projectId) {
        ProjectSchedule schedule = getOrCreateSchedule(projectId);
        List<ProjectScheduleTask> tasks = taskRepository.findByScheduleIdOrderBySortOrderAsc(schedule.getId());
        Map<String, ProjectScheduleTask> taskMap = tasks.stream()
                .collect(Collectors.toMap(ProjectScheduleTask::getId, t -> t));

        List<TaskResourceAssignment> assignments = tasks.stream()
                .flatMap(t -> resourceAssignmentRepository.findByTaskId(t.getId()).stream())
                .toList();

        // Group by resource (type + name)
        Map<String, List<TaskResourceAssignment>> byResource = assignments.stream()
                .collect(Collectors.groupingBy(a -> a.getResourceType() + "::" + a.getResourceName()));

        List<ResourceOverAllocationResponse> overAllocations = new ArrayList<>();
        BigDecimal capacityLimit = BigDecimal.valueOf(8.0); // 8 hours or 1.0 unit per day capacity

        for (Map.Entry<String, List<TaskResourceAssignment>> entry : byResource.entrySet()) {
            List<TaskResourceAssignment> resAssigns = entry.getValue();
            TaskResourceType type = resAssigns.get(0).getResourceType();
            String name = resAssigns.get(0).getResourceName();

            // Find date range
            LocalDate minDate = null;
            LocalDate maxDate = null;
            for (TaskResourceAssignment a : resAssigns) {
                ProjectScheduleTask task = taskMap.get(a.getTaskId());
                LocalDate start = a.getStartDate() != null ? a.getStartDate() :
                        (task != null ? task.getEarlyStartDate() : null);
                LocalDate end = a.getEndDate() != null ? a.getEndDate() :
                        (task != null ? task.getEarlyEndDate() : null);
                if (start != null) minDate = (minDate == null || start.isBefore(minDate)) ? start : minDate;
                if (end != null) maxDate = (maxDate == null || end.isAfter(maxDate)) ? end : maxDate;
            }

            if (minDate != null && maxDate != null) {
                for (LocalDate d = minDate; !d.isAfter(maxDate); d = d.plusDays(1)) {
                    BigDecimal dailyTotal = BigDecimal.ZERO;
                    List<String> affectedTasks = new ArrayList<>();
                    for (TaskResourceAssignment a : resAssigns) {
                        ProjectScheduleTask task = taskMap.get(a.getTaskId());
                        LocalDate start = a.getStartDate() != null ? a.getStartDate() :
                                (task != null ? task.getEarlyStartDate() : null);
                        LocalDate end = a.getEndDate() != null ? a.getEndDate() :
                                (task != null ? task.getEarlyEndDate() : null);
                        if (start != null && end != null && !d.isBefore(start) && !d.isAfter(end)) {
                            dailyTotal = dailyTotal.add(a.getQuantityAllocated());
                            if (task != null) affectedTasks.add(task.getTaskCode());
                        }
                    }
                    if (dailyTotal.compareTo(capacityLimit) > 0) {
                        overAllocations.add(new ResourceOverAllocationResponse(
                                type,
                                name,
                                localDateToEpoch(d),
                                dailyTotal,
                                capacityLimit,
                                dailyTotal.subtract(capacityLimit),
                                affectedTasks
                        ));
                    }
                }
            }
        }
        return overAllocations;
    }

    // ─── CPM Scheduling Engine (Deterministic Server-Side Calculation) ─────

    private void recalculateCpmInternal(ProjectSchedule schedule) {
        List<ProjectScheduleTask> tasks = taskRepository.findByScheduleIdOrderBySortOrderAsc(schedule.getId());
        if (tasks.isEmpty()) return;

        List<TaskDependency> dependencies = dependencyRepository.findByScheduleId(schedule.getId());
        Map<String, ProjectScheduleTask> taskMap = tasks.stream()
                .collect(Collectors.toMap(ProjectScheduleTask::getId, t -> t));

        // 1. Build Adjacency Graph & In-Degrees
        Map<String, List<TaskDependency>> outEdges = new HashMap<>();
        Map<String, List<TaskDependency>> inEdges = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();

        for (ProjectScheduleTask t : tasks) {
            outEdges.put(t.getId(), new ArrayList<>());
            inEdges.put(t.getId(), new ArrayList<>());
            inDegree.put(t.getId(), 0);
        }

        for (TaskDependency dep : dependencies) {
            if (taskMap.containsKey(dep.getPredecessorTaskId()) && taskMap.containsKey(dep.getSuccessorTaskId())) {
                outEdges.get(dep.getPredecessorTaskId()).add(dep);
                inEdges.get(dep.getSuccessorTaskId()).add(dep);
                inDegree.put(dep.getSuccessorTaskId(), inDegree.get(dep.getSuccessorTaskId()) + 1);
            }
        }

        // 2. Kahn's Topological Sort (with Cycle Detection)
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<String> sortedTaskIds = new ArrayList<>();
        while (!queue.isEmpty()) {
            String u = queue.poll();
            sortedTaskIds.add(u);
            for (TaskDependency edge : outEdges.get(u)) {
                String v = edge.getSuccessorTaskId();
                inDegree.put(v, inDegree.get(v) - 1);
                if (inDegree.get(v) == 0) {
                    queue.add(v);
                }
            }
        }

        if (sortedTaskIds.size() != tasks.size()) {
            throw new BusinessRuleException("SCHEDULE_CYCLE_DETECTED");
        }

        LocalDate scheduleStart = schedule.getStartDate() != null ? schedule.getStartDate() : LocalDate.now();

        // 3. Forward Pass (Early Dates)
        Map<String, LocalDate> earlyStarts = new HashMap<>();
        Map<String, LocalDate> earlyEnds = new HashMap<>();

        for (String taskId : sortedTaskIds) {
            ProjectScheduleTask t = taskMap.get(taskId);
            LocalDate es = t.getPlannedStartDate() != null ? t.getPlannedStartDate() : scheduleStart;

            for (TaskDependency inDep : inEdges.get(taskId)) {
                LocalDate predEs = earlyStarts.get(inDep.getPredecessorTaskId());
                LocalDate predEf = earlyEnds.get(inDep.getPredecessorTaskId());
                if (predEf == null) predEf = predEs;

                LocalDate candidateStart = switch (inDep.getDependencyType()) {
                    case FS -> predEf.plusDays(1 + inDep.getLagDays());
                    case SS -> predEs.plusDays(inDep.getLagDays());
                    case FF -> predEf.plusDays(inDep.getLagDays()).minusDays(Math.max(1, t.getDurationDays()) - 1);
                    case SF -> predEs.plusDays(inDep.getLagDays()).minusDays(Math.max(1, t.getDurationDays()) - 1);
                };

                if (candidateStart.isAfter(es)) {
                    es = candidateStart;
                }
            }

            if (t.getConstraintType() == TaskConstraintType.START_NO_EARLIER_THAN ||
                t.getConstraintType() == TaskConstraintType.MUST_START_ON) {
                if (t.getConstraintDate() != null && t.getConstraintDate().isAfter(es)) {
                    es = t.getConstraintDate();
                }
            }

            LocalDate ef = t.isMilestone() ? es : es.plusDays(Math.max(1, t.getDurationDays()) - 1);
            earlyStarts.put(taskId, es);
            earlyEnds.put(taskId, ef);
        }

        // Project Finish Date
        LocalDate projectFinish = earlyEnds.values().stream().max(LocalDate::compareTo).orElse(scheduleStart);
        schedule.setDates(scheduleStart, projectFinish);
        scheduleRepository.save(schedule);

        // 4. Backward Pass (Late Dates & Floats)
        Map<String, LocalDate> lateStarts = new HashMap<>();
        Map<String, LocalDate> lateEnds = new HashMap<>();

        List<String> reverseSorted = new ArrayList<>(sortedTaskIds);
        Collections.reverse(reverseSorted);

        for (String taskId : reverseSorted) {
            ProjectScheduleTask t = taskMap.get(taskId);
            LocalDate lf = projectFinish;

            for (TaskDependency outDep : outEdges.get(taskId)) {
                LocalDate succLs = lateStarts.get(outDep.getSuccessorTaskId());
                LocalDate succLf = lateEnds.get(outDep.getSuccessorTaskId());
                if (succLs == null) succLs = succLf;

                LocalDate candidateFinish = switch (outDep.getDependencyType()) {
                    case FS -> succLs.minusDays(1 + outDep.getLagDays());
                    case SS -> succLs.minusDays(outDep.getLagDays()).plusDays(Math.max(1, t.getDurationDays()) - 1);
                    case FF -> succLf.minusDays(outDep.getLagDays());
                    case SF -> succLf.minusDays(outDep.getLagDays()).plusDays(Math.max(1, t.getDurationDays()) - 1);
                };

                if (candidateFinish.isBefore(lf)) {
                    lf = candidateFinish;
                }
            }

            if (t.getConstraintType() == TaskConstraintType.FINISH_NO_LATER_THAN ||
                t.getConstraintType() == TaskConstraintType.MUST_FINISH_ON) {
                if (t.getConstraintDate() != null && t.getConstraintDate().isBefore(lf)) {
                    lf = t.getConstraintDate();
                }
            }

            LocalDate ls = t.isMilestone() ? lf : lf.minusDays(Math.max(1, t.getDurationDays()) - 1);
            lateStarts.put(taskId, ls);
            lateEnds.put(taskId, lf);
        }

        // 5. Update All Tasks with CPM Calculated Values
        for (ProjectScheduleTask t : tasks) {
            LocalDate es = earlyStarts.get(t.getId());
            LocalDate ef = earlyEnds.get(t.getId());
            LocalDate ls = lateStarts.get(t.getId());
            LocalDate lf = lateEnds.get(t.getId());

            int totalFloat = (int) ChronoUnit.DAYS.between(es, ls);
            int freeFloat = totalFloat;

            for (TaskDependency outDep : outEdges.get(t.getId())) {
                LocalDate succEs = earlyStarts.get(outDep.getSuccessorTaskId());
                int ffCandidate = (int) ChronoUnit.DAYS.between(ef.plusDays(1 + outDep.getLagDays()), succEs);
                if (ffCandidate < freeFloat) {
                    freeFloat = Math.max(0, ffCandidate);
                }
            }

            boolean critical = (totalFloat <= 0);
            t.setCpmCalculations(es, ef, ls, lf, Math.max(0, freeFloat), totalFloat, critical);
            taskRepository.save(t);
        }
    }

    // ─── Mapping Helpers ─────────────────────────────────────────────

    private ProjectScheduleTaskResponse mapTaskResponse(ProjectScheduleTask t, List<TaskResourceAssignmentResponse> resources) {
        return new ProjectScheduleTaskResponse(
                t.getId(),
                t.getScheduleId(),
                t.getWbsNodeId(),
                t.getParentTaskId(),
                t.getTaskCode(),
                t.getName(),
                t.getNameEn(),
                t.getDurationDays(),
                localDateToEpoch(t.getPlannedStartDate()),
                localDateToEpoch(t.getPlannedEndDate()),
                localDateToEpoch(t.getEarlyStartDate()),
                localDateToEpoch(t.getEarlyEndDate()),
                localDateToEpoch(t.getLateStartDate()),
                localDateToEpoch(t.getLateEndDate()),
                t.getFreeFloatDays(),
                t.getTotalFloatDays(),
                t.isCritical(),
                t.getPercentComplete(),
                t.isMilestone(),
                t.getConstraintType(),
                localDateToEpoch(t.getConstraintDate()),
                t.getSortOrder(),
                resources
        );
    }

    private TaskDependencyResponse mapDependencyResponse(TaskDependency d) {
        return new TaskDependencyResponse(
                d.getId(),
                d.getScheduleId(),
                d.getPredecessorTaskId(),
                d.getSuccessorTaskId(),
                d.getDependencyType(),
                d.getLagDays(),
                d.getCreatedAt()
        );
    }

    private ScheduleBaselineResponse mapBaselineResponse(ScheduleBaseline b, int taskCount) {
        return new ScheduleBaselineResponse(
                b.getId(),
                b.getScheduleId(),
                b.getVersionNumber(),
                b.getName(),
                b.getApprovedBy(),
                b.getApprovedAt(),
                b.getNotes(),
                taskCount
        );
    }

    private TaskResourceAssignmentResponse mapResourceResponse(TaskResourceAssignment r) {
        return new TaskResourceAssignmentResponse(
                r.getId(),
                r.getTaskId(),
                r.getResourceType(),
                r.getResourceName(),
                r.getPartyId(),
                r.getEmployeeId(),
                r.getQuantityAllocated(),
                localDateToEpoch(r.getStartDate()),
                localDateToEpoch(r.getEndDate()),
                r.getNotes()
        );
    }

    private Project requireProject(String projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("PROJECT_NOT_FOUND"));
    }

    private ProjectScheduleTask requireTask(String taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("SCHEDULE_TASK_NOT_FOUND"));
    }

    private static LocalDate epochToLocalDate(Long epoch) {
        if (epoch == null) return null;
        return Instant.ofEpochMilli(epoch).atZone(ZoneOffset.UTC).toLocalDate();
    }

    private static Long localDateToEpoch(LocalDate date) {
        if (date == null) return null;
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }
}
