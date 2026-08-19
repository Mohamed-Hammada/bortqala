package com.bemo.hr.product.risk;

import com.bemo.hr.shared.security.Roles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/risk-scores")
@RequiredArgsConstructor
@PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.PROCUREMENT_MANAGER + " or " + Roles.WORKFORCE_MANAGER)
public class PartnerRiskScoreController {
    private final PartnerRiskScoreService service;

    @GetMapping
    List<PartnerRiskApi.ScoreResponse> scores() {
        return service.scores();
    }

    @PostMapping("/refresh")
    List<PartnerRiskApi.ScoreResponse> refresh(@Valid @RequestBody PartnerRiskApi.RefreshRequest request, Authentication auth) {
        return service.refresh(request, auth.getName());
    }

    @GetMapping("/rules")
    PartnerRiskApi.RuleResponse rule() {
        return service.rule();
    }

    @PutMapping("/rules")
    @PreAuthorize(Roles.ADMIN_ONLY)
    PartnerRiskApi.RuleResponse rule(@Valid @RequestBody PartnerRiskApi.RuleRequest request, Authentication auth) {
        return service.updateRule(request, auth.getName());
    }
}
