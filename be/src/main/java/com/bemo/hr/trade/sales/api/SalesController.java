package com.bemo.hr.trade.sales.api;

import com.bemo.hr.trade.sales.domain.SalesOrder;
import com.bemo.hr.trade.sales.infrastructure.SalesOrderRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/v1/trade/sales")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER', 'HR_MANAGER')")
public class SalesController {

    private final SalesOrderRepository salesOrderRepository;

    public SalesController(SalesOrderRepository salesOrderRepository) {
        this.salesOrderRepository = salesOrderRepository;
    }

    @GetMapping("/orders")
    public List<SalesApi.SalesOrderResponse> listSalesOrders() {
        return salesOrderRepository.findAllByOrderBySoDateDescCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @PostMapping("/orders")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER', 'HR_MANAGER')")
    public SalesApi.SalesOrderResponse createSalesOrder(@Valid @RequestBody SalesApi.SalesOrderPayload payload) {
        LocalDate soDate = Instant.ofEpochMilli(payload.soDate()).atZone(ZoneOffset.UTC).toLocalDate();
        SalesOrder so = new SalesOrder(payload.soNumber(), soDate, payload.customerId(), payload.quotationId(), payload.totalAmount());
        return toResponse(salesOrderRepository.save(so));
    }

    @PostMapping("/orders/{id}/confirm")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER', 'HR_MANAGER')")
    public SalesApi.SalesOrderResponse confirmSalesOrder(@PathVariable String id) {
        SalesOrder so = salesOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("أمر البيع غير موجود", "SALE_ORDER_NOT_FOUND", HttpStatus.CONFLICT));
        so.updateStatus(SalesOrder.Status.CONFIRMED);
        return toResponse(salesOrderRepository.save(so));
    }

    private SalesApi.SalesOrderResponse toResponse(SalesOrder so) {
        long soDateMs = so.getSoDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        return new SalesApi.SalesOrderResponse(
                so.getId(), so.getSoNumber(), soDateMs, so.getCustomerId(),
                so.getQuotationId(), so.getStatus().name(),
                so.getTotalAmount(), so.getCreatedAt(), so.getUpdatedAt()
        );
    }
}
