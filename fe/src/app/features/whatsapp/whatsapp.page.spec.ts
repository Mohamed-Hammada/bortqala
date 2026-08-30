import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WhatsAppPage } from './whatsapp.page';
import { I18nService } from '../../core/i18n.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('WhatsAppPage', () => {
  let component: WhatsAppPage;
  let fixture: ComponentFixture<WhatsAppPage>;

  const mockI18n = { t: (key: string) => key, locale: () => 'en-US' as const };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WhatsAppPage, HttpClientTestingModule],
      providers: [
        { provide: I18nService, useValue: mockI18n },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WhatsAppPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have empty logs initially', () => {
    expect(component.logs().length).toBe(0);
  });

  it('should set test phone', () => {
    component.testPhone.set('+201234567890');
    expect(component.testPhone()).toBe('+201234567890');
  });
});
