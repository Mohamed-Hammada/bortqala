package com.bemo.hr.manufacturing.production.application;

import com.bemo.hr.manufacturing.production.domain.ProductionReceipt;
import com.bemo.hr.manufacturing.production.domain.RoutingHeader;
import com.bemo.hr.manufacturing.production.domain.WorkCenter;
import com.bemo.hr.manufacturing.production.infrastructure.ProductionReceiptRepository;
import com.bemo.hr.manufacturing.production.infrastructure.RoutingHeaderRepository;
import com.bemo.hr.manufacturing.production.infrastructure.WorkCenterRepository;
import com.bemo.hr.operations.OperationsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ManufacturingExecutionService {

    private final WorkCenterRepository workCenterRepository;
    private final RoutingHeaderRepository routingHeaderRepository;
    private final ProductionReceiptRepository receiptRepository;
    private final OperationsService operationsService;

    public ManufacturingExecutionService(WorkCenterRepository workCenterRepository,
                                         RoutingHeaderRepository routingHeaderRepository,
                                         ProductionReceiptRepository receiptRepository,
                                         OperationsService operationsService) {
        this.workCenterRepository = workCenterRepository;
        this.routingHeaderRepository = routingHeaderRepository;
        this.receiptRepository = receiptRepository;
        this.operationsService = operationsService;
    }

    @Transactional
    public WorkCenter createWorkCenter(String code, String name, BigDecimal hourlyRate, BigDecimal capacityHoursPerDay) {
        WorkCenter wc = new WorkCenter(code, name, hourlyRate, capacityHoursPerDay);
        return workCenterRepository.save(wc);
    }

    @Transactional
    public RoutingHeader createRouting(String routingCode, String name, String itemId) {
        RoutingHeader routing = new RoutingHeader(routingCode, name, itemId);
        return routingHeaderRepository.save(routing);
    }

    @Transactional
    public ProductionReceipt recordReceipt(String receiptNumber, String productionOrderId, String finishedItemId, BigDecimal receivedQuantity, LocalDate receiptDate, String warehouseId) {
        ProductionReceipt receipt = new ProductionReceipt(receiptNumber, productionOrderId, finishedItemId, receivedQuantity, receiptDate, warehouseId);
        return receiptRepository.save(receipt);
    }

    @Transactional(readOnly = true)
    public List<ProductionReceipt> getReceiptsForOrder(String productionOrderId) {
        return receiptRepository.findByProductionOrderId(productionOrderId);
    }
}
