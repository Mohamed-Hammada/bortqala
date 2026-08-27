package com.bemo.hr.trade.procurement;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.trade.procurement.application.InvoiceExtractor;
import com.bemo.hr.trade.procurement.application.NoneExtractor;
import com.bemo.hr.trade.procurement.application.OcrCaptureService;
import com.bemo.hr.trade.procurement.infrastructure.OcrCaptureJobRepository;
import com.bemo.hr.party.BusinessPartyRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcrCaptureServiceTests {

    @Mock OcrCaptureJobRepository jobRepository;
    @Mock BusinessPartyRepository partyRepository;

    private OcrCaptureService serviceWithNone;

    @BeforeEach
    void setUp() {
        TenantContext.set("test-app");
        serviceWithNone = new OcrCaptureService(jobRepository, new NoneExtractor(), partyRepository);
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
        var svc = new OcrCaptureService(jobRepository, mockExtractor, partyRepository);
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
        var svc = new OcrCaptureService(jobRepository, mockExtractor, partyRepository);
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
        var svc = new OcrCaptureService(jobRepository, mockExtractor, partyRepository);
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
}
