package com.bemo.hr.manufacturing.quality.application;

import com.bemo.hr.manufacturing.quality.domain.QualityStockDisposition;
import com.bemo.hr.manufacturing.quality.infrastructure.QualityStockDispositionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class QualityStockDispositionService {

    private final QualityStockDispositionRepository repository;

    public QualityStockDispositionService(QualityStockDispositionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public QualityStockDisposition createDisposition(String inspectionId, String dispositionType, BigDecimal quantity, String reason) {
        log.debug("createDisposition called with inspectionId={}, dispositionType={}", inspectionId, dispositionType);
        QualityStockDisposition disposition = new QualityStockDisposition(inspectionId, dispositionType, quantity, reason);
        QualityStockDisposition saved = repository.save(disposition);
        log.info("QualityStockDisposition {} created successfully", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<QualityStockDisposition> getDispositionsForInspection(String inspectionId) {
        log.debug("getDispositionsForInspection called with inspectionId={}", inspectionId);
        return repository.findByInspectionId(inspectionId);
    }
}
