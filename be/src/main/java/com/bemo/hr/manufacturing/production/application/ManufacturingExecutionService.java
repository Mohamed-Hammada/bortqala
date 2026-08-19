package com.bemo.hr.manufacturing.production.application;

import com.bemo.hr.manufacturing.production.domain.ProductionReceipt;
import com.bemo.hr.manufacturing.production.domain.RoutingHeader;
import com.bemo.hr.manufacturing.production.domain.WorkCenter;
import com.bemo.hr.manufacturing.production.infrastructure.ProductionReceiptRepository;
import com.bemo.hr.manufacturing.production.infrastructure.RoutingHeaderRepository;
import com.bemo.hr.manufacturing.production.infrastructure.WorkCenterRepository;
import com.bemo.hr.operations.OperationsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
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
        log.debug("createWorkCenter called with code={}, name={}", code, name);
        WorkCenter wc = new WorkCenter(code, name, hourlyRate, capacityHoursPerDay);
        WorkCenter saved = workCenterRepository.save(wc);
        log.info("WorkCenter {} created successfully", saved.getId());
        return saved;
    }

    @Transactional
    public RoutingHeader createRouting(String routingCode, String name, String itemId) {
        log.debug("createRouting called with routingCode={}, itemId={}", routingCode, itemId);
        RoutingHeader routing = new RoutingHeader(routingCode, name, itemId);
        RoutingHeader saved = routingHeaderRepository.save(routing);
        log.info("RoutingHeader {} created successfully", saved.getId());
        return saved;
    }

    @Transactional
    public ProductionReceipt recordReceipt(String receiptNumber, String productionOrderId, String finishedItemId, BigDecimal receivedQuantity, LocalDate receiptDate, String warehouseId) {
        log.debug("recordReceipt called with receiptNumber={}, productionOrderId={}", receiptNumber, productionOrderId);
        ProductionReceipt receipt = new ProductionReceipt(receiptNumber, productionOrderId, finishedItemId, receivedQuantity, receiptDate, warehouseId);
        ProductionReceipt saved = receiptRepository.save(receipt);
        log.info("ProductionReceipt {} recorded successfully", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<WorkCenter> listWorkCenters() {
        return workCenterRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<RoutingHeader> listRoutings() {
        return routingHeaderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ProductionReceipt> getReceiptsForOrder(String productionOrderId) {
        log.debug("getReceiptsForOrder called with productionOrderId={}", productionOrderId);
        return receiptRepository.findByProductionOrderId(productionOrderId);
    }
}
