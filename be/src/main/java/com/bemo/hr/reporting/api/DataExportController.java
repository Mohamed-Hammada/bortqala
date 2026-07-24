package com.bemo.hr.reporting.api;

import com.bemo.hr.reporting.application.DataExportService;
import com.bemo.hr.reporting.application.ExcelExportOptions;
import com.bemo.hr.shared.security.AuthService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/exports")
@RequiredArgsConstructor
public class DataExportController {
    private final DataExportService dataExportService;
    private final AuthService authService;
    @GetMapping("/{scope}.xlsx")
    ResponseEntity<byte[]> export(@PathVariable String scope, Authentication authentication) {
        var preference = authService.currentPreferences(authentication.getName());
        var options = new ExcelExportOptions(preference.locale(), preference.excelTableStyle());
        byte[] body = switch (scope) {
            case "categories" -> dataExportService.categories(options); case "employees" -> dataExportService.employees(options);
            case "imports" -> dataExportService.imports(options); case "unmatched" -> dataExportService.unmatched(options);
            case "parties" -> dataExportService.parties(options);
            default -> throw new com.bemo.hr.shared.domain.NotFoundException("Export scope not found.");
        };
        var headers = new HttpHeaders(); headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        String localizedScope = preference.locale().startsWith("ar") ? switch (scope) {
            case "categories" -> "الفئات"; case "employees" -> "الموظفون";
            case "imports" -> "سجل-الاستيراد"; case "unmatched" -> "هويات-غير-مربوطة";
            case "parties" -> "جهات-التعامل";
            default -> scope;
        } : scope;
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(localizedScope + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".xlsx",
                        StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
