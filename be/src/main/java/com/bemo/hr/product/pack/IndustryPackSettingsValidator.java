package com.bemo.hr.product.pack;

import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndustryPackSettingsValidator {
    private final ObjectMapper objectMapper;

    public void validateSettings(String packCode, String settingsJson) {
        if (settingsJson == null || settingsJson.isBlank()) {
            return;
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(settingsJson);
        } catch (Exception ex) {
            log.warn("Invalid JSON in industry pack settings for packCode={}", packCode);
            throw new BusinessRuleException("INDUSTRY_PACK_SETTINGS_INVALID", "INDUSTRY_PACK_SETTINGS_INVALID", HttpStatus.BAD_REQUEST);
        }
        if (!root.isObject()) {
            throw new BusinessRuleException("INDUSTRY_PACK_SETTINGS_INVALID", "INDUSTRY_PACK_SETTINGS_INVALID", HttpStatus.BAD_REQUEST);
        }

        if ("FOOD_DISTRIBUTION_EG".equals(packCode)) {
            validateFoodDistributionSettings(root);
        } else if ("CONTRACTOR_WORKFORCE_EG".equals(packCode)) {
            validateWorkforceSettings(root);
        }
    }

    private void validateFoodDistributionSettings(JsonNode root) {
        if (root.has("dashboard") && !root.get("dashboard").isNull()) {
            String dashboard = root.get("dashboard").asText();
            if (!"foodDistribution".equalsIgnoreCase(dashboard)) {
                throw new BusinessRuleException("INDUSTRY_PACK_SETTINGS_INVALID", "INDUSTRY_PACK_SETTINGS_INVALID", HttpStatus.BAD_REQUEST);
            }
        }
        if (root.has("issuePolicy") && !root.get("issuePolicy").isNull()) {
            String policy = root.get("issuePolicy").asText();
            if (!"FEFO".equalsIgnoreCase(policy) && !"FIFO".equalsIgnoreCase(policy)) {
                throw new BusinessRuleException("INDUSTRY_PACK_SETTINGS_INVALID", "INDUSTRY_PACK_SETTINGS_INVALID", HttpStatus.BAD_REQUEST);
            }
        }
        if (root.has("creditControl") && !root.get("creditControl").isNull()) {
            if (!root.get("creditControl").isBoolean()) {
                throw new BusinessRuleException("INDUSTRY_PACK_SETTINGS_INVALID", "INDUSTRY_PACK_SETTINGS_INVALID", HttpStatus.BAD_REQUEST);
            }
        }
        if (root.has("expiryWindowsDays") && !root.get("expiryWindowsDays").isNull()) {
            JsonNode windows = root.get("expiryWindowsDays");
            if (!windows.isArray()) {
                throw new BusinessRuleException("INDUSTRY_PACK_SETTINGS_INVALID", "INDUSTRY_PACK_SETTINGS_INVALID", HttpStatus.BAD_REQUEST);
            }
            List<Integer> days = new ArrayList<>();
            for (JsonNode n : windows) {
                if (!n.isIntegralNumber() || n.asInt() <= 0) {
                    throw new BusinessRuleException("INDUSTRY_PACK_SETTINGS_INVALID", "INDUSTRY_PACK_SETTINGS_INVALID", HttpStatus.BAD_REQUEST);
                }
                days.add(n.asInt());
            }
            for (int i = 1; i < days.size(); i++) {
                if (days.get(i) <= days.get(i - 1)) {
                    throw new BusinessRuleException("INDUSTRY_PACK_SETTINGS_INVALID", "INDUSTRY_PACK_SETTINGS_INVALID", HttpStatus.BAD_REQUEST);
                }
            }
        }
    }

    private void validateWorkforceSettings(JsonNode root) {
        if (root.has("dashboard") && !root.get("dashboard").isNull()) {
            String dashboard = root.get("dashboard").asText();
            if (!"workforce".equalsIgnoreCase(dashboard)) {
                throw new BusinessRuleException("INDUSTRY_PACK_SETTINGS_INVALID", "INDUSTRY_PACK_SETTINGS_INVALID", HttpStatus.BAD_REQUEST);
            }
        }
    }
}
