import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MarketingPage } from './marketing.page';
import { MarketingService } from './marketing.service';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';

describe('MarketingPage', () => {
  let component: MarketingPage;
  let fixture: ComponentFixture<MarketingPage>;

  const mockService = {
    listCampaigns: () => Promise.resolve([]),
    listSurveys: () => Promise.resolve([]),
    listDatasets: () => Promise.resolve([]),
    listSavedReports: () => Promise.resolve([]),
  };
  const mockI18n = { t: (key: string) => key, locale: () => 'en-US' as const };
  const mockNotification = { success: () => {}, error: () => {} };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MarketingPage],
      providers: [
        { provide: MarketingService, useValue: mockService },
        { provide: I18nService, useValue: mockI18n },
        { provide: NotificationService, useValue: mockNotification },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(MarketingPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => { expect(component).toBeTruthy(); });
  it('should load campaigns on init', () => { expect(component.loading()).toBeFalsy(); });
  it('should have status color mapping', () => {
    expect(component.statusColor('DRAFT')).toBe('var(--muted)');
    expect(component.statusColor('SENT')).toBe('var(--success)');
    expect(component.statusColor('FAILED')).toBe('var(--danger)');
  });
  it('should toggle tabs', () => {
    component.activeTab.set('surveys');
    expect(component.activeTab()).toBe('surveys');
  });
});
