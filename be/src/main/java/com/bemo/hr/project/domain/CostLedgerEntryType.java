package com.bemo.hr.project.domain;

public enum CostLedgerEntryType {
    BUDGET,
    COMMITTED,            // Purchase Orders, Subcontractor Agreements
    ACTUAL,               // Supplier Invoices, Labor Payments, Material Issues
    REVENUE,              // Certified Owner Progress Claims, Customer Invoices
    FORECAST_ADJUSTMENT   // Manual or algorithmic adjustment to ETC/EAC
}
