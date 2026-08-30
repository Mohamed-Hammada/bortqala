import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HelpdeskPage } from './helpdesk.page';
import { HelpdeskService } from './helpdesk.service';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';

describe('HelpdeskPage', () => {
  let component: HelpdeskPage;
  let fixture: ComponentFixture<HelpdeskPage>;

  const mockService = {
    listCategories: () => Promise.resolve([]),
    listTickets: () => Promise.resolve({ tickets: [], openCount: 0, myOpenCount: 0 }),
  };
  const mockI18n = { t: (key: string) => key, locale: () => 'en-US' as const };
  const mockNotification = { success: () => {}, error: () => {} };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HelpdeskPage],
      providers: [
        { provide: HelpdeskService, useValue: mockService },
        { provide: I18nService, useValue: mockI18n },
        { provide: NotificationService, useValue: mockNotification },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(HelpdeskPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => { expect(component).toBeTruthy(); });
  it('should load tickets on init', () => { expect(component.loading()).toBeFalsy(); });
  it('should have priority color mapping', () => {
    expect(component.priorityColor('URGENT')).toBe('var(--danger)');
    expect(component.priorityColor('NORMAL')).toBe('var(--gold)');
  });
  it('should have status color mapping', () => {
    expect(component.statusColor('NEW')).toBe('var(--gold)');
    expect(component.statusColor('CLOSED')).toBe('var(--muted)');
  });
});
