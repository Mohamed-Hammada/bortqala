package com.bemo.hr.manufacturing.quality.application;

import com.bemo.hr.manufacturing.quality.domain.QualityDisposition;
import com.bemo.hr.manufacturing.quality.domain.QualityPlanHeader;
import com.bemo.hr.manufacturing.quality.infrastructure.QualityDispositionRepository;
import com.bemo.hr.manufacturing.quality.infrastructure.QualityPlanHeaderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class QualityPlanService {

    private final QualityPlanHeaderRepository planHeaderRepository;
    private final QualityDispositionRepository dispositionRepository;

    public QualityPlanService(QualityPlanHeaderRepository planHeaderRepository,
                              QualityDispositionRepository dispositionRepository) {
        this.planHeaderRepository = planHeaderRepository;
        this.dispositionRepository = dispositionRepository;
    }

    @Transactional
    public QualityPlanHeader createPlan(String planCode, String name, String itemId, QualityPlanHeader.TargetCategory targetCategory) {
        log.debug("createPlan called with planCode={}, itemId={}", planCode, itemId);
        QualityPlanHeader plan = new QualityPlanHeader(planCode, name, itemId, targetCategory);
        QualityPlanHeader saved = planHeaderRepository.save(plan);
        log.info("QualityPlanHeader {} created successfully", saved.getId());
        return saved;
    }

    @Transactional
    public QualityDisposition recordDisposition(String dispositionNumber, String planId, String inspectionId, QualityDisposition.Result result, String notes) {
        log.debug("recordDisposition called with dispositionNumber={}, planId={}", dispositionNumber, planId);
        QualityDisposition disposition = new QualityDisposition(dispositionNumber, planId, inspectionId, result, notes);
        QualityDisposition saved = dispositionRepository.save(disposition);
        log.info("QualityDisposition {} created successfully", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<QualityPlanHeader> getPlansByItem(String itemId) {
        log.debug("getPlansByItem called with itemId={}", itemId);
        return planHeaderRepository.findByItemId(itemId);
    }
}
