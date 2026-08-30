package com.bemo.hr.migration.domain;

public enum MigrationEntityType {
    // Master Data
    CUSTOMERS,
    SUPPLIERS,
    EMPLOYEES,
    ITEMS,
    WAREHOUSES,
    CHART_OF_ACCOUNTS,
    PROJECTS,
    BOM,
    PRICE_LISTS,

    // Opening Balances
    OPENING_STOCK,
    OPENING_AR,
    OPENING_AP,
    BANK_BALANCES,
    CASH_BALANCES,
    FIXED_ASSETS
}
