package com.bemo.hr.trade.sales.infrastructure;

import com.bemo.hr.trade.sales.domain.CustomerReceiptAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerReceiptAllocationRepository extends JpaRepository<CustomerReceiptAllocation, String> {
    List<CustomerReceiptAllocation> findByReceiptId(String receiptId);
}
