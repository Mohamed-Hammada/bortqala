package com.bemo.hr.workforce.api;

import com.bemo.hr.workforce.application.WorkforceExcelImportService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/workforce/excel-import")
public class WorkforceExcelImportController {
    private final WorkforceExcelImportService service;

    public WorkforceExcelImportController(@org.springframework.beans.factory.annotation.Qualifier("workforceMasterDataExcelImportService") WorkforceExcelImportService service) {
        this.service = service;
    }

    @PostMapping(value = "/{kind}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public WorkforceExcelImportService.ImportResult importExcel(
            @PathVariable String kind,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "USER_CODE") String identityMode) {
        return service.importWorkbook(kind, file, identityMode);
    }
}
