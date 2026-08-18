package com.bemo.hr.employee.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.employee.api.EmployeeApi;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.reporting.infrastructure.DailyAttendanceResultRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * REM-004: controlled migration for employee codes that were persisted twice
 * (e.g. {@code QA-EMP-0807-QA-EMP-0807}) by an earlier creation bug. The
 * service reports a dry-run plan first; on apply it normalizes the codes,
 * syncs {@code daily_results} snapshots, and records an audit event per
 * correction. Idempotent - only rows still matching the duplicated pattern
 * are touched.
 */
@Service
@Slf4j
public class EmployeeCodeDedupService {

    private final EmployeeRepository employeeRepository;
    private final DailyAttendanceResultRepository dailyAttendanceResultRepository;
    private final AuditService auditService;

    public EmployeeCodeDedupService(EmployeeRepository employeeRepository,
                                    DailyAttendanceResultRepository dailyAttendanceResultRepository,
                                    AuditService auditService) {
        this.employeeRepository = employeeRepository;
        this.dailyAttendanceResultRepository = dailyAttendanceResultRepository;
        this.auditService = auditService;
    }

    /**
     * Returns the canonical code when {@code code} matches the duplicated
     * pattern (canonical + "-" + canonical), e.g. QA-EMP-0807-QA-EMP-0807.
     */
    static Optional<String> duplicatedCanonical(String code) {
        if (code == null) return Optional.empty();
        int length = code.length();
        if (length < 3 || length % 2 == 0) return Optional.empty();
        int half = (length - 1) / 2;
        String left = code.substring(0, half);
        String right = code.substring(half + 1);
        if (left.isEmpty() || !left.contains("-") || !left.equals(right)) return Optional.empty();
        return Optional.of(left);
    }

    @Transactional
    public EmployeeApi.CodeCorrectionReport correct(boolean dryRun, String actor) {
        List<EmployeeApi.CodeCorrectionItem> items = new ArrayList<>();

        List<Employee> candidates = employeeRepository.findAll();
        boolean anyDuplicated = candidates.stream()
                .anyMatch(employee -> duplicatedCanonical(employee.getEmployeeCode()).isPresent());
        if (!anyDuplicated) {
            return new EmployeeApi.CodeCorrectionReport(0, List.of());
        }

        Set<String> occupied = new HashSet<>();
        for (Employee employee : candidates) {
            occupied.add(employee.getEmployeeCode());
        }

        int corrected = 0;
        for (Employee employee : candidates) {
            Optional<String> canonical = duplicatedCanonical(employee.getEmployeeCode());
            if (canonical.isEmpty()) continue;

            String oldCode = employee.getEmployeeCode();
            String newCode = canonical.get();
            boolean conflict = occupied.contains(newCode);
            if (conflict) {
                newCode = newCode + "-"
                        + employee.getId().substring(0, Math.min(4, employee.getId().length())).toUpperCase(Locale.ROOT);
            }
            items.add(new EmployeeApi.CodeCorrectionItem(employee.getId(), oldCode, newCode, conflict));

            if (dryRun) {
                continue;
            }
            employee.applyEmployeeCode(newCode, Instant.now());
            employeeRepository.save(employee);
            dailyAttendanceResultRepository.normalizeEmployeeCode(oldCode, newCode);
            auditService.record("CODE_CORRECTION", "EMPLOYEE", employee.getId(), actor,
                    "{\"oldCode\":\"" + oldCode + "\",\"newCode\":\"" + newCode + "\"}", null);
            corrected++;
        }
        return new EmployeeApi.CodeCorrectionReport(corrected, items);
    }
}
