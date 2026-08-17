package com.bemo.hr.workforce.api;

import com.bemo.hr.workforce.application.WorkforceMasterDataExcelImportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/workforce/excel-import")
public class WorkforceExcelImportController {
    private final WorkforceMasterDataExcelImportService service;

    public WorkforceExcelImportController(WorkforceMasterDataExcelImportService service) {
        this.service = service;
    }

    @PostMapping(value = "/{kind}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public WorkforceMasterDataExcelImportService.ImportResult importExcel(
            @PathVariable String kind,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "USER_CODE") String identityMode) {
        return service.importWorkbook(kind, file, identityMode);
    }

    @GetMapping(value = "/template/{kind}", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable String kind) {
        byte[] content = service.templateWorkbook(kind);
        String safeKind = "contractors".equalsIgnoreCase(kind) ? "contractors" : "workers";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=workforce-" + safeKind + "-import-template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }
}
