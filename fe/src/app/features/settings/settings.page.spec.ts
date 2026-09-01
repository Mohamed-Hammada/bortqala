import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { SettingsPage } from './settings.page';
import { AuthService } from '../../core/auth/auth.service';
import { NotificationService } from '../../core/notification.service';
import { WebPushService } from '../../core/notification-center/web-push.service';

describe('SettingsPage - Save Semantics & Dirty State Tracking', () => {
  let authServiceMock: any;
  let notificationMock: any;
  let routerMock: any;

  beforeEach(async () => {
    authServiceMock = {
      preferences: vi.fn().mockReturnValue({
        theme: 'LIGHT',
        tableDensity: 'COMFORTABLE',
        locale: 'en-US',
        excelTableStyle: 'CLASSIC',
        defaultPage: '/dashboard',
        showFavorites: true,
        showRecentlyUsed: true,
        maxRecentlyUsed: 5,
      }),
      user: vi.fn().mockReturnValue({
        id: 'u1',
        roles: ['ADMIN'],
      }),
      app: vi.fn().mockReturnValue({ name: 'Bemo ERP' }),
      hasAnyRole: vi.fn().mockReturnValue(true),
      hasMenuAccess: vi.fn().mockReturnValue(true),
      updatePreferences: vi.fn().mockReturnValue(of({})),
      appSettings: vi.fn().mockReturnValue(of({
        sessionTimeoutMinutes: 30,
        sessionTimeoutEnabled: true,
        showReportPresets: true,
        attendanceAnomalyThresholdPercent: 70,
        automaticProcurementNumbering: true,
        automaticDocumentNumbering: true,
        adminDashboardCustomizationEnabled: true,
      })),
      updateAppSettings: vi.fn().mockReturnValue(of({
        sessionTimeoutMinutes: 45,
        sessionTimeoutEnabled: true,
        showReportPresets: true,
        attendanceAnomalyThresholdPercent: 80,
      })),
    };

    notificationMock = {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
      info: vi.fn(),
    };

    routerMock = {
      navigate: vi.fn().mockResolvedValue(true),
      navigateByUrl: vi.fn().mockResolvedValue(true),
    };

    await TestBed.configureTestingModule({
      imports: [SettingsPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceMock },
        { provide: NotificationService, useValue: notificationMock },
        {
          provide: WebPushService,
          useValue: {
            syncPreferences: vi.fn().mockResolvedValue(true),
            subscribed: vi.fn().mockReturnValue(false),
            configured: vi.fn().mockReturnValue(true),
            supported: vi.fn().mockReturnValue(true),
            busy: vi.fn().mockReturnValue(false),
            permission: vi.fn().mockReturnValue('default'),
          },
        },
        { provide: Router, useValue: routerMock },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: {
                get: vi.fn().mockReturnValue(null),
              },
            },
          },
        },
      ],
    }).compileComponents();
  });

  function createComponent() {
    const fixture = TestBed.createComponent(SettingsPage);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    return { fixture, component };
  }

  it('reports hasUnsavedChanges=false initially for pristine form', () => {
    const { component } = createComponent();
    expect(component.hasUnsavedChanges()).toBe(false);
  });

  it('reports hasUnsavedChanges=true when user preferences form is modified', () => {
    const { component } = createComponent();
    component.form.controls.theme.setValue('DARK');
    component.form.markAsDirty();
    expect(component.hasUnsavedChanges()).toBe(true);
  });

  it('saveUserPreferences updates authService, clears dirty state, and shows success', async () => {
    const { component } = createComponent();
    component.form.controls.theme.setValue('DARK');
    component.form.markAsDirty();

    await component.saveUserPreferences();

    expect(authServiceMock.updatePreferences).toHaveBeenCalledWith(
      expect.objectContaining({ theme: 'DARK' })
    );
    expect(component.form.dirty).toBe(false);
    expect(component.hasUnsavedChanges()).toBe(false);
    expect(notificationMock.success).toHaveBeenCalled();
  });

  it('preserves draft and dirty state if saveUserPreferences fails', async () => {
    const { component } = createComponent();
    authServiceMock.updatePreferences.mockReturnValue(
      throwError(() => new Error('Server error'))
    );
    component.form.controls.theme.setValue('DARK');
    component.form.markAsDirty();

    await component.saveUserPreferences();

    expect(component.form.dirty).toBe(true);
    expect(component.hasUnsavedChanges()).toBe(true);
    expect(component.form.controls.theme.value).toBe('DARK');
    expect(notificationMock.error).toHaveBeenCalled();
  });

  it('cancel restores the original preferences and clears dirty state', () => {
    const { component } = createComponent();
    component.form.controls.theme.setValue('DARK');
    component.form.markAsDirty();
    expect(component.hasUnsavedChanges()).toBe(true);

    component.cancel();

    expect(component.form.controls.theme.value).toBe('LIGHT');
    expect(component.form.dirty).toBe(false);
    expect(component.hasUnsavedChanges()).toBe(false);
  });

  it('saveAppSettings updates system settings and marks appSettingsForm pristine', async () => {
    const { component } = createComponent();
    component.appSettingsForm.controls.sessionTimeoutMinutes.setValue(45);
    component.appSettingsForm.markAsDirty();

    await component.saveAppSettings();

    expect(authServiceMock.updateAppSettings).toHaveBeenCalledWith(
      expect.objectContaining({ sessionTimeoutMinutes: 45 })
    );
    expect(component.appSettingsForm.dirty).toBe(false);
    expect(notificationMock.success).toHaveBeenCalled();
  });
});
