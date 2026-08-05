package com.bemo.hr.workforce;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workforce/imports")
@RequiredArgsConstructor
public class WorkforceImportController {
    private final WorkforceExcelImportService workforceExcelImportService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE')")
    public List<WorkforceExcelImportService.ImportBatchResponse> list() { return workforceExcelImportService.listBatches(); }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER')")
    public WorkforceExcelImportService.ImportBatchResponse upload(@RequestPart("file") MultipartFile file) {
        return workforceExcelImportService.upload(file);
    }

    @PostMapping("/{id}/mapping")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER')")
    public WorkforceExcelImportService.ImportBatchResponse map(@PathVariable String id,
            @RequestBody WorkforceExcelImportService.MappingRequest request) {
        return workforceExcelImportService.saveMapping(id, request);
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER')")
    public WorkforceExcelImportService.ValidationResponse validate(@PathVariable String id) {
        return workforceExcelImportService.validate(id);
    }

    @GetMapping("/{id}/preview")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER')")
    public WorkforceExcelImportService.ValidationResponse preview(@PathVariable String id) {
        return workforceExcelImportService.preview(id);
    }

    @PostMapping("/{id}/commit")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    public WorkforceExcelImportService.CommitResponse commit(@PathVariable String id,
            @RequestBody WorkforceExcelImportService.CommitRequest request) {
        return workforceExcelImportService.commit(id, request);
    }

    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public WorkforceExcelImportService.ImportBatchResponse reverse(@PathVariable String id) {
        return workforceExcelImportService.reverse(id);
    }

    @GetMapping("/{id}/original")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER')")
    public ResponseEntity<byte[]> original(@PathVariable String id) {
        WorkforceExcelImportService.ImportBatchResponse batch = workforceExcelImportService.getBatch(id);
        return attachment(workforceExcelImportService.originalFile(id), batch.fileName(), MediaType.APPLICATION_OCTET_STREAM);
    }

    @GetMapping("/{id}/errors.xlsx")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER')")
    public ResponseEntity<byte[]> errors(@PathVariable String id) {
        return attachment(workforceExcelImportService.errorWorkbook(id), "workforce-import-errors-" + id + ".xlsx",
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @PostMapping("/analyze")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER')")
    public WorkforceExcelImportService.ImportDiagnosticResult analyzeImport(
            @RequestParam(required = false) BigDecimal summaryDays,
            @RequestParam(required = false) BigDecimal settlementDays) {
        return workforceExcelImportService.analyzeExcelImport(summaryDays, settlementDays);
    }

    private ResponseEntity<byte[]> attachment(byte[] bytes, String fileName, MediaType mediaType) {
        return ResponseEntity.ok().contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8).build().toString()).body(bytes);
    }
}
