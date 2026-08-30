package com.bemo.hr.migration.api;

import com.bemo.hr.migration.application.DataMigrationService;
import com.bemo.hr.migration.application.DataMigrationTemplateService;
import com.bemo.hr.migration.domain.MigrationEntityType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/migration")
public class DataMigrationController {

    private final DataMigrationService migrationService;
    private final DataMigrationTemplateService templateService;

    public DataMigrationController(DataMigrationService migrationService,
                                   DataMigrationTemplateService templateService) {
        this.migrationService = migrationService;
        this.templateService = templateService;
    }

    @GetMapping("/templates/{type}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable("type") MigrationEntityType type) {
        String csv = templateService.generateCsvTemplate(type);
        byte[] bytes = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + type.name().toLowerCase() + "_template.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    @GetMapping("/templates/{type}/columns")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<String>> getTemplateColumns(@PathVariable("type") MigrationEntityType type) {
        return ResponseEntity.ok(templateService.getColumns(type));
    }

    @PostMapping("/batches")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<DataMigrationApi.BatchResponse> createBatch(@RequestBody DataMigrationApi.UploadRequest request,
                                                                      Authentication authentication) {
        return ResponseEntity.ok(migrationService.createBatch(request, authentication.getName()));
    }

    @PostMapping("/batches/{id}/validate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<DataMigrationApi.ValidationResultResponse> validateBatch(@PathVariable("id") String batchId) {
        return ResponseEntity.ok(migrationService.validateBatch(batchId));
    }

    @PostMapping("/batches/{id}/dry-run")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<DataMigrationApi.DryRunResponse> dryRun(@PathVariable("id") String batchId) {
        return ResponseEntity.ok(migrationService.dryRun(batchId));
    }

    @PostMapping("/batches/{id}/commit")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<DataMigrationApi.CommitResponse> commitBatch(@PathVariable("id") String batchId,
                                                                     Authentication authentication) {
        return ResponseEntity.ok(migrationService.commitBatch(batchId, authentication.getName()));
    }

    @PostMapping("/batches/{id}/rollback")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<DataMigrationApi.RollbackResponse> rollbackBatch(@PathVariable("id") String batchId,
                                                                         Authentication authentication) {
        return ResponseEntity.ok(migrationService.rollbackBatch(batchId, authentication.getName()));
    }

    @GetMapping("/batches")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<DataMigrationApi.BatchResponse>> listBatches() {
        return ResponseEntity.ok(migrationService.listBatches());
    }

    @GetMapping("/batches/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<DataMigrationApi.BatchResponse> getBatch(@PathVariable("id") String batchId) {
        return ResponseEntity.ok(migrationService.getBatch(batchId));
    }
}
