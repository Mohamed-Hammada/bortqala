package com.bemo.hr.shared.dataexchange;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/data-exchange")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class DataExchangeController {
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final DataExchangeWorkbookService service;

    public DataExchangeController(DataExchangeWorkbookService service) {
        this.service = service;
    }

    @GetMapping("/catalog")
    public List<DataExchangeWorkbookService.TemplateSummary> catalog() {
        return service.catalog();
    }

    @GetMapping("/templates/{key}")
    public ResponseEntity<Resource> template(@PathVariable String key,
                                             @RequestParam(defaultValue = "false") boolean sample) {
        byte[] bytes = service.createTemplate(key, sample);
        String suffix = sample ? "-sample" : "-blank";
        return workbook(bytes, key + suffix + ".xlsx");
    }

    @PostMapping(value = "/validate/{key}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DataExchangeWorkbookService.ValidationResult validate(@PathVariable String key,
                                                                 @RequestPart("file") MultipartFile file) {
        return service.validate(key, file);
    }

    @PostMapping(value = "/error-workbook/{key}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> errorWorkbook(@PathVariable String key,
                                                  @RequestPart("file") MultipartFile file) {
        byte[] bytes = service.createErrorWorkbook(key, file);
        return workbook(bytes, key + "-errors.xlsx");
    }

    @PostMapping(value = "/export", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Resource> export(@RequestBody DataExchangeWorkbookService.ExportWorkbookRequest request) {
        byte[] bytes = service.createExport(request);
        String filename = request.filename() == null || request.filename().isBlank()
                ? "export.xlsx"
                : request.filename().toLowerCase().endsWith(".xlsx") ? request.filename() : request.filename() + ".xlsx";
        return workbook(bytes, filename);
    }

    private ResponseEntity<Resource> workbook(byte[] bytes, String filename) {
        ByteArrayResource resource = new ByteArrayResource(bytes);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(XLSX)
                .contentLength(bytes.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }
}
