package com.bemo.hr.reporting.api;

import com.bemo.hr.reporting.application.DataExportService;
import com.bemo.hr.reporting.application.ExcelExportOptions;
import com.bemo.hr.shared.i18n.TranslationService;
import com.bemo.hr.shared.security.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/exports")
@PreAuthorize("@auth.hasAnyPermission('reports.read', 'dashboard.view', 'finance.read', 'employees.read')")
@RequiredArgsConstructor
public class DataExportController {
    private final DataExportService dataExportService;
    private final AuthService authService;
    private final TranslationService translationService;

    @GetMapping("/{scope}.xlsx")
    ResponseEntity<byte[]> export(@PathVariable String scope,
                                  @RequestParam(required = false) Integer months,
                                  @RequestParam(required = false) Integer year,
                                  @RequestParam(required = false) Integer month,
                                  @RequestParam(name = "categoryId", required = false) String categoryId,
                                  Authentication authentication) {
        var preference = authService.currentPreferences(authentication.getName());
        var options = new ExcelExportOptions(preference.locale(), preference.excelTableStyle());
        int monthsCount = months == null ? 6 : Math.min(Math.max(months, 1), 24);
        var current = java.time.YearMonth.now();
        int y = (year != null && year >= 2000 && year <= 2100) ? year : current.getYear();
        int m = (month != null && month >= 1 && month <= 12) ? month : current.getMonthValue();
        byte[] body = switch (scope) {
            case "categories" -> dataExportService.categories(options);
            case "employees" -> dataExportService.employees(options);
            case "imports" -> dataExportService.imports(options);
            case "unmatched" -> dataExportService.unmatched(options);
            case "parties" -> dataExportService.parties(options);
            case "fixed-assets" -> dataExportService.fixedAssets(options);
            case "inventory-valuation" -> dataExportService.inventoryValuation(options);
            case "trends" -> dataExportService.trends(monthsCount, y, m, options);
            case "clock-in-histogram" -> dataExportService.clockInHistogram(monthsCount, categoryId, options);
            default -> throw new com.bemo.hr.shared.domain.NotFoundException("Export scope not found.");
        };
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        String localizedScope = preference.locale().startsWith("ar")
                ? translationService.translateOrDefault("export.file." + scope, preference.locale(), scope)
                : scope;
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(localizedScope + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".xlsx",
                        StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
