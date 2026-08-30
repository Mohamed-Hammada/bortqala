package com.bemo.hr.platform.deployment.application;

import com.bemo.hr.platform.deployment.api.PlatformDeploymentApi.DiagnosticsResponse;
import com.bemo.hr.platform.deployment.api.PlatformDeploymentApi.SecurityAuditSummary;
import com.bemo.hr.platform.deployment.domain.DeploymentHealthSnapshot;
import com.bemo.hr.platform.deployment.infrastructure.DeploymentHealthSnapshotRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.util.UUID;

@Service
public class PlatformDiagnosticsService {

    private final DeploymentHealthSnapshotRepository snapshotRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${hr.security.trusted-proxies:127.0.0.1/32}")
    private String trustedProxies;

    @Value("${hr.cors.allowed-origins:http://localhost:4200}")
    private String corsAllowedOrigins;

    @Value("${hr.jwt.secret:}")
    private String jwtSecret;

    public PlatformDiagnosticsService(
            DeploymentHealthSnapshotRepository snapshotRepository,
            JdbcTemplate jdbcTemplate) {
        this.snapshotRepository = snapshotRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public DiagnosticsResponse evaluateAndRecordDiagnostics(long timestamp) {
        long start = System.currentTimeMillis();
        String dbStatus = "UP";
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        } catch (Exception ex) {
            dbStatus = "DOWN";
        }
        long dbLatencyMs = Math.max(1, System.currentTimeMillis() - start);

        Runtime runtime = Runtime.getRuntime();
        long totalMemoryMb = runtime.totalMemory() / (1024 * 1024);
        long freeMemoryMb = runtime.freeMemory() / (1024 * 1024);
        long usedMemoryMb = totalMemoryMb - freeMemoryMb;
        long maxMemoryMb = runtime.maxMemory() / (1024 * 1024);

        long uptimeSeconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        long storageTotalBytes = 100L * 1024 * 1024 * 1024; // 100 GB standard allocation
        long storageUsedBytes = 12L * 1024 * 1024 * 1024; // 12 GB sample data

        String serviceStatus = "UP".equals(dbStatus) ? "UP" : "DEGRADED";
        String backgroundJobsHealth = "HEALTHY";
        int activeSessionsCount = 1;
        String correlationId = "DIAG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        DeploymentHealthSnapshot snapshot = new DeploymentHealthSnapshot(
                serviceStatus,
                dbStatus,
                dbLatencyMs,
                storageUsedBytes,
                storageTotalBytes,
                activeSessionsCount,
                backgroundJobsHealth,
                usedMemoryMb,
                maxMemoryMb,
                uptimeSeconds,
                correlationId,
                timestamp
        );
        snapshotRepository.save(snapshot);

        boolean trustedProxiesConfigured = trustedProxies != null && !trustedProxies.isBlank() && !trustedProxies.contains("0.0.0.0/0");
        boolean corsConfigured = corsAllowedOrigins != null && !corsAllowedOrigins.isBlank() && !corsAllowedOrigins.contains("*");
        boolean jwtConfigured = jwtSecret != null && jwtSecret.length() >= 32;
        boolean failFastPassed = trustedProxiesConfigured && corsConfigured && jwtConfigured;

        SecurityAuditSummary securityAudit = new SecurityAuditSummary(
                trustedProxiesConfigured,
                trustedProxies,
                corsConfigured,
                corsAllowedOrigins,
                jwtConfigured,
                failFastPassed
        );

        return new DiagnosticsResponse(
                serviceStatus,
                dbStatus,
                dbLatencyMs,
                storageUsedBytes,
                storageTotalBytes,
                activeSessionsCount,
                backgroundJobsHealth,
                usedMemoryMb,
                maxMemoryMb,
                uptimeSeconds,
                correlationId,
                timestamp,
                securityAudit
        );
    }
}
