import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PaymentLinksPage } from './payment-links.page';
import { PaymentLinkService } from './payment-link.service';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { of } from 'rxjs';
import { vi } from 'vitest';

describe('PaymentLinksPage', () => {
  let component: PaymentLinksPage;
  let fixture: ComponentFixture<PaymentLinksPage>;

  const mockService = {
    listLinks: () => Promise.resolve([]),
    getGatewayConfig: () => Promise.resolve({ enabled: true }),
  };

  const mockI18n = { t: (key: string) => key, locale: () => 'en-US' as const };
  const mockNotification = { success: () => {}, error: () => {} };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaymentLinksPage],
      providers: [
        { provide: PaymentLinkService, useValue: mockService },
        { provide: I18nService, useValue: mockI18n },
        { provide: NotificationService, useValue: mockNotification },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PaymentLinksPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load links on init', () => {
    expect(component.loading()).toBeFalsy();
  });

  it('should show create dialog', () => {
    component.showCreate.set(true);
    fixture.detectChanges();
    expect(component.showCreate()).toBeTruthy();
  });

  it('should have status color mapping', () => {
    expect(component.statusColor('PENDING')).toBe('var(--gold)');
    expect(component.statusColor('PAID')).toBe('var(--success)');
    expect(component.statusColor('EXPIRED')).toBe('var(--muted)');
    expect(component.statusColor('CANCELLED')).toBe('var(--danger)');
  });
});
