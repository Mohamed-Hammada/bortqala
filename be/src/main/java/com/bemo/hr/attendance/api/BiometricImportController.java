package com.bemo.hr.attendance.api;

import com.bemo.hr.attendance.application.BiometricImportService;
import com.bemo.hr.attendance.application.BiometricDeviceSyncService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER')")
    List<ImportApi.BatchResponse> list() { return biometricImportService.listBatches(); }

    @GetMapping("/preflight")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER')")
    Map<String, Boolean> preflight(@RequestParam String sourceId, @RequestParam String checksum) {
        return Map.of("duplicate", biometricImportService.alreadyImported(sourceId, checksum));
    }

    @PostMapping(path = "/preview", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER')")
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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER')")
    @ResponseStatus(HttpStatus.CREATED)
    ImportApi.BatchResponse upload(@RequestParam MultipartFile file,
                                   @RequestParam String sourceId,
                                   Authentication authentication) {
        return biometricImportService.importFile(file, sourceId, authentication.getName());
    }

    @GetMapping("/sources")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    List<ImportApi.SourceResponse> sources() {
        return biometricDeviceSyncService.listSources();
    }

    @PostMapping("/sources")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    ImportApi.SourceResponse createSource(@Valid @RequestBody ImportApi.SourceRequest request,
                                          Authentication authentication) {
        return biometricDeviceSyncService.createSource(request, authentication.getName());
    }

    @PutMapping("/sources/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    ImportApi.SourceResponse updateSource(@PathVariable String id,
                                          @Valid @RequestBody ImportApi.SourceRequest request,
                                          Authentication authentication) {
        return biometricDeviceSyncService.updateSource(id, request, authentication.getName());
    }

    @DeleteMapping("/sources/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    void deleteSource(@PathVariable String id,
                      Authentication authentication) {
        biometricDeviceSyncService.deleteSource(id, authentication.getName());
    }

    @GetMapping("/unmatched")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER')")
    List<ImportApi.UnmatchedIdentityResponse> unmatched() { return biometricImportService.unmatchedIdentities(); }

    @GetMapping("/devices")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    List<ImportApi.DeviceResponse> devices() {
        return biometricDeviceSyncService.listDevices();
    }

    @PostMapping("/devices")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    ImportApi.DeviceResponse createDevice(@Valid @RequestBody ImportApi.DeviceRequest request,
                                           Authentication authentication) {
        return biometricDeviceSyncService.create(request, authentication.getName());
    }

    @PutMapping("/devices/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    ImportApi.DeviceResponse updateDevice(@PathVariable String id,
                                           @Valid @RequestBody ImportApi.DeviceRequest request,
                                           Authentication authentication) {
        return biometricDeviceSyncService.update(id, request, authentication.getName());
    }

    @PostMapping("/devices/{id}/sync")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER')")
    ImportApi.DeviceSyncResponse syncDevice(@PathVariable String id,
                                             Authentication authentication) {
        return biometricDeviceSyncService.sync(id, authentication.getName());
    }
}
