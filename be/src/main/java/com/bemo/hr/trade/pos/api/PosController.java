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
    @PreAuthorize("@auth.hasAnyPermission('pos.read', 'sales.read')")
    public List<PosApi.TerminalResponse> listTerminals() {
        return posService.listTerminals();
    }

    @PostMapping("/terminals")
    @PreAuthorize("@auth.hasAnyPermission('pos.manage', 'sales.manage')")
    public PosApi.TerminalResponse saveTerminal(@RequestBody PosApi.SaveTerminalRequest request) {
        return posService.saveTerminal(request);
    }

    @GetMapping("/sessions")
    @PreAuthorize("@auth.hasAnyPermission('pos.read', 'sales.read')")
    public List<PosApi.SessionResponse> listSessions() {
        return posService.listSessions();
    }

    @GetMapping("/sessions/active")
    @PreAuthorize("@auth.hasAnyPermission('pos.read', 'sales.read')")
    public ResponseEntity<PosApi.SessionResponse> getActiveSession(@RequestParam String terminalId) {
        return posService.getActiveSession(terminalId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/sessions/open")
    @PreAuthorize("@auth.hasAnyPermission('pos.operate', 'sales.manage')")
    public PosApi.SessionResponse openSession(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody PosApi.OpenSessionRequest request
    ) {
        String userId = jwt != null ? jwt.getSubject() : "SYSTEM";
        return posService.openSession(userId, request);
    }

    @PostMapping("/sessions/{id}/close")
    @PreAuthorize("@auth.hasAnyPermission('pos.operate', 'sales.manage')")
    public PosApi.SessionResponse closeSession(
            @PathVariable String id,
            @RequestBody PosApi.CloseSessionRequest request
    ) {
        return posService.closeSession(id, request);
    }

    @PostMapping("/transactions/sale")
    @PreAuthorize("@auth.hasAnyPermission('pos.operate', 'sales.manage')")
    public PosApi.TransactionResponse processSale(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody PosApi.ProcessSaleRequest request
    ) {
        String userId = jwt != null ? jwt.getSubject() : "SYSTEM";
        return posService.processSale(userId, request);
    }

    @PostMapping("/transactions/return")
    @PreAuthorize("@auth.hasAnyPermission('pos.operate', 'sales.manage')")
    public PosApi.TransactionResponse processReturn(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody PosApi.ProcessReturnRequest request
    ) {
        String userId = jwt != null ? jwt.getSubject() : "SYSTEM";
        return posService.processReturn(userId, request);
    }

    @GetMapping("/transactions")
    @PreAuthorize("@auth.hasAnyPermission('pos.read', 'sales.read')")
    public List<PosApi.TransactionResponse> listTransactions() {
        return posService.listTransactions();
    }

    @GetMapping("/summary")
    @PreAuthorize("@auth.hasAnyPermission('pos.read', 'sales.read')")
    public PosApi.PosSummaryResponse getSummary() {
        return posService.getSummary();
    }
}
