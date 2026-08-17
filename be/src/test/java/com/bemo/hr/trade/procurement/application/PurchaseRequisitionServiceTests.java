package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.trade.procurement.domain.PurchaseRequisition;
import com.bemo.hr.trade.procurement.domain.PurchaseRequisitionLine;
import com.bemo.hr.trade.procurement.infrastructure.PurchaseRequisitionLineRepository;
import com.bemo.hr.trade.procurement.infrastructure.PurchaseRequisitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PurchaseRequisitionServiceTests {

    private PurchaseRequisitionRepository requisitionRepository;
    private PurchaseRequisitionLineRepository requisitionLineRepository;
    private PurchaseRequisitionService requisitionService;

    @BeforeEach
    void setUp() {
        requisitionRepository = mock(PurchaseRequisitionRepository.class);
        requisitionLineRepository = mock(PurchaseRequisitionLineRepository.class);
        requisitionService = new PurchaseRequisitionService(requisitionRepository, requisitionLineRepository);
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
    void requisitionSubmitAndApproveFlowWithLines() {
        PurchaseRequisition req = new PurchaseRequisition("REQ-100", "dept-1", "purchaser1");
        when(requisitionRepository.findById("req-1")).thenReturn(Optional.of(req));
        when(requisitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(requisitionLineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PurchaseRequisitionLine line = new PurchaseRequisitionLine("req-1", "item-10", "Steel Rods", new BigDecimal("100.00"), new BigDecimal("50.00"), "Urgent");
        when(requisitionLineRepository.findByRequisitionId("req-1")).thenReturn(List.of(line));

        PurchaseRequisitionLine addedLine = requisitionService.addRequisitionLine("req-1", "item-10", "Steel Rods", new BigDecimal("100.00"), new BigDecimal("50.00"), "Urgent");
        assertThat(addedLine).isNotNull();

        requisitionService.submitRequisition("req-1");
        assertThat(req.getStatus()).isEqualTo(PurchaseRequisition.Status.SUBMITTED);

        requisitionService.approveRequisition("req-1");
        assertThat(req.getStatus()).isEqualTo(PurchaseRequisition.Status.APPROVED);
    }
}
