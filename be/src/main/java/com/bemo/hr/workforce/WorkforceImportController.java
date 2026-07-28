package com.bemo.hr.workforce;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/workforce/import")
@RequiredArgsConstructor
public class WorkforceImportController {
    private final WorkforceExcelImportService importService;

    @PostMapping("/analyze")
    public WorkforceExcelImportService.ImportDiagnosticResult analyzeImport(
        @RequestParam(required = false) BigDecimal summaryDays,
        @RequestParam(required = false) BigDecimal settlementDays
    ) {
        return importService.analyzeExcelImport(summaryDays, settlementDays);
    }
}
