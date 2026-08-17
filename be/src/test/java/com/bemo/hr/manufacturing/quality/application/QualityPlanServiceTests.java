package com.bemo.hr.manufacturing.quality.application;

import com.bemo.hr.manufacturing.quality.domain.QualityDisposition;
import com.bemo.hr.manufacturing.quality.domain.QualityPlanHeader;
import com.bemo.hr.manufacturing.quality.infrastructure.QualityDispositionRepository;
import com.bemo.hr.manufacturing.quality.infrastructure.QualityPlanHeaderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QualityPlanServiceTests {

    private QualityPlanHeaderRepository planHeaderRepository;
    private QualityDispositionRepository dispositionRepository;
    private QualityPlanService qualityPlanService;

    @BeforeEach
    void setUp() {
        planHeaderRepository = mock(QualityPlanHeaderRepository.class);
        dispositionRepository = mock(QualityDispositionRepository.class);
        qualityPlanService = new QualityPlanService(planHeaderRepository, dispositionRepository);
    }

    @Test
    void createsPlanAndRecordsDispositionSuccessfully() {
        when(planHeaderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(dispositionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        QualityPlanHeader plan = qualityPlanService.createPlan("QP-001", "Raw Material Check", "item-10", QualityPlanHeader.TargetCategory.INCOMING_INSPECTION);
        assertThat(plan).isNotNull();
        assertThat(plan.getPlanCode()).isEqualTo("QP-001");

        QualityDisposition disp = qualityPlanService.recordDisposition("DISP-001", plan.getId(), "insp-1", QualityDisposition.Result.PASSED, "Passed visual and spec inspection");
        assertThat(disp).isNotNull();
        assertThat(disp.getDispositionResult()).isEqualTo(QualityDisposition.Result.PASSED);
    }
}
