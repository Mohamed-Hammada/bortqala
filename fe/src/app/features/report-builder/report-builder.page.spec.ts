import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReportBuilderPage } from './report-builder.page';
import { MarketingService } from '../marketing/marketing.service';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';

describe('ReportBuilderPage', () => {
  let component: ReportBuilderPage;
  let fixture: ComponentFixture<ReportBuilderPage>;

  const mockService = { listDatasets: () => Promise.resolve([]), listSavedReports: () => Promise.resolve([]) };
  const mockI18n = { t: (key: string) => key, locale: () => 'en-US' as const };
  const mockNotification = { success: () => {}, error: () => {} };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReportBuilderPage],
      providers: [
        { provide: MarketingService, useValue: mockService },
        { provide: I18nService, useValue: mockI18n },
        { provide: NotificationService, useValue: mockNotification },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ReportBuilderPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => { expect(component).toBeTruthy(); });
  it('should load datasets on init', () => { expect(component.loading()).toBeFalsy(); });
  it('should toggle field selections', () => {
    component.toggleField('branchName', true);
    expect(component.dimensions()).toContain('branchName');
    component.toggleField('branchName', true);
    expect(component.dimensions()).not.toContain('branchName');
  });
});
