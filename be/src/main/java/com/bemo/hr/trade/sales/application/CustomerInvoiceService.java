package com.bemo.hr.trade.sales.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.sales.domain.CustomerInvoice;
import com.bemo.hr.trade.sales.infrastructure.CustomerInvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class CustomerInvoiceService {

    private final CustomerInvoiceRepository repository;

    public CustomerInvoiceService(CustomerInvoiceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CustomerInvoice createInvoiceFromDelivery(String salesOrderId, BigDecimal deliveredQuantity, BigDecimal unitPrice, BigDecimal unitCogs) {
        log.debug("createInvoiceFromDelivery called with salesOrderId={}, deliveredQuantity={}, unitPrice={}", salesOrderId, deliveredQuantity, unitPrice);
        CustomerInvoice invoice = new CustomerInvoice(salesOrderId, deliveredQuantity, unitPrice, unitCogs);
        CustomerInvoice saved = repository.save(invoice);
        log.info("CustomerInvoice {} created for salesOrder {} successfully", saved.getId(), salesOrderId);
        return saved;
    }

    @Transactional(readOnly = true)
    public CustomerInvoice getInvoice(String id) {
        log.debug("getInvoice called with id={}", id);
        return repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Customer invoice not found for id={}", id);
                    return new BusinessRuleException("Customer invoice not found", "CUSTOMER_INVOICE_NOT_FOUND", HttpStatus.NOT_FOUND);
                });
    }

    @Transactional(readOnly = true)
    public List<CustomerInvoice> getInvoicesForSalesOrder(String salesOrderId) {
        log.debug("getInvoicesForSalesOrder called with salesOrderId={}", salesOrderId);
        List<CustomerInvoice> results = repository.findBySalesOrderId(salesOrderId);
        log.debug("getInvoicesForSalesOrder returned {} results for salesOrderId={}", results.size(), salesOrderId);
        return results;
    }
}
