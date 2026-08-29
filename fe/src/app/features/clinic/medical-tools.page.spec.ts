import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { MedicalToolsPageComponent } from './medical-tools.page';
import { ClinicService } from './clinic.service';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { of } from 'rxjs';

describe('MedicalToolsPageComponent', () => {
  let component: MedicalToolsPageComponent;
  let fixture: ComponentFixture<MedicalToolsPageComponent>;
  let clinicService: ClinicService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MedicalToolsPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ClinicService,
        I18nService,
        NotificationService,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MedicalToolsPageComponent);
    component = fixture.componentInstance;
    clinicService = TestBed.inject(ClinicService);
  });

  it('should create and compute pediatric dose on init', () => {
    vi.spyOn(clinicService, 'calculatePediatricDose').mockReturnValue(of({
      weightKg: 15,
      dailyDoseMg: 600,
      singleDoseMg: 200,
      singleDoseMl: 4,
      frequencyPerDay: 3,
      administrationInstructions: 'Administer 200 mg (4 ml) 3 times daily',
    }));

    vi.spyOn(clinicService, 'getAllLicenses').mockReturnValue(of([]));

    component.ngOnInit();
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.calcResult()?.dailyDoseMg).toBe(600);
    expect(component.calcResult()?.singleDoseMl).toBe(4);
  });

  it('should switch tabs and open modals', () => {
    component.activeTab.set('telemedicine');
    component.openTelemedModal();
    expect(component.showTelemedModal()).toBe(true);

    component.activeTab.set('licenses');
    component.openLicenseModal();
    expect(component.showLicenseModal()).toBe(true);
  });
});
