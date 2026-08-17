package com.bemo.hr.party;

import com.bemo.hr.approval.ApprovalApi;
import com.bemo.hr.approval.ApprovalWorkflowService;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class SupplierOnboardingService {
    static final String DOCUMENT_TYPE = "SUPPLIER_ONBOARDING";

    private final BusinessPartyService businessPartyService;
    private final BusinessPartyRepository businessPartyRepository;
    private final SupplierDocumentRepository supplierDocumentRepository;
    private final SupplierBankAccountRepository supplierBankAccountRepository;
    private final ApprovalWorkflowService approvalWorkflowService;
    private final AuditService auditService;

    @Transactional
    BusinessPartyApi.Response createRequest(SupplierOnboardingApi.SupplierRequest request) {
        return businessPartyService.createSupplierRequest(request);
    }

    SupplierOnboardingApi.DuplicateResponse duplicates(String taxId, String iban, String excludeSupplierId) {
        List<SupplierOnboardingApi.DuplicateMatch> taxMatches = taxId == null || taxId.isBlank() ? List.of()
                : businessPartyRepository.findByTaxIdIgnoreCase(taxId.strip()).stream()
                  .filter(p -> !p.getId().equals(excludeSupplierId))
                  .map(p -> new SupplierOnboardingApi.DuplicateMatch(p.getId(), p.getCode(), p.getName(), "TAX_ID"))
                  .toList();
        List<SupplierOnboardingApi.DuplicateMatch> bankMatches = new ArrayList<>();
        if (iban != null && !iban.isBlank()) {
            supplierBankAccountRepository.findByNormalizedIban(SupplierBankAccount.normalize(iban))
                    .filter(a -> !a.getSupplierId().equals(excludeSupplierId))
                    .flatMap(a -> businessPartyRepository.findById(a.getSupplierId()))
                    .ifPresent(p -> bankMatches.add(new SupplierOnboardingApi.DuplicateMatch(
                            p.getId(), p.getCode(), p.getName(), "BANK_ACCOUNT")));
        }
        return new SupplierOnboardingApi.DuplicateResponse(taxMatches, bankMatches);
    }

    SupplierOnboardingApi.Supplier360 get360(String supplierId) {
        BusinessParty supplier = requireSupplier(supplierId);
        List<SupplierDocument> documents = supplierDocumentRepository.findBySupplierIdOrderByCreatedAtDesc(supplierId);
        List<SupplierBankAccount> accounts = supplierBankAccountRepository.findBySupplierIdOrderByPrimaryDescCreatedAtAsc(supplierId);
        List<SupplierOnboardingApi.ComplianceItem> compliance = compliance(supplier, documents, accounts);
        return new SupplierOnboardingApi.Supplier360(
                businessPartyService.toResponse(supplier),
                documents.stream().map(this::documentResponse).toList(),
                accounts.stream().map(this::bankResponse).toList(),
                compliance,
                documents.size(), documents.stream().filter(d -> d.isExpired(LocalDate.now())).count(),
                accounts.stream().filter(SupplierBankAccount::isVerified).count(),
                supplier.isProcurementAllowed(), supplier.isPaymentAllowed());
    }

    @Transactional
    SupplierOnboardingApi.DocumentResponse addDocument(String supplierId, SupplierOnboardingApi.DocumentRequest request,
                                                       MultipartFile file) {
        requireSupplier(supplierId);
        if (request.expiryDate() != null && request.issueDate() != null && request.expiryDate().isBefore(request.issueDate())) {
            throw conflict("Document expiry date cannot precede its issue date.", "SUPPLIER_DOCUMENT_DATE_INVALID");
        }
        if (file == null || file.isEmpty())
            throw conflict("Supplier document file is required.", "SUPPLIER_DOCUMENT_FILE_REQUIRED");
        if (file.getSize() > 5L * 1024 * 1024)
            throw conflict("Supplier document cannot exceed 5 MB.", "SUPPLIER_DOCUMENT_FILE_TOO_LARGE");
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        if (!java.util.Set.of("application/pdf", "image/png", "image/jpeg",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").contains(contentType)) {
            throw conflict("Unsupported supplier document type.", "SUPPLIER_DOCUMENT_FILE_TYPE_INVALID");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (java.io.IOException ex) {
            throw conflict("Supplier document could not be read.", "SUPPLIER_DOCUMENT_FILE_READ_FAILED");
        }
        String fileName = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                ? "supplier-document" : file.getOriginalFilename();
        SupplierDocument document = supplierDocumentRepository.save(new SupplierDocument(supplierId,
                request.documentType(), request.documentNumber(), fileName, contentType, content,
                request.issueDate(), request.expiryDate(), request.mandatory()));
        audit("ADD_DOCUMENT", "SUPPLIER_DOCUMENT", document.getId(), supplierId);
        return documentResponse(document);
    }

    SupplierDocument downloadDocument(String supplierId, String documentId) {
        requireSupplier(supplierId);
        return supplierDocumentRepository.findById(documentId)
                .filter(d -> d.getSupplierId().equals(supplierId))
                .orElseThrow(() -> new NotFoundException("Supplier document not found.", "SUPPLIER_DOCUMENT_NOT_FOUND"));
    }

    @Transactional
    SupplierOnboardingApi.DocumentResponse verifyDocument(String supplierId, String documentId) {
        requireSupplier(supplierId);
        SupplierDocument document = supplierDocumentRepository.findById(documentId)
                .filter(d -> d.getSupplierId().equals(supplierId))
                .orElseThrow(() -> new NotFoundException("Supplier document not found.", "SUPPLIER_DOCUMENT_NOT_FOUND"));
        if (document.isExpired(LocalDate.now()))
            throw conflict("Expired supplier documents cannot be verified.", "SUPPLIER_DOCUMENT_EXPIRED");
        document.verify(businessPartyService.currentUser());
        audit("VERIFY", "SUPPLIER_DOCUMENT", document.getId(), supplierId);
        return documentResponse(document);
    }

    @Transactional
    SupplierOnboardingApi.BankAccountResponse addBankAccount(String supplierId, SupplierOnboardingApi.BankAccountRequest request) {
        requireSupplier(supplierId);
        String normalized = SupplierBankAccount.normalize(request.iban());
        if (normalized.length() < 12) throw conflict("Bank account or IBAN is invalid.", "SUPPLIER_BANK_INVALID");
        if (supplierBankAccountRepository.existsByNormalizedIbanAndSupplierIdNot(normalized, supplierId)
                || supplierBankAccountRepository.findByNormalizedIban(normalized).isPresent()) {
            throw conflict("This bank account is already registered.", "SUPPLIER_DUPLICATE_BANK_ACCOUNT");
        }
        SupplierBankAccount account = supplierBankAccountRepository.save(new SupplierBankAccount(supplierId,
                request.accountName(), request.iban(), request.bankName(), request.currencyCode(), request.primary()));
        audit("ADD_BANK", "SUPPLIER_BANK_ACCOUNT", account.getId(), supplierId);
        return bankResponse(account);
    }

    @Transactional
    SupplierOnboardingApi.BankAccountResponse verifyBankAccount(String supplierId, String accountId) {
        BusinessParty supplier = requireSupplier(supplierId);
        SupplierBankAccount account = supplierBankAccountRepository.findById(accountId)
                .filter(a -> a.getSupplierId().equals(supplierId))
                .orElseThrow(() -> new NotFoundException("Supplier bank account not found.", "SUPPLIER_BANK_NOT_FOUND"));
        account.verify(businessPartyService.currentUser());
        if (account.isPrimary())
            supplier.verifyBank(account.getIban(), account.getVerifiedBy(), account.getVerifiedAt());
        audit("VERIFY", "SUPPLIER_BANK_ACCOUNT", account.getId(), supplierId);
        return bankResponse(account);
    }

    @Transactional
    BusinessPartyApi.Response submit(String supplierId) {
        BusinessParty supplier = requireSupplier(supplierId);
        String resolvedInstanceId = null;
        if (approvalWorkflowService.hasActiveWorkflow(DOCUMENT_TYPE)) {
            resolvedInstanceId = approvalWorkflowService.submit(new ApprovalApi.SubmitDocumentRequest(
                    DOCUMENT_TYPE, supplierId, null)).instanceId();
        }
        String instanceId = resolvedInstanceId;
        transition(() -> supplier.submitForReview(instanceId));
        audit("SUBMIT", "SUPPLIER_ONBOARDING", supplierId, instanceId);
        return businessPartyService.toResponse(supplier);
    }

    @Transactional
    BusinessPartyApi.Response approve(String supplierId) {
        BusinessParty supplier = requireSupplier(supplierId);
        validateCompliance(supplierId);
        if (supplier.getApprovalInstanceId() != null) {
            var approval = approvalWorkflowService.getHistory(DOCUMENT_TYPE, supplierId);
            if (!"APPROVED".equals(approval.status())) {
                throw conflict("The configured approval workflow is not complete.", "SUPPLIER_APPROVAL_PENDING");
            }
        }
        transition(supplier::approveOnboarding);
        audit("APPROVE", "SUPPLIER_ONBOARDING", supplierId, null);
        return businessPartyService.toResponse(supplier);
    }

    @Transactional
    BusinessPartyApi.Response activate(String supplierId) {
        BusinessParty supplier = requireSupplier(supplierId);
        validateCompliance(supplierId);
        if (!supplier.isBankVerified())
            throw conflict("A verified primary bank account is required.", "SUPPLIER_BANK_VERIFICATION_REQUIRED");
        transition(supplier::activateSupplier);
        audit("ACTIVATE", "SUPPLIER_ONBOARDING", supplierId, null);
        return businessPartyService.toResponse(supplier);
    }

    @Transactional
    BusinessPartyApi.Response suspend(String supplierId, String reason) {
        BusinessParty supplier = requireSupplier(supplierId);
        transition(supplier::suspendSupplier);
        audit("SUSPEND", "SUPPLIER_ONBOARDING", supplierId, reason);
        return businessPartyService.toResponse(supplier);
    }

    @Transactional
    BusinessPartyApi.Response blacklist(String supplierId, String reason) {
        if (reason == null || reason.isBlank())
            throw conflict("Blacklist reason is required.", "SUPPLIER_BLACKLIST_REASON_REQUIRED");
        BusinessParty supplier = requireSupplier(supplierId);
        transition(supplier::blacklistSupplier);
        audit("BLACKLIST", "SUPPLIER_ONBOARDING", supplierId, reason);
        return businessPartyService.toResponse(supplier);
    }

    private void validateCompliance(String supplierId) {
        List<SupplierDocument> documents = supplierDocumentRepository.findBySupplierIdOrderByCreatedAtDesc(supplierId);
        if (documents.stream().noneMatch(SupplierDocument::isMandatory)) {
            throw conflict("At least one mandatory compliance document is required.", "SUPPLIER_MANDATORY_DOCUMENT_REQUIRED");
        }
        if (documents.stream().anyMatch(d -> d.isMandatory() && d.isExpired(LocalDate.now()))) {
            throw conflict("A mandatory supplier document is expired.", "SUPPLIER_MANDATORY_DOCUMENT_EXPIRED");
        }
        if (documents.stream().anyMatch(d -> d.isMandatory() && !d.isVerified())) {
            throw conflict("All mandatory supplier documents must be verified.", "SUPPLIER_MANDATORY_DOCUMENT_UNVERIFIED");
        }
    }

    private List<SupplierOnboardingApi.ComplianceItem> compliance(BusinessParty supplier,
                                                                  List<SupplierDocument> documents, List<SupplierBankAccount> accounts) {
        LocalDate today = LocalDate.now();
        boolean mandatoryPresent = documents.stream().anyMatch(SupplierDocument::isMandatory);
        boolean documentsValid = mandatoryPresent && documents.stream()
                .filter(SupplierDocument::isMandatory).allMatch(d -> d.isVerified() && !d.isExpired(today));
        boolean bankVerified = accounts.stream().anyMatch(SupplierBankAccount::isVerified) || supplier.isBankVerified();
        return List.of(
                new SupplierOnboardingApi.ComplianceItem("TAX_ID", supplier.getTaxId() != null, "Tax identifier recorded"),
                new SupplierOnboardingApi.ComplianceItem("MANDATORY_DOCUMENTS", documentsValid, "Mandatory documents verified and current"),
                new SupplierOnboardingApi.ComplianceItem("BANK_VERIFICATION", bankVerified, "Primary payment account verified"));
    }

    private BusinessParty requireSupplier(String id) {
        BusinessParty party = businessPartyService.requireParty(id);
        if (!"SUPPLIER".equals(party.getPartyType()))
            throw conflict("The selected party is not a supplier.", "SUPPLIER_REQUIRED");
        return party;
    }

    private SupplierOnboardingApi.DocumentResponse documentResponse(SupplierDocument d) {
        return new SupplierOnboardingApi.DocumentResponse(d.getId(), d.getDocumentType(), d.getDocumentNumber(), d.getFileName(),
                d.getContentType(), d.getFileSize(),
                d.getIssueDate(), d.getExpiryDate(), d.isMandatory(), d.isVerified(), d.getVerifiedBy(),
                d.getVerifiedAt() == null ? null : d.getVerifiedAt().toEpochMilli(), d.getCreatedAt().toEpochMilli(),
                d.isExpired(LocalDate.now()));
    }

    private SupplierOnboardingApi.BankAccountResponse bankResponse(SupplierBankAccount a) {
        return new SupplierOnboardingApi.BankAccountResponse(a.getId(), a.getAccountName(), a.getIban(), a.getBankName(),
                a.getCurrencyCode(), a.isPrimary(), a.getVerificationStatus(), a.getVerifiedBy(),
                a.getVerifiedAt() == null ? null : a.getVerifiedAt().toEpochMilli(), a.getCreatedAt().toEpochMilli());
    }

    private void transition(Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException ex) {
            throw conflict(ex.getMessage(), "SUPPLIER_STATUS_TRANSITION_INVALID");
        }
    }

    private void audit(String action, String type, String id, String detail) {
        auditService.record(action, type, id, businessPartyService.currentUser(),
                "{\"detail\":\"" + safe(detail) + "\"}", null);
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private BusinessRuleException conflict(String message, String code) {
        return new BusinessRuleException(message, code, HttpStatus.CONFLICT);
    }
}
