import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { DeviceSigningService } from './device-signing.service';
import { EnrolledDevice } from './device-signing.models';

describe('DeviceSigningService', () => {
  let service: DeviceSigningService;
  let httpMock: HttpTestingController;

  const mockDevice: EnrolledDevice = {
    id: 'dev-1',
    deviceIdentifier: 'ident-1',
    deviceName: 'Work Laptop',
    algorithm: 'ECDSA-P256',
    status: 'ACTIVE',
    enrolledAt: '2026-08-30T10:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), DeviceSigningService],
    });
    service = TestBed.inject(DeviceSigningService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loadDevices fetches devices and updates signal', () => {
    service.loadDevices().subscribe((devices) => {
      expect(devices.length).toBe(1);
      expect(devices[0].deviceName).toBe('Work Laptop');
    });

    const req = httpMock.expectOne('/api/v1/auth/devices');
    expect(req.request.method).toBe('GET');
    req.flush({ devices: [mockDevice] });

    expect(service.devices().length).toBe(1);
  });

  it('enrollDevice posts request and updates signal', () => {
    service.enrollDevice({
      deviceIdentifier: 'ident-1',
      deviceName: 'Work Laptop',
      publicKey: 'pubkey-123',
      algorithm: 'ECDSA-P256',
    }).subscribe((device) => {
      expect(device.id).toBe('dev-1');
    });

    const req = httpMock.expectOne('/api/v1/auth/devices/enroll');
    expect(req.request.method).toBe('POST');
    req.flush(mockDevice);

    expect(service.devices().length).toBe(1);
    expect(service.devices()[0].id).toBe('dev-1');
  });

  it('revokeDevice updates device status in signal', () => {
    // Populate devices first
    service.loadDevices().subscribe();
    httpMock.expectOne('/api/v1/auth/devices').flush({ devices: [mockDevice] });

    service.revokeDevice('dev-1', 'Lost device').subscribe();

    const req = httpMock.expectOne('/api/v1/auth/devices/dev-1/revoke');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ reason: 'Lost device' });
    req.flush(null);

    expect(service.devices()[0].status).toBe('REVOKED');
  });

  it('requestChallenge posts payload and receives challenge', () => {
    service.requestChallenge('dev-1', 'PAYROLL_DISBURSEMENT', '{"amount":1000}').subscribe((challenge) => {
      expect(challenge.challengeId).toBe('chal-1');
      expect(challenge.nonce).toBe('nonce-123');
    });

    const req = httpMock.expectOne('/api/v1/auth/devices/challenge');
    expect(req.request.method).toBe('POST');
    req.flush({
      challengeId: 'chal-1',
      deviceId: 'dev-1',
      nonce: 'nonce-123',
      operationType: 'PAYROLL_DISBURSEMENT',
      payloadHash: 'hash-abc',
      expiresAt: '2026-08-30T10:05:00Z',
    });
  });

  it('verifySignature posts signature and receives verification', () => {
    service.verifySignature('chal-1', 'sig-xyz', '{"amount":1000}').subscribe((res) => {
      expect(res.verified).toBe(true);
    });

    const req = httpMock.expectOne('/api/v1/auth/devices/verify');
    expect(req.request.method).toBe('POST');
    req.flush({
      verified: true,
      challengeId: 'chal-1',
      verifiedAt: '2026-08-30T10:01:00Z',
    });
  });
});
