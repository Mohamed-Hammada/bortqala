package com.bemo.hr.approval;

import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApprovalEscalationScheduler {
    private static final Logger log = LoggerFactory.getLogger(ApprovalEscalationScheduler.class);
    private final TenantApplicationRepository tenantApplicationRepository;
    private final ApprovalWorkflowService approvalWorkflowService;

    @Scheduled(fixedDelayString = "${hr.approval.escalation-scan-ms:300000}",
            initialDelayString = "${hr.approval.escalation-initial-delay-ms:60000}")
    public void escalateDueApprovals() {
        tenantApplicationRepository.findAll().stream().filter(app -> app.isActive()).forEach(app -> {
            TenantContext.set(app.getId());
            try {
                int count = approvalWorkflowService.escalateOverdue();
                if (count > 0) log.info("Escalated {} approval instances for tenant {}", count, app.getCode());
            } catch (RuntimeException ex) {
                log.warn("Approval escalation failed for tenant {}", app.getCode(), ex);
            } finally {
                TenantContext.clear();
            }
        });
    }
}
