package com.bemo.hr.reporting.api;

import com.bemo.hr.reporting.application.DataExportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/exports")
public class DataExportController {
    private final DataExportService dataExportService;
    public DataExportController(DataExportService dataExportService) { this.dataExportService = dataExportService; }

    @GetMapping("/{scope}.xlsx")
    ResponseEntity<byte[]> export(@PathVariable String scope) {
        byte[] body = switch (scope) {
            case "categories" -> dataExportService.categories(); case "employees" -> dataExportService.employees();
            case "imports" -> dataExportService.imports(); case "unmatched" -> dataExportService.unmatched();
            default -> throw new com.bemo.hr.shared.domain.NotFoundException("Export scope not found.");
        };
        var headers = new HttpHeaders(); headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(scope + ".xlsx", StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
