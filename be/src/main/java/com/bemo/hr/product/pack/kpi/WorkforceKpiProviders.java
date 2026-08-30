package com.bemo.hr.product.pack.kpi;

import com.bemo.hr.product.pack.IndustryKpiProvider;
import com.bemo.hr.reporting.infrastructure.AttendanceExceptionRepository;
import com.bemo.hr.reporting.infrastructure.DailyAttendanceResultRepository;
import com.bemo.hr.workforce.ContractorSettlementAdjustmentRepository;
import com.bemo.hr.workforce.LaborRequestRepository;
import com.bemo.hr.workforce.WorkerAssignmentRepository;
import com.bemo.hr.workforce.WorkforceDisputeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

public final class WorkforceKpiProviders {
    private WorkforceKpiProviders() {
    }

    @Component
    @RequiredArgsConstructor
    public static class ContractorFillRateKpiProvider implements IndustryKpiProvider {
        private final LaborRequestRepository laborRequestRepository;
        private final WorkerAssignmentRepository workerAssignmentRepository;

        @Override
        public String key() {
            return "contractorFillRate";
        }

        @Override
        public KpiResult calculate() {
            long requests = laborRequestRepository.count();
            long assignments = workerAssignmentRepository.count();
            double value;
            if (requests == 0) {
                value = assignments > 0 ? 100.0 : 0.0;
            } else {
                value = Math.min(100.0, Math.round((double) assignments / requests * 1000.0) / 10.0);
            }
            String status = value >= 85.0 ? "HEALTHY" : (value > 0 ? "WARNING" : "PENDING");
            return new KpiResult(key(), "kpi." + key(), value, "%", status);
        }
    }

    @Component
    @RequiredArgsConstructor
    public static class AttendanceExceptionRateKpiProvider implements IndustryKpiProvider {
        private final DailyAttendanceResultRepository dailyResultRepository;
        private final AttendanceExceptionRepository exceptionRepository;

        @Override
        public String key() {
            return "attendanceExceptionRate";
        }

        @Override
        public KpiResult calculate() {
            long totalResults = dailyResultRepository.count();
            long exceptions = exceptionRepository.count();
            double value = totalResults == 0 ? 0.0 : Math.min(100.0, Math.round((double) exceptions / totalResults * 1000.0) / 10.0);
            String status = value <= 5.0 ? "HEALTHY" : (value <= 15.0 ? "WARNING" : "CRITICAL");
            return new KpiResult(key(), "kpi." + key(), value, "%", status);
        }
    }

    @Component
    @RequiredArgsConstructor
    public static class SettlementVarianceKpiProvider implements IndustryKpiProvider {
        private final ContractorSettlementAdjustmentRepository adjustmentRepository;

        @Override
        public String key() {
            return "settlementVariance";
        }

        @Override
        public KpiResult calculate() {
            long adjustmentCount = adjustmentRepository.count();
            double value = (double) adjustmentCount * 125.0;
            String status = value == 0.0 ? "HEALTHY" : (value < 5000.0 ? "ACTIVE" : "WARNING");
            return new KpiResult(key(), "kpi." + key(), value, "EGP", status);
        }
    }

    @Component
    @RequiredArgsConstructor
    public static class ContractorReliabilityKpiProvider implements IndustryKpiProvider {
        private final WorkerAssignmentRepository assignmentRepository;
        private final WorkforceDisputeRepository disputeRepository;

        @Override
        public String key() {
            return "contractorReliability";
        }

        @Override
        public KpiResult calculate() {
            long assignments = assignmentRepository.count();
            long disputes = disputeRepository.count();
            double value;
            if (assignments == 0) {
                value = 100.0;
            } else {
                double penalty = (double) disputes / assignments * 100.0;
                value = Math.max(0.0, Math.min(100.0, Math.round((100.0 - penalty) * 10.0) / 10.0));
            }
            String status = value >= 95.0 ? "HEALTHY" : (value >= 80.0 ? "WARNING" : "CRITICAL");
            return new KpiResult(key(), "kpi." + key(), value, "%", status);
        }
    }
}
