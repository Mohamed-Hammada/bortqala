package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.FxRevaluationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/fx-revaluation")
@PreAuthorize("@auth.hasPermission('finance.read')")
public class FxRevaluationController {

    private final FxRevaluationService fxRevaluationService;

    public FxRevaluationController(FxRevaluationService fxRevaluationService) {
        this.fxRevaluationService = fxRevaluationService;
    }

    @PostMapping("/run")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public FxRevaluationApi.RevaluationRunResponse runRevaluation(
            @RequestParam long asOf,
            Authentication authentication) {
        LocalDate asOfDate = Instant.ofEpochMilli(asOf).atZone(ZoneOffset.UTC).toLocalDate();
        return fxRevaluationService.runRevaluation(asOfDate, authentication.getName());
    }

    @GetMapping("/history")
    public List<FxRevaluationApi.RevaluationResponse> getHistory() {
        return fxRevaluationService.getHistory();
    }
}
