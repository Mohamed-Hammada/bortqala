import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AutomationPage } from './automation.page';
import { I18nService } from '../../core/i18n.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('AutomationPage', () => {
  let component: AutomationPage;
  let fixture: ComponentFixture<AutomationPage>;

  const mockI18n = { t: (key: string) => key, locale: () => 'en-US' as const };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AutomationPage, HttpClientTestingModule],
      providers: [
        { provide: I18nService, useValue: mockI18n },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AutomationPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should default to templates tab', () => {
    expect(component.activeTab()).toBe('templates');
  });

  it('should switch tabs', () => {
    component.activeTab.set('dunning');
    expect(component.activeTab()).toBe('dunning');
    component.activeTab.set('jobs');
    expect(component.activeTab()).toBe('jobs');
  });
});
