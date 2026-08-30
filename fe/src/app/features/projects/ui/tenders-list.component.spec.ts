import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TendersListComponent } from './tenders-list.component';
import { TenderService } from '../data-access/tender.service';
import { of } from 'rxjs';
import { ProjectTender } from '../models/tender.models';

describe('TendersListComponent', () => {
  let component: TendersListComponent;
  let fixture: ComponentFixture<TendersListComponent>;
  let tenderService: TenderService;

  const mockTenders: ProjectTender[] = [
    {
      id: 'tnd-1',
      tenderNumber: 'TND-2026-001',
      title: 'MEP Subcontractor Competition',
      tenderType: 'INTERNAL',
      submissionDeadline: Date.now() + 86400000 * 15,
      estimatedValue: 25000000,
      currencyCode: 'EGP',
      technicalWeightPercent: 70,
      financialWeightPercent: 30,
      bidBondRequired: true,
      status: 'DRAFT',
      boqItemsCount: 5,
      biddersCount: 2,
      createdAt: Date.now(),
      updatedAt: Date.now(),
      version: 1
    }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TendersListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        TenderService
      ]
    }).compileComponents();

    tenderService = TestBed.inject(TenderService);
    vi.spyOn(tenderService, 'loadTenders').mockReturnValue(of(mockTenders));
    tenderService.tenders.set(mockTenders);

    fixture = TestBed.createComponent(TendersListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('renders tenders table and computes KPI metrics', () => {
    expect(component.kpiTotal()).toBe(1);
    expect(component.kpiInternal()).toBe(1);
    expect(component.kpiExternal()).toBe(0);
    expect(component.kpiTotalValue()).toBe(25000000);
  });

  it('filters list by search term', () => {
    component.searchTerm.set('Nonexistent');
    expect(component.filteredTenders().length).toBe(0);

    component.searchTerm.set('MEP');
    expect(component.filteredTenders().length).toBe(1);
  });
});
