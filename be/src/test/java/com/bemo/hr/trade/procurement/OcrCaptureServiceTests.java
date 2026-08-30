package com.bemo.hr.trade.procurement;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.trade.procurement.api.OcrCaptureApi;
import com.bemo.hr.trade.procurement.application.InvoiceExtractor;
import com.bemo.hr.trade.procurement.application.NoneExtractor;
import com.bemo.hr.trade.procurement.application.OcrCaptureService;
import com.bemo.hr.trade.procurement.domain.GoodsReceipt;
import com.bemo.hr.trade.procurement.domain.OcrCaptureJob;
import com.bemo.hr.trade.procurement.infrastructure.GoodsReceiptRepository;
import com.bemo.hr.trade.procurement.infrastructure.OcrCaptureJobRepository;
import com.bemo.hr.party.BusinessPartyRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcrCaptureServiceTests {

    @Mock OcrCaptureJobRepository jobRepository;
    @Mock BusinessPartyRepository partyRepository;
    @Mock GoodsReceiptRepository goodsReceiptRepository;

    private OcrCaptureService serviceWithNone;

    @BeforeEach
    void setUp() {
        TenantContext.set("test-app");
        mkService();
    }

    private void mkService() {
        serviceWithNone = new OcrCaptureService(jobRepository, new NoneExtractor(), partyRepository,
                goodsReceiptRepository, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void providerStatusReturnsNoneWhenNotConfigured() {
        var status = serviceWithNone.providerStatus();
        assertThat(status.configured()).isEqualTo("NONE");
        assertThat(status.providerName()).isEqualTo("NONE");
    }

    @Test
    void providerStatusReturnsConfiguredWhenReady() {
        InvoiceExtractor mockExtractor = mock(InvoiceExtractor.class, withSettings().withoutAnnotations());
        lenient().when(mockExtractor.isConfigured()).thenReturn(true);
        lenient().when(mockExtractor.providerName()).thenReturn("MOCK");
        var svc = new OcrCaptureService(jobRepository, mockExtractor, partyRepository,
                goodsReceiptRepository, new ObjectMapper());
        var status = svc.providerStatus();
        assertThat(status.configured()).isEqualTo("CONFIGURED");
        assertThat(status.providerName()).isEqualTo("MOCK");
    }

    @Test
    void uploadWithNoneExtractorThrowsNotConfigured() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "invoice.jpg", "image/jpeg", new byte[]{1, 2, 3});
        assertThatThrownBy(() -> serviceWithNone.upload(file, "test-user"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode()).isEqualTo("OCR_NOT_CONFIGURED"));
    }

    @Test
    void uploadEmptyFileThrowsInvalid() {
        InvoiceExtractor mockExtractor = mock(InvoiceExtractor.class, withSettings().withoutAnnotations());
        lenient().when(mockExtractor.isConfigured()).thenReturn(true);
        var svc = new OcrCaptureService(jobRepository, mockExtractor, partyRepository,
                goodsReceiptRepository, new ObjectMapper());
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);
        assertThatThrownBy(() -> svc.upload(file, "test-user"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode()).isEqualTo("OCR_IMAGE_INVALID"));
    }

    @Test
    void uploadNonImageFileThrowsInvalid() {
        InvoiceExtractor mockExtractor = mock(InvoiceExtractor.class, withSettings().withoutAnnotations());
        lenient().when(mockExtractor.isConfigured()).thenReturn(true);
        var svc = new OcrCaptureService(jobRepository, mockExtractor, partyRepository,
                goodsReceiptRepository, new ObjectMapper());
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});
        assertThatThrownBy(() -> svc.upload(file, "test-user"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode()).isEqualTo("OCR_IMAGE_INVALID"));
    }

    @Test
    void getJobNotFoundThrows404() {
        when(jobRepository.findById("missing")).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> serviceWithNone.getJob("missing"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void convertToGrn_createsDraftWithExtractedLines_ledgerUntouched() {
        OcrCaptureJob job = new OcrCaptureJob("ocr-user", "inv.jpg", "image/jpeg", "path");
        job.setStatus("REVIEW");
        job.setExtractedPayload(
                "{\"supplierName\":\"Acme\",\"invoiceNo\":\"INV-1\",\"lines\":["
                        + "{\"name\":\"Bolt M8\",\"qty\":\"50\",\"unitPrice\":\"2.50\"},"
                        + "{\"name\":\"Nut M8\",\"qty\":\"30\",\"unitPrice\":\"0.75\"}]}");
        when(jobRepository.findById("job-1")).thenReturn(java.util.Optional.of(job));
        when(partyRepository.existsById("party-9")).thenReturn(true);
        when(goodsReceiptRepository.existsByGrnNumberIgnoreCase(any())).thenReturn(false);
        when(goodsReceiptRepository.save(any(GoodsReceipt.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jobRepository.save(any(OcrCaptureJob.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = serviceWithNone.convertToGrn("job-1", new OcrCaptureApi.ConvertOcrPayload("party-9", "wh-1"));

        ArgumentCaptor<GoodsReceipt> grnCaptor = ArgumentCaptor.forClass(GoodsReceipt.class);
        verify(goodsReceiptRepository).save(grnCaptor.capture());
        GoodsReceipt saved = grnCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo("DRAFT");
        assertThat(saved.getSupplierId()).isEqualTo("party-9");
        assertThat(saved.getWarehouseId()).isEqualTo("wh-1");
        assertThat(saved.getGrnNumber()).startsWith("OCR-");
        assertThat(saved.getLines()).hasSize(2);
        assertThat(saved.getLines().get(0).getItemName()).isEqualTo("Bolt M8");
        assertThat(saved.getLines().get(0).getQuantity()).isEqualByComparingTo("50");
        assertThat(saved.getLines().get(0).getUnitPrice()).isEqualByComparingTo("2.50");
        assertThat(result).containsEntry("grnNumber", saved.getGrnNumber());
        assertThat(result).containsEntry("lineCount", 2);
        verify(jobRepository).save(job);
        assertThat(job.getStatus()).isEqualTo("CONVERTED");
        assertThat(job.getDraftGrnId()).isEqualTo(saved.getId());
        verify(goodsReceiptRepository, never()).save(argThat(grn -> !"DRAFT".equals(grn.getStatus())));
    }

    @Test
    void convertToGrn_withoutExtractedPayload_createsEmptyDraft() {
        OcrCaptureJob job = new OcrCaptureJob("ocr-user", "inv.jpg", "image/jpeg", "path");
        job.setStatus("REVIEW");
        when(jobRepository.findById("job-1")).thenReturn(java.util.Optional.of(job));
        when(partyRepository.existsById("party-9")).thenReturn(true);
        when(goodsReceiptRepository.existsByGrnNumberIgnoreCase(any())).thenReturn(false);
        when(goodsReceiptRepository.save(any(GoodsReceipt.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jobRepository.save(any(OcrCaptureJob.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = serviceWithNone.convertToGrn("job-1", new OcrCaptureApi.ConvertOcrPayload("party-9", null));

        ArgumentCaptor<GoodsReceipt> grnCaptor = ArgumentCaptor.forClass(GoodsReceipt.class);
        verify(goodsReceiptRepository).save(grnCaptor.capture());
        assertThat(grnCaptor.getValue().getLines()).isEmpty();
        assertThat(result).containsEntry("lineCount", 0);
    }

    @Test
    void convertToGrn_nonReviewStatus_rejected() {
        OcrCaptureJob job = new OcrCaptureJob("ocr-user", "inv.jpg", "image/jpeg", "path");
        job.setStatus("PROCESSING");
        when(jobRepository.findById("job-1")).thenReturn(java.util.Optional.of(job));

        assertThatThrownBy(() -> serviceWithNone.convertToGrn("job-1",
                new OcrCaptureApi.ConvertOcrPayload("party-9", null)))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode()).isEqualTo("OCR_INVALID_STATE"));
    }

    @Test
    void convertToGrn_unknownSupplier_rejected() {
        OcrCaptureJob job = new OcrCaptureJob("ocr-user", "inv.jpg", "image/jpeg", "path");
        job.setStatus("REVIEW");
        when(jobRepository.findById("job-1")).thenReturn(java.util.Optional.of(job));
        when(partyRepository.existsById("party-x")).thenReturn(false);

        assertThatThrownBy(() -> serviceWithNone.convertToGrn("job-1",
                new OcrCaptureApi.ConvertOcrPayload("party-x", null)))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode()).isEqualTo("OCR_SUPPLIER_NOT_FOUND"));
    }

    @Test
    void uploadWithMockExtractor_extractsPayloadAndConfidenceSummary() {
        InvoiceExtractor mockExtractor = mock(InvoiceExtractor.class, withSettings().withoutAnnotations());
        when(mockExtractor.isConfigured()).thenReturn(true);
        when(mockExtractor.extract(any(), any())).thenReturn("{\"supplierName\":\"Acme Supplies\",\"invoiceNo\":\"INV-992\",\"lines\":[{\"name\":\"Item A\",\"qty\":\"10\",\"unitPrice\":\"100\"}]}");
        when(mockExtractor.confidenceSummary()).thenReturn("{\"supplierName\":0.95,\"invoiceNo\":0.90,\"overall\":0.92}");

        when(jobRepository.save(any(OcrCaptureJob.class))).thenAnswer(inv -> inv.getArgument(0));

        var svc = new OcrCaptureService(jobRepository, mockExtractor, partyRepository,
                goodsReceiptRepository, new ObjectMapper());

        MockMultipartFile file = new MockMultipartFile("file", "invoice.png", "image/png", new byte[]{1, 2, 3, 4});
        var res = svc.upload(file, "test-user");

        assertThat(res.status()).isEqualTo("REVIEW");
        assertThat(res.extractedPayload()).contains("Acme Supplies");
        assertThat(res.extractedPayload()).contains("INV-992");
        assertThat(res.confidenceSummary()).contains("0.95");
    }

    @Test
    void uploadWithFailingExtractor_setsFailedAndErrorCode() {
        InvoiceExtractor mockExtractor = mock(InvoiceExtractor.class, withSettings().withoutAnnotations());
        when(mockExtractor.isConfigured()).thenReturn(true);
        when(mockExtractor.extract(any(), any())).thenThrow(new RuntimeException("Vision API connection timeout"));

        when(jobRepository.save(any(OcrCaptureJob.class))).thenAnswer(inv -> inv.getArgument(0));

        var svc = new OcrCaptureService(jobRepository, mockExtractor, partyRepository,
                goodsReceiptRepository, new ObjectMapper());

        MockMultipartFile file = new MockMultipartFile("file", "invoice.png", "image/png", new byte[]{1, 2, 3, 4});
        var res = svc.upload(file, "test-user");

        assertThat(res.status()).isEqualTo("FAILED");
        assertThat(res.errorCode()).isEqualTo("OCR_PROVIDER_FAILED");
    }
}
