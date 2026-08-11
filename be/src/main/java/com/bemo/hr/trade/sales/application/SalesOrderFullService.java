package com.bemo.hr.trade.sales.application;

import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.sales.domain.CustomerReturnHeader;
import com.bemo.hr.trade.sales.domain.SalesDeliveryHeader;
import com.bemo.hr.trade.sales.domain.SalesOrderLine;
import com.bemo.hr.trade.sales.infrastructure.CustomerReturnHeaderRepository;
import com.bemo.hr.trade.sales.infrastructure.SalesDeliveryHeaderRepository;
import com.bemo.hr.trade.sales.infrastructure.SalesOrderLineRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class SalesOrderFullService {

    private final SalesOrderLineRepository salesOrderLineRepository;
    private final SalesDeliveryHeaderRepository deliveryHeaderRepository;
    private final CustomerReturnHeaderRepository returnHeaderRepository;
    private final OperationsService operationsService;

    public SalesOrderFullService(SalesOrderLineRepository salesOrderLineRepository,
                                 SalesDeliveryHeaderRepository deliveryHeaderRepository,
                                 CustomerReturnHeaderRepository returnHeaderRepository,
                                 OperationsService operationsService) {
        this.salesOrderLineRepository = salesOrderLineRepository;
        this.deliveryHeaderRepository = deliveryHeaderRepository;
        this.returnHeaderRepository = returnHeaderRepository;
        this.operationsService = operationsService;
    }

    @Transactional
    public SalesOrderLine addSalesOrderLine(String salesOrderId, String itemId, String itemName, BigDecimal orderedQuantity, BigDecimal unitPrice, BigDecimal discountRate) {
        SalesOrderLine line = new SalesOrderLine(salesOrderId, itemId, itemName, orderedQuantity, unitPrice, discountRate);
        return salesOrderLineRepository.save(line);
    }

    @Transactional
    public SalesDeliveryHeader createDelivery(String deliveryNumber, String salesOrderId, String customerId, LocalDate deliveryDate) {
        SalesDeliveryHeader delivery = new SalesDeliveryHeader(deliveryNumber, salesOrderId, customerId, deliveryDate);
        delivery.ship();
        delivery.deliver();
        return deliveryHeaderRepository.save(delivery);
    }

    @Transactional
    public CustomerReturnHeader createCustomerReturn(String returnNumber, String salesOrderId, String customerId, LocalDate returnDate, String reason) {
        CustomerReturnHeader returnHeader = new CustomerReturnHeader(returnNumber, salesOrderId, customerId, returnDate, reason);
        returnHeader.receive();
        return returnHeaderRepository.save(returnHeader);
    }

    @Transactional(readOnly = true)
    public List<SalesOrderLine> getSalesOrderLines(String salesOrderId) {
        return salesOrderLineRepository.findBySalesOrderId(salesOrderId);
    }

    @Transactional(readOnly = true)
    public List<SalesDeliveryHeader> getDeliveriesForOrder(String salesOrderId) {
        return deliveryHeaderRepository.findBySalesOrderId(salesOrderId);
    }

    @Transactional(readOnly = true)
    public List<CustomerReturnHeader> getReturnsForOrder(String salesOrderId) {
        return returnHeaderRepository.findBySalesOrderId(salesOrderId);
    }
}
