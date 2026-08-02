package com.bemo.hr.attendance.api;

import com.bemo.hr.attendance.application.BiometricImportService;
import com.bemo.hr.attendance.application.BiometricDeviceSyncService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
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
    private final BiometricDeviceSyncService biometricDeviceSyncService;

    public BiometricImportController(BiometricImportService biometricImportService,
                                     BiometricDeviceSyncService biometricDeviceSyncService) {
        this.biometricImportService = biometricImportService;
        this.biometricDeviceSyncService = biometricDeviceSyncService;
    }

    @GetMapping
    List<ImportApi.BatchResponse> list() { return biometricImportService.listBatches(); }

    @PostMapping(path = "/preview", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'HR_REVIEWER')")
    ImportApi.PreviewResponse preview(@RequestParam MultipartFile file) {
        return biometricImportService.preview(file);
    }

    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    ImportApi.BatchResponse reverse(@org.springframework.web.bind.annotation.PathVariable String id,
                                    Authentication authentication) {
        return biometricImportService.reverse(id, authentication.getName());
    }

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

    @GetMapping("/devices")
    List<ImportApi.DeviceResponse> devices() {
        return biometricDeviceSyncService.listDevices();
    }

    @PostMapping("/devices")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    ImportApi.DeviceResponse createDevice(@Valid @org.springframework.web.bind.annotation.RequestBody ImportApi.DeviceRequest request,
                                           Authentication authentication) {
        return biometricDeviceSyncService.create(request, authentication.getName());
    }

    @org.springframework.web.bind.annotation.PutMapping("/devices/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    ImportApi.DeviceResponse updateDevice(@org.springframework.web.bind.annotation.PathVariable String id,
                                           @Valid @org.springframework.web.bind.annotation.RequestBody ImportApi.DeviceRequest request,
                                           Authentication authentication) {
        return biometricDeviceSyncService.update(id, request, authentication.getName());
    }

    @PostMapping("/devices/{id}/sync")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER')")
    ImportApi.DeviceSyncResponse syncDevice(@org.springframework.web.bind.annotation.PathVariable String id,
                                             Authentication authentication) {
        return biometricDeviceSyncService.sync(id, authentication.getName());
    }
}
