package com.bemo.hr.attendance.application;

import com.bemo.hr.attendance.domain.BiometricSource;
import com.bemo.hr.attendance.infrastructure.PunchRecordRepository;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.employee.domain.*;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeCodeSequenceRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.employee.infrastructure.ScheduleRuleRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BiometricEmployeeProvisioningService {
    private static final List<CategoryScope> EMPLOYEE_SCOPES =
            List.of(CategoryScope.EMPLOYEE, CategoryScope.BOTH);
    private static final int SATURDAY_TO_THURSDAY_MASK = 111;
    private static final String AUTO_CATEGORY_CODE = "BIO_AUTO";

    private final EmployeeRepository employeeRepository;
    private final AttendanceCategoryRepository attendanceCategoryRepository;
    private final EmployeeCodeSequenceRepository employeeCodeSequenceRepository;
    private final ScheduleRuleRepository scheduleRuleRepository;
    private final PunchRecordRepository punchRecordRepository;
    private final AuditService auditService;

    /**
     * Enforces the source's category rule:
     * - if at least one active employee category exists, the user must explicitly select one;
     * - if no active employee category exists at all, create a safe default biometric employee
     * category and use it automatically.
     */
    @Transactional
    public void configureSource(BiometricSource source, boolean enabled, String categoryId, String employmentType,
                                String activeFromMode, boolean employeeActive) {
        log.debug("configureSource called with sourceId={}, enabled={}, categoryId={}", source.getId(), enabled, categoryId);
        String normalizedType = normalizeEmploymentType(employmentType);
        String normalizedActiveFromMode = normalizeActiveFromMode(activeFromMode);
        String resolvedCategoryId = null;
        if (enabled) {
            if (categoryId == null || categoryId.isBlank()) {
                resolvedCategoryId = createDefaultCategoryOnlyWhenNoneExist();
            } else {
                resolvedCategoryId = validateSelectedCategory(categoryId).getId();
            }
        }
        source.configureAutoEmployeeCreation(
                enabled, resolvedCategoryId, normalizedType, normalizedActiveFromMode, employeeActive);
    }

    private AttendanceCategory validateSelectedCategory(String categoryId) {
        var category = attendanceCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessRuleException(
                        "The selected employee attendance category does not exist.",
                        "BIO_AUTO_EMPLOYEE_CATEGORY_NOT_FOUND", HttpStatus.CONFLICT));
        if (!category.isActive()) {
            throw new BusinessRuleException(
                    "The selected employee attendance category is inactive.",
                    "BIO_AUTO_EMPLOYEE_CATEGORY_INACTIVE", HttpStatus.CONFLICT);
        }
        if (category.getScope() == CategoryScope.WORKER) {
            throw new BusinessRuleException(
                    "The selected category is worker-only and cannot be assigned to employees.",
                    "BIO_AUTO_EMPLOYEE_CATEGORY_SCOPE", HttpStatus.CONFLICT);
        }
        return category;
    }

    private String createDefaultCategoryOnlyWhenNoneExist() {
        var activeEmployeeCategories = attendanceCategoryRepository
                .findByScopeInOrderByNameAsc(EMPLOYEE_SCOPES)
                .stream()
                .filter(AttendanceCategory::isActive)
                .toList();
        if (!activeEmployeeCategories.isEmpty()) {
            throw new BusinessRuleException(
                    "Select an employee attendance category before enabling automatic employee creation.",
                    "BIO_AUTO_EMPLOYEE_CATEGORY_REQUIRED", HttpStatus.CONFLICT);
        }

        String code = nextAvailableAutoCategoryCode();
        var category = new AttendanceCategory(
                code,
                "Biometric Employees - Auto",
                480,
                PayCycle.MONTHLY,
                AttendanceMode.BIOMETRIC,
                false,
                SATURDAY_TO_THURSDAY_MASK,
                true,
                CategoryScope.EMPLOYEE);
        category.configureAdvanceEligibility(false);
        category = attendanceCategoryRepository.saveAndFlush(category);
        employeeCodeSequenceRepository.save(new EmployeeCodeSequence(category.getId()));
        scheduleRuleRepository.save(new ScheduleRule(
                category.getId(),
                "Default Schedule",
                LocalDate.of(2000, 1, 1),
                null,
                LocalTime.of(8, 0),
                null,
                15,
                LocalTime.of(16, 0),
                "ALL",
                null));
        auditService.record(
                "AUTO_CREATE",
                "ATTENDANCE_CATEGORY",
                category.getId(),
                "biometric-import",
                "{\"code\":\"" + safe(category.getCode()) + "\",\"reason\":\"no-employee-category-exists\"}",
                null);
        return category.getId();
    }

    private String nextAvailableAutoCategoryCode() {
        if (!attendanceCategoryRepository.existsByCodeIgnoreCase(AUTO_CATEGORY_CODE)) {
            return AUTO_CATEGORY_CODE;
        }
        for (int suffix = 2; suffix <= 999; suffix++) {
            String candidate = AUTO_CATEGORY_CODE + "_" + suffix;
            if (!attendanceCategoryRepository.existsByCodeIgnoreCase(candidate)) {
                return candidate;
            }
        }
        throw new BusinessRuleException(
                "Could not allocate a default biometric employee category code.",
                "BIO_AUTO_EMPLOYEE_CATEGORY_CODE_EXHAUSTED", HttpStatus.CONFLICT);
    }

    @Transactional
    public String resolveEmployeeId(BiometricSource source, String deviceUserId, String observedName,
                                    Instant observedAt, String actor) {
        log.debug("resolveEmployeeId called with deviceUserId={}, sourceId={}", deviceUserId, source.getId());
        if (deviceUserId == null || deviceUserId.isBlank()) return null;
        String normalizedDeviceUserId = deviceUserId.strip();
        String appId = TenantContext.require();

        var existing = employeeRepository.findByEmployeeCodeIgnoreCase(normalizedDeviceUserId)
                .or(() -> employeeRepository.findByDeviceUserId(normalizedDeviceUserId));
        if (existing.isPresent()) {
            punchRecordRepository.linkUnmatchedToEmployee(appId, normalizedDeviceUserId, existing.get().getId());
            return existing.get().getId();
        }
        if (!source.isAutoCreateEmployees()) return null;

        configureSource(source, true, source.getAutoCreateCategoryId(), source.getAutoCreateEmploymentType(),
                source.getAutoCreateActiveFromMode(), source.isAutoCreateEmployeeActive());
        String categoryId = source.getAutoCreateCategoryId();

        String employeeCode = employeeCode(normalizedDeviceUserId);
        String fullName = observedName == null || observedName.isBlank()
                ? "Biometric " + normalizedDeviceUserId
                : observedName.strip();
        if (fullName.length() > 200) fullName = fullName.substring(0, 200);
        if (normalizedDeviceUserId.length() > 100) {
            throw new BusinessRuleException(
                    "Biometric user ID is longer than the supported employee device ID length.",
                    "BIO_AUTO_EMPLOYEE_DEVICE_ID_TOO_LONG", HttpStatus.CONFLICT);
        }

        String id = UUID.randomUUID().toString();
        Instant effectiveInstant = "IMPORT_DATE".equals(source.getAutoCreateActiveFromMode())
                ? Instant.now()
                : (observedAt == null ? Instant.now() : observedAt);
        LocalDate activeFrom = effectiveInstant.atZone(ZoneOffset.UTC).toLocalDate();
        int inserted = employeeRepository.insertAutoProvisioned(
                id, appId, employeeCode, fullName, normalizedDeviceUserId, categoryId,
                normalizeEmploymentType(source.getAutoCreateEmploymentType()), activeFrom,
                source.isAutoCreateEmployeeActive());

        var resolved = employeeRepository.findByDeviceUserId(normalizedDeviceUserId)
                .or(() -> employeeRepository.findByEmployeeCodeIgnoreCase(employeeCode))
                .orElseThrow(() -> new IllegalStateException(
                        "Automatic biometric employee creation could not resolve employee " + normalizedDeviceUserId));

        punchRecordRepository.linkUnmatchedToEmployee(appId, normalizedDeviceUserId, resolved.getId());
        if (inserted == 1) {
            log.info("Auto-provisioned employee created successfully with id={}, deviceUserId={}", resolved.getId(), normalizedDeviceUserId);
            auditService.record("AUTO_CREATE", "EMPLOYEE", resolved.getId(),
                    actor == null || actor.isBlank() ? "biometric-import" : actor,
                    "{\"deviceUserId\":\"" + safe(normalizedDeviceUserId)
                            + "\",\"sourceId\":\"" + safe(source.getId()) + "\"}", null);
        }
        return resolved.getId();
    }

    private String normalizeEmploymentType(String value) {
        String normalized = value == null || value.isBlank() ? EmploymentType.FIXED.name()
                : value.strip().toUpperCase(Locale.ROOT);
        try {
            return EmploymentType.valueOf(normalized).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException(
                    "Automatic employee employment type must be FIXED or DAILY.",
                    "BIO_AUTO_EMPLOYEE_TYPE_INVALID", HttpStatus.CONFLICT);
        }
    }

    private String normalizeActiveFromMode(String value) {
        String normalized = value == null || value.isBlank()
                ? "FIRST_PUNCH" : value.strip().toUpperCase(Locale.ROOT);
        if (!"FIRST_PUNCH".equals(normalized) && !"IMPORT_DATE".equals(normalized)) {
            throw new BusinessRuleException(
                    "Automatic employee active-from mode must be FIRST_PUNCH or IMPORT_DATE.",
                    "BIO_AUTO_EMPLOYEE_ACTIVE_FROM_MODE_INVALID", HttpStatus.CONFLICT);
        }
        return normalized;
    }

    private String employeeCode(String deviceUserId) {
        if (deviceUserId.length() <= 50) return deviceUserId.toUpperCase(Locale.ROOT);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(deviceUserId.getBytes(StandardCharsets.UTF_8));
            return "BIO-" + HexFormat.of().formatHex(digest).substring(0, 24).toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

// BORTQALA_ATTENDANCE_PIPELINE_20260816_V1_AUTO_CATEGORY_MONTHLY
