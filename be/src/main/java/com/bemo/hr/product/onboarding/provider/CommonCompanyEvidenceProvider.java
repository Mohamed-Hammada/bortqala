package com.bemo.hr.product.onboarding.provider;

import com.bemo.hr.product.onboarding.OnboardingEvidenceProvider;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CommonCompanyEvidenceProvider implements OnboardingEvidenceProvider {
    private final TenantApplicationRepository applicationRepository;

    @Override
    public boolean supports(String packCode, String stepKey) {
        return "industryPack.step.company".equals(stepKey)
                || "industryPack.food.step.company".equals(stepKey)
                || "company".equalsIgnoreCase(stepKey);
    }

    @Override
    public EvidenceResult evaluate(String packCode, String stepKey) {
        String appId = TenantContext.require();
        Optional<TenantApplication> app = applicationRepository.findById(appId);
        boolean satisfied = app.isPresent()
                && app.get().isActive()
                && app.get().getName() != null
                && !app.get().getName().isBlank()
                && app.get().getCode() != null
                && !app.get().getCode().isBlank();
        return new EvidenceResult(
                satisfied,
                satisfied ? 1 : 0,
                true,
                "COMPANY",
                "onboarding.issue.company",
                "/settings"
        );
    }
}
