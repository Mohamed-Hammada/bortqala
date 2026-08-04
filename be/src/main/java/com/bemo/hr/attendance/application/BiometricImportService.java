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
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
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

    private final BiometricFileReader biometricFileReader;
    private final BiometricSourceRepository biometricSourceRepository;
    private final ImportBatchRepository importBatchRepository;
    private final PunchRecordRepository punchRecordRepository;
    private final PunchImportEvidenceRepository punchImportEvidenceRepository;
    private final ImportRowErrorRepository importRowErrorRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditService auditService;

    @Transactional
    public ImportApi.PreviewResponse preview(MultipartFile file) {
        if (file.isEmpty()) throw new BusinessRuleException("Select a non-empty biometric file.", "BIO_FILE_EMPTY", HttpStatus.CONFLICT);
        try {
            byte[] content = file.getBytes();
            var parsed = biometricFileReader.read(file.getOriginalFilename() == null ? "biometric-file"
                    : file.getOriginalFilename(), new ByteArrayInputStream(content));
            return new ImportApi.PreviewResponse(
                    file.getOriginalFilename() == null ? "biometric-file" : file.getOriginalFilename(),
                    sha256(content), parsed.totalRows(), parsed.importedRows(), parsed.errors().size(),
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
        punchImportEvidenceRepository.deleteByBatchId(batchId);
        punchRecordRepository.deleteOrphanedByBatch(batchId);
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
            byte[] content = file.getBytes();
            String checksum = sha256(content);
            var existing = importBatchRepository.findBySourceIdAndChecksum(sourceId, checksum);
            if (existing.isPresent()) return toResponse(existing.get(), true);

            var parsed = biometricFileReader.read(file.getOriginalFilename(), new ByteArrayInputStream(content));
            var batch = importBatchRepository.save(new ImportBatch(checksum,
                    file.getOriginalFilename() == null ? "biometric-file" : file.getOriginalFilename(),
                    sourceId, source.getName(), actor, parsed.totalRows(), parsed.importedRows(), parsed.errors().size()));

            String appId = TenantContext.require();
            int imported = 0;
            int duplicates = 0;
            List<PunchImportEvidence> evidence = new ArrayList<>(parsed.rows().size());
            for (var row : parsed.rows()) {
                String employeeId = employeeRepository.findByEmployeeCodeIgnoreCase(row.deviceUserId())
                        .or(() -> employeeRepository.findByDeviceUserId(row.deviceUserId()))
                        .map(employee -> employee.getId()).orElse(null);
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
            }
            punchImportEvidenceRepository.saveAll(evidence);
            if (duplicates > 0) {
                batch.updateCounts(parsed.totalRows(), imported, parsed.errors().size());
                importBatchRepository.save(batch);
            }
            importRowErrorRepository.saveAll(parsed.errors().stream()
                    .map(error -> new ImportRowError(batch.getId(), error.rowNumber(), error.message(), error.rawLine()))
                    .toList());
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
                batch.getTotalRows(), batch.getImportedRows(), batch.getErrorRows(), batch.getImportedBy(),
                batch.getImportedAt(), duplicate, errors);
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
