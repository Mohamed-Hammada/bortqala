import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { PerformancePage } from './performance.page';
import { PerformanceService } from './performance.service';
import { PerformanceAppraisal, PerformanceCycle, PerformanceKpi } from './performance.models';

describe('PerformancePage', () => {
  let service: PerformanceService;

  const mockCycles: PerformanceCycle[] = [
    {
      id: 'cyc-1',
      nameAr: 'تقييم 2026',
      nameEn: 'Evaluation 2026',
      periodYear: 2026,
      startDate: '2026-01-01',
      endDate: '2026-12-31',
      status: 'ACTIVE',
      createdAt: 1770000000000,
    },
  ];

  const mockAppraisals: PerformanceAppraisal[] = [
    {
      id: 'appr-1',
      cycleId: 'cyc-1',
      cycleNameAr: 'تقييم 2026',
      cycleNameEn: 'Evaluation 2026',
      employeeId: 'emp-1',
      employeeName: 'Ahmed Ali',
      employeeCode: 'EMP-01',
      reviewerId: 'mgr-1',
      reviewerName: 'Manager 1',
      selfScore: 90,
      managerScore: 95,
      finalScore: 95,
      ratingBand: 'OUTSTANDING',
      status: 'SUBMITTED',
      managerFeedback: 'Good job',
      developmentPlan: 'Leadership training',
      scores: [],
      createdAt: 1770000000000,
      updatedAt: 1770000000000,
      version: 0,
    },
  ];

  const mockKpis: PerformanceKpi[] = [
    {
      id: 'kpi-1',
      cycleId: 'cyc-1',
      code: 'KPI-01',
      titleAr: 'مؤشر الجودة',
      titleEn: 'Quality Index',
      category: 'OPERATIONAL',
      targetValue: 100,
      weightPercentage: 20,
      createdAt: 1770000000000,
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PerformancePage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        PerformanceService,
      ],
    }).compileComponents();

    service = TestBed.inject(PerformanceService);
    vi.spyOn(service, 'listCycles').mockReturnValue(of(mockCycles));
    vi.spyOn(service, 'listAppraisals').mockReturnValue(of(mockAppraisals));
    vi.spyOn(service, 'listKpis').mockReturnValue(of(mockKpis));
    vi.spyOn(service, 'listEmployees').mockReturnValue(of([{ id: 'emp-1', fullName: 'Ahmed Ali', employeeCode: 'EMP-01' }]));
  });

  it('initializes and loads performance data', () => {
    const fixture = TestBed.createComponent(PerformancePage);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.cycles().length).toBe(1);
    expect(component.appraisals().length).toBe(1);
    expect(component.outstandingCount()).toBe(1);
  });

  it('switches tabs properly', () => {
    const fixture = TestBed.createComponent(PerformancePage);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.setTab('cycles');
    expect(component.activeTab()).toBe('cycles');

    component.setTab('kpis');
    expect(component.activeTab()).toBe('kpis');
  });
});
