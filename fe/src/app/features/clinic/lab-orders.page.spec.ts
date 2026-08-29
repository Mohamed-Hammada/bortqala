import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { LabOrdersPageComponent } from './lab-orders.page';
import { ClinicService } from './clinic.service';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { of } from 'rxjs';

describe('LabOrdersPageComponent', () => {
  let component: LabOrdersPageComponent;
  let fixture: ComponentFixture<LabOrdersPageComponent>;
  let clinicService: ClinicService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LabOrdersPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ClinicService,
        I18nService,
        NotificationService,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LabOrdersPageComponent);
    component = fixture.componentInstance;
    clinicService = TestBed.inject(ClinicService);
  });

  it('should create and load initial orders and tests', () => {
    vi.spyOn(clinicService, 'getAllLabOrders').mockReturnValue(of([
      {
        id: 'order-1',
        patientId: 'pat-1',
        patientName: 'Ahmed Ali',
        patientMrn: 'MRN-00001',
        doctorEmployeeId: 'doc-1',
        doctorName: 'Dr. Tarek',
        testId: 'test-1',
        category: 'LAB',
        testCode: 'CBC',
        testName: 'Complete Blood Count',
        status: 'ORDERED',
        orderedAt: 1788700000000,
        isCriticalAcknowledged: false,
      }
    ]));

    vi.spyOn(clinicService, 'getAllLabTests').mockReturnValue(of([
      {
        id: 'test-1',
        code: 'CBC',
        category: 'LAB',
        name: 'Complete Blood Count',
        sampleType: 'BLOOD',
        price: 150,
      }
    ]));

    component.ngOnInit();
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.orders().length).toBe(1);
    expect(component.labTests().length).toBe(1);
  });

  it('should switch to aging tab and load delayed external orders', () => {
    vi.spyOn(clinicService, 'getAgingSentOutOrders').mockReturnValue(of([
      {
        id: 'order-2',
        patientId: 'pat-2',
        patientName: 'Sara Hassan',
        patientMrn: 'MRN-00002',
        doctorEmployeeId: 'doc-1',
        doctorName: 'Dr. Tarek',
        testId: 'test-2',
        category: 'IMAGING',
        testCode: 'MRI-BRAIN',
        testName: 'Brain MRI with Contrast',
        status: 'SENT_OUT',
        orderedAt: 1788500000000,
        sentOutAt: 1788510000000,
        externalLabName: 'Al-Mokhtabar / Cairo Scan',
        isCriticalAcknowledged: false,
      }
    ]));

    component.setTab('AGING');
    fixture.detectChanges();

    expect(component.activeTab()).toBe('AGING');
    expect(component.agingOrders().length).toBe(1);
  });
});
