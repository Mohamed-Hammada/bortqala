package com.bemo.hr.medical.application;

import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.domain.*;
import com.bemo.hr.medical.infrastructure.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class InsuranceService {

    private final InsurancePayerRepository payerRepository;
    private final InsurancePlanRepository planRepository;
    private final PatientInsurancePolicyRepository policyRepository;
    private final InsurancePreAuthorizationRepository preAuthRepository;
    private final InsuranceClaimBatchRepository batchRepository;
    private final InsuranceClaimLineRepository lineRepository;
    private final PatientRepository patientRepository;
    private final ClinicVisitRepository visitRepository;

    public InsuranceService(InsurancePayerRepository payerRepository,
                            InsurancePlanRepository planRepository,
                            PatientInsurancePolicyRepository policyRepository,
                            InsurancePreAuthorizationRepository preAuthRepository,
                            InsuranceClaimBatchRepository batchRepository,
                            InsuranceClaimLineRepository lineRepository,
                            PatientRepository patientRepository,
                            ClinicVisitRepository visitRepository) {
        this.payerRepository = payerRepository;
        this.planRepository = planRepository;
        this.policyRepository = policyRepository;
        this.preAuthRepository = preAuthRepository;
        this.batchRepository = batchRepository;
        this.lineRepository = lineRepository;
        this.patientRepository = patientRepository;
        this.visitRepository = visitRepository;
    }

    public InsurancePayerDto savePayer(SaveInsurancePayerRequest req) {
        String appId = TenantContext.require();
        InsurancePayer.Type type = InsurancePayer.Type.PRIVATE;
        try {
            type = InsurancePayer.Type.valueOf(req.type().trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {}

        InsurancePayer payer = new InsurancePayer(req.name(), type, req.contactPhone(), req.contactEmail());
        if (req.active() != null) {
            payer.setActive(req.active());
        }
        InsurancePayer saved = payerRepository.save(payer);
        log.info("Saved insurance payer {} ({}) in tenant {}", saved.getName(), saved.getType(), appId);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<InsurancePayerDto> getAllPayers(boolean activeOnly) {
        String appId = TenantContext.require();
        if (activeOnly) {
            return payerRepository.findAllByAppIdAndActiveTrueOrderByNameAsc(appId).stream().map(this::toDto).toList();
        }
        return payerRepository.findAllByAppIdOrderByNameAsc(appId).stream().map(this::toDto).toList();
    }

    public InsurancePlanDto savePlan(SaveInsurancePlanRequest req) {
        String appId = TenantContext.require();
        payerRepository.findByAppIdAndId(appId, req.payerId())
                .orElseThrow(() -> new NotFoundException("Payer not found", "INS_PAYER_NOT_FOUND"));

        InsurancePlan plan = new InsurancePlan(
                req.payerId(),
                req.name(),
                req.coveragePercent(),
                req.copayFlat(),
                req.annualLimit(),
                req.exclusionsText()
        );
        if (req.active() != null) {
            plan.setActive(req.active());
        }
        InsurancePlan saved = planRepository.save(plan);
        log.info("Saved insurance plan {} for payer {} in tenant {}", saved.getName(), saved.getPayerId(), appId);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<InsurancePlanDto> getPlansByPayer(String payerId) {
        String appId = TenantContext.require();
        if (payerId != null && !payerId.trim().isEmpty()) {
            return planRepository.findAllByAppIdAndPayerIdOrderByNameAsc(appId, payerId).stream().map(this::toDto).toList();
        }
        return planRepository.findAllByAppIdOrderByNameAsc(appId).stream().map(this::toDto).toList();
    }

    public PatientInsurancePolicyDto attachPolicy(AttachInsurancePolicyRequest req) {
        String appId = TenantContext.require();
        patientRepository.findByAppIdAndId(appId, req.patientId())
                .orElseThrow(() -> new NotFoundException("Patient not found", "PATIENT_NOT_FOUND"));
        InsurancePlan plan = planRepository.findByAppIdAndId(appId, req.planId())
                .orElseThrow(() -> new NotFoundException("Plan not found", "INS_PLAN_NOT_FOUND"));

        boolean isPrimary = req.isPrimary() == null || req.isPrimary();
        if (isPrimary) {
            policyRepository.findByAppIdAndPatientIdAndIsPrimaryTrue(appId, req.patientId())
                    .ifPresent(existing -> {
                        existing.setPrimary(false);
                        policyRepository.save(existing);
                    });
        }

        PatientInsurancePolicy policy = new PatientInsurancePolicy(
                req.patientId(),
                req.planId(),
                req.memberNumber(),
                req.validFrom(),
                req.validTo(),
                isPrimary
        );
        PatientInsurancePolicy saved = policyRepository.save(policy);
        log.info("Attached policy {} to patient {} in tenant {}", saved.getMemberNumber(), saved.getPatientId(), appId);
        return toDto(saved, plan);
    }

    @Transactional(readOnly = true)
    public List<PatientInsurancePolicyDto> getPatientPolicies(String patientId) {
        String appId = TenantContext.require();
        return policyRepository.findAllByAppIdAndPatientIdOrderByCreatedAtDesc(appId, patientId)
                .stream().map(pol -> {
                    InsurancePlan plan = planRepository.findByAppIdAndId(appId, pol.getPlanId()).orElse(null);
                    return toDto(pol, plan);
                }).toList();
    }

    @Transactional(readOnly = true)
    public InsuranceSplitCalculationResult calculateSplit(CalculateInsuranceSplitRequest req) {
        String appId = TenantContext.require();
        BigDecimal totalFee = req.feeCharged() != null ? req.feeCharged() : BigDecimal.ZERO;
        String today = req.visitDate() != null && !req.visitDate().trim().isEmpty()
                ? req.visitDate()
                : LocalDate.now().toString();

        Optional<PatientInsurancePolicy> optPolicy = policyRepository.findByAppIdAndPatientIdAndIsPrimaryTrue(appId, req.patientId());
        if (optPolicy.isEmpty()) {
            return new InsuranceSplitCalculationResult(totalFee, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, totalFee, false);
        }

        PatientInsurancePolicy policy = optPolicy.get();
        if (policy.getValidTo().compareTo(today) < 0) {
            log.warn("Policy {} expired on {} for patient {}", policy.getMemberNumber(), policy.getValidTo(), req.patientId());
            return new InsuranceSplitCalculationResult(totalFee, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, totalFee, false);
        }

        InsurancePlan plan = planRepository.findByAppIdAndId(appId, policy.getPlanId()).orElse(null);
        if (plan == null) {
            return new InsuranceSplitCalculationResult(totalFee, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, totalFee, false);
        }

        BigDecimal coverage = plan.getCoveragePercent() != null ? plan.getCoveragePercent() : BigDecimal.valueOf(80);
        BigDecimal copay = plan.getCopayFlat() != null ? plan.getCopayFlat() : BigDecimal.ZERO;

        // patientShare = totalFee * (100 - coverage) / 100 + copay
        BigDecimal patientPctShare = totalFee.multiply(BigDecimal.valueOf(100).subtract(coverage))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal rawPatientShare = patientPctShare.add(copay);

        BigDecimal patientShare = rawPatientShare.max(BigDecimal.ZERO).min(totalFee);
        BigDecimal insurerShare = totalFee.subtract(patientShare).max(BigDecimal.ZERO);

        return new InsuranceSplitCalculationResult(totalFee, coverage, copay, insurerShare, patientShare, true);
    }

    public InsurancePreAuthorizationDto requestPreAuthorization(RequestPreAuthorizationRequest req) {
        String appId = TenantContext.require();
        payerRepository.findByAppIdAndId(appId, req.payerId())
                .orElseThrow(() -> new NotFoundException("Payer not found", "INS_PAYER_NOT_FOUND"));
        patientRepository.findByAppIdAndId(appId, req.patientId())
                .orElseThrow(() -> new NotFoundException("Patient not found", "PATIENT_NOT_FOUND"));

        InsurancePreAuthorization preAuth = new InsurancePreAuthorization(
                req.payerId(),
                req.patientId(),
                req.visitId(),
                req.procedureText(),
                req.approvalCode(),
                req.requestedAmount()
        );
        InsurancePreAuthorization saved = preAuthRepository.save(preAuth);
        log.info("Requested pre-auth code {} for patient {} in tenant {}", saved.getApprovalCode(), saved.getPatientId(), appId);
        return toDto(saved);
    }

    public InsurancePreAuthorizationDto decidePreAuthorization(String id, DecidePreAuthorizationRequest req) {
        String appId = TenantContext.require();
        InsurancePreAuthorization preAuth = preAuthRepository.findByAppIdAndId(appId, id)
                .orElseThrow(() -> new NotFoundException("Pre-auth not found", "INS_PRE_AUTH_NOT_FOUND"));

        if ("APPROVED".equalsIgnoreCase(req.status())) {
            preAuth.approve(req.approvedAmount());
        } else {
            preAuth.reject();
        }
        return toDto(preAuthRepository.save(preAuth));
    }

    @Transactional(readOnly = true)
    public List<InsurancePreAuthorizationDto> getPreAuthorizations(String status, String patientId) {
        String appId = TenantContext.require();
        if (patientId != null && !patientId.trim().isEmpty()) {
            return preAuthRepository.findAllByAppIdAndPatientIdOrderByCreatedAtDesc(appId, patientId)
                    .stream().map(this::toDto).toList();
        }
        if (status != null && !status.trim().isEmpty()) {
            try {
                InsurancePreAuthorization.Status st = InsurancePreAuthorization.Status.valueOf(status.trim().toUpperCase());
                return preAuthRepository.findAllByAppIdAndStatusOrderByCreatedAtDesc(appId, st)
                        .stream().map(this::toDto).toList();
            } catch (IllegalArgumentException ignored) {}
        }
        return preAuthRepository.findAllByAppIdOrderByCreatedAtDesc(appId)
                .stream().map(this::toDto).toList();
    }

    public InsuranceClaimBatchDto createClaimBatch(CreateClaimBatchRequest req) {
        String appId = TenantContext.require();
        InsurancePayer payer = payerRepository.findByAppIdAndId(appId, req.payerId())
                .orElseThrow(() -> new NotFoundException("Payer not found", "INS_PAYER_NOT_FOUND"));

        String batchNumber = "BATCH-" + req.period() + "-" + (int)(Math.random() * 9000 + 1000);
        InsuranceClaimBatch batch = new InsuranceClaimBatch(batchNumber, payer.getId(), req.period(), req.notes());
        InsuranceClaimBatch savedBatch = batchRepository.save(batch);

        // Find visits for this period
        List<ClinicVisit> visits = visitRepository.findCompletedVisitsInPeriod(appId, req.period());
        BigDecimal totalClaimed = BigDecimal.ZERO;
        List<InsuranceClaimLine> lines = new ArrayList<>();

        for (ClinicVisit v : visits) {
            if (v.getInsuranceCovered() != null && v.getInsuranceCovered().compareTo(BigDecimal.ZERO) > 0) {
                Patient p = patientRepository.findByAppIdAndId(appId, v.getPatientId()).orElse(null);
                String pMrn = p != null ? p.getMrn() : "—";
                String pName = p != null ? p.getFullName() : "Patient " + v.getPatientId();

                InsuranceClaimLine line = new InsuranceClaimLine(
                        savedBatch.getId(),
                        v.getId(),
                        v.getPatientId(),
                        pMrn,
                        pName,
                        "MEM-" + v.getPatientId().substring(0, Math.min(6, v.getPatientId().length())),
                        v.getChiefComplaint() != null ? v.getChiefComplaint() : "Clinic Consultation",
                        v.getFeeCharged() != null ? v.getFeeCharged() : BigDecimal.ZERO,
                        v.getInsuranceCovered(),
                        v.getPatientShare() != null ? v.getPatientShare() : BigDecimal.ZERO
                );
                lines.add(lineRepository.save(line));
                totalClaimed = totalClaimed.add(v.getInsuranceCovered());
            }
        }

        savedBatch.setTotalClaimedAmount(totalClaimed);
        InsuranceClaimBatch finalBatch = batchRepository.save(savedBatch);
        log.info("Created claim batch {} with {} lines for payer {} in tenant {}", finalBatch.getBatchNumber(), lines.size(), payer.getName(), appId);
        return toDto(finalBatch, lines);
    }

    public InsuranceClaimBatchDto submitClaimBatch(String batchId) {
        String appId = TenantContext.require();
        InsuranceClaimBatch batch = batchRepository.findByAppIdAndId(appId, batchId)
                .orElseThrow(() -> new NotFoundException("Claim batch not found", "INS_CLAIM_BATCH_NOT_FOUND"));

        if (batch.getStatus() != InsuranceClaimBatch.Status.DRAFT) {
            throw new BusinessRuleException("Cannot modify claim batch after submission", "INS_CLAIM_BATCH_NOT_DRAFT", HttpStatus.CONFLICT);
        }

        batch.submit();
        InsuranceClaimBatch saved = batchRepository.save(batch);
        List<InsuranceClaimLine> lines = lineRepository.findAllByAppIdAndBatchIdOrderByCreatedAtAsc(appId, batchId);
        return toDto(saved, lines);
    }

    public InsuranceClaimBatchDto settleClaimBatch(String batchId, SettleClaimBatchRequest req) {
        String appId = TenantContext.require();
        InsuranceClaimBatch batch = batchRepository.findByAppIdAndId(appId, batchId)
                .orElseThrow(() -> new NotFoundException("Claim batch not found", "INS_CLAIM_BATCH_NOT_FOUND"));

        List<InsuranceClaimLine> lines = lineRepository.findAllByAppIdAndBatchIdOrderByCreatedAtAsc(appId, batchId);
        BigDecimal approvedSum = BigDecimal.ZERO;
        BigDecimal rejectedSum = BigDecimal.ZERO;

        for (InsuranceClaimLine line : lines) {
            if (req.lineDecisions() != null) {
                for (SettleLineDecision d : req.lineDecisions()) {
                    if (d.lineId().equals(line.getId())) {
                        if ("APPROVED".equalsIgnoreCase(d.decision())) {
                            line.approve();
                        } else {
                            line.reject(d.rejectionReason());
                        }
                        lineRepository.save(line);
                        break;
                    }
                }
            }
            if (line.getStatus() == InsuranceClaimLine.Status.APPROVED) {
                approvedSum = approvedSum.add(line.getInsurerShare());
            } else if (line.getStatus() == InsuranceClaimLine.Status.REJECTED) {
                rejectedSum = rejectedSum.add(line.getInsurerShare());
            }
        }

        batch.settle(approvedSum, rejectedSum);
        if (req.notes() != null) {
            batch.setNotes(req.notes());
        }
        InsuranceClaimBatch saved = batchRepository.save(batch);
        log.info("Settled claim batch {} (approved: {}, rejected: {}) in tenant {}", saved.getBatchNumber(), approvedSum, rejectedSum, appId);
        return toDto(saved, lines);
    }

    public InsuranceClaimLineDto resubmitClaimLine(ResubmitClaimLineRequest req) {
        String appId = TenantContext.require();
        InsuranceClaimLine orig = lineRepository.findByAppIdAndId(appId, req.originalLineId())
                .orElseThrow(() -> new NotFoundException("Original claim line not found", "INS_CLAIM_LINE_NOT_FOUND"));

        InsuranceClaimBatch targetBatch = batchRepository.findByAppIdAndId(appId, req.newBatchId())
                .orElseThrow(() -> new NotFoundException("Target claim batch not found", "INS_CLAIM_BATCH_NOT_FOUND"));

        if (targetBatch.getStatus() != InsuranceClaimBatch.Status.DRAFT) {
            throw new BusinessRuleException("Target batch must be in DRAFT status", "INS_CLAIM_BATCH_NOT_DRAFT", HttpStatus.CONFLICT);
        }

        BigDecimal adjustedShare = req.adjustedInsurerShare() != null ? req.adjustedInsurerShare() : orig.getInsurerShare();

        InsuranceClaimLine newLine = new InsuranceClaimLine(
                targetBatch.getId(),
                orig.getVisitId(),
                orig.getPatientId(),
                orig.getPatientMrn(),
                orig.getPatientName(),
                orig.getMemberNumber(),
                orig.getProcedureText() + " (Resubmitted)",
                orig.getTotalFee(),
                adjustedShare,
                orig.getTotalFee().subtract(adjustedShare)
        );
        newLine.setResubmittedLineId(orig.getId());

        InsuranceClaimLine saved = lineRepository.save(newLine);
        targetBatch.setTotalClaimedAmount(targetBatch.getTotalClaimedAmount().add(adjustedShare));
        batchRepository.save(targetBatch);

        log.info("Resubmitted line {} as {} into batch {} in tenant {}", orig.getId(), saved.getId(), targetBatch.getBatchNumber(), appId);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<InsuranceClaimBatchDto> getAllClaimBatches(String payerId) {
        String appId = TenantContext.require();
        List<InsuranceClaimBatch> batches = (payerId != null && !payerId.trim().isEmpty())
                ? batchRepository.findAllByAppIdAndPayerIdOrderByCreatedAtDesc(appId, payerId)
                : batchRepository.findAllByAppIdOrderByCreatedAtDesc(appId);

        return batches.stream().map(b -> {
            List<InsuranceClaimLine> lines = lineRepository.findAllByAppIdAndBatchIdOrderByCreatedAtAsc(appId, b.getId());
            return toDto(b, lines);
        }).toList();
    }

    private InsurancePayerDto toDto(InsurancePayer p) {
        return new InsurancePayerDto(p.getId(), p.getName(), p.getType().name(), p.getContactPhone(), p.getContactEmail(), p.isActive());
    }

    private InsurancePlanDto toDto(InsurancePlan pl) {
        return new InsurancePlanDto(pl.getId(), pl.getPayerId(), pl.getName(), pl.getCoveragePercent(), pl.getCopayFlat(), pl.getAnnualLimit(), pl.getExclusionsText(), pl.isActive());
    }

    private PatientInsurancePolicyDto toDto(PatientInsurancePolicy pol, InsurancePlan plan) {
        String appId = TenantContext.require();
        String planName = plan != null ? plan.getName() : "—";
        String payerId = plan != null ? plan.getPayerId() : "";
        String payerName = plan != null ? payerRepository.findByAppIdAndId(appId, plan.getPayerId()).map(InsurancePayer::getName).orElse("—") : "—";

        return new PatientInsurancePolicyDto(
                pol.getId(),
                pol.getPatientId(),
                pol.getPlanId(),
                planName,
                payerId,
                payerName,
                pol.getMemberNumber(),
                pol.getValidFrom(),
                pol.getValidTo(),
                pol.isPrimary()
        );
    }

    private InsurancePreAuthorizationDto toDto(InsurancePreAuthorization pa) {
        String appId = TenantContext.require();
        String payerName = payerRepository.findByAppIdAndId(appId, pa.getPayerId()).map(InsurancePayer::getName).orElse("—");
        Patient p = patientRepository.findByAppIdAndId(appId, pa.getPatientId()).orElse(null);
        String pMrn = p != null ? p.getMrn() : "—";
        String pName = p != null ? p.getFullName() : "—";

        return new InsurancePreAuthorizationDto(
                pa.getId(),
                pa.getPayerId(),
                payerName,
                pa.getPatientId(),
                pMrn,
                pName,
                pa.getVisitId(),
                pa.getProcedureText(),
                pa.getApprovalCode(),
                pa.getRequestedAmount(),
                pa.getApprovedAmount(),
                pa.getStatus().name(),
                pa.getDecidedAt()
        );
    }

    private InsuranceClaimBatchDto toDto(InsuranceClaimBatch b, List<InsuranceClaimLine> lines) {
        String appId = TenantContext.require();
        String payerName = payerRepository.findByAppIdAndId(appId, b.getPayerId()).map(InsurancePayer::getName).orElse("—");
        List<InsuranceClaimLineDto> lineDtos = lines.stream().map(this::toDto).toList();

        return new InsuranceClaimBatchDto(
                b.getId(),
                b.getBatchNumber(),
                b.getPayerId(),
                payerName,
                b.getPeriod(),
                b.getStatus().name(),
                b.getTotalClaimedAmount(),
                b.getTotalApprovedAmount(),
                b.getTotalRejectedAmount(),
                b.getSubmittedAt(),
                b.getSettledAt(),
                b.getNotes(),
                lineDtos
        );
    }

    private InsuranceClaimLineDto toDto(InsuranceClaimLine l) {
        return new InsuranceClaimLineDto(
                l.getId(),
                l.getBatchId(),
                l.getVisitId(),
                l.getPatientId(),
                l.getPatientMrn(),
                l.getPatientName(),
                l.getMemberNumber(),
                l.getProcedureText(),
                l.getTotalFee(),
                l.getInsurerShare(),
                l.getPatientShare(),
                l.getStatus().name(),
                l.getRejectionReason(),
                l.getResubmittedLineId()
        );
    }
}
