package com.bemo.hr.manufacturing.quality.application;

import com.bemo.hr.manufacturing.quality.domain.QualityDisposition;
import com.bemo.hr.manufacturing.quality.domain.QualityPlanHeader;
import com.bemo.hr.manufacturing.quality.infrastructure.QualityDispositionRepository;
import com.bemo.hr.manufacturing.quality.infrastructure.QualityPlanHeaderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        QualityPlanHeader plan = new QualityPlanHeader(planCode, name, itemId, targetCategory);
        return planHeaderRepository.save(plan);
    }

    @Transactional
    public QualityDisposition recordDisposition(String dispositionNumber, String planId, String inspectionId, QualityDisposition.Result result, String notes) {
        QualityDisposition disposition = new QualityDisposition(dispositionNumber, planId, inspectionId, result, notes);
        return dispositionRepository.save(disposition);
    }

    @Transactional(readOnly = true)
    public List<QualityPlanHeader> getPlansByItem(String itemId) {
        return planHeaderRepository.findByItemId(itemId);
    }
}
