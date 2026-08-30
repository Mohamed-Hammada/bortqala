import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SecuritySettingsComponent } from './security-settings.component';
import { SecurityPackService } from '../../../core/auth/security-pack.service';
import { AuthService } from '../../../core/auth/auth.service';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { ConfirmDialogService } from '../../../core/confirm-dialog.service';
import { of } from 'rxjs';

describe('SecuritySettingsComponent', () => {
  let component: SecuritySettingsComponent;
  let fixture: ComponentFixture<SecuritySettingsComponent>;

  const mockSecurityService = {
    getTotpStatus: vi.fn().mockReturnValue(of({ enabled: false, enabledAt: null, remainingBackupCodes: 0 })),
    loadDevices: vi.fn().mockReturnValue(of([])),
    getPolicy: vi.fn().mockReturnValue(
      of({
        minPasswordLength: 8,
        requireUppercase: true,
        requireLowercase: true,
        requireDigits: true,
        requireSpecialChars: false,
        passwordHistoryCount: 3,
        maxPasswordAgeDays: 0,
        sessionTimeoutMinutes: 30,
        superAdminIpBypass: true,
      })
    ),
    loadIpRules: vi.fn().mockReturnValue(of([])),
    enrollTotp: vi.fn().mockReturnValue(of({ secret: 'S3CR3T', otpauthUri: 'otpauth://...', backupCodes: ['C1'] })),
    activateTotp: vi.fn().mockReturnValue(of(undefined)),
    updatePolicy: vi.fn().mockReturnValue(of({})),
    revokeDevice: vi.fn().mockReturnValue(of(undefined)),
    createIpRule: vi.fn().mockReturnValue(of({ id: 'r1', roleCode: 'ADMIN', cidrBlock: '10.0.0.0/8', description: '', createdAt: '' })),
    deleteIpRule: vi.fn().mockReturnValue(of(undefined)),
  };

  const mockAuthService = {
    hasAnyRole: vi.fn().mockReturnValue(true),
  };

  const mockI18nService = {
    t: vi.fn().mockImplementation((key: string) => key),
  };

  const mockNotificationService = {
    success: vi.fn(),
    error: vi.fn(),
  };

  const mockConfirmService = {
    confirm: vi.fn().mockResolvedValue(true),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SecuritySettingsComponent],
      providers: [
        { provide: SecurityPackService, useValue: mockSecurityService },
        { provide: AuthService, useValue: mockAuthService },
        { provide: I18nService, useValue: mockI18nService },
        { provide: NotificationService, useValue: mockNotificationService },
        { provide: ConfirmDialogService, useValue: mockConfirmService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SecuritySettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load security settings', () => {
    expect(component).toBeTruthy();
    expect(mockSecurityService.getTotpStatus).toHaveBeenCalled();
    expect(mockSecurityService.getPolicy).toHaveBeenCalled();
    expect(mockSecurityService.loadDevices).toHaveBeenCalled();
    expect(mockSecurityService.loadIpRules).toHaveBeenCalled();
  });

  it('should enroll TOTP when startEnroll is called', () => {
    component.startEnroll();
    expect(mockSecurityService.enrollTotp).toHaveBeenCalled();
    expect(component.enrollData()?.secret).toBe('S3CR3T');
  });
});
