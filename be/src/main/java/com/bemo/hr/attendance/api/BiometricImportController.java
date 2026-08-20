package com.bemo.hr.attendance.api;

import com.bemo.hr.attendance.application.BiometricDeviceSyncService;
import com.bemo.hr.attendance.application.BiometricImportService;
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
    @PreAuthorize("@auth.hasPermission('imports.read')")
    List<ImportApi.BatchResponse> list() {
        return biometricImportService.listBatches();
    }

    @GetMapping("/preflight")
    @PreAuthorize("@auth.hasPermission('imports.read')")
    Map<String, Boolean> preflight(@RequestParam String sourceId, @RequestParam String checksum) {
        return Map.of("duplicate", biometricImportService.alreadyImported(sourceId, checksum));
    }

    @PostMapping(path = "/preview", consumes = "multipart/form-data")
    @PreAuthorize("@auth.hasPermission('imports.read')")
    ImportApi.PreviewResponse preview(@RequestParam MultipartFile file) {
        return biometricImportService.preview(file);
    }

    @PostMapping("/{id}/reverse")
    @PreAuthorize("@auth.hasPermission('imports.manage')")
    ImportApi.BatchResponse reverse(@org.springframework.web.bind.annotation.PathVariable String id,
                                    Authentication authentication) {
        return biometricImportService.reverse(id, authentication.getName());
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("@auth.hasPermission('imports.manage')")
    @ResponseStatus(HttpStatus.CREATED)
    ImportApi.BatchResponse upload(@RequestParam MultipartFile file,
                                   @RequestParam String sourceId,
                                   Authentication authentication) {
        return biometricImportService.importFile(file, sourceId, authentication.getName());
    }

    @GetMapping("/sources")
    @PreAuthorize("@auth.hasPermission('imports.read')")
    List<ImportApi.SourceResponse> sources() {
        return biometricDeviceSyncService.listSources();
    }

    @PostMapping("/sources")
    @PreAuthorize("@auth.hasPermission('imports.manage')")
    @ResponseStatus(HttpStatus.CREATED)
    ImportApi.SourceResponse createSource(@Valid @RequestBody ImportApi.SourceRequest request,
                                          Authentication authentication) {
        return biometricDeviceSyncService.createSource(request, authentication.getName());
    }

    @PutMapping("/sources/{id}")
    @PreAuthorize("@auth.hasPermission('imports.manage')")
    ImportApi.SourceResponse updateSource(@PathVariable String id,
                                          @Valid @RequestBody ImportApi.SourceRequest request,
                                          Authentication authentication) {
        return biometricDeviceSyncService.updateSource(id, request, authentication.getName());
    }

    @DeleteMapping("/sources/{id}")
    @PreAuthorize("@auth.hasPermission('imports.manage')")
    void deleteSource(@PathVariable String id,
                      Authentication authentication) {
        biometricDeviceSyncService.deleteSource(id, authentication.getName());
    }

    @GetMapping("/unmatched")
    @PreAuthorize("@auth.hasPermission('imports.read')")
    List<ImportApi.UnmatchedIdentityResponse> unmatched() {
        return biometricImportService.unmatchedIdentities();
    }

    @GetMapping("/devices")
    @PreAuthorize("@auth.hasPermission('imports.read')")
    List<ImportApi.DeviceResponse> devices() {
        return biometricDeviceSyncService.listDevices();
    }

    @PostMapping("/devices")
    @PreAuthorize("@auth.hasPermission('imports.manage')")
    @ResponseStatus(HttpStatus.CREATED)
    ImportApi.DeviceResponse createDevice(@Valid @RequestBody ImportApi.DeviceRequest request,
                                          Authentication authentication) {
        return biometricDeviceSyncService.create(request, authentication.getName());
    }

    @PutMapping("/devices/{id}")
    @PreAuthorize("@auth.hasPermission('imports.manage')")
    ImportApi.DeviceResponse updateDevice(@PathVariable String id,
                                          @Valid @RequestBody ImportApi.DeviceRequest request,
                                          Authentication authentication) {
        return biometricDeviceSyncService.update(id, request, authentication.getName());
    }

    @PostMapping("/devices/{id}/sync")
    @PreAuthorize("@auth.hasPermission('imports.manage')")
    ImportApi.DeviceSyncResponse syncDevice(@PathVariable String id,
                                            Authentication authentication) {
        return biometricDeviceSyncService.sync(id, authentication.getName());
    }
}
