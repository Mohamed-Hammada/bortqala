package com.bemo.hr.trade.sales.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.sales.domain.CustomerInvoice;
import com.bemo.hr.trade.sales.infrastructure.CustomerInvoiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CustomerInvoiceService {

    private final CustomerInvoiceRepository repository;

    public CustomerInvoiceService(CustomerInvoiceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CustomerInvoice createInvoiceFromDelivery(String salesOrderId, BigDecimal deliveredQuantity, BigDecimal unitPrice, BigDecimal unitCogs) {
        CustomerInvoice invoice = new CustomerInvoice(salesOrderId, deliveredQuantity, unitPrice, unitCogs);
        return repository.save(invoice);
    }

    @Transactional(readOnly = true)
    public CustomerInvoice getInvoice(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Customer invoice not found", "CUSTOMER_INVOICE_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<CustomerInvoice> getInvoicesForSalesOrder(String salesOrderId) {
        return repository.findBySalesOrderId(salesOrderId);
    }
}
