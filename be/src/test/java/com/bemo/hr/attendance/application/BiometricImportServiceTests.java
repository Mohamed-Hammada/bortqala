package com.bemo.hr.attendance.application;

import com.bemo.hr.attendance.application.BiometricFileReader;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BiometricImportServiceTests {

    @Mock private BiometricFileReader reader;
    @InjectMocks private BiometricImportService biometricImportService;

    @Test
    void preview_throwsBusinessRuleExceptionWithSafeKey_onIOException() throws IOException {
        MultipartFile mockFile = org.mockito.Mockito.mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getBytes()).thenThrow(new IOException("Simulated disk error"));

        assertThatThrownBy(() -> biometricImportService.preview(mockFile))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Could not read the uploaded file.")
                .hasFieldOrPropertyWithValue("code", "EXCEL_READ_FAILED")
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }
}
