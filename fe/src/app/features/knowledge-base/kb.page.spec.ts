import { ComponentFixture, TestBed } from '@angular/core/testing';
import { KbPage } from './kb.page';
import { HelpdeskService } from '../helpdesk/helpdesk.service';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';

describe('KbPage', () => {
  let component: KbPage;
  let fixture: ComponentFixture<KbPage>;

  const mockService = { listKbArticles: () => Promise.resolve([]) };
  const mockI18n = { t: (key: string) => key, locale: () => 'en-US' as const };
  const mockNotification = { success: () => {}, error: () => {} };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [KbPage],
      providers: [
        { provide: HelpdeskService, useValue: mockService },
        { provide: I18nService, useValue: mockI18n },
        { provide: NotificationService, useValue: mockNotification },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(KbPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => { expect(component).toBeTruthy(); });
  it('should load articles on init', () => { expect(component.loading()).toBeFalsy(); });
  it('should toggle create dialog', () => {
    component.showCreate.set(true);
    fixture.detectChanges();
    expect(component.showCreate()).toBeTruthy();
  });
});
