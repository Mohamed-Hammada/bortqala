package com.bemo.hr.attendance.api;

import com.bemo.hr.attendance.application.BiometricImportService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/imports")
public class BiometricImportController {
    private final BiometricImportService biometricImportService;

    public BiometricImportController(BiometricImportService biometricImportService) {
        this.biometricImportService = biometricImportService;
    }

    @GetMapping
    List<ImportApi.BatchResponse> list() { return biometricImportService.listBatches(); }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'HR_REVIEWER')")
    @ResponseStatus(HttpStatus.CREATED)
    ImportApi.BatchResponse upload(@RequestParam MultipartFile file,
                                   @RequestParam String deviceName,
                                   @RequestParam(defaultValue = "HR User") String actor) {
        return biometricImportService.importFile(file, deviceName, actor);
    }

    @GetMapping("/unmatched")
    List<ImportApi.UnmatchedIdentityResponse> unmatched() { return biometricImportService.unmatchedIdentities(); }
}
