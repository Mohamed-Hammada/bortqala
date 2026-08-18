package com.bemo.hr.product.pack;

import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class IndustryKpiRegistry {
    private final Map<String, IndustryKpiProvider> providers = new HashMap<>();

    public IndustryKpiRegistry(List<IndustryKpiProvider> customProviders) {
        if (customProviders != null) {
            for (var p : customProviders) {
                providers.put(p.key(), p);
            }
        }
        registerDefaultKpis();
    }

    private void registerDefaultKpis() {
        registerIfAbsent("contractorFillRate", "%");
        registerIfAbsent("attendanceExceptionRate", "%");
        registerIfAbsent("settlementVariance", "EGP");
        registerIfAbsent("contractorReliability", "%");
        registerIfAbsent("expiryRiskValue", "EGP");
        registerIfAbsent("stockoutItems", "count");
        registerIfAbsent("slowMovingInventory", "count");
        registerIfAbsent("salesByRoute", "EGP");
        registerIfAbsent("grossMarginByRoute", "%");
        registerIfAbsent("customerOverdue", "EGP");
        registerIfAbsent("deliverySuccessRate", "%");
        registerIfAbsent("returnRate", "%");
        registerIfAbsent("fillRate", "%");
    }

    private void registerIfAbsent(String key, String unit) {
        providers.putIfAbsent(key, new IndustryKpiProvider() {
            @Override
            public String key() {
                return key;
            }

            @Override
            public KpiResult calculate() {
                return new KpiResult(key, "kpi." + key, 0.0, unit, "ACTIVE");
            }
        });
    }

    public boolean supports(String key) {
        return providers.containsKey(key);
    }

    public void validateKpiKeys(List<String> keys) {
        if (keys != null) {
            for (String key : keys) {
                if (!supports(key)) {
                    log.warn("Unknown KPI key in industry pack: {}", key);
                    throw new BusinessRuleException("INDUSTRY_PACK_KPI_UNKNOWN", "INDUSTRY_PACK_KPI_UNKNOWN", HttpStatus.BAD_REQUEST);
                }
            }
        }
    }

    public List<IndustryKpiProvider.KpiResult> calculate(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        List<IndustryKpiProvider.KpiResult> results = new ArrayList<>();
        for (String key : keys) {
            var provider = providers.get(key);
            if (provider != null) {
                results.add(provider.calculate());
            }
        }
        return results;
    }
}
