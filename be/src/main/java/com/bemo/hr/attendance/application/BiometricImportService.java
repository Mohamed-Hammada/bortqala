package com.bemo.hr.attendance.application;

import com.bemo.hr.attendance.api.ImportApi;
import com.bemo.hr.attendance.domain.ImportBatch;
import com.bemo.hr.attendance.domain.ImportRowError;
import com.bemo.hr.attendance.domain.PunchRecord;
import com.bemo.hr.attendance.infrastructure.ImportBatchRepository;
import com.bemo.hr.attendance.infrastructure.ImportRowErrorRepository;
import com.bemo.hr.attendance.infrastructure.PunchRecordRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BiometricImportService {
    private final BiometricFileReader biometricFileReader;
    private final ImportBatchRepository importBatchRepository;
    private final PunchRecordRepository punchRecordRepository;
    private final ImportRowErrorRepository importRowErrorRepository;
    private final EmployeeRepository employeeRepository;

    public BiometricImportService(BiometricFileReader biometricFileReader,
                                  ImportBatchRepository importBatchRepository,
                                  PunchRecordRepository punchRecordRepository,
                                  ImportRowErrorRepository importRowErrorRepository,
                                  EmployeeRepository employeeRepository) {
        this.biometricFileReader = biometricFileReader;
        this.importBatchRepository = importBatchRepository;
        this.punchRecordRepository = punchRecordRepository;
        this.importRowErrorRepository = importRowErrorRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public ImportApi.BatchResponse importFile(MultipartFile file, String deviceName, String actor) {
        if (file.isEmpty()) throw new BusinessRuleException("Select a non-empty biometric file.");
        if (deviceName == null || deviceName.isBlank()) throw new BusinessRuleException("Device name is required.");
        if (actor == null || actor.isBlank()) throw new BusinessRuleException("Importer name is required.");
        try {
            byte[] content = file.getBytes();
            String checksum = sha256(content);
            var existing = importBatchRepository.findByChecksum(checksum);
            if (existing.isPresent()) return toResponse(existing.get(), true);

            var parsed = biometricFileReader.read(file.getOriginalFilename(), new ByteArrayInputStream(content));
            var batch = importBatchRepository.save(new ImportBatch(checksum,
                    file.getOriginalFilename() == null ? "biometric-file" : file.getOriginalFilename(),
                    deviceName, actor, parsed.totalRows(), parsed.rows().size(), parsed.errors().size()));

            var punches = parsed.rows().stream().map(row -> {
                String employeeId = employeeRepository.findByDeviceUserId(row.deviceUserId())
                        .map(employee -> employee.getId()).orElse(null);
                return new PunchRecord(batch.getId(), employeeId, row.deviceUserId(), row.employeeName(),
                        row.punchedAt(), row.rawLine(), row.rowNumber());
            }).toList();
            punchRecordRepository.saveAll(punches);
            importRowErrorRepository.saveAll(parsed.errors().stream()
                    .map(error -> new ImportRowError(batch.getId(), error.rowNumber(), error.message(), error.rawLine()))
                    .toList());
            return toResponse(batch, false);
        } catch (IOException exception) {
            throw new BusinessRuleException("Could not read the uploaded file.");
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
        return new ImportApi.BatchResponse(batch.getId(), batch.getFileName(), batch.getDeviceName(), batch.getStatus(),
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
}
