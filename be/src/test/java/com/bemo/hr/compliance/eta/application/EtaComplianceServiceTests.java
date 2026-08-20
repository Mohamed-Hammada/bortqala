package com.bemo.hr.compliance.eta.application;

import com.bemo.hr.compliance.eta.api.EtaComplianceApi;
import com.bemo.hr.compliance.eta.domain.*;
import com.bemo.hr.compliance.eta.infrastructure.EtaConfigRepository;
import com.bemo.hr.compliance.eta.infrastructure.EtaInvoiceSubmissionRepository;
import com.bemo.hr.compliance.eta.infrastructure.EtaItemCodeMappingRepository;
import com.bemo.hr.trade.sales.domain.CustomerInvoice;
import com.bemo.hr.trade.sales.infrastructure.CustomerInvoiceRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EtaComplianceServiceTests {

    @Mock
    private EtaConfigRepository configRepository;

    @Mock
    private EtaInvoiceSubmissionRepository submissionRepository;

    @Mock
    private EtaItemCodeMappingRepository mappingRepository;

    @Mock
    private CustomerInvoiceRepository customerInvoiceRepository;

    private EtaComplianceService service;

    @BeforeEach
    void setUp() {
        service = new EtaComplianceService(configRepository, submissionRepository, mappingRepository, customerInvoiceRepository);
    }

    @Test
    void savesAndRetrievesConfigurationWithMaskedSecret() {
        when(configRepository.findFirstByActiveTrue()).thenReturn(Optional.empty());
        when(configRepository.save(any(EtaConfig.class))).thenAnswer(i -> i.getArgument(0));

        EtaComplianceApi.SaveConfigRequest request = new EtaComplianceApi.SaveConfigRequest(
                "client-123",
                "secret-super-confidential",
                "123456789",
                "Bemo Egypt S.A.E.",
                EtaEnvironment.PRE_PRODUCTION,
                null,
                null,
                true
        );

        EtaComplianceApi.ConfigResponse response = service.saveConfig(request);

        assertThat(response.clientId()).isEqualTo("client-123");
        assertThat(response.issuerTaxId()).isEqualTo("123456789");
        assertThat(response.maskedSecret()).contains("****");
        assertThat(response.tokenUrl()).contains("id.preprod.eta.gov.eg");
    }

    @Test
    void queuesInvoiceForEtaSubmissionWithVatBreakdown() {
        CustomerInvoice invoice = new CustomerInvoice("INV-2026-001", "CUST-01", null, LocalDate.of(2026, 8, 19), LocalDate.of(2026, 9, 19), "EGP", new BigDecimal("1140.00"));
        when(customerInvoiceRepository.findById("inv-1")).thenReturn(Optional.of(invoice));
        when(submissionRepository.findByInvoiceId("inv-1")).thenReturn(Optional.empty());
        when(submissionRepository.save(any(EtaInvoiceSubmission.class))).thenAnswer(i -> i.getArgument(0));

        EtaComplianceApi.QueueInvoiceRequest request = new EtaComplianceApi.QueueInvoiceRequest("inv-1", EtaDocumentType.INVOICE);
        EtaComplianceApi.SubmissionResponse response = service.queueInvoice(request);

        assertThat(response.internalId()).isEqualTo("INV-2026-001");
        assertThat(response.documentType()).isEqualTo(EtaDocumentType.INVOICE);
        assertThat(response.status()).isEqualTo(EtaSubmissionStatus.VALIDATED);
        assertThat(response.totalAmount()).isEqualByComparingTo("1140.00");
        assertThat(response.netAmount()).isEqualByComparingTo("1000.00");
        assertThat(response.taxAmount()).isEqualByComparingTo("140.00");
        assertThat(response.canonicalJsonHash()).isNotBlank();
    }

    @Test
    void submitsDocumentToEtaAndGeneratesOfficialEtaUuid() {
        EtaInvoiceSubmission submission = new EtaInvoiceSubmission(
                "inv-1", "INV-2026-001", EtaDocumentType.INVOICE, 1700000000000L,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"),
                new BigDecimal("140.00"), new BigDecimal("1140.00"), "hash123"
        );

        EtaConfig config = new EtaConfig("cid", "sec", "123456789", "Bemo", EtaEnvironment.PRE_PRODUCTION, null, null);

        when(submissionRepository.findById("sub-1")).thenReturn(Optional.of(submission));
        when(configRepository.findFirstByActiveTrue()).thenReturn(Optional.of(config));
        when(submissionRepository.save(any(EtaInvoiceSubmission.class))).thenAnswer(i -> i.getArgument(0));

        EtaComplianceApi.SubmissionResponse response = service.submitToEta("sub-1");

        assertThat(response.status()).isEqualTo(EtaSubmissionStatus.VALID);
        assertThat(response.etaUuid()).startsWith("ETA-");
        assertThat(response.submissionAttempts()).isEqualTo(1);
        assertThat(response.rawResponseJson()).contains("Valid");
    }

    @Test
    void cancelsDocumentWithReason() {
        EtaInvoiceSubmission submission = new EtaInvoiceSubmission(
                "inv-1", "INV-2026-001", EtaDocumentType.INVOICE, 1700000000000L,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"),
                new BigDecimal("140.00"), new BigDecimal("1140.00"), "hash123"
        );

        when(submissionRepository.findById("sub-1")).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any(EtaInvoiceSubmission.class))).thenAnswer(i -> i.getArgument(0));

        EtaComplianceApi.SubmissionResponse response = service.cancelDocument("sub-1", "Wrong customer tax registration ID");

        assertThat(response.status()).isEqualTo(EtaSubmissionStatus.CANCELLED);
        assertThat(response.cancellationReason()).isEqualTo("Wrong customer tax registration ID");
    }

    @Test
    void managesItemCodeMappingsForEgsAndGs1() {
        when(mappingRepository.findByItemId("item-1")).thenReturn(Optional.empty());
        when(mappingRepository.save(any(EtaItemCodeMapping.class))).thenAnswer(i -> i.getArgument(0));
        when(mappingRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(
                new EtaItemCodeMapping("item-1", "ITM-01", "EGS", "EG-113322445-001", "خدمات استشارية", "Consulting Services")
        ));

        EtaComplianceApi.SaveItemMappingRequest request = new EtaComplianceApi.SaveItemMappingRequest(
                "item-1", "ITM-01", "EGS", "EG-113322445-001", "خدمات استشارية", "Consulting Services", true
        );

        EtaComplianceApi.ItemMappingResponse saved = service.saveItemMapping(request);
        assertThat(saved.codeType()).isEqualTo("EGS");
        assertThat(saved.itemCodeValue()).isEqualTo("EG-113322445-001");

        List<EtaComplianceApi.ItemMappingResponse> list = service.listItemMappings();
        assertThat(list).hasSize(1);
    }
}
