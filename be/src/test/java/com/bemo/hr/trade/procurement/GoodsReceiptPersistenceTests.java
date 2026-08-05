package com.bemo.hr.trade.procurement;

import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.trade.procurement.domain.GoodsReceipt;
import com.bemo.hr.trade.procurement.domain.GoodsReceiptLine;
import com.bemo.hr.trade.procurement.infrastructure.GoodsReceiptRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GoodsReceiptPersistenceTests {

    private final TenantApplicationRepository tenantApplicationRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    GoodsReceiptPersistenceTests(TenantApplicationRepository tenantApplicationRepository,
                                 GoodsReceiptRepository goodsReceiptRepository,
                                 PlatformTransactionManager transactionManager) {
        this.tenantApplicationRepository = tenantApplicationRepository;
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void savesReceiptAndItsLinesWithTheParentForeignKeyInOneTransaction() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        TenantApplication app = tenantApplicationRepository.save(
                new TenantApplication("GRN-" + suffix, "Goods receipt test"));
        TenantContext.set(app.getId());

        String receiptId = transactionTemplate.execute(status -> {
            GoodsReceiptLine line = new GoodsReceiptLine(
                    "po-line-1", "item-1", "برتقال خام", "خامات",
                    new BigDecimal("2.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                    new BigDecimal("2.00"), "KG", new BigDecimal("10.00"),
                    null, "LOT-1", null);
            GoodsReceipt receipt = new GoodsReceipt("1", LocalDate.of(2026, 7, 29),
                    "po-1", "supplier-1", null, null, List.of(line));
            return goodsReceiptRepository.saveAndFlush(receipt).getId();
        });

        SavedLine saved = transactionTemplate.execute(status -> {
            GoodsReceipt receipt = goodsReceiptRepository.findById(receiptId).orElseThrow();
            GoodsReceiptLine line = receipt.getLines().get(0);
            return new SavedLine(receipt.getLines().size(), line.getGoodsReceiptId(), line.getQuantity());
        });

        assertThat(saved.lineCount()).isEqualTo(1);
        assertThat(saved.goodsReceiptId()).isEqualTo(receiptId);
        assertThat(saved.quantity()).isEqualByComparingTo("2.00");
    }

    private record SavedLine(int lineCount, String goodsReceiptId, BigDecimal quantity) { }
}
