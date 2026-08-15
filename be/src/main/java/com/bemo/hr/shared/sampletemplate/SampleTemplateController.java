package com.bemo.hr.shared.sampletemplate;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

@RestController
public class SampleTemplateController {
    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final SampleTemplateCatalog catalog;
    private final SampleTemplateWorkbookService workbookService;
    private static final Map<String,String> SMART = Map.of(
        "EMPLOYEE_MASTER","EMPLOYEE_MASTER", "CHART_OF_ACCOUNTS","CHART_OF_ACCOUNTS", "BUSINESS_PARTIES","BUSINESS_PARTIES", "INVENTORY_ITEMS","INVENTORY_ITEMS", "BOM_MASTER","BOM_MASTER");

    public SampleTemplateController(SampleTemplateCatalog catalog, SampleTemplateWorkbookService workbookService) { this.catalog = catalog; this.workbookService = workbookService; }

    @GetMapping("/api/v1/attendance/imports/sample-template")
    public ResponseEntity<byte[]> attendance(@RequestParam(defaultValue="xlsx") String format) {
        if (!"xlsx".equalsIgnoreCase(format)) return ResponseEntity.badRequest().build();
        return response("ATTENDANCE");
    }
    @GetMapping("/api/v1/smart-import/{entityType}/sample-template")
    public ResponseEntity<byte[]> smart(@PathVariable String entityType) {
        String key = SMART.get(entityType.toUpperCase(Locale.ROOT)); if (key == null) return ResponseEntity.notFound().build(); return response(key);
    }
    @GetMapping("/api/v1/workforce/imports/sample-template")
    public ResponseEntity<byte[]> workforce(@RequestParam String type) {
        String key = switch(type.toUpperCase(Locale.ROOT)) { case "WORKERS", "CONTRACTOR_WORKERS" -> "WORKERS"; case "ATTENDANCE", "WORKFORCE_ATTENDANCE" -> "WORKFORCE_ATTENDANCE"; default -> null; };
        return key == null ? ResponseEntity.badRequest().build() : response(key);
    }
    @GetMapping("/api/v1/finance/bank-reconciliation/sample-template") public ResponseEntity<byte[]> bank() { return response("BANK_STATEMENT"); }
    @GetMapping({"/api/v1/admin/translations/sample-template", "/api/v1/i18n/admin/translations/sample-template"}) public ResponseEntity<byte[]> translations() { return response("TRANSLATIONS"); }
    @GetMapping("/api/v1/parties/documents/sample-template") public ResponseEntity<byte[]> supplierDocuments() { return response("SUPPLIER_DOCUMENTS"); }

    private ResponseEntity<byte[]> response(String key) {
        var template = catalog.get(key); return ResponseEntity.ok().contentType(XLSX)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(template.fileName(), StandardCharsets.UTF_8).build().toString())
            .body(workbookService.create(template));
    }
}
