package com.bemo.hr.attendance.api;

import com.bemo.hr.attendance.application.DeviceIntegrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import java.util.List;

@RestController
@RequestMapping("/api/v1/device-integrations")
@RequiredArgsConstructor
public class DeviceIntegrationController {
    private final DeviceIntegrationService service;

    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER')")
    JsonNode health() {
        return service.health();
    }

    @GetMapping("/suppliers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER')")
    JsonNode suppliers() {
        return service.suppliers();
    }

    @GetMapping("/suppliers/{vendor}/routes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER')")
    JsonNode routes(@PathVariable String vendor) {
        return service.routes(vendor);
    }

    @PostMapping("/resolve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER')")
    DeviceIntegrationApi.RouteResolution resolve(@Valid @RequestBody DeviceIntegrationApi.RouteRequest request) {
        return service.resolve(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER')")
    List<DeviceIntegrationApi.DeviceResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER')")
    DeviceIntegrationApi.DeviceResponse get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    DeviceIntegrationApi.DeviceResponse create(
            @Valid @RequestBody DeviceIntegrationApi.DeviceRequest request,
            Authentication authentication) {
        return service.create(request, authentication.getName());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    DeviceIntegrationApi.DeviceResponse update(
            @PathVariable String id,
            @Valid @RequestBody DeviceIntegrationApi.DeviceRequest request,
            Authentication authentication) {
        return service.update(id, request, authentication.getName());
    }

    @PostMapping("/{id}/probe")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER')")
    DeviceIntegrationApi.ProbeResponse probe(@PathVariable String id, Authentication authentication) {
        return service.probe(id, authentication.getName());
    }

    @PostMapping("/{id}/sync")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER')")
    ImportApi.DeviceSyncResponse sync(@PathVariable String id, Authentication authentication) {
        return service.sync(id, authentication.getName());
    }
}
