package com.bemo.hr.audit;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.audit.domain.AuditLog;
import com.bemo.hr.audit.infrastructure.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuditServiceBreakGlassTests {

    private AuditLogRepository repository;
    private AuditService service;

    @BeforeEach
    void setUp() {
        repository = mock(AuditLogRepository.class);
        service = new AuditService(repository);
    }

    @Test
    @DisplayName("Record standard audit log")
    void record_standard() {
        service.record("UPDATE", "PURCHASE_ORDER", "PO-101", "admin", "{\"status\":\"APPROVED\"}", "127.0.0.1");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo("UPDATE");
        assertThat(saved.getEntityType()).isEqualTo("PURCHASE_ORDER");
        assertThat(saved.getEntityId()).isEqualTo("PO-101");
        assertThat(saved.getUsername()).isEqualTo("admin");
        assertThat(saved.isBreakGlass()).isFalse();
    }

    @Test
    @DisplayName("Record break-glass emergency audit log")
    void record_breakGlass() {
        service.recordBreakGlass("OVERRIDE_CLOSE", "PROJECT", "PRJ-999", "superadmin",
                "Site emergency safety closeout", "{\"forceClose\":true}", "192.168.1.50", "Mozilla/5.0");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo("OVERRIDE_CLOSE");
        assertThat(saved.getEntityType()).isEqualTo("PROJECT");
        assertThat(saved.getEntityId()).isEqualTo("PRJ-999");
        assertThat(saved.getUsername()).isEqualTo("superadmin");
        assertThat(saved.getReason()).isEqualTo("Site emergency safety closeout");
        assertThat(saved.isBreakGlass()).isTrue();
        assertThat(saved.getUserAgent()).isEqualTo("Mozilla/5.0");
    }
}
