package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.procurement.api.ProcurementApi;
import com.bemo.hr.trade.procurement.domain.*;
import com.bemo.hr.trade.procurement.infrastructure.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class SourcingService {

    private final RfqHeaderRepository rfqHeaderRepository;
    private final SupplierQuoteHeaderRepository quoteHeaderRepository;
    private final SourcingAwardRepository awardRepository;
    private final ProcurementService procurementService;

    public SourcingService(RfqHeaderRepository rfqHeaderRepository,
                           SupplierQuoteHeaderRepository quoteHeaderRepository,
                           SourcingAwardRepository awardRepository,
                           ProcurementService procurementService) {
        this.rfqHeaderRepository = rfqHeaderRepository;
        this.quoteHeaderRepository = quoteHeaderRepository;
        this.awardRepository = awardRepository;
        this.procurementService = procurementService;
    }

    @Transactional
    public RfqHeader createRfq(String rfqNumber, String requisitionId, LocalDate issueDate, LocalDate dueDate) {
        RfqHeader rfq = new RfqHeader(rfqNumber, requisitionId, issueDate, dueDate);
        return rfqHeaderRepository.save(rfq);
    }

    @Transactional
    public RfqHeader issueRfq(String rfqId) {
        RfqHeader rfq = getRfq(rfqId);
        rfq.issue();
        return rfqHeaderRepository.save(rfq);
    }

    @Transactional
    public SupplierQuoteHeader submitQuote(String rfqId, String supplierId, String quoteNumber, LocalDate quoteDate, LocalDate validUntil, BigDecimal totalAmount) {
        RfqHeader rfq = getRfq(rfqId);
        if (rfq.getStatus() != RfqHeader.Status.ISSUED && rfq.getStatus() != RfqHeader.Status.EVALUATING) {
            throw new BusinessRuleException("Quotes can only be submitted for ISSUED or EVALUATING RFQs", "RFQ_NOT_ISSUED", HttpStatus.CONFLICT);
        }
        SupplierQuoteHeader quote = new SupplierQuoteHeader(rfqId, supplierId, quoteNumber, quoteDate, validUntil, totalAmount);
        return quoteHeaderRepository.save(quote);
    }

    @Transactional
    public SourcingAward awardQuote(String rfqId, String quoteId, String awardedBy) {
        RfqHeader rfq = getRfq(rfqId);
        SupplierQuoteHeader quote = quoteHeaderRepository.findById(quoteId)
                .orElseThrow(() -> new BusinessRuleException("Quote not found", "QUOTE_NOT_FOUND", HttpStatus.NOT_FOUND));

        rfq.award();
        rfqHeaderRepository.save(rfq);

        quote.award();
        quoteHeaderRepository.save(quote);

        // Convert award into Purchase Order
        ProcurementApi.PurchaseOrderPayload poPayload = new ProcurementApi.PurchaseOrderPayload(
                "PO-" + System.currentTimeMillis(),
                System.currentTimeMillis(),
                quote.getSupplierId(),
                rfq.getRequisitionId(),
                null,
                "NET30",
                "EGP",
                BigDecimal.ONE,
                null,
                List.of(new ProcurementApi.PurchaseOrderLinePayload("ITEM-1", "Awarded Item", "GENERAL", BigDecimal.ONE, "PCS", quote.getTotalAmount()))
        );
        ProcurementApi.PurchaseOrderResponse poResponse = procurementService.create(poPayload);

        SourcingAward award = new SourcingAward(rfqId, quoteId, quote.getSupplierId(), quote.getTotalAmount(), poResponse.id(), awardedBy);
        return awardRepository.save(award);
    }

    @Transactional(readOnly = true)
    public List<SupplierQuoteHeader> getQuotesForRfq(String rfqId) {
        return quoteHeaderRepository.findByRfqId(rfqId);
    }

    private RfqHeader getRfq(String id) {
        return rfqHeaderRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("RFQ not found", "RFQ_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
