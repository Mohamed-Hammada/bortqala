package com.bemo.hr.platform.deployment;

import com.bemo.hr.platform.deployment.api.PlatformDeploymentApi.DiagnosticsResponse;
import com.bemo.hr.platform.deployment.application.PlatformDiagnosticsService;
import com.bemo.hr.platform.deployment.domain.DeploymentHealthSnapshot;
import com.bemo.hr.platform.deployment.infrastructure.DeploymentHealthSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformDiagnosticsServiceTests {

    @Mock
    private DeploymentHealthSnapshotRepository snapshotRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private PlatformDiagnosticsService diagnosticsService;

    @BeforeEach
    void setUp() {
        diagnosticsService = new PlatformDiagnosticsService(snapshotRepository, jdbcTemplate);
    }

    @Test
    @DisplayName("evaluateAndRecordDiagnostics saves snapshot and returns UP status when DB is healthy")
    void evaluateHealthyDiagnostics() {
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class))).thenReturn(1);
        when(snapshotRepository.save(any(DeploymentHealthSnapshot.class))).thenAnswer(i -> i.getArgument(0));

        long timestamp = 1755600000000L;
        DiagnosticsResponse response = diagnosticsService.evaluateAndRecordDiagnostics(timestamp);

        assertThat(response).isNotNull();
        assertThat(response.serviceStatus()).isEqualTo("UP");
        assertThat(response.dbStatus()).isEqualTo("UP");
        assertThat(response.correlationId()).startsWith("DIAG-");
        assertThat(response.securityAudit()).isNotNull();

        verify(snapshotRepository).save(any(DeploymentHealthSnapshot.class));
    }
}
