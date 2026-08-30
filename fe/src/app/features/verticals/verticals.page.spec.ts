import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { VerticalsPage } from './verticals.page';
import { VerticalsSummary } from './verticals.models';

describe('VerticalsPage', () => {
  let component: VerticalsPage;
  let fixture: ComponentFixture<VerticalsPage>;
  let httpTesting: HttpTestingController;

  const mockSummary: VerticalsSummary = {
    totalActiveStudents: 50,
    totalTuitionBilled: 1250000,
    totalActiveBookings: 12,
    totalTourismRevenue: 420000,
    averageTourismMarginPct: 28.5,
    totalOpenCustomsFiles: 8,
    totalDutyDisbursements: 680000,
    totalActive3plContracts: 6,
    total3plPalletCapacity: 1800,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VerticalsPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(VerticalsPage);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('loads vertical summaries, students, bookings, customs, and contracts on init', () => {
    fixture.detectChanges();

    const sumReq = httpTesting.expectOne('/api/v1/verticals/summary');
    expect(sumReq.request.method).toBe('GET');
    sumReq.flush(mockSummary);

    const stuReq = httpTesting.expectOne('/api/v1/verticals/school/students');
    expect(stuReq.request.method).toBe('GET');
    stuReq.flush([]);

    const tourReq = httpTesting.expectOne('/api/v1/verticals/tourism/bookings');
    expect(tourReq.request.method).toBe('GET');
    tourReq.flush([]);

    const custReq = httpTesting.expectOne('/api/v1/verticals/customs/declarations');
    expect(custReq.request.method).toBe('GET');
    custReq.flush([]);

    const threePlReq = httpTesting.expectOne('/api/v1/verticals/3pl/contracts');
    expect(threePlReq.request.method).toBe('GET');
    threePlReq.flush([]);

    expect(component.summary()).toEqual(mockSummary);
    expect(component.activeTab()).toBe('school');
  });

  it('switches vertical tabs', () => {
    fixture.detectChanges();

    httpTesting.expectOne('/api/v1/verticals/summary').flush(mockSummary);
    httpTesting.expectOne('/api/v1/verticals/school/students').flush([]);
    httpTesting.expectOne('/api/v1/verticals/tourism/bookings').flush([]);
    httpTesting.expectOne('/api/v1/verticals/customs/declarations').flush([]);
    httpTesting.expectOne('/api/v1/verticals/3pl/contracts').flush([]);

    component.setTab('tourism');
    expect(component.activeTab()).toBe('tourism');

    component.setTab('customs');
    expect(component.activeTab()).toBe('customs');

    component.setTab('3pl');
    expect(component.activeTab()).toBe('3pl');
  });
});
