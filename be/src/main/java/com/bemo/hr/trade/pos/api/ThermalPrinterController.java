package com.bemo.hr.trade.pos.api;

import com.bemo.hr.trade.pos.application.ThermalPrintService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trade/pos")
public class ThermalPrinterController {

    private final ThermalPrintService thermalPrintService;

    public ThermalPrinterController(ThermalPrintService thermalPrintService) {
        this.thermalPrintService = thermalPrintService;
    }

    @GetMapping("/printers")
    @PreAuthorize("@auth.hasAnyPermission('pos.read', 'sales.read')")
    public List<ThermalPrinterApi.PrinterResponse> listPrinters() {
        return thermalPrintService.listPrinters();
    }

    @GetMapping("/printers/{id}")
    @PreAuthorize("@auth.hasAnyPermission('pos.read', 'sales.read')")
    public ThermalPrinterApi.PrinterResponse getPrinter(@PathVariable String id) {
        return thermalPrintService.getPrinter(id);
    }

    @PostMapping("/printers")
    @PreAuthorize("@auth.hasAnyPermission('pos.manage', 'sales.manage')")
    public ThermalPrinterApi.PrinterResponse savePrinter(@RequestBody ThermalPrinterApi.SavePrinterRequest request) {
        return thermalPrintService.savePrinter(request);
    }

    @DeleteMapping("/printers/{id}")
    @PreAuthorize("@auth.hasAnyPermission('pos.manage', 'sales.manage')")
    public ResponseEntity<Void> deletePrinter(@PathVariable String id) {
        thermalPrintService.deletePrinter(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/printers/{id}/test-print")
    @PreAuthorize("@auth.hasAnyPermission('pos.operate', 'pos.manage')")
    public ThermalPrinterApi.TestPrintResponse testPrint(@PathVariable String id) {
        return thermalPrintService.testPrint(id);
    }

    @GetMapping("/transactions/{id}/receipt-escpos")
    @PreAuthorize("@auth.hasAnyPermission('pos.read', 'pos.operate', 'sales.read')")
    public ThermalPrinterApi.ReceiptPrintDataResponse getReceiptEscPos(
            @PathVariable String id,
            @RequestParam(required = false) String printerId
    ) {
        return thermalPrintService.generateReceiptBytes(id, printerId, false);
    }

    @PostMapping("/transactions/{id}/reprint")
    @PreAuthorize("@auth.hasAnyPermission('pos.operate', 'pos.manage', 'sales.manage')")
    public ThermalPrinterApi.ReceiptPrintDataResponse reprintReceipt(
            @PathVariable String id,
            @RequestBody ThermalPrinterApi.ReprintReceiptRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest servletRequest
    ) {
        String username = jwt != null ? jwt.getSubject() : "SYSTEM";
        String ipAddress = servletRequest != null ? servletRequest.getRemoteAddr() : "127.0.0.1";
        return thermalPrintService.reprintReceipt(id, request, username, ipAddress);
    }
}
