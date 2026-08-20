import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DeploymentService } from './deployment.service';
import { BackupSnapshot, DiagnosticsResponse, LicenseStatus } from './deployment.models';

describe('DeploymentService', () => {
  let service: DeploymentService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DeploymentService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DeploymentService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should fetch diagnostics data', () => {
    const mockDiag: DiagnosticsResponse = {
      serviceStatus: 'UP',
      dbStatus: 'UP',
      dbLatencyMs: 2,
      storageUsedBytes: 1000,
      storageTotalBytes: 5000,
      activeSessionsCount: 3,
      backgroundJobsHealth: 'HEALTHY',
      jvmMemoryUsedMb: 250,
      jvmMemoryMaxMb: 1024,
      uptimeSeconds: 3600,
      correlationId: 'DIAG-1234',
      evaluatedAt: 1755600000000,
      securityAudit: {
        trustedProxiesConfigured: true,
        trustedProxiesCidr: '127.0.0.1/32',
        corsConfigured: true,
        corsAllowedOrigins: 'http://localhost:4200',
        jwtConfigured: true,
        failFastPassed: true,
      },
    };

    service.getDiagnostics().subscribe((res) => {
      expect(res.serviceStatus).toBe('UP');
      expect(res.securityAudit?.failFastPassed).toBeTrue();
    });

    const req = httpTesting.expectOne('/api/v1/platform/diagnostics/health');
    expect(req.request.method).toBe('GET');
    req.flush(mockDiag);
  });

  it('should trigger and list backup snapshots', () => {
    const mockBackup: BackupSnapshot = {
      id: 'snap-1',
      snapshotName: 'SNAP_TEST',
      backupType: 'FULL',
      fileSizeBytes: 500000,
      sha256Checksum: 'abcdef1234567890',
      encryptionAlgorithm: 'AES_256_GCM',
      storageLocation: 's3://backups/snap.enc',
      status: 'COMPLETED',
      verificationDrillStatus: 'PENDING',
      createdAt: 1755600000000,
    };

    service.triggerBackup({ snapshotName: 'SNAP_TEST' }).subscribe((res) => {
      expect(res.snapshotName).toBe('SNAP_TEST');
      expect(res.encryptionAlgorithm).toBe('AES_256_GCM');
    });

    const req = httpTesting.expectOne('/api/v1/platform/backups/trigger');
    expect(req.request.method).toBe('POST');
    req.flush(mockBackup);
  });

  it('should install license certificate', () => {
    const mockLicense: LicenseStatus = {
      id: 'lic-1',
      licenseKeyHash: 'hash-abc',
      licensedSeats: 50,
      licensedModules: ['CORE', 'PROJECTS'],
      issueDate: 1755600000000,
      isPerpetual: true,
      gracePeriodDays: 14,
      lastValidatedAt: 1755600000000,
      status: 'ACTIVE',
      isSignatureValid: true,
      daysRemaining: 9999,
    };

    service.installLicense({ licenseKey: 'BEMO-PROD-KEY' }).subscribe((res) => {
      expect(res.isPerpetual).toBeTrue();
      expect(res.licensedSeats).toBe(50);
    });

    const req = httpTesting.expectOne('/api/v1/platform/licensing/install');
    expect(req.request.method).toBe('POST');
    req.flush(mockLicense);
  });
});
