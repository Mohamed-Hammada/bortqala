package com.bemo.hr.manufacturing.quality.application;

import com.bemo.hr.manufacturing.quality.domain.QualityStockDisposition;
import com.bemo.hr.manufacturing.quality.infrastructure.QualityStockDispositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class QualityStockDispositionService {

    private final QualityStockDispositionRepository repository;

    public QualityStockDispositionService(QualityStockDispositionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public QualityStockDisposition createDisposition(String inspectionId, String dispositionType, BigDecimal quantity, String reason) {
        QualityStockDisposition disposition = new QualityStockDisposition(inspectionId, dispositionType, quantity, reason);
        return repository.save(disposition);
    }

    @Transactional(readOnly = true)
    public List<QualityStockDisposition> getDispositionsForInspection(String inspectionId) {
        return repository.findByInspectionId(inspectionId);
    }
}
