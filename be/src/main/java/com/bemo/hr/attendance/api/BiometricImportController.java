package com.bemo.hr.attendance.api;

import com.bemo.hr.attendance.application.BiometricDeviceSyncService;
import com.bemo.hr.attendance.application.BiometricImportService;
import com.bemo.hr.shared.security.Roles;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

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
    @PreAuthorize(Roles.ADMIN_HR_MANAGER_HR_REVIEWER)
    List<ImportApi.BatchResponse> list() {
        return biometricImportService.listBatches();
    }

    @GetMapping("/preflight")
    @PreAuthorize(Roles.ADMIN_HR_MANAGER_HR_REVIEWER)
    Map<String, Boolean> preflight(@RequestParam String sourceId, @RequestParam String checksum) {
        return Map.of("duplicate", biometricImportService.alreadyImported(sourceId, checksum));
    }

    @PostMapping(path = "/preview", consumes = "multipart/form-data")
    @PreAuthorize(Roles.ADMIN_HR_MANAGER_HR_REVIEWER)
    ImportApi.PreviewResponse preview(@RequestParam MultipartFile file) {
        return biometricImportService.preview(file);
    }

    @PostMapping("/{id}/reverse")
    @PreAuthorize(Roles.ADMIN_HR_MANAGER)
    ImportApi.BatchResponse reverse(@org.springframework.web.bind.annotation.PathVariable String id,
                                    Authentication authentication) {
        return biometricImportService.reverse(id, authentication.getName());
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize(Roles.ADMIN_HR_MANAGER_HR_REVIEWER)
    @ResponseStatus(HttpStatus.CREATED)
    ImportApi.BatchResponse upload(@RequestParam MultipartFile file,
                                   @RequestParam String sourceId,
                                   Authentication authentication) {
        return biometricImportService.importFile(file, sourceId, authentication.getName());
    }

    @GetMapping("/sources")
    @PreAuthorize(Roles.ADMIN_HR_MANAGER)
    List<ImportApi.SourceResponse> sources() {
        return biometricDeviceSyncService.listSources();
    }

    @PostMapping("/sources")
    @PreAuthorize(Roles.ADMIN_ONLY)
    @ResponseStatus(HttpStatus.CREATED)
    ImportApi.SourceResponse createSource(@Valid @RequestBody ImportApi.SourceRequest request,
                                          Authentication authentication) {
        return biometricDeviceSyncService.createSource(request, authentication.getName());
    }

    @PutMapping("/sources/{id}")
    @PreAuthorize(Roles.ADMIN_ONLY)
    ImportApi.SourceResponse updateSource(@PathVariable String id,
                                          @Valid @RequestBody ImportApi.SourceRequest request,
                                          Authentication authentication) {
        return biometricDeviceSyncService.updateSource(id, request, authentication.getName());
    }

    @DeleteMapping("/sources/{id}")
    @PreAuthorize(Roles.ADMIN_ONLY)
    void deleteSource(@PathVariable String id,
                      Authentication authentication) {
        biometricDeviceSyncService.deleteSource(id, authentication.getName());
    }

    @GetMapping("/unmatched")
    @PreAuthorize(Roles.ADMIN_HR_MANAGER_HR_REVIEWER)
    List<ImportApi.UnmatchedIdentityResponse> unmatched() {
        return biometricImportService.unmatchedIdentities();
    }

    @GetMapping("/devices")
    @PreAuthorize(Roles.ADMIN_HR_MANAGER)
    List<ImportApi.DeviceResponse> devices() {
        return biometricDeviceSyncService.listDevices();
    }

    @PostMapping("/devices")
    @PreAuthorize(Roles.ADMIN_HR_MANAGER)
    @ResponseStatus(HttpStatus.CREATED)
    ImportApi.DeviceResponse createDevice(@Valid @RequestBody ImportApi.DeviceRequest request,
                                          Authentication authentication) {
        return biometricDeviceSyncService.create(request, authentication.getName());
    }

    @PutMapping("/devices/{id}")
    @PreAuthorize(Roles.ADMIN_HR_MANAGER)
    ImportApi.DeviceResponse updateDevice(@PathVariable String id,
                                          @Valid @RequestBody ImportApi.DeviceRequest request,
                                          Authentication authentication) {
        return biometricDeviceSyncService.update(id, request, authentication.getName());
    }

    @PostMapping("/devices/{id}/sync")
    @PreAuthorize(Roles.ADMIN_HR_MANAGER_HR_REVIEWER)
    ImportApi.DeviceSyncResponse syncDevice(@PathVariable String id,
                                            Authentication authentication) {
        return biometricDeviceSyncService.sync(id, authentication.getName());
    }
}
