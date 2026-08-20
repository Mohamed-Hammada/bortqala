import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { BusinessVerticalSetupComponent } from './business-vertical-setup.component';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { TenantSetupService } from '../../../core/tenant/tenant-setup.service';

describe('BusinessVerticalSetupComponent', () => {
  let component: BusinessVerticalSetupComponent;
  let fixture: ComponentFixture<BusinessVerticalSetupComponent>;
  let mockTenantSetup: { getVerticalSetup: () => any; configureVertical: (v: any) => any };
  let mockNotification: { success: (msg: string) => void; error: (msg: string) => void };

  beforeEach(() => {
    mockTenantSetup = {
      getVerticalSetup: () =>
        of({
          appId: 'tenant-1',
          vertical: 'GENERAL',
          activeFeatures: ['finance.enabled', 'payroll.enabled'],
          provisionedPolicyGroups: ['Operations Lead'],
        }),
      configureVertical: (v: any) =>
        of({
          appId: 'tenant-1',
          vertical: v,
          activeFeatures: ['sales.enabled', 'procurement.enabled'],
          provisionedPolicyGroups: ['Clinic Administrator'],
        }),
    };

    mockNotification = {
      success: () => undefined,
      error: () => undefined,
    };

    TestBed.configureTestingModule({
      imports: [BusinessVerticalSetupComponent],
      providers: [
        { provide: I18nService, useValue: { t: (key: string) => key } },
        { provide: TenantSetupService, useValue: mockTenantSetup },
        { provide: NotificationService, useValue: mockNotification },
      ],
    });

    fixture = TestBed.createComponent(BusinessVerticalSetupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads current vertical setup on init', () => {
    expect(component.currentVertical()).toBe('GENERAL');
    expect(component.provisionedGroups()).toEqual(['Operations Lead']);
  });

  it('selects and applies a new business vertical', () => {
    component.selectVertical('MEDICAL');
    expect(component.selectedVertical()).toBe('MEDICAL');

    component.applyVertical();
    expect(component.currentVertical()).toBe('MEDICAL');
    expect(component.provisionedGroups()).toEqual(['Clinic Administrator']);
  });
});
