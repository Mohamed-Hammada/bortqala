package com.bemo.hr.product.subscription;

import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SubscriptionLimitService {
    private final TenantSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public void assertCanAddUser(long currentUsers) {
        var subscription = subscriptionRepository.findFirstBy().orElse(null);
        if (subscription == null) return;
        if ("CANCELED".equals(subscription.getStatus()) || "PAST_DUE".equals(subscription.getStatus()))
            throw new BusinessRuleException("SUBSCRIPTION_INACTIVE", "SUBSCRIPTION_INACTIVE", HttpStatus.CONFLICT);
        var plan = planRepository.findById(subscription.getPlanCode()).orElse(null);
        if (plan == null) return;
        try {
            Map<?, ?> limits = objectMapper.readValue(plan.getLimitsJson(), Map.class);
            Object raw = limits.get("users");
            int maximum = raw instanceof Number n ? n.intValue() : 0;
            if (maximum > 0 && currentUsers >= maximum)
                throw new BusinessRuleException("SUBSCRIPTION_USER_LIMIT_REACHED", "SUBSCRIPTION_USER_LIMIT_REACHED", HttpStatus.CONFLICT, java.util.List.of(Integer.toString(maximum)));
        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessRuleException("SUBSCRIPTION_PLAN_CONFIGURATION_INVALID", "SUBSCRIPTION_PLAN_CONFIGURATION_INVALID", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
