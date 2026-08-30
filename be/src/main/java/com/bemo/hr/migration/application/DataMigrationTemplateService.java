package com.bemo.hr.migration.application;

import com.bemo.hr.migration.domain.MigrationEntityType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DataMigrationTemplateService {

    private static final Map<MigrationEntityType, List<String>> TEMPLATE_COLUMNS = Map.ofEntries(
            Map.entry(MigrationEntityType.CUSTOMERS, List.of("code", "nameAr", "nameEn", "taxNumber", "phone", "email", "creditLimit", "address")),
            Map.entry(MigrationEntityType.SUPPLIERS, List.of("code", "nameAr", "nameEn", "taxNumber", "phone", "email", "paymentTermsDays", "address")),
            Map.entry(MigrationEntityType.EMPLOYEES, List.of("employeeCode", "fullNameAr", "fullNameEn", "nationalId", "departmentCode", "jobTitle", "basicSalary", "hireDate")),
            Map.entry(MigrationEntityType.ITEMS, List.of("itemCode", "nameAr", "nameEn", "categoryCode", "unitOfMeasure", "costPrice", "sellingPrice", "taxRate")),
            Map.entry(MigrationEntityType.WAREHOUSES, List.of("warehouseCode", "nameAr", "nameEn", "location", "managerName")),
            Map.entry(MigrationEntityType.CHART_OF_ACCOUNTS, List.of("accountCode", "nameAr", "nameEn", "accountType", "parentCode", "currency")),
            Map.entry(MigrationEntityType.PROJECTS, List.of("projectCode", "nameAr", "nameEn", "contractValue", "startDate", "expectedEndDate", "managerId")),
            Map.entry(MigrationEntityType.BOM, List.of("parentItemCode", "componentItemCode", "quantity", "unitOfMeasure", "scrapFactorPercent")),
            Map.entry(MigrationEntityType.PRICE_LISTS, List.of("priceListCode", "itemCode", "unitPrice", "currency", "effectiveFrom")),
            Map.entry(MigrationEntityType.OPENING_STOCK, List.of("warehouseCode", "itemCode", "quantity", "unitCost", "lotNumber", "expiryDate")),
            Map.entry(MigrationEntityType.OPENING_AR, List.of("customerCode", "invoiceNumber", "invoiceDate", "dueDate", "originalAmount", "outstandingAmount", "currency")),
            Map.entry(MigrationEntityType.OPENING_AP, List.of("supplierCode", "billNumber", "billDate", "dueDate", "originalAmount", "outstandingAmount", "currency")),
            Map.entry(MigrationEntityType.BANK_BALANCES, List.of("bankAccountCode", "bankName", "accountNumber", "currency", "openingBalanceDate", "balanceAmount")),
            Map.entry(MigrationEntityType.CASH_BALANCES, List.of("cashBoxCode", "nameAr", "nameEn", "custodianName", "openingBalanceDate", "balanceAmount")),
            Map.entry(MigrationEntityType.FIXED_ASSETS, List.of("assetCode", "nameAr", "nameEn", "acquisitionDate", "costValue", "accumulatedDepreciation", "usefulLifeMonths", "location"))
    );

    public List<String> getColumns(MigrationEntityType type) {
        return TEMPLATE_COLUMNS.getOrDefault(type, List.of("code", "nameAr", "nameEn", "amount"));
    }

    public String generateCsvTemplate(MigrationEntityType type) {
        List<String> cols = getColumns(type);
        return String.join(",", cols) + "\n";
    }
}
