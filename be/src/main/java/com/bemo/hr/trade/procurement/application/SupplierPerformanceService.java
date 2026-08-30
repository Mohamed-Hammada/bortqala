package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.trade.procurement.domain.GoodsReceipt;
import com.bemo.hr.trade.procurement.domain.ProcurementThreeWayMatch;
import com.bemo.hr.trade.procurement.domain.ProcurementThreeWayMatchRepository;
import com.bemo.hr.trade.procurement.domain.PurchaseOrder;
import com.bemo.hr.trade.procurement.domain.SupplierInvoice;
import com.bemo.hr.trade.procurement.infrastructure.GoodsReceiptRepository;
import com.bemo.hr.trade.procurement.infrastructure.PurchaseOrderRepository;
import com.bemo.hr.trade.procurement.infrastructure.SupplierInvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@Transactional(readOnly = true)
public class SupplierPerformanceService {

    private final BusinessPartyRepository businessPartyRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final ProcurementThreeWayMatchRepository threeWayMatchRepository;

    public SupplierPerformanceService(
            BusinessPartyRepository businessPartyRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            GoodsReceiptRepository goodsReceiptRepository,
            SupplierInvoiceRepository supplierInvoiceRepository,
            ProcurementThreeWayMatchRepository threeWayMatchRepository) {
        this.businessPartyRepository = businessPartyRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.supplierInvoiceRepository = supplierInvoiceRepository;
        this.threeWayMatchRepository = threeWayMatchRepository;
    }

    public record SupplierScorecardResponse(
            String supplierId,
            String supplierName,
            int totalOrdersCount,
            BigDecimal totalOrdersValue,
            BigDecimal onTimeDeliveryRate,
            BigDecimal priceVarianceRate,
            int matchExceptionsCount,
            String overallRating
    ) {}

    public List<SupplierScorecardResponse> getSupplierScorecards() {
        List<BusinessParty> parties = businessPartyRepository.findAll();
        List<SupplierScorecardResponse> results = new ArrayList<>();

        for (BusinessParty party : parties) {
            SupplierScorecardResponse scorecard = getSupplierScorecard(party.getId(), party.getName());
            if (scorecard.totalOrdersCount() > 0) {
                results.add(scorecard);
            }
        }

        return results;
    }

    public SupplierScorecardResponse getSupplierScorecard(String supplierId) {
        BusinessParty party = businessPartyRepository.findById(supplierId).orElse(null);
        String name = party != null ? party.getName() : "Unknown Supplier";
        return getSupplierScorecard(supplierId, name);
    }

    private SupplierScorecardResponse getSupplierScorecard(String supplierId, String supplierName) {
        List<PurchaseOrder> orders = purchaseOrderRepository.findBySupplierId(supplierId);
        int totalOrders = orders.size();
        BigDecimal totalValue = BigDecimal.ZERO;

        if (totalOrders == 0) {
            return new SupplierScorecardResponse(
                    supplierId,
                    supplierName,
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.valueOf(100.0),
                    BigDecimal.ZERO,
                    0,
                    "GOOD"
            );
        }

        int onTimeCount = 0;
        int evaluatedDeliveries = 0;

        for (PurchaseOrder po : orders) {
            if (po.getTotalAmount() != null) {
                totalValue = totalValue.add(po.getTotalAmount());
            }

            List<GoodsReceipt> receipts = goodsReceiptRepository.findByPurchaseOrderId(po.getId());
            for (GoodsReceipt grn : receipts) {
                evaluatedDeliveries++;
                if (po.getPoDate() != null && grn.getReceiptDate() != null) {
                    if (!grn.getReceiptDate().isAfter(po.getPoDate().plusDays(14))) {
                        onTimeCount++;
                    }
                }
            }
        }

        BigDecimal onTimeRate = evaluatedDeliveries > 0 ?
                BigDecimal.valueOf((double) onTimeCount / evaluatedDeliveries * 100.0).setScale(1, RoundingMode.HALF_UP) :
                BigDecimal.valueOf(100.0);

        List<SupplierInvoice> invoices = supplierInvoiceRepository.findBySupplierId(supplierId);
        int matchExceptions = 0;
        for (SupplierInvoice inv : invoices) {
            Optional<ProcurementThreeWayMatch> matchOpt = threeWayMatchRepository.findBySupplierInvoiceId(inv.getId());
            if (matchOpt.isPresent()) {
                ProcurementThreeWayMatch m = matchOpt.get();
                if (!"MATCHED".equalsIgnoreCase(m.getMatchStatus())) {
                    matchExceptions++;
                }
            }
        }

        BigDecimal priceVarianceRate = BigDecimal.valueOf(Math.min(matchExceptions * 1.5, 20.0)).setScale(1, RoundingMode.HALF_UP);

        String rating;
        if (onTimeRate.compareTo(BigDecimal.valueOf(90.0)) >= 0 && priceVarianceRate.compareTo(BigDecimal.valueOf(3.0)) <= 0) {
            rating = "EXCELLENT";
        } else if (onTimeRate.compareTo(BigDecimal.valueOf(75.0)) >= 0 && priceVarianceRate.compareTo(BigDecimal.valueOf(7.0)) <= 0) {
            rating = "GOOD";
        } else if (onTimeRate.compareTo(BigDecimal.valueOf(60.0)) >= 0) {
            rating = "FAIR";
        } else {
            rating = "AT_RISK";
        }

        return new SupplierScorecardResponse(
                supplierId,
                supplierName,
                totalOrders,
                totalValue,
                onTimeRate,
                priceVarianceRate,
                matchExceptions,
                rating
        );
    }
}
