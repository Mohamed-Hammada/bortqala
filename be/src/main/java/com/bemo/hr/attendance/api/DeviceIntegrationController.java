package com.bemo.hr.attendance.api;

import com.bemo.hr.attendance.application.DeviceIntegrationService;
import com.bemo.hr.shared.security.Roles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

import java.util.List;

@RestController
@RequestMapping("/api/v1/device-integrations")
@RequiredArgsConstructor
public class DeviceIntegrationController {
    private final DeviceIntegrationService service;

    @GetMapping("/health")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_TEAM)
    JsonNode health() {
        return service.health();
    }

    @GetMapping("/suppliers")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_TEAM)
    JsonNode suppliers() {
        return service.suppliers();
    }

    @GetMapping("/suppliers/{vendor}/routes")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_TEAM)
    JsonNode routes(@PathVariable String vendor) {
        return service.routes(vendor);
    }

    @PostMapping("/resolve")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_TEAM)
    DeviceIntegrationApi.RouteResolution resolve(@Valid @RequestBody DeviceIntegrationApi.RouteRequest request) {
        return service.resolve(request);
    }

    @GetMapping
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_TEAM)
    List<DeviceIntegrationApi.DeviceResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_TEAM)
    DeviceIntegrationApi.DeviceResponse get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_MANAGER)
    @ResponseStatus(HttpStatus.CREATED)
    DeviceIntegrationApi.DeviceResponse create(
            @Valid @RequestBody DeviceIntegrationApi.DeviceRequest request,
            Authentication authentication) {
        return service.create(request, authentication.getName());
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_MANAGER)
    DeviceIntegrationApi.DeviceResponse update(
            @PathVariable String id,
            @Valid @RequestBody DeviceIntegrationApi.DeviceRequest request,
            Authentication authentication) {
        return service.update(id, request, authentication.getName());
    }

    @PostMapping("/{id}/probe")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_TEAM)
    DeviceIntegrationApi.ProbeResponse probe(@PathVariable String id, Authentication authentication) {
        return service.probe(id, authentication.getName());
    }

    @PostMapping("/{id}/sync")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_TEAM)
    ImportApi.DeviceSyncResponse sync(@PathVariable String id, Authentication authentication) {
        return service.sync(id, authentication.getName());
    }
}
