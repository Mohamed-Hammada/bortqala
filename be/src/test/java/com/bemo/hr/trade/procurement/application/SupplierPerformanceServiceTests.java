package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.trade.procurement.application.SupplierPerformanceService.SupplierScorecardResponse;
import com.bemo.hr.trade.procurement.domain.GoodsReceipt;
import com.bemo.hr.trade.procurement.domain.PurchaseOrder;
import com.bemo.hr.trade.procurement.domain.ProcurementThreeWayMatchRepository;
import com.bemo.hr.trade.procurement.infrastructure.GoodsReceiptRepository;
import com.bemo.hr.trade.procurement.infrastructure.PurchaseOrderRepository;
import com.bemo.hr.trade.procurement.infrastructure.SupplierInvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierPerformanceServiceTests {

    @Mock
    private BusinessPartyRepository businessPartyRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private GoodsReceiptRepository goodsReceiptRepository;

    @Mock
    private SupplierInvoiceRepository supplierInvoiceRepository;

    @Mock
    private ProcurementThreeWayMatchRepository threeWayMatchRepository;

    private SupplierPerformanceService service;

    private BusinessParty supplier1;

    @BeforeEach
    void setUp() {
        service = new SupplierPerformanceService(
                businessPartyRepository,
                purchaseOrderRepository,
                goodsReceiptRepository,
                supplierInvoiceRepository,
                threeWayMatchRepository
        );

        supplier1 = new BusinessParty(
                "SUP-101",
                "شركة الأهرام للتوريدات",
                "Al-Ahram Supplies",
                "SUPPLIER",
                "Ahmed",
                "01012345678",
                "info@ahram.com",
                "Cairo",
                "Notes",
                true,
                "DIRECT",
                null,
                null,
                null,
                "EGP",
                "STANDARD",
                "NET_30",
                "123-456-789",
                "EG123456"
        );
    }

    @Test
    void getSupplierScorecard_withNoOrders_returnsDefaultGood() {
        when(businessPartyRepository.findById(supplier1.getId())).thenReturn(Optional.of(supplier1));
        when(purchaseOrderRepository.findBySupplierId(supplier1.getId())).thenReturn(List.of());

        SupplierScorecardResponse scorecard = service.getSupplierScorecard(supplier1.getId());

        assertThat(scorecard).isNotNull();
        assertThat(scorecard.totalOrdersCount()).isEqualTo(0);
        assertThat(scorecard.overallRating()).isEqualTo("GOOD");
    }

    @Test
    void getSupplierScorecard_withOnTimeOrdersAndNoExceptions_returnsExcellent() {
        when(businessPartyRepository.findById(supplier1.getId())).thenReturn(Optional.of(supplier1));

        PurchaseOrder po = new PurchaseOrder("PO-101", LocalDate.now().minusDays(10), supplier1.getId(),
                null, "Net 30", "EGP", BigDecimal.valueOf(500000));
        when(purchaseOrderRepository.findBySupplierId(supplier1.getId())).thenReturn(List.of(po));

        GoodsReceipt grn = new GoodsReceipt("GRN-101", LocalDate.now().minusDays(5), po.getId(),
                supplier1.getId(), "wh-1", "On-time delivery", List.of());
        when(goodsReceiptRepository.findByPurchaseOrderId(po.getId())).thenReturn(List.of(grn));
        when(supplierInvoiceRepository.findBySupplierId(supplier1.getId())).thenReturn(List.of());

        SupplierScorecardResponse scorecard = service.getSupplierScorecard(supplier1.getId());

        assertThat(scorecard).isNotNull();
        assertThat(scorecard.totalOrdersCount()).isEqualTo(1);
        assertThat(scorecard.totalOrdersValue()).isEqualTo(BigDecimal.valueOf(500000));
        assertThat(scorecard.onTimeDeliveryRate()).isEqualTo(BigDecimal.valueOf(100.0));
        assertThat(scorecard.overallRating()).isEqualTo("EXCELLENT");
    }
}
