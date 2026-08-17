package com.bemo.hr.attendance.application;

import com.bemo.hr.attendance.api.ImportApi;
import com.bemo.hr.attendance.domain.BiometricSource;
import com.bemo.hr.attendance.domain.ImportBatch;
import com.bemo.hr.attendance.domain.ImportRowError;
import com.bemo.hr.attendance.domain.ImportStatus;
import com.bemo.hr.attendance.domain.PunchImportEvidence;
import com.bemo.hr.attendance.domain.PunchRecord;
import com.bemo.hr.attendance.infrastructure.BiometricSourceRepository;
import com.bemo.hr.attendance.infrastructure.ImportBatchRepository;
import com.bemo.hr.attendance.infrastructure.ImportRowErrorRepository;
import com.bemo.hr.attendance.infrastructure.PunchImportEvidenceRepository;
import com.bemo.hr.attendance.infrastructure.PunchRecordRepository;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.reporting.application.ReportingService;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BiometricImportService {
    private static final int PREVIEW_LIMIT = 100;
    private static final int BATCH_FLUSH_SIZE = 500;

    private final BiometricFileReader biometricFileReader;
    private final BiometricSourceRepository biometricSourceRepository;
    private final ImportBatchRepository importBatchRepository;
    private final PunchRecordRepository punchRecordRepository;
    private final PunchImportEvidenceRepository punchImportEvidenceRepository;
    private final ImportRowErrorRepository importRowErrorRepository;
    private final BiometricEmployeeProvisioningService employeeProvisioningService;
    private final EmployeeRepository employeeRepository;
    private final AttendanceCategoryRepository attendanceCategoryRepository;
    private final AuditService auditService;
    private final ReportingService reportingService;
    @Value("${hr.company-zone:Africa/Cairo}")
    private String companyZoneId;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public boolean alreadyImported(String sourceId, String checksum) {
        if (sourceId == null || sourceId.isBlank() || checksum == null || !checksum.matches("(?i)[0-9a-f]{64}")) return false;
        return importBatchRepository.findFirstBySourceIdAndChecksumAndStatusNotOrderByImportedAtDesc(
                sourceId, checksum.toLowerCase(java.util.Locale.ROOT), ImportStatus.REVERSED).isPresent();
    }

    @Transactional
    public ImportApi.PreviewResponse preview(MultipartFile file) {
        if (file.isEmpty()) throw new BusinessRuleException("Select a non-empty biometric file.", "BIO_FILE_EMPTY", HttpStatus.CONFLICT);
        try {
                        var parsed = biometricFileReader.read(file.getOriginalFilename() == null ? "biometric-file"
                    : file.getOriginalFilename(), file.getInputStream());
            return new ImportApi.PreviewResponse(
                    file.getOriginalFilename() == null ? "biometric-file" : file.getOriginalFilename(),
                    sha256(file), parsed.totalRows(), parsed.importedRows(), parsed.errors().size(),
                    parsed.rows().stream().limit(PREVIEW_LIMIT).map(row -> new ImportApi.PreviewRowResponse(
                            row.rowNumber(), row.deviceUserId(), row.employeeName(),
                            row.punchedAt().toEpochMilli(), row.rawLine())).toList(),
                    parsed.errors().stream().limit(PREVIEW_LIMIT).map(error -> new ImportApi.RowErrorResponse(
                            error.rowNumber(), error.message(), error.rawLine())).toList());
        } catch (IOException exception) {
            throw new BusinessRuleException("Could not read the uploaded file.", "EXCEL_READ_FAILED", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional
    public ImportApi.BatchResponse reverse(String batchId, String actor) {
        var batch = importBatchRepository.findById(batchId)
                .orElseThrow(() -> new NotFoundException("سجل الاستيراد غير موجود.", "IMP_BATCH_NOT_FOUND"));
        if (batch.getStatus() == ImportStatus.REVERSED) return toResponse(batch, false);
        List<String> punchIds = punchImportEvidenceRepository.findPunchIdsByBatchId(batchId);
        punchImportEvidenceRepository.deleteByBatchId(batchId);
        punchRecordRepository.deleteUnclaimedPunches(TenantContext.require(), punchIds);
        importRowErrorRepository.deleteByBatchId(batchId);
        batch.reverse();
        importBatchRepository.saveAndFlush(batch);
        auditService.record("REVERSE", "ATTENDANCE_IMPORT", batchId, actor,
                "{\"fileName\":\"" + safe(batch.getFileName()) + "\",\"totalRows\":" + batch.getTotalRows() + "}", null);
        return toResponse(batch, false);
    }

    @Transactional
    public ImportApi.BatchResponse importFile(MultipartFile file, String sourceId, String actor) {
        if (file.isEmpty()) throw new BusinessRuleException("Select a non-empty biometric file.", "BIO_FILE_EMPTY", HttpStatus.CONFLICT);
        if (sourceId == null || sourceId.isBlank()) throw new BusinessRuleException("Source is required.", "BIO_SOURCE_REQUIRED", HttpStatus.CONFLICT);
        if (actor == null || actor.isBlank()) throw new BusinessRuleException("Importer name is required.", "BIO_IMPORTER_NAME_REQUIRED", HttpStatus.CONFLICT);
        BiometricSource source = biometricSourceRepository.findById(sourceId)
                .orElseThrow(() -> new NotFoundException("مصدر البصمة غير موجود.", "BIO_SOURCE_NOT_FOUND"));
        if (source.getSourceType() != BiometricSource.SourceType.FILE_DEVICE) {
            throw new BusinessRuleException("Selected source is not a file-import source.", "BIO_SOURCE_WRONG_TYPE", HttpStatus.CONFLICT);
        }
        if (!source.isActive()) {
            throw new BusinessRuleException("Selected source is inactive.", "BIO_SOURCE_INACTIVE", HttpStatus.CONFLICT);
        }
        try {
                        String checksum = sha256(file);
            var existing = importBatchRepository
                    .findFirstBySourceIdAndChecksumAndStatusNotOrderByImportedAtDesc(
                            sourceId, checksum, ImportStatus.REVERSED);
            if (existing.isPresent()) {
            // BORTQALA_CORRECTIVE_20260816_DUPLICATE_FASTPATH
            // The checksum already proves this exact payload was imported. Do not parse the same large file
            // again and do not repeat per-user provisioning on an idempotent duplicate request.
            return toResponse(existing.get(), true);
        }

            var parsed = biometricFileReader.read(file.getOriginalFilename(), file.getInputStream());
            String appId = TenantContext.require();
            String fileName = file.getOriginalFilename() == null ? "biometric-file" : file.getOriginalFilename();
            int totalRows = parsed.totalRows();
            int validRows = parsed.importedRows();
            int errorRows = parsed.errors().size();
            String batchId = UUID.randomUUID().toString();
            int reserved = importBatchRepository.insertIfAbsent(batchId, appId, checksum, fileName,
                    sourceId, source.getName(), errorRows == 0 ? ImportStatus.COMPLETED.name() : ImportStatus.COMPLETED_WITH_ERRORS.name(),
                    totalRows, validRows, errorRows, actor);
            ImportBatch batch;
            if (reserved == 0) {
                batch = importBatchRepository
                        .findFirstBySourceIdAndChecksumAndStatusNotOrderByImportedAtDesc(
                                sourceId, checksum, ImportStatus.REVERSED)
                        .orElseThrow(() -> new IllegalStateException("Reserved batch could not be loaded: " + checksum));
                return toResponse(batch, true);
            }
            batch = importBatchRepository.findById(batchId)
                    .orElseThrow(() -> new IllegalStateException("Reserved batch could not be loaded: " + batchId));

            int imported = 0;
            int duplicates = 0;
            List<PunchImportEvidence> evidence = new ArrayList<>(parsed.rows().size());
            java.util.Map<String, java.util.Optional<String>> employeeIdCache = new java.util.HashMap<>();
            int processed = 0;
            for (var row : parsed.rows()) {
                String employeeId = employeeIdCache.computeIfAbsent(row.deviceUserId(), key ->
                        java.util.Optional.ofNullable(employeeProvisioningService.resolveEmployeeId(
                                source, key, row.employeeName(), row.punchedAt(), actor))).orElse(null);
                String punchId = UUID.randomUUID().toString();
                int inserted = punchRecordRepository.insertIfAbsent(punchId, appId,
                        batch.getId(), sourceId, null, employeeId, row.deviceUserId(), row.employeeName(),
                        row.punchedAt(), row.rawLine(), row.rowNumber());
                if (inserted == 1) {
                    imported++;
                } else {
                    duplicates++;
                    punchId = punchRecordRepository.findBySourceIdAndDeviceUserIdAndPunchedAt(
                                    sourceId, row.deviceUserId(), row.punchedAt())
                            .map(PunchRecord::getId)
                            .orElse(null);
                }
                if (punchId != null) {
                    evidence.add(new PunchImportEvidence(punchId, batch.getId(), appId,
                            row.rowNumber(), row.rawLine()));
                }
                processed++;
                if (processed % BATCH_FLUSH_SIZE == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }
            if (!evidence.isEmpty()) {
                jdbcTemplate.batchUpdate(
                    "INSERT INTO punch_import_evidence (punch_id, batch_id, app_id, row_number, raw_line) VALUES (?, ?, ?, ?, ?)",
                    evidence, BATCH_FLUSH_SIZE,
                    (ps, ev) -> {
                        ps.setString(1, ev.getPunchId());
                        ps.setString(2, ev.getBatchId());
                        ps.setString(3, ev.getAppId());
                        ps.setInt(4, ev.getRowNumber());
                        ps.setString(5, ev.getRawLine());
                    });
            }
            batch.updateCounts(totalRows, validRows, errorRows, imported, duplicates);
            importBatchRepository.save(batch);
            importRowErrorRepository.saveAll(parsed.errors().stream()
                    .map(error -> new ImportRowError(batch.getId(), error.rowNumber(), error.message(), error.rawLine()))
                    .toList());

            // BORTQALA_ATTENDANCE_PIPELINE_20260816_V1_IMPORT_GENERATES_REPORTS: a successful biometric import is not complete until
            // the affected calendar-month attendance snapshots exist. The existing ReportingService
            // remains the single source of truth for PRESENT/LATE/overtime calculations.
            var affectedAttendanceMonths = new java.util.TreeSet<java.time.YearMonth>();
            var attendanceZone = java.time.ZoneId.of(companyZoneId);
            for (var importedRow : parsed.rows()) {
                if (importedRow.punchedAt() != null) {
                    affectedAttendanceMonths.add(java.time.YearMonth.from(importedRow.punchedAt().atZone(attendanceZone)));
                }
            }
            boolean hasMonthlyCategory = attendanceCategoryRepository.findByScopeIn(
                    java.util.List.of(com.bemo.hr.employee.domain.CategoryScope.EMPLOYEE, com.bemo.hr.employee.domain.CategoryScope.BOTH)
            ).stream().anyMatch(c -> c.isActive() && c.getPayCycle() == com.bemo.hr.employee.domain.PayCycle.MONTHLY);

            if (hasMonthlyCategory) {
                for (var period : affectedAttendanceMonths) {
                    reportingService.create(new com.bemo.hr.reporting.api.ReportingApi.CreateRequest(period.atDay(1), period.atEndOfMonth(), com.bemo.hr.employee.domain.PayCycle.MONTHLY), actor);
                }
            }
            var firstImportedPunch = parsed.rows().stream().map(row -> row.punchedAt())
                    .filter(java.util.Objects::nonNull).min(java.util.Comparator.naturalOrder()).orElse(null);
            var lastImportedPunch = parsed.rows().stream().map(row -> row.punchedAt())
                    .filter(java.util.Objects::nonNull).max(java.util.Comparator.naturalOrder()).orElse(null);
            eventPublisher.publishEvent(new BiometricImportCompletedEvent(firstImportedPunch, lastImportedPunch, actor));
            return toResponse(batch, false);
        } catch (IOException exception) {
            throw new BusinessRuleException("Could not read the uploaded file.", "EXCEL_READ_FAILED", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }

    public List<ImportApi.BatchResponse> listBatches() {
        return importBatchRepository.findAllByOrderByImportedAtDesc().stream()
                .map(batch -> toResponse(batch, false)).toList();
    }

    public List<ImportApi.UnmatchedIdentityResponse> unmatchedIdentities() {
        return punchRecordRepository.summarizeUnmatched().stream()
                .filter(row -> employeeRepository.findByDeviceUserId((String) row[0]).isEmpty())
                .map(row -> new ImportApi.UnmatchedIdentityResponse((String) row[0], (String) row[1],
                        ((Number) row[2]).longValue(), (Instant) row[3], (Instant) row[4]))
                .toList();
    }

    private ImportApi.BatchResponse toResponse(ImportBatch batch, boolean duplicate) {
        var errors = importRowErrorRepository.findByBatchIdOrderByRowNumber(batch.getId()).stream()
                .map(error -> new ImportApi.RowErrorResponse(error.getRowNumber(), error.getMessage(), error.getRawLine()))
                .toList();
        return new ImportApi.BatchResponse(batch.getId(), batch.getFileName(), batch.getSourceId(),
                batch.getDeviceName(), batch.getStatus(),
                batch.getTotalRows(), batch.getImportedRows(), batch.getValidRows(),
                batch.getNewPunches(), batch.getDuplicatePunches(), batch.getErrorRows(), batch.getImportedBy(),
                batch.getImportedAt(), duplicate, errors);
    }

    private String punchKey(String deviceUserId, Instant punchedAt) {
        return deviceUserId + "\u001f" + punchedAt;
    }

    private String sha256(MultipartFile file) {
        // BORTQALA_FEEDBACK_20260816_STREAMING_UPLOAD
        try (var input = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[64 * 1024];
            for (int read; (read = input.read(buffer)) != -1;) digest.update(buffer, 0, read);
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.io.IOException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Could not calculate biometric file checksum", ex);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
// BORTQALA_RUNTIME_20260816_V2_ATTENDANCE_IMPORT_PERF

// BORTQALA_ATTENDANCE_PIPELINE_20260816_V1_IMPORT_GENERATES_REPORTS_METHOD_create
