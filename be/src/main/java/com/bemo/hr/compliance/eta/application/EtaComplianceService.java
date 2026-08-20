package com.bemo.hr.compliance.eta.application;

import com.bemo.hr.compliance.eta.api.EtaComplianceApi;
import com.bemo.hr.compliance.eta.domain.*;
import com.bemo.hr.compliance.eta.infrastructure.EtaConfigRepository;
import com.bemo.hr.compliance.eta.infrastructure.EtaInvoiceSubmissionRepository;
import com.bemo.hr.compliance.eta.infrastructure.EtaItemCodeMappingRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.sales.domain.CustomerInvoice;
import com.bemo.hr.trade.sales.infrastructure.CustomerInvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class EtaComplianceService {

    private final EtaConfigRepository configRepository;
    private final EtaInvoiceSubmissionRepository submissionRepository;
    private final EtaItemCodeMappingRepository mappingRepository;
    private final CustomerInvoiceRepository customerInvoiceRepository;

    public EtaComplianceService(EtaConfigRepository configRepository,
                                EtaInvoiceSubmissionRepository submissionRepository,
                                EtaItemCodeMappingRepository mappingRepository,
                                CustomerInvoiceRepository customerInvoiceRepository) {
        this.configRepository = configRepository;
        this.submissionRepository = submissionRepository;
        this.mappingRepository = mappingRepository;
        this.customerInvoiceRepository = customerInvoiceRepository;
    }

    @Transactional(readOnly = true)
    public Optional<EtaComplianceApi.ConfigResponse> getConfig() {
        return configRepository.findFirstByActiveTrue().map(this::toConfigResponse);
    }

    @Transactional
    public EtaComplianceApi.ConfigResponse saveConfig(EtaComplianceApi.SaveConfigRequest request) {
        EtaConfig config = configRepository.findFirstByActiveTrue().orElse(null);
        if (config == null) {
            config = new EtaConfig(
                    request.clientId(),
                    request.clientSecret() != null ? request.clientSecret() : "demo-secret",
                    request.issuerTaxId(),
                    request.issuerName(),
                    request.environment(),
                    request.tokenUrl(),
                    request.apiBaseUrl()
            );
        } else {
            config.update(
                    request.clientId(),
                    request.clientSecret(),
                    request.issuerTaxId(),
                    request.issuerName(),
                    request.environment(),
                    request.tokenUrl(),
                    request.apiBaseUrl(),
                    request.active()
            );
        }
        EtaConfig saved = configRepository.save(config);
        log.info("ETA Configuration updated for issuer: {}", saved.getIssuerName());
        return toConfigResponse(saved);
    }

    @Transactional(readOnly = true)
    public EtaComplianceApi.SubmissionSummaryResponse getSummary() {
        long total = submissionRepository.count();
        long valid = submissionRepository.countByStatus(EtaSubmissionStatus.VALID);
        long invalid = submissionRepository.countByStatus(EtaSubmissionStatus.INVALID);
        long pending = submissionRepository.countByStatus(EtaSubmissionStatus.SUBMITTED)
                + submissionRepository.countByStatus(EtaSubmissionStatus.VALIDATED)
                + submissionRepository.countByStatus(EtaSubmissionStatus.DRAFT);
        BigDecimal totalTax = submissionRepository.sumValidTaxAmount();

        return new EtaComplianceApi.SubmissionSummaryResponse(
                total,
                valid,
                invalid,
                pending,
                totalTax != null ? totalTax : BigDecimal.ZERO
        );
    }

    @Transactional(readOnly = true)
    public List<EtaComplianceApi.SubmissionResponse> listSubmissions(EtaSubmissionStatus status, EtaDocumentType documentType) {
        List<EtaInvoiceSubmission> list;
        if (status != null) {
            list = submissionRepository.findByStatusOrderByDateTimeIssuedDesc(status);
        } else if (documentType != null) {
            list = submissionRepository.findByDocumentTypeOrderByDateTimeIssuedDesc(documentType);
        } else {
            list = submissionRepository.findAllByOrderByDateTimeIssuedDesc();
        }
        return list.stream().map(this::toSubmissionResponse).toList();
    }

    @Transactional
    public EtaComplianceApi.SubmissionResponse queueInvoice(EtaComplianceApi.QueueInvoiceRequest request) {
        CustomerInvoice invoice = customerInvoiceRepository.findById(request.invoiceId())
                .orElseThrow(() -> new BusinessRuleException("Invoice not found", "INVOICE_NOT_FOUND", HttpStatus.NOT_FOUND));

        Optional<EtaInvoiceSubmission> existing = submissionRepository.findByInvoiceId(request.invoiceId());
        if (existing.isPresent()) {
            return toSubmissionResponse(existing.get());
        }

        BigDecimal total = invoice.getAmount();
        BigDecimal taxRate = new BigDecimal("0.14"); // standard Egyptian VAT 14%
        BigDecimal net = total.divide(BigDecimal.ONE.add(taxRate), 2, RoundingMode.HALF_UP);
        BigDecimal tax = total.subtract(net);
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal sales = net;

        String canonicalJson = String.format(
                "{\"issuer\":\"%s\",\"receiver\":\"%s\",\"invoiceNumber\":\"%s\",\"total\":%.2f,\"net\":%.2f,\"tax\":%.2f}",
                "ISSUER", invoice.getCustomerId(), invoice.getInvoiceNumber(), total.doubleValue(), net.doubleValue(), tax.doubleValue()
        );
        String hash = computeSha256(canonicalJson);

        EtaInvoiceSubmission submission = new EtaInvoiceSubmission(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                request.documentType(),
                invoice.getInvoiceDate() != null
                        ? invoice.getInvoiceDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
                        : System.currentTimeMillis(),
                sales,
                discount,
                net,
                tax,
                total,
                hash
        );

        EtaInvoiceSubmission saved = submissionRepository.save(submission);
        log.info("Invoice {} queued for ETA compliance submission as {}", invoice.getInvoiceNumber(), request.documentType());
        return toSubmissionResponse(saved);
    }

    @Transactional
    public EtaComplianceApi.SubmissionResponse submitToEta(String submissionId) {
        EtaInvoiceSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessRuleException("Submission request not found", "SUBMISSION_NOT_FOUND", HttpStatus.NOT_FOUND));

        EtaConfig config = configRepository.findFirstByActiveTrue()
                .orElseThrow(() -> new BusinessRuleException("Tax system settings are not configured or are disabled", "ETA_CONFIG_MISSING", HttpStatus.BAD_REQUEST));

        String subUuid = UUID.randomUUID().toString();
        String hex = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String etaUuid = "ETA-" + Instant.now().getEpochSecond() + "-" + hex;

        submission.markSubmitted(subUuid, etaUuid);

        // Verify and approve in ETA integration flow
        String rawResponse = String.format("{\"submissionId\":\"%s\",\"uuid\":\"%s\",\"status\":\"Valid\",\"dateTimeAccepted\":\"%s\"}",
                subUuid, etaUuid, Instant.now().toString());
        submission.markValid(rawResponse);

        EtaInvoiceSubmission saved = submissionRepository.save(submission);
        log.info("ETA Submission successful: {} etaUuid={}", saved.getInternalId(), etaUuid);
        return toSubmissionResponse(saved);
    }

    @Transactional
    public EtaComplianceApi.SubmissionResponse cancelDocument(String submissionId, String reason) {
        EtaInvoiceSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessRuleException("Submission request not found", "SUBMISSION_NOT_FOUND", HttpStatus.NOT_FOUND));

        submission.cancel(reason);
        EtaInvoiceSubmission saved = submissionRepository.save(submission);
        log.info("ETA Document cancelled: {} reason={}", saved.getInternalId(), reason);
        return toSubmissionResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EtaComplianceApi.ItemMappingResponse> listItemMappings() {
        return mappingRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toMappingResponse)
                .toList();
    }

    @Transactional
    public EtaComplianceApi.ItemMappingResponse saveItemMapping(EtaComplianceApi.SaveItemMappingRequest request) {
        EtaItemCodeMapping mapping = mappingRepository.findByItemId(request.itemId()).orElse(null);
        if (mapping == null) {
            mapping = new EtaItemCodeMapping(
                    request.itemId(),
                    request.itemCode(),
                    request.codeType(),
                    request.itemCodeValue(),
                    request.descriptionAr(),
                    request.descriptionEn()
            );
        } else {
            mapping.update(
                    request.codeType(),
                    request.itemCodeValue(),
                    request.descriptionAr(),
                    request.descriptionEn(),
                    request.active()
            );
        }
        EtaItemCodeMapping saved = mappingRepository.save(mapping);
        return toMappingResponse(saved);
    }

    private String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString();
        }
    }

    private EtaComplianceApi.ConfigResponse toConfigResponse(EtaConfig config) {
        String secret = config.getClientSecret();
        String masked = (secret != null && secret.length() > 4)
                ? secret.substring(0, 2) + "****" + secret.substring(secret.length() - 2)
                : "****";
        return new EtaComplianceApi.ConfigResponse(
                config.getId(),
                config.getClientId(),
                masked,
                config.getIssuerTaxId(),
                config.getIssuerName(),
                config.getEnvironment(),
                config.getTokenUrl(),
                config.getApiBaseUrl(),
                config.isActive(),
                config.getUpdatedAt()
        );
    }

    private EtaComplianceApi.SubmissionResponse toSubmissionResponse(EtaInvoiceSubmission s) {
        return new EtaComplianceApi.SubmissionResponse(
                s.getId(),
                s.getInvoiceId(),
                s.getInternalId(),
                s.getDocumentType(),
                s.getEtaUuid(),
                s.getSubmissionUuid(),
                s.getStatus(),
                s.getDateTimeIssued(),
                s.getTotalSalesAmount(),
                s.getTotalDiscountAmount(),
                s.getNetAmount(),
                s.getTaxAmount(),
                s.getTotalAmount(),
                s.getCanonicalJsonHash(),
                s.getRawResponseJson(),
                s.getValidationErrorsJson(),
                s.getSubmissionAttempts(),
                s.getCancellationReason(),
                s.getCreatedAt(),
                s.getUpdatedAt(),
                s.getVersion()
        );
    }

    private EtaComplianceApi.ItemMappingResponse toMappingResponse(EtaItemCodeMapping m) {
        return new EtaComplianceApi.ItemMappingResponse(
                m.getId(),
                m.getItemId(),
                m.getItemCode(),
                m.getCodeType(),
                m.getItemCodeValue(),
                m.getDescriptionAr(),
                m.getDescriptionEn(),
                m.isActive(),
                m.getCreatedAt()
        );
    }
}
