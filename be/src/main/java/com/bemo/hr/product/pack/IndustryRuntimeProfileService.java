package com.bemo.hr.product.pack;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndustryRuntimeProfileService {
    private final TenantIndustryPackRepository tenantPackRepository;
    private final IndustryPackRepository packRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public EffectiveIndustryProfile getEffectiveProfile() {
        List<TenantIndustryPack> installedList = tenantPackRepository.findAll();
        if (installedList.isEmpty()) {
            return defaultProfile();
        }

        TenantIndustryPack primary = installedList.get(0);
        IndustryPack definition = packRepository.findById(primary.getPackId()).orElse(null);
        String packCode = definition != null ? definition.getCode() : "UNKNOWN";

        String effectiveJson = primary.getSettingsJson();
        String dashboard = "default";
        String issuePolicy = "FIFO";
        boolean creditControl = false;
        List<Integer> expiryWindows = List.of(7, 30, 60);
        Map<String, String> terminology = new HashMap<>();
        Map<String, Object> raw = new HashMap<>();

        try {
            if (effectiveJson != null && !effectiveJson.isBlank()) {
                JsonNode node = objectMapper.readTree(effectiveJson);
                if (node.has("dashboard")) dashboard = node.get("dashboard").asText();
                if (node.has("issuePolicy")) issuePolicy = node.get("issuePolicy").asText();
                if (node.has("creditControl")) creditControl = node.get("creditControl").asBoolean();
                if (node.has("expiryWindowsDays") && node.get("expiryWindowsDays").isArray()) {
                    List<Integer> days = new ArrayList<>();
                    node.get("expiryWindowsDays").forEach(n -> days.add(n.asInt()));
                    expiryWindows = days;
                }
                if (node.has("terminology") && node.get("terminology").isObject()) {
                    terminology = objectMapper.convertValue(node.get("terminology"), new TypeReference<Map<String, String>>() {});
                }
                raw = objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception ex) {
            log.warn("Error parsing effective profile settings json", ex);
        }

        return new EffectiveIndustryProfile(
                packCode,
                primary.getInstalledVersion(),
                dashboard,
                issuePolicy,
                creditControl,
                expiryWindows,
                terminology,
                raw
        );
    }

    private EffectiveIndustryProfile defaultProfile() {
        return new EffectiveIndustryProfile(
                "NONE",
                0,
                "default",
                "FIFO",
                false,
                List.of(7, 30, 60),
                Map.of(),
                Map.of()
        );
    }

    public record EffectiveIndustryProfile(
            String packCode,
            int installedVersion,
            String dashboard,
            String issuePolicy,
            boolean creditControl,
            List<Integer> expiryWindowsDays,
            Map<String, String> terminology,
            Map<String, Object> rawSettings
    ) {}
}
