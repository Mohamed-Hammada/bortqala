package com.bemo.hr.trade.sales.application;

import com.bemo.hr.operations.InventoryItem;
import com.bemo.hr.operations.InventoryItemRepository;
import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.sales.api.SalesQuotationApi;
import com.bemo.hr.trade.sales.domain.*;
import com.bemo.hr.trade.sales.infrastructure.SalesOrderLineRepository;
import com.bemo.hr.trade.sales.infrastructure.SalesOrderRepository;
import com.bemo.hr.trade.sales.infrastructure.SalesQuotationLineRepository;
import com.bemo.hr.trade.sales.infrastructure.SalesQuotationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SalesQuotationService {

    private final SalesQuotationRepository quotationRepository;
    private final SalesQuotationLineRepository quotationLineRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderLineRepository salesOrderLineRepository;
    private final BusinessPartyRepository businessPartyRepository;
    private final InventoryItemRepository inventoryItemRepository;

    public SalesQuotationService(SalesQuotationRepository quotationRepository,
                                 SalesQuotationLineRepository quotationLineRepository,
                                 SalesOrderRepository salesOrderRepository,
                                 SalesOrderLineRepository salesOrderLineRepository,
                                 BusinessPartyRepository businessPartyRepository,
                                 InventoryItemRepository inventoryItemRepository) {
        this.quotationRepository = quotationRepository;
        this.quotationLineRepository = quotationLineRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderLineRepository = salesOrderLineRepository;
        this.businessPartyRepository = businessPartyRepository;
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Transactional(readOnly = true)
    public List<SalesQuotationApi.QuotationResponse> listQuotations(String customerId, QuotationStatus status) {
        List<SalesQuotation> quotes;
        if (customerId != null && !customerId.isBlank()) {
            quotes = quotationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        } else if (status != null) {
            quotes = quotationRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            quotes = quotationRepository.findAllByOrderByCreatedAtDesc();
        }

        Map<String, BusinessParty> partyMap = businessPartyRepository.findAll().stream()
                .collect(Collectors.toMap(BusinessParty::getId, p -> p, (a, b) -> a));
        Map<String, InventoryItem> itemMap = inventoryItemRepository.findAll().stream()
                .collect(Collectors.toMap(InventoryItem::getId, i -> i, (a, b) -> a));

        return quotes.stream()
                .map(q -> {
                    List<SalesQuotationLine> lines = quotationLineRepository.findByQuotationId(q.getId());
                    return toQuotationResponse(q, partyMap.get(q.getCustomerId()), lines, itemMap);
                })
                .toList();
    }

    @Transactional
    public SalesQuotationApi.QuotationResponse createQuotation(SalesQuotationApi.CreateQuotationRequest request) {
        BusinessParty customer = businessPartyRepository.findById(request.customerId())
                .orElseThrow(() -> new BusinessRuleException("العميل غير موجود", "CUSTOMER_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (request.validUntil().isBefore(request.quoteDate())) {
            throw new BusinessRuleException("تاريخ الصلاحية يسبق تاريخ عرض السعر", "INVALID_QUOTE_DATES", HttpStatus.BAD_REQUEST);
        }

        int year = request.quoteDate().getYear();
        String prefix = "QUO-" + year + "-";
        long count = quotationRepository.countByQuotationNumberStartingWith(prefix) + 1;
        String quoteNumber = String.format("%s%03d", prefix, count);

        SalesQuotation quote = new SalesQuotation(
                quoteNumber,
                request.customerId(),
                request.quoteDate(),
                request.validUntil(),
                request.termsAndConditions()
        );

        SalesQuotation savedQuote = quotationRepository.save(quote);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        List<SalesQuotationLine> savedLines = new ArrayList<>();

        for (SalesQuotationApi.QuotationLineItem lineItem : request.lines()) {
            BigDecimal qty = lineItem.quantity();
            BigDecimal price = lineItem.unitPrice();
            BigDecimal discount = lineItem.discountAmount() != null ? lineItem.discountAmount() : BigDecimal.ZERO;
            BigDecimal tax = lineItem.taxAmount() != null ? lineItem.taxAmount() : BigDecimal.ZERO;
            BigDecimal base = qty.multiply(price);
            BigDecimal lineTotal = base.subtract(discount).add(tax);

            subtotal = subtotal.add(base);
            totalDiscount = totalDiscount.add(discount);
            totalTax = totalTax.add(tax);

            SalesQuotationLine line = new SalesQuotationLine(
                    savedQuote.getId(),
                    lineItem.itemId(),
                    qty,
                    price,
                    discount,
                    tax,
                    lineTotal,
                    lineItem.notes()
            );
            savedLines.add(quotationLineRepository.save(line));
        }

        BigDecimal finalTotal = subtotal.subtract(totalDiscount).add(totalTax);
        savedQuote.updateTotals(subtotal, totalDiscount, totalTax, finalTotal);
        quotationRepository.save(savedQuote);

        Map<String, InventoryItem> itemMap = inventoryItemRepository.findAll().stream()
                .collect(Collectors.toMap(InventoryItem::getId, i -> i, (a, b) -> a));

        log.info("SalesQuotation created: {} total={}", quoteNumber, finalTotal);
        return toQuotationResponse(savedQuote, customer, savedLines, itemMap);
    }

    @Transactional
    public SalesQuotationApi.QuotationResponse sendQuotation(String id) {
        SalesQuotation quote = quotationRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("عرض السعر غير موجود", "QUOTATION_NOT_FOUND", HttpStatus.NOT_FOUND));

        quote.send();
        SalesQuotation saved = quotationRepository.save(quote);
        return reloadResponse(saved);
    }

    @Transactional
    public SalesQuotationApi.QuotationResponse acceptQuotation(String id) {
        SalesQuotation quote = quotationRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("عرض السعر غير موجود", "QUOTATION_NOT_FOUND", HttpStatus.NOT_FOUND));

        quote.accept();
        SalesQuotation saved = quotationRepository.save(quote);
        return reloadResponse(saved);
    }

    @Transactional
    public SalesQuotationApi.QuotationResponse rejectQuotation(String id) {
        SalesQuotation quote = quotationRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("عرض السعر غير موجود", "QUOTATION_NOT_FOUND", HttpStatus.NOT_FOUND));

        quote.reject();
        SalesQuotation saved = quotationRepository.save(quote);
        return reloadResponse(saved);
    }

    @Transactional
    public SalesQuotationApi.QuotationResponse convertToSalesOrder(String quotationId) {
        SalesQuotation quote = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new BusinessRuleException("عرض السعر غير موجود", "QUOTATION_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (quote.getStatus() == QuotationStatus.CONVERTED) {
            throw new BusinessRuleException("تم تحويل عرض السعر مسبقاً", "QUOTATION_ALREADY_CONVERTED", HttpStatus.BAD_REQUEST);
        }

        List<SalesQuotationLine> quoteLines = quotationLineRepository.findByQuotationId(quotationId);
        if (quoteLines.isEmpty()) {
            throw new BusinessRuleException("عرض السعر لا يحتوي على بنود", "QUOTATION_HAS_NO_LINES", HttpStatus.BAD_REQUEST);
        }

        int year = LocalDate.now().getYear();
        String prefix = "SO-" + year + "-";
        long count = salesOrderRepository.countBySoNumberStartingWith(prefix) + 1;
        String orderNumber = String.format("%s%03d", prefix, count);

        SalesOrder order = new SalesOrder(
                orderNumber,
                quote.getQuoteDate(),
                quote.getCustomerId(),
                quote.getId(),
                quote.getTotalAmount()
        );

        SalesOrder savedOrder = salesOrderRepository.save(order);
        Map<String, InventoryItem> itemMap = inventoryItemRepository.findAll().stream()
                .collect(Collectors.toMap(InventoryItem::getId, i -> i, (a, b) -> a));

        for (SalesQuotationLine ql : quoteLines) {
            InventoryItem item = itemMap.get(ql.getItemId());
            String itemName = item != null ? item.getName() : ql.getItemId();
            SalesOrderLine sol = new SalesOrderLine(
                    savedOrder.getId(),
                    ql.getItemId(),
                    itemName,
                    ql.getQuantity(),
                    ql.getUnitPrice(),
                    BigDecimal.ZERO
            );
            salesOrderLineRepository.save(sol);
        }

        quote.markConverted(savedOrder.getId());
        SalesQuotation savedQuote = quotationRepository.save(quote);

        log.info("SalesQuotation {} converted to SalesOrder {}", quote.getQuotationNumber(), savedOrder.getSoNumber());
        return reloadResponse(savedQuote);
    }

    private SalesQuotationApi.QuotationResponse reloadResponse(SalesQuotation quote) {
        BusinessParty customer = businessPartyRepository.findById(quote.getCustomerId()).orElse(null);
        List<SalesQuotationLine> lines = quotationLineRepository.findByQuotationId(quote.getId());
        Map<String, InventoryItem> itemMap = inventoryItemRepository.findAll().stream()
                .collect(Collectors.toMap(InventoryItem::getId, i -> i, (a, b) -> a));
        return toQuotationResponse(quote, customer, lines, itemMap);
    }

    private SalesQuotationApi.QuotationResponse toQuotationResponse(
            SalesQuotation q,
            BusinessParty customer,
            List<SalesQuotationLine> lines,
            Map<String, InventoryItem> itemMap) {

        List<SalesQuotationApi.QuotationLineResponse> lineDtos = lines.stream().map(l -> {
            InventoryItem item = itemMap.get(l.getItemId());
            return new SalesQuotationApi.QuotationLineResponse(
                    l.getId(),
                    l.getItemId(),
                    item != null ? item.getCode() : l.getItemId(),
                    item != null ? item.getName() : l.getItemId(),
                    l.getQuantity(),
                    l.getUnitPrice(),
                    l.getDiscountAmount(),
                    l.getTaxAmount(),
                    l.getLineTotal(),
                    l.getNotes()
            );
        }).toList();

        return new SalesQuotationApi.QuotationResponse(
                q.getId(),
                q.getQuotationNumber(),
                q.getCustomerId(),
                customer != null ? customer.getName() : q.getCustomerId(),
                q.getQuoteDate(),
                q.getValidUntil(),
                q.getSubtotal(),
                q.getDiscountAmount(),
                q.getTaxAmount(),
                q.getTotalAmount(),
                q.getStatus(),
                q.getTermsAndConditions(),
                q.getSalesOrderId(),
                lineDtos,
                q.getCreatedAt(),
                q.getUpdatedAt(),
                q.getVersion()
        );
    }
}
