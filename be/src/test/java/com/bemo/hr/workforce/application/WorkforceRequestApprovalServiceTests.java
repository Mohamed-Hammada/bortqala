package com.bemo.hr.workforce.application;

import com.bemo.hr.workforce.domain.WorkforceRequestApproval;
import com.bemo.hr.workforce.infrastructure.WorkforceRequestApprovalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WorkforceRequestApprovalServiceTests {

    private WorkforceRequestApprovalRepository repository;
    private WorkforceRequestApprovalService service;

    @BeforeEach
    void setUp() {
        repository = mock(WorkforceRequestApprovalRepository.class);
        service = new WorkforceRequestApprovalService(repository);
    }

    @Test
    void submitsApprovalDecisionSuccessfully() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkforceRequestApproval approval = service.submitDecision("req-77", "mgr-1", WorkforceRequestApproval.Decision.APPROVED, "Headcount approved within budget");
        assertThat(approval).isNotNull();
        assertThat(approval.getRequestId()).isEqualTo("req-77");
        assertThat(approval.getDecision()).isEqualTo(WorkforceRequestApproval.Decision.APPROVED);

        when(repository.findByRequestId("req-77")).thenReturn(List.of(approval));
        assertThat(service.getApprovalsForRequest("req-77")).hasSize(1);
    }
}
