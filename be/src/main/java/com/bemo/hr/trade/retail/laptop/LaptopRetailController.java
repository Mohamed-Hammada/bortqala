package com.bemo.hr.trade.retail.laptop;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/retail/laptops")
public class LaptopRetailController {

    private final LaptopRetailService laptopRetailService;

    public LaptopRetailController(LaptopRetailService laptopRetailService) {
        this.laptopRetailService = laptopRetailService;
    }

    @PostMapping("/devices")
    @PreAuthorize("@auth.hasAnyPermission('pos:manage', 'sales:manage', 'inventory:manage')")
    @ResponseStatus(HttpStatus.CREATED)
    public LaptopRetailApi.SerializedDeviceResponse registerDevice(
            @Valid @RequestBody LaptopRetailApi.RegisterDeviceRequest request,
            Authentication authentication
    ) {
        SerializedDevice device = laptopRetailService.registerDevice(authentication.getName(), request);
        return LaptopRetailApi.SerializedDeviceResponse.from(device);
    }

    @GetMapping("/devices")
    @PreAuthorize("@auth.hasAnyPermission('pos:read', 'sales:read', 'inventory:read')")
    public List<LaptopRetailApi.SerializedDeviceResponse> listDevices(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String brand
    ) {
        return laptopRetailService.listDevices(status, brand).stream()
                .map(LaptopRetailApi.SerializedDeviceResponse::from)
                .toList();
    }

    @GetMapping("/devices/{serialNumber}")
    @PreAuthorize("@auth.hasAnyPermission('pos:read', 'sales:read', 'inventory:read')")
    public LaptopRetailApi.SerializedDeviceResponse getDevice(@PathVariable String serialNumber) {
        SerializedDevice device = laptopRetailService.getDeviceBySerial(serialNumber);
        return LaptopRetailApi.SerializedDeviceResponse.from(device);
    }

    @PostMapping("/devices/{id}/sell")
    @PreAuthorize("@auth.hasAnyPermission('pos:manage', 'sales:manage')")
    public LaptopRetailApi.SerializedDeviceResponse sellDevice(
            @PathVariable String id,
            @Valid @RequestBody LaptopRetailApi.SellDeviceRequest request,
            Authentication authentication
    ) {
        SerializedDevice device = laptopRetailService.sellDevice(authentication.getName(), id, request);
        return LaptopRetailApi.SerializedDeviceResponse.from(device);
    }

    @PostMapping("/devices/{id}/return")
    @PreAuthorize("@auth.hasAnyPermission('pos:manage', 'sales:manage')")
    public LaptopRetailApi.SerializedDeviceResponse returnDevice(
            @PathVariable String id,
            @RequestBody(required = false) LaptopRetailApi.ReturnDeviceRequest request,
            Authentication authentication
    ) {
        String reason = request != null ? request.reason() : "CUSTOMER_RETURN";
        SerializedDevice device = laptopRetailService.returnDevice(authentication.getName(), id, reason);
        return LaptopRetailApi.SerializedDeviceResponse.from(device);
    }

    @PostMapping("/repairs")
    @PreAuthorize("@auth.hasAnyPermission('pos:manage', 'sales:manage', 'services:manage')")
    @ResponseStatus(HttpStatus.CREATED)
    public LaptopRetailApi.RepairTicketResponse createRepairTicket(
            @Valid @RequestBody LaptopRetailApi.CreateRepairTicketRequest request,
            Authentication authentication
    ) {
        DeviceRepairTicket ticket = laptopRetailService.createRepairTicket(authentication.getName(), request);
        return LaptopRetailApi.RepairTicketResponse.from(ticket);
    }

    @GetMapping("/repairs")
    @PreAuthorize("@auth.hasAnyPermission('pos:read', 'sales:read', 'services:read')")
    public List<LaptopRetailApi.RepairTicketResponse> listRepairs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String serialNumber
    ) {
        return laptopRetailService.listRepairTickets(status, serialNumber).stream()
                .map(LaptopRetailApi.RepairTicketResponse::from)
                .toList();
    }

    @PutMapping("/repairs/{id}/status")
    @PreAuthorize("@auth.hasAnyPermission('pos:manage', 'sales:manage', 'services:manage')")
    public LaptopRetailApi.RepairTicketResponse updateRepairStatus(
            @PathVariable String id,
            @RequestBody LaptopRetailApi.UpdateRepairStatusRequest request,
            Authentication authentication
    ) {
        DeviceRepairTicket ticket = laptopRetailService.updateRepairStatus(authentication.getName(), id, request);
        return LaptopRetailApi.RepairTicketResponse.from(ticket);
    }
}
