package com.bemo.hr.product.onboarding.provider;

import com.bemo.hr.operations.InventoryItemRepository;
import com.bemo.hr.operations.infrastructure.StockStatusBalanceRepository;
import com.bemo.hr.organization.infrastructure.WarehouseRepository;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.product.onboarding.OnboardingEvidenceProvider;
import com.bemo.hr.trade.procurement.infrastructure.GoodsReceiptRepository;
import com.bemo.hr.trade.procurement.infrastructure.PurchaseOrderRepository;
import com.bemo.hr.trade.sales.infrastructure.CustomerCreditProfileRepository;
import com.bemo.hr.trade.sales.infrastructure.CustomerInvoiceRepository;
import com.bemo.hr.trade.sales.infrastructure.SalesOrderRepository;
import com.bemo.hr.trade.sales.infrastructure.SalesPricingSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FoodDistributionOnboardingEvidenceProvider implements OnboardingEvidenceProvider {
    private final WarehouseRepository warehouseRepository;
    private final InventoryItemRepository itemRepository;
    private final BusinessPartyRepository partyRepository;
    private final StockStatusBalanceRepository stockBalanceRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final CustomerInvoiceRepository customerInvoiceRepository;
    private final SalesPricingSnapshotRepository pricingSnapshotRepository;
    private final CustomerCreditProfileRepository creditProfileRepository;

    @Override
    public boolean supports(String packCode, String stepKey) {
        return "FOOD_DISTRIBUTION_EG".equals(packCode) && stepKey != null && (
                stepKey.startsWith("industryPack.food.step.")
                        || stepKey.equals("warehouses")
                        || stepKey.equals("items")
                        || stepKey.equals("suppliers")
                        || stepKey.equals("customers")
                        || stepKey.equals("openingStock")
                        || stepKey.equals("firstPurchase")
                        || stepKey.equals("firstSale")
                        || stepKey.equals("margin")
        ) && !stepKey.contains("company");
    }

    @Override
    public EvidenceResult evaluate(String packCode, String stepKey) {
        return switch (stepKey) {
            case "industryPack.food.step.warehouses", "warehouses" -> {
                long count = warehouseRepository.count();
                yield new EvidenceResult(count > 0, count, true, "WAREHOUSES", "onboarding.issue.warehouses", "/operations/warehouses");
            }
            case "industryPack.food.step.items", "items" -> {
                long count = itemRepository.count();
                yield new EvidenceResult(count > 0, count, true, "ITEMS", "onboarding.issue.items", "/operations");
            }
            case "industryPack.food.step.suppliers", "suppliers" -> {
                long count = partyRepository.findByPartyTypeOrderByNameAsc("SUPPLIER").size();
                yield new EvidenceResult(count > 0, count, true, "SUPPLIERS", "onboarding.issue.suppliers", "/parties");
            }
            case "industryPack.food.step.customers", "customers" -> {
                long count = partyRepository.findByPartyTypeOrderByNameAsc("CUSTOMER").size();
                yield new EvidenceResult(count > 0, count, true, "CUSTOMERS", "onboarding.issue.customers", "/parties");
            }
            case "industryPack.food.step.openingStock", "openingStock" -> {
                long count = stockBalanceRepository.count() > 0 ? stockBalanceRepository.count() : itemRepository.count();
                yield new EvidenceResult(count > 0, count, true, "OPENING_STOCK", "onboarding.issue.openingStock", "/operations");
            }
            case "industryPack.food.step.firstPurchase", "firstPurchase" -> {
                long count = purchaseOrderRepository.count() + goodsReceiptRepository.count();
                yield new EvidenceResult(count > 0, count, true, "PURCHASE", "onboarding.issue.firstPurchase", "/procurement");
            }
            case "industryPack.food.step.firstSale", "firstSale" -> {
                long count = salesOrderRepository.count() + customerInvoiceRepository.count();
                yield new EvidenceResult(count > 0, count, true, "SALE", "onboarding.issue.firstSale", "/sales/orders");
            }
            case "industryPack.food.step.margin", "margin" -> {
                long count = pricingSnapshotRepository.count() + creditProfileRepository.count();
                yield new EvidenceResult(count > 0, count, false, "MARGIN", "onboarding.issue.margin", "/sales/pricing");
            }
            default -> new EvidenceResult(false, 0, false, "UNKNOWN", stepKey, "");
        };
    }
}
