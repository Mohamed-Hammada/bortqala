package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.trade.procurement.api.OcrCaptureApi;
import com.bemo.hr.trade.procurement.domain.OcrCaptureJob;
import com.bemo.hr.trade.procurement.infrastructure.OcrCaptureJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional(readOnly = true)
public class OcrCaptureService {

    private final OcrCaptureJobRepository jobRepository;
    private final InvoiceExtractor extractor;
    private final BusinessPartyRepository partyRepository;

    @Value("${hr.ocr.max-image-bytes:5242880}")
    private long maxImageBytes;

    public OcrCaptureService(OcrCaptureJobRepository jobRepository,
                             InvoiceExtractor extractor,
                             BusinessPartyRepository partyRepository) {
        this.jobRepository = jobRepository;
        this.extractor = extractor;
        this.partyRepository = partyRepository;
    }

    public OcrCaptureApi.OcrProviderStatus providerStatus() {
        return new OcrCaptureApi.OcrProviderStatus(
                extractor.isConfigured() ? "CONFIGURED" : "NONE",
                extractor.providerName());
    }

    @Transactional
    public OcrCaptureApi.OcrJobResponse upload(MultipartFile file, String actor) {
        if (!extractor.isConfigured()) {
            throw new BusinessRuleException(
                    "OCR provider is not configured. Contact your administrator to enable OCR.",
                    "OCR_NOT_CONFIGURED", HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (file.isEmpty()) {
            throw new BusinessRuleException("Uploaded image is empty.", "OCR_IMAGE_INVALID", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > maxImageBytes) {
            throw new BusinessRuleException(
                    "Image exceeds maximum size of " + (maxImageBytes / 1048576) + " MB.",
                    "OCR_IMAGE_INVALID", HttpStatus.BAD_REQUEST);
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/"))) {
            throw new BusinessRuleException("Only image files are accepted.", "OCR_IMAGE_INVALID", HttpStatus.BAD_REQUEST);
        }
        String appId = TenantContext.require();

        OcrCaptureJob job = new OcrCaptureJob(actor, file.getOriginalFilename(), contentType, "pending-storage");
        job.setAppId(appId);
        job.setStatus("PROCESSING");

        try {
            byte[] imageBytes = file.getBytes();
            String extracted = extractor.extract(imageBytes, contentType);
            job.setExtractedPayload(extracted);
            job.setConfidenceSummary(extractor.confidenceSummary());
            job.setStatus("REVIEW");
        } catch (Exception e) {
            log.warn("OCR extraction failed for job {}: {}", job.getId(), e.getMessage());
            job.setStatus("FAILED");
            job.setErrorCode("OCR_PROVIDER_FAILED");
        }
        job = jobRepository.save(job);
        return toResponse(job);
    }

    public OcrCaptureApi.OcrJobResponse getJob(String jobId) {
        OcrCaptureJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("OCR capture job not found.", "OCR_JOB_NOT_FOUND"));
        return toResponse(job);
    }

    public List<OcrCaptureApi.OcrJobResponse> listJobs() {
        String appId = TenantContext.require();
        return jobRepository.findByAppIdOrderByCreatedAtDesc(appId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public Map<String, Object> convertToGrn(String jobId, OcrCaptureApi.ConvertOcrPayload payload) {
        OcrCaptureJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("OCR capture job not found.", "OCR_JOB_NOT_FOUND"));
        if (!"REVIEW".equals(job.getStatus())) {
            throw new BusinessRuleException(
                    "Only jobs in REVIEW status can be converted.", "OCR_INVALID_STATE", HttpStatus.CONFLICT);
        }
        if (!partyRepository.existsById(payload.partyId())) {
            throw new BusinessRuleException("Selected supplier not found.", "OCR_SUPPLIER_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
        job.setStatus("CONVERTED");
        jobRepository.save(job);
        return Map.of("jobId", jobId, "partyId", payload.partyId(), "warehouseId",
                payload.warehouseId() != null ? payload.warehouseId() : "default",
                "message", "OCR capture converted to draft GRN. Use the procurement page to review and confirm.");
    }

    private OcrCaptureApi.OcrJobResponse toResponse(OcrCaptureJob job) {
        return new OcrCaptureApi.OcrJobResponse(
                job.getId(), job.getUploadedBy(), job.getImageOriginalName(),
                job.getImageContentType(), job.getStatus(), job.getExtractedPayload(),
                job.getConfidenceSummary(), job.getErrorCode(), job.getCreatedAt());
    }
}
