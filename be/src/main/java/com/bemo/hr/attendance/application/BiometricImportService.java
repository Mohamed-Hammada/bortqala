package com.bemo.hr.attendance.application;

import com.bemo.hr.attendance.api.ImportApi;
import com.bemo.hr.attendance.domain.*;
import com.bemo.hr.attendance.infrastructure.*;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.reporting.application.ReportingService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BiometricImportService {
    private static final Logger log = LoggerFactory.getLogger(BiometricImportService.class);
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
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;
    @Value("${hr.company-zone:Africa/Cairo}")
    private String companyZoneId;

    public boolean alreadyImported(String sourceId, String checksum) {
        if (sourceId == null || sourceId.isBlank() || checksum == null || !checksum.matches("(?i)[0-9a-f]{64}"))
            return false;
        return importBatchRepository.findFirstBySourceIdAndChecksumAndStatusNotOrderByImportedAtDesc(
                sourceId, checksum.toLowerCase(java.util.Locale.ROOT), ImportStatus.REVERSED).isPresent();
    }

    @Transactional
    public ImportApi.PreviewResponse preview(MultipartFile file) {
        if (file.isEmpty())
            throw new BusinessRuleException("Select a non-empty biometric file.", "BIO_FILE_EMPTY", HttpStatus.CONFLICT);
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
        long totalStart = System.currentTimeMillis();
        if (file.isEmpty())
            throw new BusinessRuleException("Select a non-empty biometric file.", "BIO_FILE_EMPTY", HttpStatus.CONFLICT);
        if (sourceId == null || sourceId.isBlank())
            throw new BusinessRuleException("Source is required.", "BIO_SOURCE_REQUIRED", HttpStatus.CONFLICT);
        if (actor == null || actor.isBlank())
            throw new BusinessRuleException("Importer name is required.", "BIO_IMPORTER_NAME_REQUIRED", HttpStatus.CONFLICT);
        BiometricSource source = biometricSourceRepository.findById(sourceId)
                .orElseThrow(() -> new NotFoundException("مصدر البصمة غير موجود.", "BIO_SOURCE_NOT_FOUND"));
        if (source.getSourceType() != BiometricSource.SourceType.FILE_DEVICE) {
            throw new BusinessRuleException("Selected source is not a file-import source.", "BIO_SOURCE_WRONG_TYPE", HttpStatus.CONFLICT);
        }
        if (!source.isActive()) {
            throw new BusinessRuleException("Selected source is inactive.", "BIO_SOURCE_INACTIVE", HttpStatus.CONFLICT);
        }
        try {
            long t0 = System.currentTimeMillis();
            byte[] fileBytes = file.getBytes();
            String checksum = sha256(fileBytes);
            log.info("[IMPORT] Received biometric file: name={}, size={} bytes, checksum={}, sourceId={}, actor={}",
                    file.getOriginalFilename(), fileBytes.length, checksum, sourceId, actor);

            String fileName = file.getOriginalFilename() == null ? "biometric-file" : file.getOriginalFilename();
            long parseStart = System.currentTimeMillis();
            var parsed = biometricFileReader.read(fileName, new ByteArrayInputStream(fileBytes));
            fileBytes = null; // allow GC of the raw buffer
            log.info("[IMPORT] File parsed in {}ms: totalRows={}, importedRows={}, errors={}",
                    System.currentTimeMillis() - parseStart, parsed.totalRows(), parsed.importedRows(), parsed.errors().size());

            String appId = TenantContext.require();
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
                log.info("[IMPORT] Processing existing batchId={}", batch.getId());
            } else {
                batch = importBatchRepository.findById(batchId)
                        .orElseThrow(() -> new IllegalStateException("Reserved batch could not be loaded: " + batchId));
                log.info("[IMPORT] Reserved new batch: batchId={}, appId={}", batchId, appId);
            }

            // ------------------------------------------------------------------
            // Pre-load known employees in bulk
            // ------------------------------------------------------------------
            long empStart = System.currentTimeMillis();
            Set<String> allDeviceUserIds = parsed.rows().stream()
                    .map(BiometricFileReader.PunchRow::deviceUserId)
                    .filter(id -> id != null && !id.isBlank())
                    .map(String::strip)
                    .collect(Collectors.toSet());

            Map<String, String> employeeIdCache = new HashMap<>();
            if (!allDeviceUserIds.isEmpty()) {
                employeeRepository.findByDeviceUserIdIn(allDeviceUserIds)
                        .forEach(e -> employeeIdCache.put(e.getDeviceUserId(), e.getId()));
            }
            int knownPreloaded = employeeIdCache.size();

            for (String duid : allDeviceUserIds) {
                if (!employeeIdCache.containsKey(duid)) {
                    var sampleRow = parsed.rows().stream()
                            .filter(r -> duid.equals(r.deviceUserId() == null ? "" : r.deviceUserId().strip()))
                            .findFirst().orElse(null);
                    if (sampleRow != null) {
                        String empId = employeeProvisioningService.resolveEmployeeId(
                                source, duid, sampleRow.employeeName(), sampleRow.punchedAt(), actor);
                        if (empId != null) employeeIdCache.put(duid, empId);
                    }
                }
            }
            log.info("[IMPORT] Employee resolution in {}ms: uniqueDeviceUsers={}, preloaded={}, totalResolved={}",
                    System.currentTimeMillis() - empStart, allDeviceUserIds.size(), knownPreloaded, employeeIdCache.size());

            // ------------------------------------------------------------------
            // Batch INSERT punch records via JdbcTemplate
            // ------------------------------------------------------------------
            long punchInsertStart = System.currentTimeMillis();
            List<Object[]> punchParams = new ArrayList<>(parsed.rows().size());
            for (var row : parsed.rows()) {
                String normalized = row.deviceUserId() == null ? "" : row.deviceUserId().strip();
                String employeeId = employeeIdCache.get(normalized);
                punchParams.add(new Object[]{
                        UUID.randomUUID().toString(), appId, batch.getId(), sourceId, null,
                        employeeId, row.deviceUserId(), row.employeeName(),
                        java.sql.Timestamp.from(row.punchedAt()), row.rawLine(), row.rowNumber()
                });
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO punch_records (id, app_id, batch_id, source_id, device_id, employee_id, " +
                            "device_user_id, raw_name, punched_at, raw_line, row_number) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING",
                    punchParams, BATCH_FLUSH_SIZE,
                    (ps, args) -> {
                        ps.setString(1, (String) args[0]);
                        ps.setString(2, (String) args[1]);
                        ps.setString(3, (String) args[2]);
                        ps.setString(4, (String) args[3]);
                        ps.setString(5, (String) args[4]);
                        ps.setString(6, (String) args[5]);
                        ps.setString(7, (String) args[6]);
                        ps.setString(8, (String) args[7]);
                        ps.setTimestamp(9, (java.sql.Timestamp) args[8]);
                        ps.setString(10, (String) args[9]);
                        ps.setInt(11, (int) args[10]);
                    });
            log.info("[IMPORT] Punch records batch insert in {}ms: {} rows processed",
                    System.currentTimeMillis() - punchInsertStart, punchParams.size());

            int newPunches = (int) punchRecordRepository.countByBatchId(batch.getId());
            int duplicatePunches = validRows - newPunches;
            log.info("[IMPORT] Punch insert counts: new={}, duplicates={}, validTotal={}",
                    newPunches, duplicatePunches, validRows);

            // ------------------------------------------------------------------
            // Resolve punch IDs in bulk via lightweight JDBC query
            // ------------------------------------------------------------------
            long resolveStart = System.currentTimeMillis();
            Instant minTime = parsed.rows().stream().map(BiometricFileReader.PunchRow::punchedAt)
                    .filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
            Instant maxTime = parsed.rows().stream().map(BiometricFileReader.PunchRow::punchedAt)
                    .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);

            Map<String, String> resolvedPunchIds = new HashMap<>();
            if (minTime != null && maxTime != null) {
                jdbcTemplate.query(
                        "SELECT id, device_user_id, punched_at FROM punch_records WHERE source_id = ? AND punched_at >= ? AND punched_at <= ?",
                        rs -> {
                            String punchId = rs.getString("id");
                            String duid = rs.getString("device_user_id");
                            java.sql.Timestamp ts = rs.getTimestamp("punched_at");
                            if (punchId != null && duid != null && ts != null) {
                                resolvedPunchIds.put(punchKey(duid, ts.toInstant()), punchId);
                            }
                        },
                        sourceId, java.sql.Timestamp.from(minTime), java.sql.Timestamp.from(maxTime));
            }
            log.info("[IMPORT] Punch ID resolution in {}ms: resolved {} IDs for range [{} to {}]",
                    System.currentTimeMillis() - resolveStart, resolvedPunchIds.size(), minTime, maxTime);

            // ------------------------------------------------------------------
            // Batch INSERT evidence
            // ------------------------------------------------------------------
            long evidenceStart = System.currentTimeMillis();
            List<PunchImportEvidence> evidence = new ArrayList<>(parsed.rows().size());
            for (var row : parsed.rows()) {
                String punchId = resolvedPunchIds.get(punchKey(row.deviceUserId(), row.punchedAt()));
                if (punchId != null) {
                    evidence.add(new PunchImportEvidence(punchId, batch.getId(), appId,
                            row.rowNumber(), row.rawLine()));
                }
            }
            if (!evidence.isEmpty()) {
                jdbcTemplate.batchUpdate(
                        "INSERT INTO punch_import_evidence (punch_id, batch_id, app_id, row_number, raw_line) " +
                                "VALUES (?, ?, ?, ?, ?) ON CONFLICT DO NOTHING",
                        evidence, BATCH_FLUSH_SIZE,
                        (ps, ev) -> {
                            ps.setString(1, ev.getPunchId());
                            ps.setString(2, ev.getBatchId());
                            ps.setString(3, ev.getAppId());
                            ps.setInt(4, ev.getRowNumber());
                            ps.setString(5, ev.getRawLine());
                        });
            }
            log.info("[IMPORT] Punch evidence batch insert in {}ms: {} evidence records",
                    System.currentTimeMillis() - evidenceStart, evidence.size());

            batch.updateCounts(totalRows, validRows, errorRows, newPunches, duplicatePunches);
            importBatchRepository.save(batch);
            if (reserved == 0) {
                importRowErrorRepository.deleteByBatchId(batch.getId());
            }
            if (!parsed.errors().isEmpty()) {
                importRowErrorRepository.saveAll(parsed.errors().stream()
                        .map(error -> new ImportRowError(batch.getId(), error.rowNumber(), error.message(), error.rawLine()))
                        .toList());
            }

            // ------------------------------------------------------------------
            // Auto-generate attendance reports for recent/active affected months
            // ------------------------------------------------------------------
            var affectedAttendanceMonths = new TreeSet<YearMonth>();
            var attendanceZone = ZoneId.of(companyZoneId);
            for (var importedRow : parsed.rows()) {
                if (importedRow.punchedAt() != null) {
                    affectedAttendanceMonths.add(YearMonth.from(importedRow.punchedAt().atZone(attendanceZone)));
                }
            }
            boolean hasMonthlyCategory = attendanceCategoryRepository.findByScopeIn(
                    List.of(com.bemo.hr.employee.domain.CategoryScope.EMPLOYEE, com.bemo.hr.employee.domain.CategoryScope.BOTH)
            ).stream().anyMatch(c -> c.isActive() && c.getPayCycle() == com.bemo.hr.employee.domain.PayCycle.MONTHLY);

            if (hasMonthlyCategory && !affectedAttendanceMonths.isEmpty()) {
                // Scope auto-generation to the latest month in file and any recent active months.
                // This prevents multi-year backup files (40+ historical months) from blocking import for minutes.
                YearMonth currentMonth = YearMonth.now(attendanceZone);
                var targetMonths = new TreeSet<YearMonth>();
                targetMonths.add(affectedAttendanceMonths.last()); // Always include latest month in file
                for (var m : affectedAttendanceMonths) {
                    if (!m.isBefore(currentMonth.minusMonths(1))) {
                        targetMonths.add(m);
                    }
                }
                log.info("[IMPORT] Auto-generating attendance reports for {} target month(s) (out of {} in file): {}",
                        targetMonths.size(), affectedAttendanceMonths.size(), targetMonths);
                int count = 0;
                for (var period : targetMonths) {
                    count++;
                    long reportStart = System.currentTimeMillis();
                    log.info("[IMPORT] Generating attendance report [{}/{}]: period={} ...",
                            count, targetMonths.size(), period);
                    try {
                        reportingService.recalculateMonth(period.getYear(), period.getMonthValue(), actor);
                        log.info("[IMPORT] Attendance report [{}/{}] for period={} completed in {}ms",
                                count, targetMonths.size(), period, System.currentTimeMillis() - reportStart);
                    } catch (Exception ex) {
                        log.warn("[IMPORT] Attendance report for period={} failed: {}",
                                period, ex.getMessage());
                    }
                }
            }

            Instant firstImportedPunch = minTime;
            Instant lastImportedPunch = maxTime;
            eventPublisher.publishEvent(new BiometricImportCompletedEvent(firstImportedPunch, lastImportedPunch, actor));

            long totalElapsed = System.currentTimeMillis() - totalStart;
            log.info("[IMPORT] >>> Biometric import COMPLETED in {}ms ({}s): batchId={}, file={}, totalRows={}, validRows={}, newPunches={}, duplicatePunches={}, errorRows={}",
                    totalElapsed, totalElapsed / 1000.0, batch.getId(), fileName, totalRows, validRows, newPunches, duplicatePunches, errorRows);

            return toResponse(batch, false);
        } catch (IOException exception) {
            log.error("[IMPORT] Biometric file read error: {}", exception.getMessage(), exception);
            throw new BusinessRuleException("Could not read the uploaded file.", "EXCEL_READ_FAILED", HttpStatus.BAD_REQUEST);
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
        return (deviceUserId == null ? "" : deviceUserId.strip()) + "\u001f" + punchedAt;
    }

    private String sha256(MultipartFile file) {
        // BORTQALA_FEEDBACK_20260816_STREAMING_UPLOAD
        try (var input = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[64 * 1024];
            for (int read; (read = input.read(buffer)) != -1; ) digest.update(buffer, 0, read);
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.io.IOException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Could not calculate biometric file checksum", ex);
        }
    }

    /**
     * PERF-FIX-4: Hash from an already-read byte array (avoids double file read in importFile).
     */
    private String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Could not calculate biometric file checksum", ex);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
// BORTQALA_RUNTIME_20260816_V2_ATTENDANCE_IMPORT_PERF

// BORTQALA_ATTENDANCE_PIPELINE_20260816_V1_IMPORT_GENERATES_REPORTS_METHOD_create
