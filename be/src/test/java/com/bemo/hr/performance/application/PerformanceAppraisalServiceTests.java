package com.bemo.hr.performance.application;

import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.performance.api.PerformanceAppraisalApi;
import com.bemo.hr.performance.domain.*;
import com.bemo.hr.performance.infrastructure.AppraisalKpiScoreRepository;
import com.bemo.hr.performance.infrastructure.PerformanceAppraisalRepository;
import com.bemo.hr.performance.infrastructure.PerformanceCycleRepository;
import com.bemo.hr.performance.infrastructure.PerformanceKpiRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerformanceAppraisalServiceTests {

    @Mock
    private PerformanceCycleRepository cycleRepository;
    @Mock
    private PerformanceKpiRepository kpiRepository;
    @Mock
    private PerformanceAppraisalRepository appraisalRepository;
    @Mock
    private AppraisalKpiScoreRepository kpiScoreRepository;
    @Mock
    private EmployeeRepository employeeRepository;

    private PerformanceAppraisalService service;

    @BeforeEach
    void setUp() {
        service = new PerformanceAppraisalService(
                cycleRepository,
                kpiRepository,
                appraisalRepository,
                kpiScoreRepository,
                employeeRepository
        );
    }

    @Test
    void createCycle_createsActiveCycle() {
        PerformanceAppraisalApi.CreateCycleRequest request = new PerformanceAppraisalApi.CreateCycleRequest(
                "تقييم 2026",
                "Appraisal 2026",
                2026,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        );

        when(cycleRepository.save(any(PerformanceCycle.class))).thenAnswer(inv -> inv.getArgument(0));

        PerformanceAppraisalApi.PerformanceCycleResponse res = service.createCycle(request);

        assertThat(res).isNotNull();
        assertThat(res.periodYear()).isEqualTo(2026);
        assertThat(res.status()).isEqualTo(CycleStatus.ACTIVE);
    }

    @Test
    void submitAppraisal_calculatesWeightedScoreAndRatingBand() {
        String cycleId = "cyc-1";
        String employeeId = "emp-1";
        String appraisalId = "appr-1";
        String kpiId = "kpi-1";

        PerformanceCycle cycle = new PerformanceCycle("دورة 2026", "Cycle 2026", 2026, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        PerformanceAppraisal appraisal = new PerformanceAppraisal(cycleId, employeeId, "mgr-1");
        PerformanceKpi kpi = new PerformanceKpi(cycleId, "KPI-01", "الجودة", "Quality", KpiCategory.OPERATIONAL, new BigDecimal("100"), new BigDecimal("100"));

        when(appraisalRepository.findById(appraisalId)).thenReturn(Optional.of(appraisal));
        when(cycleRepository.findById(cycleId)).thenReturn(Optional.of(cycle));
        when(kpiRepository.findByCycleIdOrderByCodeAsc(cycleId)).thenReturn(List.of(kpi));
        when(kpiScoreRepository.save(any(AppraisalKpiScore.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appraisalRepository.save(any(PerformanceAppraisal.class))).thenAnswer(inv -> inv.getArgument(0));

        PerformanceAppraisalApi.SubmitAppraisalRequest request = new PerformanceAppraisalApi.SubmitAppraisalRequest(
                List.of(new PerformanceAppraisalApi.KpiScoreInput(kpi.getId(), new BigDecimal("90.0"), new BigDecimal("95.0"), "ممتاز")),
                "أداء استثنائي وتفان في العمل",
                "الاستمرار في قيادة المبادرات"
        );

        PerformanceAppraisalApi.PerformanceAppraisalResponse res = service.submitAppraisal(appraisalId, request);

        assertThat(res).isNotNull();
        assertThat(res.finalScore()).isEqualByComparingTo(new BigDecimal("95.00"));
        assertThat(res.ratingBand()).isEqualTo(RatingBand.OUTSTANDING);
        assertThat(res.status()).isEqualTo(AppraisalStatus.SUBMITTED);
    }

    @Test
    void lockCycle_locksCyclePreventingNewAppraisals() {
        String cycleId = "cyc-1";
        PerformanceCycle cycle = new PerformanceCycle("دورة 2026", "Cycle 2026", 2026, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        when(cycleRepository.findById(cycleId)).thenReturn(Optional.of(cycle));
        when(cycleRepository.save(any(PerformanceCycle.class))).thenAnswer(inv -> inv.getArgument(0));

        PerformanceAppraisalApi.PerformanceCycleResponse res = service.lockCycle(cycleId);
        assertThat(res.status()).isEqualTo(CycleStatus.LOCKED);

        // Verify trying to init appraisal in locked cycle throws exception
        PerformanceAppraisalApi.InitAppraisalRequest initReq = new PerformanceAppraisalApi.InitAppraisalRequest(cycleId, "emp-1", "mgr-1");
        assertThatThrownBy(() -> service.initAppraisal(initReq))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode()).isEqualTo("CYCLE_LOCKED"));
    }
}
