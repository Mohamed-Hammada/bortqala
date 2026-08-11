package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.trade.procurement.domain.PurchaseRequisition;
import com.bemo.hr.trade.procurement.infrastructure.PurchaseRequisitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PurchaseRequisitionServiceTests {

    private PurchaseRequisitionRepository requisitionRepository;
    private PurchaseRequisitionService requisitionService;

    @BeforeEach
    void setUp() {
        requisitionRepository = mock(PurchaseRequisitionRepository.class);
        requisitionService = new PurchaseRequisitionService(requisitionRepository);
    }

    @Test
    void createsRequisitionSuccessfully() {
        when(requisitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PurchaseRequisition req = requisitionService.createRequisition("REQ-100", "dept-1", "purchaser1");

        assertThat(req).isNotNull();
        assertThat(req.getRequisitionNumber()).isEqualTo("REQ-100");
        assertThat(req.getStatus()).isEqualTo(PurchaseRequisition.Status.DRAFT);
    }

    @Test
    void requisitionSubmitAndApproveFlow() {
        PurchaseRequisition req = new PurchaseRequisition("REQ-100", "dept-1", "purchaser1");
        when(requisitionRepository.findById("req-1")).thenReturn(Optional.of(req));
        when(requisitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        requisitionService.submitRequisition("req-1");
        assertThat(req.getStatus()).isEqualTo(PurchaseRequisition.Status.SUBMITTED);

        requisitionService.approveRequisition("req-1");
        assertThat(req.getStatus()).isEqualTo(PurchaseRequisition.Status.APPROVED);
    }
}
