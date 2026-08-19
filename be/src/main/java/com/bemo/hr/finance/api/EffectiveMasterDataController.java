package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.EffectiveMasterDataService;
import com.bemo.hr.finance.domain.EffectiveMasterValue;
import com.bemo.hr.shared.security.Roles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/effective-master-data")
@PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.ACCOUNTANT + " or " + Roles.AUDITOR + " or " + Roles.FINANCE_MANAGER)
public class EffectiveMasterDataController {
    private final EffectiveMasterDataService s;

    public EffectiveMasterDataController(EffectiveMasterDataService s) {
        this.s = s;
    }

    @PostMapping
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER)
    public EffectiveMasterValue add(@RequestBody Payload p, Authentication a) {
        return s.add(p.masterType(), p.masterId(), p.valueKey(), p.valueText(), LocalDate.parse(p.effectiveFrom()), p.effectiveTo() == null ? null : LocalDate.parse(p.effectiveTo()), p.reason(), a.getName());
    }

    @GetMapping("/resolve")
    public EffectiveMasterValue resolve(@RequestParam String masterType, @RequestParam String masterId, @RequestParam String valueKey, @RequestParam LocalDate date) {
        return s.resolve(masterType, masterId, valueKey, date);
    }

    @GetMapping("/history")
    public List<EffectiveMasterValue> history(@RequestParam String masterType, @RequestParam String masterId, @RequestParam String valueKey) {
        return s.history(masterType, masterId, valueKey);
    }

    public record Payload(String masterType, String masterId, String valueKey, String valueText, String effectiveFrom,
                          String effectiveTo, String reason) {
    }
}
