package com.bemo.hr.trade.pos.api;

import com.bemo.hr.trade.pos.application.PosService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trade/pos")
public class PosController {

    private final PosService posService;

    public PosController(PosService posService) {
        this.posService = posService;
    }

    @GetMapping("/terminals")
    @PreAuthorize("hasAuthority('pos.read') or hasAuthority('sales.read') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public List<PosApi.TerminalResponse> listTerminals() {
        return posService.listTerminals();
    }

    @PostMapping("/terminals")
    @PreAuthorize("hasAuthority('pos.manage') or hasAuthority('sales.manage') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public PosApi.TerminalResponse saveTerminal(@RequestBody PosApi.SaveTerminalRequest request) {
        return posService.saveTerminal(request);
    }

    @GetMapping("/sessions")
    @PreAuthorize("hasAuthority('pos.read') or hasAuthority('sales.read') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public List<PosApi.SessionResponse> listSessions() {
        return posService.listSessions();
    }

    @GetMapping("/sessions/active")
    @PreAuthorize("hasAuthority('pos.read') or hasAuthority('sales.read') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<PosApi.SessionResponse> getActiveSession(@RequestParam String terminalId) {
        return posService.getActiveSession(terminalId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/sessions/open")
    @PreAuthorize("hasAuthority('pos.operate') or hasAuthority('sales.write') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public PosApi.SessionResponse openSession(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody PosApi.OpenSessionRequest request
    ) {
        String userId = jwt != null ? jwt.getSubject() : "SYSTEM";
        return posService.openSession(userId, request);
    }

    @PostMapping("/sessions/{id}/close")
    @PreAuthorize("hasAuthority('pos.operate') or hasAuthority('sales.write') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public PosApi.SessionResponse closeSession(
            @PathVariable String id,
            @RequestBody PosApi.CloseSessionRequest request
    ) {
        return posService.closeSession(id, request);
    }

    @PostMapping("/transactions/sale")
    @PreAuthorize("hasAuthority('pos.operate') or hasAuthority('sales.write') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public PosApi.TransactionResponse processSale(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody PosApi.ProcessSaleRequest request
    ) {
        String userId = jwt != null ? jwt.getSubject() : "SYSTEM";
        return posService.processSale(userId, request);
    }

    @PostMapping("/transactions/return")
    @PreAuthorize("hasAuthority('pos.operate') or hasAuthority('sales.write') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public PosApi.TransactionResponse processReturn(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody PosApi.ProcessReturnRequest request
    ) {
        String userId = jwt != null ? jwt.getSubject() : "SYSTEM";
        return posService.processReturn(userId, request);
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAuthority('pos.read') or hasAuthority('sales.read') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public List<PosApi.TransactionResponse> listTransactions() {
        return posService.listTransactions();
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('pos.read') or hasAuthority('sales.read') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public PosApi.PosSummaryResponse getSummary() {
        return posService.getSummary();
    }
}
