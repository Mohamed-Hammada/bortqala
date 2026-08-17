package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.procurement.domain.GrirReconciliationRecord;
import com.bemo.hr.trade.procurement.infrastructure.GrirReconciliationRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class GrirReconciliationService {

    private final GrirReconciliationRecordRepository repository;

    public GrirReconciliationService(GrirReconciliationRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public GrirReconciliationRecord reconcileLine(String goodsReceiptLineId, String invoiceLineId, BigDecimal receivedAmount, BigDecimal invoicedAmount) {
        log.debug("reconcileLine called with goodsReceiptLineId={}, invoiceLineId={}", goodsReceiptLineId, invoiceLineId);
        GrirReconciliationRecord record = new GrirReconciliationRecord(goodsReceiptLineId, invoiceLineId, receivedAmount, invoicedAmount);
        GrirReconciliationRecord saved = repository.save(record);
        log.info("GRIR reconciliation record {} created", saved.getId());
        return saved;
    }

    @Transactional
    public GrirReconciliationRecord closeRecord(String id) {
        log.debug("closeRecord called with id={}", id);
        GrirReconciliationRecord record = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("GRIR record not found", "GRIR_RECORD_NOT_FOUND", HttpStatus.NOT_FOUND));
        record.close();
        GrirReconciliationRecord saved = repository.save(record);
        log.info("GRIR record {} closed", id);
        return saved;
    }

    @Transactional(readOnly = true)
    public GrirSummaryReport getSummaryReport() {
        log.debug("getSummaryReport called");
        List<GrirReconciliationRecord> records = repository.findAll();

        int total = records.size();
        int balanced = (int) records.stream().filter(r -> r.getStatus() == GrirReconciliationRecord.Status.BALANCED).count();
        int varianceCount = (int) records.stream().filter(r -> r.getStatus() == GrirReconciliationRecord.Status.VARIANCE).count();

        BigDecimal received = records.stream().map(GrirReconciliationRecord::getReceivedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal invoiced = records.stream().map(GrirReconciliationRecord::getInvoicedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal variance = records.stream().map(GrirReconciliationRecord::getVarianceAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new GrirSummaryReport(total, balanced, varianceCount, received, invoiced, variance);
    }

    public record GrirSummaryReport(
            int totalRecords,
            int balancedCount,
            int varianceCount,
            BigDecimal totalReceived,
            BigDecimal totalInvoiced,
            BigDecimal totalVariance
    ) {
    }
}
