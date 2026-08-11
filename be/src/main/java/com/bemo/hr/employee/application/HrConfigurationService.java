package com.bemo.hr.employee.application;

import com.bemo.hr.employee.api.CategoryApi;
import com.bemo.hr.employee.api.EmployeeApi;
import com.bemo.hr.employee.domain.AttendanceCategory;
import com.bemo.hr.employee.domain.CategoryScope;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.domain.EmployeeAssignment;
import com.bemo.hr.employee.domain.ScheduleRule;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeAssignmentRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.employee.infrastructure.EmployeeCodeSequenceRepository;
import com.bemo.hr.employee.infrastructure.ScheduleRuleRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.AppUserRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class HrConfigurationService {
    private final AttendanceCategoryRepository attendanceCategoryRepository;
    private final ScheduleRuleRepository scheduleRuleRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeCodeSequenceRepository employeeCodeSequenceRepository;
    private final EmployeeAssignmentRepository employeeAssignmentRepository;
    private final AppUserRepository appUserRepository;
    private final com.bemo.hr.audit.application.AuditService auditService;

    public HrConfigurationService(AttendanceCategoryRepository attendanceCategoryRepository,
                                  ScheduleRuleRepository scheduleRuleRepository,
                                  EmployeeRepository employeeRepository,
                                  EmployeeCodeSequenceRepository employeeCodeSequenceRepository,
                                  EmployeeAssignmentRepository employeeAssignmentRepository,
                                  AppUserRepository appUserRepository,
                                  com.bemo.hr.audit.application.AuditService auditService) {
        this.attendanceCategoryRepository = attendanceCategoryRepository;
        this.scheduleRuleRepository = scheduleRuleRepository;
        this.employeeRepository = employeeRepository;
        this.employeeCodeSequenceRepository = employeeCodeSequenceRepository;
        this.employeeAssignmentRepository = employeeAssignmentRepository;
        this.appUserRepository = appUserRepository;
        this.auditService = auditService;
    }

    private static final List<CategoryScope> EMPLOYEE_SCOPES = List.of(CategoryScope.EMPLOYEE, CategoryScope.BOTH);

    public List<CategoryApi.Response> listCategories() {
        return attendanceCategoryRepository.findByScopeInOrderByNameAsc(EMPLOYEE_SCOPES)
                .stream().map(this::toCategoryResponse).toList();
    }

    public CategoryApi.Response getCategory(String id) { return toCategoryResponse(requireCategory(id)); }

    public List<CategoryApi.ScheduleResponse> getScheduleHistory(String categoryId) {
        requireCategory(categoryId);
        return scheduleRuleRepository.findByCategoryIdOrderByEffectiveFromAsc(categoryId).stream()
                .map(rule -> new CategoryApi.ScheduleResponse(rule.getId(), rule.getName(), rule.getEffectiveFrom(),
                        rule.getEffectiveTo(), rule.getStartTime(), rule.getExpectedMinutesOverride(), rule.getGraceMinutes(),
                        rule.getEndTime(), rule.getScope(), rule.getScopeCategoryId()))
                .toList();
    }

    @Transactional
    public CategoryApi.Response createCategory(CategoryApi.UpsertRequest request) {
        validateCategoryRequest(request);
        if (attendanceCategoryRepository.existsByCodeIgnoreCase(request.code())) {
            throw new BusinessRuleException("Category code already exists.",
                    "HRCFG_CATEGORY_CODE_EXISTS", HttpStatus.CONFLICT);
        }
        var category = new AttendanceCategory(request.code(), request.name(), request.expectedDailyMinutes(),
                request.payCycle(), request.attendanceMode(), request.singlePunchCounts(), toMask(request.workDays()), request.active(),
                request.scope() == null ? CategoryScope.EMPLOYEE : request.scope());
        category.configureAdvanceEligibility(request.allowsEmployeeAdvances());
        attendanceCategoryRepository.save(category);
        employeeCodeSequenceRepository.save(new com.bemo.hr.employee.domain.EmployeeCodeSequence(category.getId()));
        replaceSchedules(category.getId(), request.schedules());
        return toCategoryResponse(category);
    }

    @Transactional
    public CategoryApi.Response updateCategory(String id, CategoryApi.UpsertRequest request) {
        validateCategoryRequest(request);
        var category = requireCategory(id);
        requireVersion(category.getVersion(), request.version());
        if (attendanceCategoryRepository.existsByCodeIgnoreCaseAndIdNot(request.code(), id)) {
            throw new BusinessRuleException("Category code already exists.",
                    "HRCFG_CATEGORY_CODE_EXISTS", HttpStatus.CONFLICT);
        }
        category.update(request.code(), request.name(), request.expectedDailyMinutes(), request.payCycle(), request.attendanceMode(),
                request.singlePunchCounts(), toMask(request.workDays()), request.active());
        category.updateScope(request.scope());
        category.configureAdvanceEligibility(request.allowsEmployeeAdvances());
        replaceSchedules(id, request.schedules());
        return toCategoryResponse(category);
    }

    @Transactional
    public void deactivateCategory(String id) {
        var category = requireCategory(id);
        if (employeeRepository.existsByCategoryIdAndActiveTrue(id)) {
            throw new BusinessRuleException("Deactivate or move active employees before deactivating this category.",
                    "HRCFG_CATEGORY_HAS_ACTIVE_EMPLOYEES", HttpStatus.CONFLICT);
        }
        category.update(category.getCode(), category.getName(), category.getExpectedDailyMinutes(),
                category.getPayCycle(), category.getAttendanceMode(), category.isSinglePunchCounts(), category.getWorkDaysMask(), false);
    }

    public List<EmployeeApi.Response> listEmployees() {
        var categories = attendanceCategoryRepository.findAll().stream()
                .collect(Collectors.toMap(AttendanceCategory::getId, Function.identity()));
        return employeeRepository.findAllByOrderByFullNameAsc().stream()
                .map(employee -> toEmployeeResponse(employee, categories.get(employee.getCategoryId())))
                .toList();
    }

    @Transactional
    public EmployeeApi.Response createEmployee(EmployeeApi.UpsertRequest request) {
        validateEmployeeRequest(request, null);
        var category = requireCategory(request.categoryId());
        var employeeCode = standardizeEmployeeCode(request.employeeCode(), category, true, null);
        var employee = new Employee(employeeCode, request.fullName(), request.deviceUserId(),
                request.categoryId(), request.employmentType(), request.baseSalary(), request.activeFrom(), request.activeTo(), request.active());
        employeeRepository.save(employee);
        employeeAssignmentRepository.save(new EmployeeAssignment(employee.getId(), employee.getCategoryId(),
                employee.getActiveFrom(), employee.getActiveTo()));
        auditService.record("CREATE", "EMPLOYEE", employee.getId(), currentActor(),
                "{\"employeeCode\":\"" + safeJson(employeeCode) + "\",\"fullName\":\"" + safeJson(employee.getFullName())
                        + "\",\"categoryId\":\"" + category.getId() + "\"}", null);
        return toEmployeeResponse(employee, category);
    }

    @Transactional
    public EmployeeApi.Response updateEmployee(String id, EmployeeApi.UpsertRequest request) {
        var employee = employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Employee not found.", "HRCFG_EMPLOYEE_NOT_FOUND"));
        requireVersion(employee.getVersion(), request.version());
        validateEmployeeRequest(request, id);
        var category = requireCategory(request.categoryId());
        var employeeCode = standardizeEmployeeCode(request.employeeCode(), category, false, employee.getEmployeeCode());
        boolean assignmentChanged = !request.categoryId().equals(employee.getCategoryId())
                || !request.activeFrom().equals(employee.getActiveFrom())
                || !java.util.Objects.equals(request.activeTo(), employee.getActiveTo());
        employee.update(employeeCode, request.fullName(), request.deviceUserId(), request.categoryId(),
                request.employmentType(), request.baseSalary(), request.activeFrom(), request.activeTo(), request.active());
        if (assignmentChanged) recordAssignmentChange(employee, employee.getActiveFrom());
        return toEmployeeResponse(employee, category);
    }

    @Transactional
    public void deactivateEmployee(String id) {
        var employee = employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Employee not found.", "HRCFG_EMPLOYEE_NOT_FOUND"));
        employee.update(employee.getEmployeeCode(), employee.getFullName(), employee.getDeviceUserId(),
                employee.getCategoryId(), employee.getEmploymentType(), employee.getBaseSalary(), employee.getActiveFrom(),
                employee.getActiveTo(), false);
        closeOpenAssignment(employee.getId(), employee.getActiveTo() != null ? employee.getActiveTo() : LocalDate.now());
    }

    public List<EmployeeApi.AssignmentResponse> getEmployeeAssignments(String id) {
        employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Employee not found.", "HRCFG_EMPLOYEE_NOT_FOUND"));
        var categories = attendanceCategoryRepository.findAll().stream()
                .collect(Collectors.toMap(AttendanceCategory::getId, Function.identity()));
        return employeeAssignmentRepository.findByEmployeeIdOrderByEffectiveFromDesc(id).stream()
                .map(assignment -> new EmployeeApi.AssignmentResponse(assignment.getEmployeeId(),
                        assignment.getCategoryId(), categoryName(categories.get(assignment.getCategoryId())),
                        assignment.getEffectiveFrom(), assignment.getEffectiveTo(), assignment.getCreatedAt()))
                .toList();
    }

    private String categoryName(AttendanceCategory category) { return category == null ? "—" : category.getName(); }

    private void recordAssignmentChange(Employee employee, LocalDate newEffectiveFrom) {
        var open = employeeAssignmentRepository.findFirstByEmployeeIdAndEffectiveToIsNullOrderByEffectiveFromDesc(employee.getId());
        if (open != null) {
            var closeOn = newEffectiveFrom.minusDays(1);
            if (closeOn.isBefore(open.getEffectiveFrom())) closeOn = open.getEffectiveFrom();
            open.closeOn(closeOn);
        }
        employeeAssignmentRepository.save(new EmployeeAssignment(employee.getId(), employee.getCategoryId(),
                employee.getActiveFrom(), employee.getActiveTo()));
    }

    private void closeOpenAssignment(String employeeId, LocalDate effectiveTo) {
        var open = employeeAssignmentRepository.findFirstByEmployeeIdAndEffectiveToIsNullOrderByEffectiveFromDesc(employeeId);
        if (open != null && effectiveTo != null && !effectiveTo.isBefore(open.getEffectiveFrom())) open.closeOn(effectiveTo);
    }

    private AttendanceCategory requireCategory(String id) {
        return attendanceCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Attendance category not found.", "HRCFG_CATEGORY_NOT_FOUND"));
    }

    private void validateCategoryRequest(CategoryApi.UpsertRequest request) {
        if (request.workDays().stream().anyMatch(java.util.Objects::isNull)) {
            throw new BusinessRuleException("Work days cannot contain an empty value.",
                    "HRCFG_WORK_DAYS_EMPTY", HttpStatus.CONFLICT);
        }
        validateScheduleRanges(request.schedules());
    }

    static void validateScheduleRanges(List<CategoryApi.ScheduleRequest> schedules) {
        var order = java.util.stream.IntStream.range(0, schedules.size()).boxed()
                .sorted(Comparator.comparing(index -> schedules.get(index).effectiveFrom())).toList();
        CategoryApi.ScheduleRequest covering = null;
        int coveringOriginalIndex = -1;
        for (int originalIndex : order) {
            var current = schedules.get(originalIndex);
            if (current.effectiveTo() != null && current.effectiveTo().isBefore(current.effectiveFrom())) {
                throw new BusinessRuleException("Schedule end date cannot be before its start date.",
                        "HRCFG_SCHEDULE_END_BEFORE_START", HttpStatus.CONFLICT);
            }
            if (covering != null && (covering.effectiveTo() == null
                    || !covering.effectiveTo().isBefore(current.effectiveFrom()))) {
                throw new BusinessRuleException("Schedule effective date ranges cannot overlap.",
                        "SCHEDULE_RULE_OVERLAP", HttpStatus.UNPROCESSABLE_ENTITY,
                        List.of("schedules[" + coveringOriginalIndex + "]", "schedules[" + originalIndex + "]"));
            }
            if (covering == null || (covering.effectiveTo() != null
                    && (current.effectiveTo() == null || current.effectiveTo().isAfter(covering.effectiveTo())))) {
                covering = current;
                coveringOriginalIndex = originalIndex;
            }
        }
    }

    private void validateEmployeeRequest(EmployeeApi.UpsertRequest request, String currentId) {
        if (request.activeTo() != null && request.activeTo().isBefore(request.activeFrom())) {
            throw new BusinessRuleException("Employee active-to date cannot be before active-from date.",
                    "HRCFG_EMPLOYEE_DATES_INVALID", HttpStatus.CONFLICT);
        }
        var category = attendanceCategoryRepository.findById(request.categoryId()).orElse(null);
        if (category != null && category.getAttendanceMode() == com.bemo.hr.employee.domain.AttendanceMode.BIOMETRIC) {
            if (request.active() && (request.deviceUserId() == null || request.deviceUserId().isBlank())) {
                throw new BusinessRuleException("A unique biometric device ID is required for active employees in biometric categories.",
                        "HRCFG_BIOMETRIC_ID_REQUIRED", HttpStatus.CONFLICT);
            }
        }
        if (request.deviceUserId() != null && !request.deviceUserId().isBlank()) {
            boolean duplicateDeviceId = currentId == null
                    ? employeeRepository.existsByDeviceUserId(request.deviceUserId().strip())
                    : employeeRepository.existsByDeviceUserIdAndIdNot(request.deviceUserId().strip(), currentId);
            if (duplicateDeviceId) {
                throw new BusinessRuleException("Device user id is already mapped to another employee.",
                        "HRCFG_DEVICE_USER_ALREADY_MAPPED", HttpStatus.CONFLICT);
            }
        }
    }

    private String standardizeEmployeeCode(String requested, AttendanceCategory category, boolean creating, String currentCode) {
        if (!creating && (requested == null || requested.isBlank())) return currentCode;
        String prefix = category.getCode() + "-";
        if (requested != null && !requested.isBlank()) {
            // A user-supplied code is a complete business identifier. Normalize it,
            // but never prepend the category code again (BUG-006).
            String code = requested.strip().toUpperCase(java.util.Locale.ROOT);
            boolean duplicate = creating ? employeeRepository.existsByEmployeeCodeIgnoreCase(code)
                    : employeeRepository.existsByEmployeeCodeIgnoreCaseAndIdNot(
                            code,
                            employeeRepository.findByEmployeeCodeIgnoreCase(currentCode)
                                    .map(Employee::getId).orElse(""));
            if (duplicate) {
                throw new BusinessRuleException("Employee code already exists.",
                        "HRCFG_EMPLOYEE_CODE_EXISTS", HttpStatus.CONFLICT);
            }
            return code;
        }

        var sequence = employeeCodeSequenceRepository.findForUpdate(category.getId())
                .orElseGet(() -> employeeCodeSequenceRepository.save(
                        new com.bemo.hr.employee.domain.EmployeeCodeSequence(category.getId())));
        String generated;
        do { generated = prefix + "%04d".formatted(sequence.takeNext()); }
        while (employeeRepository.existsByEmployeeCodeIgnoreCase(generated));
        return generated;
    }

    private void replaceSchedules(String categoryId, List<CategoryApi.ScheduleRequest> requests) {
        scheduleRuleRepository.deleteByCategoryId(categoryId);
        scheduleRuleRepository.flush();
        var schedules = requests.stream()
                .map(request -> new ScheduleRule(categoryId, request.name(), request.effectiveFrom(),
                        request.effectiveTo(), request.startTime(), request.expectedMinutesOverride(), request.graceMinutes(),
                        request.endTime(), request.scope(), request.scopeCategoryId()))
                .toList();
        scheduleRuleRepository.saveAll(schedules);
    }

    private CategoryApi.Response toCategoryResponse(AttendanceCategory category) {
        var schedules = scheduleRuleRepository.findByCategoryIdOrderByEffectiveFromAsc(category.getId()).stream()
                .map(rule -> new CategoryApi.ScheduleResponse(rule.getId(), rule.getName(), rule.getEffectiveFrom(),
                        rule.getEffectiveTo(), rule.getStartTime(), rule.getExpectedMinutesOverride(), rule.getGraceMinutes(),
                        rule.getEndTime(), rule.getScope(), rule.getScopeCategoryId()))
                .toList();
        return new CategoryApi.Response(category.getId(), category.getCode(), category.getName(),
                category.getScope(), category.getExpectedDailyMinutes(), category.getPayCycle(), category.getAttendanceMode(),
                category.isSinglePunchCounts(), category.isAllowsEmployeeAdvances(),
                fromMask(category.getWorkDaysMask()), category.isActive(), category.getVersion(),
                category.getCreatedAt(), category.getUpdatedAt(), schedules);
    }

    private EmployeeApi.Response toEmployeeResponse(Employee employee, AttendanceCategory category) {
        boolean canViewSalary = currentUserCanViewSalary();
        return new EmployeeApi.Response(employee.getId(), employee.getEmployeeCode(), employee.getFullName(),
                employee.getDeviceUserId(), employee.getCategoryId(), category == null ? "—" : category.getName(),
                employee.getEmploymentType(), canViewSalary ? employee.getBaseSalary() : null,
                employee.getActiveFrom(), employee.getActiveTo(), employee.isActive(), employee.getVersion());
    }

    private boolean currentUserCanViewSalary() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) return true;
        return appUserRepository.findByAppIdAndUsernameIgnoreCase(TenantContext.require(), authentication.getName())
                .map(com.bemo.hr.shared.security.AppUser::isCanViewSalary).orElse(true);
    }

    private int toMask(Set<DayOfWeek> days) {
        return days.stream().mapToInt(day -> 1 << (day.getValue() - 1)).reduce(0, (left, right) -> left | right);
    }

    private Set<DayOfWeek> fromMask(int mask) {
        var days = EnumSet.noneOf(DayOfWeek.class);
        for (var day : DayOfWeek.values()) {
            if ((mask & (1 << (day.getValue() - 1))) != 0) days.add(day);
        }
        return days;
    }

    private void requireVersion(long actual, Long requested) {
        if (requested == null || requested != actual) {
            throw new BusinessRuleException("This record changed since it was loaded. Refresh and try again.",
                    "HRCFG_VERSION_CONFLICT", HttpStatus.CONFLICT);
        }
    }

    private String currentActor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null && !auth.getName().isBlank()) ? auth.getName() : "system";
    }

    private String safeJson(String value) { return value == null ? "" : value.replace("\"", "'"); }
}
