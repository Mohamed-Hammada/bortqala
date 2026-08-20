package com.bemo.hr.platform.deployment;

import com.bemo.hr.platform.deployment.api.PlatformDeploymentApi.InstallLicenseRequest;
import com.bemo.hr.platform.deployment.api.PlatformDeploymentApi.LicenseStatusResponse;
import com.bemo.hr.platform.deployment.application.OfflineLicensingService;
import com.bemo.hr.platform.deployment.domain.TenantLicenseCertificate;
import com.bemo.hr.platform.deployment.infrastructure.TenantLicenseCertificateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfflineLicensingServiceTests {

    @Mock
    private TenantLicenseCertificateRepository certificateRepository;

    private OfflineLicensingService licensingService;

    @BeforeEach
    void setUp() {
        licensingService = new OfflineLicensingService(certificateRepository);
    }

    @Test
    @DisplayName("installCertificate saves new Ed25519 signed license certificate with perpetual flag")
    void installPerpetualCertificate() {
        when(certificateRepository.save(any(TenantLicenseCertificate.class))).thenAnswer(i -> i.getArgument(0));

        InstallLicenseRequest request = new InstallLicenseRequest(
                "BEMO-PROD-2026-PERPETUAL-KEY",
                "{\"seats\": 100, \"perpetual\": true}",
                "SIG-ED25519-ABCDEF",
                "FINGERPRINT-WIN64-SERVER-01"
        );

        LicenseStatusResponse response = licensingService.installCertificate(request, 1755600000000L);

        assertThat(response).isNotNull();
        assertThat(response.isPerpetual()).isTrue();
        assertThat(response.licensedSeats()).isEqualTo(100);
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.isSignatureValid()).isTrue();

        verify(certificateRepository).save(any(TenantLicenseCertificate.class));
    }

    @Test
    @DisplayName("validateCurrentLicense marks expired annual certificate when past expiry date")
    void validateExpiredCertificate() {
        TenantLicenseCertificate cert = new TenantLicenseCertificate(
                "hash-123",
                "{\"seats\": 10}",
                "sig-123",
                "fp-123",
                10,
                "[\"CORE\"]",
                1750000000000L,
                1755000000000L, // expired before 1755600000000L
                false,
                14,
                1750000000000L,
                "ACTIVE",
                1750000000000L
        );
        when(certificateRepository.findFirstByStatusOrderByCreatedAtDesc("ACTIVE")).thenReturn(Optional.of(cert));
        when(certificateRepository.save(any(TenantLicenseCertificate.class))).thenAnswer(i -> i.getArgument(0));

        LicenseStatusResponse response = licensingService.validateCurrentLicense(1755600000000L);

        assertThat(response.status()).isEqualTo("EXPIRED");
        assertThat(response.daysRemaining()).isEqualTo(0);
    }
}
