package com.bemo.hr.workforce;

import com.bemo.hr.audit.application.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkforceDisputeServiceTests {

    private WorkforceDisputeRepository disputeRepository;
    private WorkforceDisputeService disputeService;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        disputeRepository = mock(WorkforceDisputeRepository.class);
        auditService = mock(AuditService.class);
        disputeService = new WorkforceDisputeService(disputeRepository, auditService);
    }

    @Test
    void createsDisputeSuccessfully() {
        when(disputeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkforceDispute dispute = disputeService.createDispute("sp-1", "c-1", new BigDecimal("1500.00"), "Incorrect overtime hours", "manager");

        assertThat(dispute).isNotNull();
        assertThat(dispute.getStatus()).isEqualTo(WorkforceDispute.Status.DRAFT);
        assertThat(dispute.getDisputedAmount()).isEqualTo(new BigDecimal("1500.00"));
    }

    @Test
    void disputeLifecycleFlow() {
        WorkforceDispute dispute = new WorkforceDispute("sp-1", "c-1", new BigDecimal("1500.00"), "Hours mismatch");
        when(disputeRepository.findById("d-1")).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        disputeService.submitForReview("d-1", "manager1");
        assertThat(dispute.getStatus()).isEqualTo(WorkforceDispute.Status.UNDER_REVIEW);

        disputeService.resolveDispute("d-1", "Approved adjustment", "manager1");
        assertThat(dispute.getStatus()).isEqualTo(WorkforceDispute.Status.RESOLVED);
        assertThat(dispute.getResolvedBy()).isEqualTo("manager1");
    }
}
