package com.bemo.hr.medical.application;

import com.bemo.hr.medical.api.MedicalClinicApi.PrescriptionLineRequest;
import com.bemo.hr.medical.api.MedicalClinicApi.PrescriptionLineResponse;
import com.bemo.hr.medical.domain.ClinicPrescriptionLine;
import com.bemo.hr.medical.infrastructure.ClinicPrescriptionLineRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ClinicPrescriptionService {

    private final ClinicPrescriptionLineRepository prescriptionRepository;

    public ClinicPrescriptionService(ClinicPrescriptionLineRepository prescriptionRepository) {
        this.prescriptionRepository = prescriptionRepository;
    }

    public List<PrescriptionLineResponse> savePrescriptions(String visitId, List<PrescriptionLineRequest> lines) {
        String appId = TenantContext.require();
        prescriptionRepository.deleteAllByAppIdAndVisitId(appId, visitId);

        if (lines == null || lines.isEmpty()) {
            return List.of();
        }

        List<ClinicPrescriptionLine> entities = lines.stream()
                .map(req -> new ClinicPrescriptionLine(
                        visitId,
                        req.drugName().trim(),
                        req.dose().trim(),
                        req.frequency().trim(),
                        req.duration().trim(),
                        req.instructions() != null ? req.instructions().trim() : null
                ))
                .toList();

        List<ClinicPrescriptionLine> saved = prescriptionRepository.saveAll(entities);
        return saved.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PrescriptionLineResponse> getPrescriptions(String visitId) {
        String appId = TenantContext.require();
        return prescriptionRepository.findAllByAppIdAndVisitIdOrderByCreatedAtAsc(appId, visitId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PrescriptionLineResponse toResponse(ClinicPrescriptionLine entity) {
        return new PrescriptionLineResponse(
                entity.getId(),
                entity.getDrugName(),
                entity.getDose(),
                entity.getFrequency(),
                entity.getDuration(),
                entity.getInstructions(),
                entity.getCreatedAt().toEpochMilli()
        );
    }
}
