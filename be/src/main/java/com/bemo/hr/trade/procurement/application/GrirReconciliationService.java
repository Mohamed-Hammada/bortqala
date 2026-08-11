package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.procurement.domain.GrirReconciliationRecord;
import com.bemo.hr.trade.procurement.infrastructure.GrirReconciliationRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class GrirReconciliationService {

    private final GrirReconciliationRecordRepository repository;

    public GrirReconciliationService(GrirReconciliationRecordRepository repository) {
        this.repository = repository;
    }

    public record GrirSummaryReport(
            int totalRecords,
            int balancedCount,
            int varianceCount,
            BigDecimal totalReceived,
            BigDecimal totalInvoiced,
            BigDecimal totalVariance
    ) {}

    @Transactional
    public GrirReconciliationRecord reconcileLine(String goodsReceiptLineId, String invoiceLineId, BigDecimal receivedAmount, BigDecimal invoicedAmount) {
        GrirReconciliationRecord record = new GrirReconciliationRecord(goodsReceiptLineId, invoiceLineId, receivedAmount, invoicedAmount);
        return repository.save(record);
    }

    @Transactional
    public GrirReconciliationRecord closeRecord(String id) {
        GrirReconciliationRecord record = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("GRIR record not found", "GRIR_RECORD_NOT_FOUND", HttpStatus.NOT_FOUND));
        record.close();
        return repository.save(record);
    }

    @Transactional(readOnly = true)
    public GrirSummaryReport getSummaryReport() {
        List<GrirReconciliationRecord> records = repository.findAll();

        int total = records.size();
        int balanced = (int) records.stream().filter(r -> r.getStatus() == GrirReconciliationRecord.Status.BALANCED).count();
        int varianceCount = (int) records.stream().filter(r -> r.getStatus() == GrirReconciliationRecord.Status.VARIANCE).count();

        BigDecimal received = records.stream().map(GrirReconciliationRecord::getReceivedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal invoiced = records.stream().map(GrirReconciliationRecord::getInvoicedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal variance = records.stream().map(GrirReconciliationRecord::getVarianceAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new GrirSummaryReport(total, balanced, varianceCount, received, invoiced, variance);
    }
}
