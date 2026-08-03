package com.bemo.hr.workforce;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class WorkforceExcelImportServiceTests {

    @Mock private WorkforceImportBatchRepository batchRepository;
    @Mock private WorkforceImportRowRepository rowRepository;
    @Mock private WorkforceImportChangeRepository changeRepository;
    @Mock private WorkerRepository workerRepository;
    @Mock private ManualAttendanceEntryRepository attendanceRepository;
    @Mock private com.bemo.hr.audit.application.AuditService auditService;

    @InjectMocks
    private WorkforceExcelImportService importService;

    @BeforeEach
    void setUp() {
        TenantContext.set("test-tenant");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void upload_throwsBusinessRuleException_onCorruptedFile() {
        byte[] badFile = new byte[] { 0, 1, 2, 3, 4, 5 };
        
        assertThatThrownBy(() -> importService.upload(new org.springframework.mock.web.MockMultipartFile("file", "file.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", badFile)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("تعذر قراءة ملف البصمة.")
                .hasFieldOrPropertyWithValue("code", "EXCEL_READ_FAILED")
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }
}
