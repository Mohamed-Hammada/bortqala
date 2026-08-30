package com.bemo.hr.product.onboarding.provider;

import com.bemo.hr.attendance.infrastructure.ImportBatchRepository;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.product.onboarding.OnboardingEvidenceProvider;
import com.bemo.hr.workforce.ContractorRepository;
import com.bemo.hr.workforce.ContractorSettlementRepository;
import com.bemo.hr.workforce.WorkerRepository;
import com.bemo.hr.workforce.WorkforceAdvanceRepository;
import com.bemo.hr.workforce.WorkforceImportBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkforceOnboardingEvidenceProvider implements OnboardingEvidenceProvider {
    private final ContractorRepository contractorRepository;
    private final AttendanceCategoryRepository categoryRepository;
    private final WorkerRepository workerRepository;
    private final WorkforceImportBatchRepository workforceImportRepository;
    private final ImportBatchRepository attendanceImportRepository;
    private final WorkforceAdvanceRepository advanceRepository;
    private final ContractorSettlementRepository settlementRepository;

    @Override
    public boolean supports(String packCode, String stepKey) {
        return "CONTRACTOR_WORKFORCE_EG".equals(packCode) && stepKey != null && (
                stepKey.startsWith("industryPack.step.")
                        || stepKey.equals("contractors")
                        || stepKey.equals("categories")
                        || stepKey.equals("workers")
                        || stepKey.equals("attendance")
                        || stepKey.equals("advances")
                        || stepKey.equals("settlement")
        ) && !stepKey.contains("company");
    }

    @Override
    public EvidenceResult evaluate(String packCode, String stepKey) {
        return switch (stepKey) {
            case "industryPack.step.contractors", "contractors" -> {
                long count = contractorRepository.count();
                yield new EvidenceResult(count > 0, count, true, "CONTRACTORS", "onboarding.issue.contractors", "/workforce/contractors");
            }
            case "industryPack.step.categories", "categories" -> {
                long count = categoryRepository.count();
                yield new EvidenceResult(count > 0, count, true, "CATEGORIES", "onboarding.issue.categories", "/categories");
            }
            case "industryPack.step.workers", "workers" -> {
                long count = workerRepository.count();
                yield new EvidenceResult(count > 0, count, true, "WORKERS", "onboarding.issue.workers", "/workforce/workers");
            }
            case "industryPack.step.attendance", "attendance" -> {
                boolean imported = workforceImportRepository.existsByStatus("IMPORTED") || attendanceImportRepository.count() > 0;
                long count = imported ? 1 : 0;
                yield new EvidenceResult(imported, count, true, "IMPORT", "onboarding.issue.import", "/imports");
            }
            case "industryPack.step.advances", "advances" -> {
                long count = advanceRepository.count();
                yield new EvidenceResult(count > 0, count, false, "ADVANCES", "industryPack.step.advances", "/workforce/advances");
            }
            case "industryPack.step.settlement", "settlement" -> {
                long count = settlementRepository.count();
                yield new EvidenceResult(count > 0, count, true, "SETTLEMENT", "onboarding.issue.settlement", "/workforce/settlement-periods");
            }
            default -> new EvidenceResult(false, 0, false, "UNKNOWN", stepKey, "");
        };
    }
}
