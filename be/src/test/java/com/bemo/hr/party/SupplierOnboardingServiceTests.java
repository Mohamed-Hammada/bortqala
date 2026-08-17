package com.bemo.hr.party;

import com.bemo.hr.approval.ApprovalWorkflowService;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierOnboardingServiceTests {
    @Mock
    private BusinessPartyService businessPartyService;
    @Mock
    private BusinessPartyRepository businessPartyRepository;
    @Mock
    private SupplierDocumentRepository supplierDocumentRepository;
    @Mock
    private SupplierBankAccountRepository supplierBankAccountRepository;
    @Mock
    private ApprovalWorkflowService approvalWorkflowService;
    @Mock
    private AuditService auditService;

    private SupplierOnboardingService service;
    private BusinessParty supplier;

    @BeforeEach
    void setUp() {
        service = new SupplierOnboardingService(businessPartyService, businessPartyRepository,
                supplierDocumentRepository, supplierBankAccountRepository, approvalWorkflowService, auditService);
        supplier = new BusinessParty("SUP-1", "Supplier", null, "SUPPLIER", null, null, null, null,
                null, false, "DIRECT", null, null, null, "EGP", "E_INVOICE", "NET_30", "TAX12345", null);
        supplier.beginSupplierRequest();
        org.mockito.Mockito.lenient().when(businessPartyService.requireParty(supplier.getId())).thenReturn(supplier);
    }

    @Test
    void duplicateTaxIdIsReportedBeforeRequestCreation() {
        when(businessPartyRepository.findByTaxIdIgnoreCase("TAX12345")).thenReturn(List.of(supplier));

        SupplierOnboardingApi.DuplicateResponse result = service.duplicates("TAX12345", null, null);

        assertThat(result.duplicateFound()).isTrue();
        assertThat(result.taxIdMatches()).singleElement().extracting(SupplierOnboardingApi.DuplicateMatch::reason)
                .isEqualTo("TAX_ID");
    }

    @Test
    void duplicateBankAccountIsBlockedAcrossSuppliers() {
        String normalized = SupplierBankAccount.normalize("EG12 3456 7890 1234");
        when(supplierBankAccountRepository.existsByNormalizedIbanAndSupplierIdNot(normalized, supplier.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.addBankAccount(supplier.getId(),
                new SupplierOnboardingApi.BankAccountRequest("Supplier", "EG12 3456 7890 1234", "Bank", "EGP", true)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo("SUPPLIER_DUPLICATE_BANK_ACCOUNT");
    }

    @Test
    void expiredMandatoryDocumentBlocksApproval() {
        supplier.submitForReview(null);
        SupplierDocument expired = new SupplierDocument(supplier.getId(), "TAX_CARD", "1", "tax.pdf", "application/pdf", new byte[]{1},
                LocalDate.now().minusYears(2), LocalDate.now().minusDays(1), true);
        when(supplierDocumentRepository.findBySupplierIdOrderByCreatedAtDesc(supplier.getId())).thenReturn(List.of(expired));

        assertThatThrownBy(() -> service.approve(supplier.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo("SUPPLIER_MANDATORY_DOCUMENT_EXPIRED");
    }

    @Test
    void unverifiedMandatoryDocumentBlocksApproval() {
        supplier.submitForReview(null);
        SupplierDocument document = new SupplierDocument(supplier.getId(), "TAX_CARD", "1", "tax.pdf", "application/pdf", new byte[]{1},
                LocalDate.now().minusDays(1), LocalDate.now().plusYears(1), true);
        when(supplierDocumentRepository.findBySupplierIdOrderByCreatedAtDesc(supplier.getId())).thenReturn(List.of(document));

        assertThatThrownBy(() -> service.approve(supplier.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo("SUPPLIER_MANDATORY_DOCUMENT_UNVERIFIED");
    }

    @Test
    void suspendedSupplierCannotBeUsedForProcurement() {
        BusinessParty active = new BusinessParty("SUP-2", "Active supplier", null, "SUPPLIER", null, null,
                null, null, null, true, "DIRECT", null, null, null, "EGP", "E_INVOICE", "CASH",
                "TAX99999", "EG123456789012345678901234");

        active.suspendSupplier();

        assertThat(active.isProcurementAllowed()).isFalse();
        assertThat(active.isPaymentAllowed()).isFalse();
        assertThat(active.getOnboardingStatus()).isEqualTo("SUSPENDED");
    }

    @Test
    void duplicateBankLookupReturnsOwningSupplier() {
        BusinessParty other = new BusinessParty("SUP-2", "Other", null, "SUPPLIER", null, null, null, null,
                null, true, "DIRECT", null, null, null, "EGP", "E_INVOICE", "CASH", "TAX99999", "EG999999999999");
        SupplierBankAccount account = new SupplierBankAccount(other.getId(), "Other", "EG999999999999", "Bank", "EGP", true);
        when(supplierBankAccountRepository.findByNormalizedIban("EG999999999999")).thenReturn(Optional.of(account));
        when(businessPartyRepository.findById(other.getId())).thenReturn(Optional.of(other));

        assertThat(service.duplicates(null, "EG999999999999", supplier.getId()).bankMatches())
                .singleElement().extracting(SupplierOnboardingApi.DuplicateMatch::supplierId).isEqualTo(other.getId());
    }

    @Test
    void storesRealDocumentContentAndMetadata() {
        when(supplierDocumentRepository.save(any(SupplierDocument.class))).thenAnswer(invocation -> {
            SupplierDocument document = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(document, "createdAt", java.time.Instant.now());
            return document;
        });
        var file = new org.springframework.mock.web.MockMultipartFile(
                "file", "tax-card.pdf", "application/pdf", new byte[]{1, 2, 3});

        var result = service.addDocument(supplier.getId(),
                new SupplierOnboardingApi.DocumentRequest("TAX_CARD", "TC-1", LocalDate.now(),
                        LocalDate.now().plusYears(1), true), file);

        assertThat(result.fileName()).isEqualTo("tax-card.pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.fileSize()).isEqualTo(3);
    }
}
