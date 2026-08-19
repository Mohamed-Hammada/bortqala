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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PerformanceAppraisalService {

    private final PerformanceCycleRepository cycleRepository;
    private final PerformanceKpiRepository kpiRepository;
    private final PerformanceAppraisalRepository appraisalRepository;
    private final AppraisalKpiScoreRepository kpiScoreRepository;
    private final EmployeeRepository employeeRepository;

    public PerformanceAppraisalService(PerformanceCycleRepository cycleRepository,
                                       PerformanceKpiRepository kpiRepository,
                                       PerformanceAppraisalRepository appraisalRepository,
                                       AppraisalKpiScoreRepository kpiScoreRepository,
                                       EmployeeRepository employeeRepository) {
        this.cycleRepository = cycleRepository;
        this.kpiRepository = kpiRepository;
        this.appraisalRepository = appraisalRepository;
        this.kpiScoreRepository = kpiScoreRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public List<PerformanceAppraisalApi.PerformanceCycleResponse> listCycles() {
        return cycleRepository.findAllByOrderByPeriodYearDescCreatedAtDesc().stream()
                .map(this::toCycleResponse)
                .toList();
    }

    @Transactional
    public PerformanceAppraisalApi.PerformanceCycleResponse createCycle(PerformanceAppraisalApi.CreateCycleRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessRuleException("تاريخ نهاية الدورة يسبق تاريخ البداية", "INVALID_CYCLE_DATES", HttpStatus.BAD_REQUEST);
        }
        int year = request.periodYear() > 0 ? request.periodYear() : request.startDate().getYear();
        PerformanceCycle cycle = new PerformanceCycle(
                request.nameAr(),
                request.nameEn(),
                year,
                request.startDate(),
                request.endDate()
        );
        PerformanceCycle saved = cycleRepository.save(cycle);
        log.info("PerformanceCycle created: {}", saved.getId());
        return toCycleResponse(saved);
    }

    @Transactional
    public PerformanceAppraisalApi.PerformanceCycleResponse lockCycle(String id) {
        PerformanceCycle cycle = cycleRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("دورة التقييم غير موجودة", "CYCLE_NOT_FOUND", HttpStatus.NOT_FOUND));
        cycle.lock();
        PerformanceCycle saved = cycleRepository.save(cycle);
        log.info("PerformanceCycle locked: {}", saved.getId());
        return toCycleResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PerformanceAppraisalApi.PerformanceKpiResponse> listKpis(String cycleId) {
        List<PerformanceKpi> list = (cycleId != null && !cycleId.isBlank())
                ? kpiRepository.findByCycleIdOrderByCodeAsc(cycleId)
                : kpiRepository.findAllByOrderByCodeAsc();
        return list.stream().map(this::toKpiResponse).toList();
    }

    @Transactional
    public PerformanceAppraisalApi.PerformanceKpiResponse createKpi(PerformanceAppraisalApi.CreateKpiRequest request) {
        cycleRepository.findById(request.cycleId())
                .orElseThrow(() -> new BusinessRuleException("دورة التقييم غير موجودة", "CYCLE_NOT_FOUND", HttpStatus.NOT_FOUND));

        PerformanceKpi kpi = new PerformanceKpi(
                request.cycleId(),
                request.code(),
                request.titleAr(),
                request.titleEn(),
                request.category(),
                request.targetValue(),
                request.weightPercentage()
        );
        PerformanceKpi saved = kpiRepository.save(kpi);
        log.info("PerformanceKpi created: {}", saved.getCode());
        return toKpiResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PerformanceAppraisalApi.PerformanceAppraisalResponse> listAppraisals(String cycleId, String employeeId) {
        List<PerformanceAppraisal> list;
        if (cycleId != null && !cycleId.isBlank()) {
            list = appraisalRepository.findByCycleIdOrderByCreatedAtDesc(cycleId);
        } else if (employeeId != null && !employeeId.isBlank()) {
            list = appraisalRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
        } else {
            list = appraisalRepository.findAllByOrderByCreatedAtDesc();
        }

        Map<String, PerformanceCycle> cycleMap = cycleRepository.findAll().stream()
                .collect(Collectors.toMap(PerformanceCycle::getId, c -> c, (a, b) -> a));
        Map<String, Employee> empMap = employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));
        Map<String, PerformanceKpi> kpiMap = kpiRepository.findAll().stream()
                .collect(Collectors.toMap(PerformanceKpi::getId, k -> k, (a, b) -> a));

        return list.stream()
                .map(appr -> {
                    List<AppraisalKpiScore> scores = kpiScoreRepository.findByAppraisalId(appr.getId());
                    return toAppraisalResponse(appr, cycleMap.get(appr.getCycleId()), empMap.get(appr.getEmployeeId()), empMap.get(appr.getReviewerId()), scores, kpiMap);
                })
                .toList();
    }

    @Transactional
    public PerformanceAppraisalApi.PerformanceAppraisalResponse initAppraisal(PerformanceAppraisalApi.InitAppraisalRequest request) {
        PerformanceCycle cycle = cycleRepository.findById(request.cycleId())
                .orElseThrow(() -> new BusinessRuleException("دورة التقييم غير موجودة", "CYCLE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (cycle.getStatus() == CycleStatus.LOCKED || cycle.getStatus() == CycleStatus.CLOSED) {
            throw new BusinessRuleException("دورة التقييم مقفلة ولا يمكن إنشاء تقييم جديد فيها", "CYCLE_LOCKED", HttpStatus.BAD_REQUEST);
        }

        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new BusinessRuleException("الموظف غير موجود", "EMPLOYEE_NOT_FOUND", HttpStatus.NOT_FOUND));

        PerformanceAppraisal appraisal = appraisalRepository.findByCycleIdAndEmployeeId(request.cycleId(), request.employeeId())
                .orElseGet(() -> new PerformanceAppraisal(request.cycleId(), request.employeeId(), request.reviewerId()));

        PerformanceAppraisal saved = appraisalRepository.save(appraisal);
        Employee reviewer = request.reviewerId() != null ? employeeRepository.findById(request.reviewerId()).orElse(null) : null;
        return toAppraisalResponse(saved, cycle, employee, reviewer, List.of(), Map.of());
    }

    @Transactional
    public PerformanceAppraisalApi.PerformanceAppraisalResponse submitAppraisal(String appraisalId, PerformanceAppraisalApi.SubmitAppraisalRequest request) {
        PerformanceAppraisal appraisal = appraisalRepository.findById(appraisalId)
                .orElseThrow(() -> new BusinessRuleException("التقييم غير موجود", "APPRAISAL_NOT_FOUND", HttpStatus.NOT_FOUND));

        PerformanceCycle cycle = cycleRepository.findById(appraisal.getCycleId())
                .orElseThrow(() -> new BusinessRuleException("دورة التقييم غير موجودة", "CYCLE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (cycle.getStatus() == CycleStatus.LOCKED || cycle.getStatus() == CycleStatus.CLOSED) {
            throw new BusinessRuleException("دورة التقييم مقفلة", "CYCLE_LOCKED", HttpStatus.BAD_REQUEST);
        }

        if (appraisal.getStatus() == AppraisalStatus.FINALIZED) {
            throw new BusinessRuleException("التقييم معتمد نهائياً ومقفل", "APPRAISAL_ALREADY_FINALIZED", HttpStatus.BAD_REQUEST);
        }

        kpiScoreRepository.deleteByAppraisalId(appraisalId);

        BigDecimal totalScore = BigDecimal.ZERO;
        List<AppraisalKpiScore> savedScores = new ArrayList<>();
        Map<String, PerformanceKpi> kpiMap = kpiRepository.findByCycleIdOrderByCodeAsc(appraisal.getCycleId()).stream()
                .collect(Collectors.toMap(PerformanceKpi::getId, k -> k, (a, b) -> a));

        if (request.kpiScores() != null) {
            for (PerformanceAppraisalApi.KpiScoreInput input : request.kpiScores()) {
                PerformanceKpi kpi = kpiMap.get(input.kpiId());
                BigDecimal rating = input.managerRating() != null ? input.managerRating() : (input.selfRating() != null ? input.selfRating() : BigDecimal.ZERO);
                BigDecimal weight = kpi != null ? kpi.getWeightPercentage() : new BigDecimal("20.0");
                BigDecimal weighted = rating.multiply(weight).divide(new BigDecimal("100.0"), 2, RoundingMode.HALF_UP);
                totalScore = totalScore.add(weighted);

                AppraisalKpiScore score = new AppraisalKpiScore(
                        appraisalId,
                        input.kpiId(),
                        input.selfRating(),
                        input.managerRating(),
                        weighted,
                        input.comments()
                );
                savedScores.add(kpiScoreRepository.save(score));
            }
        }

        appraisal.evaluate(totalScore, request.managerFeedback(), request.developmentPlan());
        PerformanceAppraisal saved = appraisalRepository.save(appraisal);

        Employee employee = employeeRepository.findById(appraisal.getEmployeeId()).orElse(null);
        Employee reviewer = appraisal.getReviewerId() != null ? employeeRepository.findById(appraisal.getReviewerId()).orElse(null) : null;
        log.info("Appraisal {} evaluated with final score {}", saved.getId(), totalScore);
        return toAppraisalResponse(saved, cycle, employee, reviewer, savedScores, kpiMap);
    }

    @Transactional
    public PerformanceAppraisalApi.PerformanceAppraisalResponse finalizeAppraisal(String appraisalId) {
        PerformanceAppraisal appraisal = appraisalRepository.findById(appraisalId)
                .orElseThrow(() -> new BusinessRuleException("التقييم غير موجود", "APPRAISAL_NOT_FOUND", HttpStatus.NOT_FOUND));

        appraisal.finalizeAppraisal();
        PerformanceAppraisal saved = appraisalRepository.save(appraisal);

        PerformanceCycle cycle = cycleRepository.findById(appraisal.getCycleId()).orElse(null);
        Employee employee = employeeRepository.findById(appraisal.getEmployeeId()).orElse(null);
        Employee reviewer = appraisal.getReviewerId() != null ? employeeRepository.findById(appraisal.getReviewerId()).orElse(null) : null;
        List<AppraisalKpiScore> scores = kpiScoreRepository.findByAppraisalId(appraisalId);
        Map<String, PerformanceKpi> kpiMap = kpiRepository.findAll().stream().collect(Collectors.toMap(PerformanceKpi::getId, k -> k, (a, b) -> a));

        log.info("Appraisal {} finalized", saved.getId());
        return toAppraisalResponse(saved, cycle, employee, reviewer, scores, kpiMap);
    }

    private PerformanceAppraisalApi.PerformanceCycleResponse toCycleResponse(PerformanceCycle c) {
        return new PerformanceAppraisalApi.PerformanceCycleResponse(
                c.getId(),
                c.getNameAr(),
                c.getNameEn(),
                c.getPeriodYear(),
                c.getStartDate(),
                c.getEndDate(),
                c.getStatus(),
                c.getCreatedAt()
        );
    }

    private PerformanceAppraisalApi.PerformanceKpiResponse toKpiResponse(PerformanceKpi k) {
        return new PerformanceAppraisalApi.PerformanceKpiResponse(
                k.getId(),
                k.getCycleId(),
                k.getCode(),
                k.getTitleAr(),
                k.getTitleEn(),
                k.getCategory(),
                k.getTargetValue(),
                k.getWeightPercentage(),
                k.getCreatedAt()
        );
    }

    private PerformanceAppraisalApi.PerformanceAppraisalResponse toAppraisalResponse(
            PerformanceAppraisal a,
            PerformanceCycle c,
            Employee emp,
            Employee reviewer,
            List<AppraisalKpiScore> scores,
            Map<String, PerformanceKpi> kpiMap) {

        List<PerformanceAppraisalApi.AppraisalKpiScoreResponse> scoreDtos = scores.stream().map(s -> {
            PerformanceKpi kpi = kpiMap.get(s.getKpiId());
            return new PerformanceAppraisalApi.AppraisalKpiScoreResponse(
                    s.getId(),
                    s.getKpiId(),
                    kpi != null ? kpi.getCode() : s.getKpiId(),
                    kpi != null ? kpi.getTitleAr() : s.getKpiId(),
                    kpi != null ? kpi.getTitleEn() : s.getKpiId(),
                    kpi != null ? kpi.getCategory() : KpiCategory.OPERATIONAL,
                    kpi != null ? kpi.getWeightPercentage() : BigDecimal.ZERO,
                    s.getSelfRating(),
                    s.getManagerRating(),
                    s.getWeightedScore(),
                    s.getComments()
            );
        }).toList();

        return new PerformanceAppraisalApi.PerformanceAppraisalResponse(
                a.getId(),
                a.getCycleId(),
                c != null ? c.getNameAr() : a.getCycleId(),
                c != null ? c.getNameEn() : a.getCycleId(),
                a.getEmployeeId(),
                emp != null ? emp.getFullName() : a.getEmployeeId(),
                emp != null ? emp.getEmployeeCode() : a.getEmployeeId(),
                a.getReviewerId(),
                reviewer != null ? reviewer.getFullName() : (a.getReviewerId() != null ? a.getReviewerId() : "—"),
                a.getSelfScore(),
                a.getManagerScore(),
                a.getFinalScore(),
                a.getRatingBand(),
                a.getStatus(),
                a.getManagerFeedback(),
                a.getDevelopmentPlan(),
                scoreDtos,
                a.getCreatedAt(),
                a.getUpdatedAt(),
                a.getVersion()
        );
    }
}
