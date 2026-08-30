package com.bemo.hr.product.onboarding;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OnboardingEvidenceRegistry {
    private final List<OnboardingEvidenceProvider> providers;

    public Optional<OnboardingEvidenceProvider.EvidenceResult> evaluate(String packCode, String stepKey) {
        for (OnboardingEvidenceProvider provider : providers) {
            if (provider.supports(packCode, stepKey)) {
                return Optional.of(provider.evaluate(packCode, stepKey));
            }
        }
        return Optional.empty();
    }
}
