package com.bemo.hr.serviceops;

import com.bemo.hr.serviceops.api.ServiceOpsApi;
import com.bemo.hr.serviceops.application.WorkOrderService;
import com.bemo.hr.serviceops.domain.WorkOrder;
import com.bemo.hr.serviceops.infrastructure.WorkOrderLaborLineRepository;
import com.bemo.hr.serviceops.infrastructure.WorkOrderPartsLineRepository;
import com.bemo.hr.serviceops.infrastructure.WorkOrderRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTests {

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private WorkOrderLaborLineRepository laborLineRepository;

    @Mock
    private WorkOrderPartsLineRepository partsLineRepository;

    private WorkOrderService workOrderService;

    private static final String APP_ID = "test-app";

    @BeforeEach
    void setUp() {
        TenantContext.set(APP_ID);
        workOrderService = new WorkOrderService(workOrderRepository, laborLineRepository, partsLineRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createWorkOrder_createsAndReturnsResponse() {
        ServiceOpsApi.WorkOrderCreateRequest request = new ServiceOpsApi.WorkOrderCreateRequest(
                "WO-1001", "cust-1", "Acme Corp", "Pump repair",
                "Fix leaking seal", "emp-1", WorkOrder.Priority.HIGH, "2026-09-01"
        );

        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceOpsApi.WorkOrderResponse response = workOrderService.createWorkOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.ticketNo()).isEqualTo("WO-1001");
        assertThat(response.status()).isEqualTo(WorkOrder.Status.OPEN);
        verify(workOrderRepository).save(any(WorkOrder.class));
    }

    @Test
    void addLaborAndParts_updatesTotals() {
        WorkOrder wo = new WorkOrder(APP_ID, "WO-1002", "cust-1", "Acme Corp",
                "Engine overhaul", "Full overhaul", "emp-1", WorkOrder.Priority.URGENT, "2026-09-05");

        when(workOrderRepository.findByAppIdAndId(APP_ID, wo.getId())).thenReturn(Optional.of(wo));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        workOrderService.addLaborLine(wo.getId(), new ServiceOpsApi.AddLaborLineRequest("Disassembly", new BigDecimal("3.5"), new BigDecimal("100.00")));
        workOrderService.addPartsLine(wo.getId(), new ServiceOpsApi.AddPartsLineRequest("GASKET-01", "Head Gasket", BigDecimal.valueOf(2), new BigDecimal("150.00")));

        assertThat(wo.getLaborTotal()).isEqualByComparingTo("350.00");
        assertThat(wo.getPartsTotal()).isEqualByComparingTo("300.00");
        assertThat(wo.getGrandTotal()).isEqualByComparingTo("650.00");
    }

    @Test
    void updateStatus_fromWaitingPartsToDone_requiresPartsOrOverrideNote() {
        WorkOrder wo = new WorkOrder(APP_ID, "WO-1003", "cust-1", "Acme", "Inspection", "Desc", "emp-1", WorkOrder.Priority.NORMAL, "2026-09-01");
        wo.setStatus(WorkOrder.Status.WAITING_PARTS);

        when(workOrderRepository.findByAppIdAndId(APP_ID, wo.getId())).thenReturn(Optional.of(wo));

        // Without parts and without override note -> Throws exception
        ServiceOpsApi.UpdateWorkOrderStatusRequest badReq = new ServiceOpsApi.UpdateWorkOrderStatusRequest(WorkOrder.Status.DONE, null);
        assertThatThrownBy(() -> workOrderService.updateStatus(wo.getId(), badReq))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("WORK_ORDER_PARTS_REQUIRED");

        // With override note -> Succeeds
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ServiceOpsApi.UpdateWorkOrderStatusRequest goodReq = new ServiceOpsApi.UpdateWorkOrderStatusRequest(WorkOrder.Status.DONE, "Used existing shop stock");
        ServiceOpsApi.WorkOrderResponse response = workOrderService.updateStatus(wo.getId(), goodReq);

        assertThat(response.status()).isEqualTo(WorkOrder.Status.DONE);
        assertThat(response.overrideNote()).isEqualTo("Used existing shop stock");
    }

    @Test
    void deliverAndCreateInvoice_createsDraftInvoice() {
        WorkOrder wo = new WorkOrder(APP_ID, "WO-1004", "cust-1", "Acme", "Delivery test", "Desc", "emp-1", WorkOrder.Priority.NORMAL, "2026-09-01");
        wo.setStatus(WorkOrder.Status.DONE);

        when(workOrderRepository.findByAppIdAndId(APP_ID, wo.getId())).thenReturn(Optional.of(wo));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceOpsApi.WorkOrderResponse response = workOrderService.deliverAndCreateInvoice(wo.getId());

        assertThat(response.status()).isEqualTo(WorkOrder.Status.DELIVERED);
        assertThat(response.invoiceId()).isNotNull().startsWith("INV-DRAFT-");
    }
}
