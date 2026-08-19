import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AttendanceBrowserPage } from './attendance-browser.page';
import { AttendanceApiService } from './attendance-api.service';

describe('AttendanceBrowserPage', () => {
  let apiService: AttendanceApiService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AttendanceBrowserPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();

    apiService = TestBed.inject(AttendanceApiService);
  });

  it('loads months and selects the latest month on init', async () => {
    const punchTime1 = new Date('2026-08-15T08:00:00').getTime();
    const punchTime2 = new Date('2026-08-15T17:00:00').getTime();

    vi.spyOn(apiService, 'months').mockResolvedValue([
      {
        month: '2026-08',
        punchCount: 120,
        employeeCount: 15,
        mappedEmployeeCount: 14,
        unmatchedEmployeeCount: 1,
        firstPunch: punchTime1,
        lastPunch: punchTime2,
      },
      {
        month: '2026-07',
        punchCount: 200,
        employeeCount: 15,
        mappedEmployeeCount: 15,
        unmatchedEmployeeCount: 0,
        firstPunch: new Date('2026-07-01T08:00:00').getTime(),
        lastPunch: new Date('2026-07-31T17:00:00').getTime(),
      },
    ]);
    vi.spyOn(apiService, 'employees').mockResolvedValue([
      {
        deviceUserId: 'DEV-001',
        employeeId: 'emp-1',
        employeeCode: 'EMP-001',
        employeeName: 'Mohamed Ahmed',
        observedName: 'Mohamed Ahmed',
        punchCount: 20,
        firstPunch: punchTime1,
        lastPunch: punchTime2,
        mapped: true,
      },
    ]);

    const fixture = TestBed.createComponent(AttendanceBrowserPage);
    const component = fixture.componentInstance;
    await component.load();

    expect(component.months().length).toBe(2);
    expect(component.selectedMonth()).toBe('2026-08');
    expect(component.employees().length).toBe(1);
    expect(component.filteredEmployees().length).toBe(1);
    expect(component.displayName(component.employees()[0])).toBe('Mohamed Ahmed');
  });

  it('filters employees by search query', async () => {
    const punchTime1 = new Date('2026-08-10T08:00:00').getTime();
    const punchTime2 = new Date('2026-08-10T17:00:00').getTime();

    vi.spyOn(apiService, 'months').mockResolvedValue([
      {
        month: '2026-08',
        punchCount: 10,
        employeeCount: 2,
        mappedEmployeeCount: 2,
        unmatchedEmployeeCount: 0,
        firstPunch: punchTime1,
        lastPunch: punchTime2,
      },
    ]);
    vi.spyOn(apiService, 'employees').mockResolvedValue([
      {
        deviceUserId: 'DEV-001',
        employeeId: 'emp-1',
        employeeCode: 'EMP-001',
        employeeName: 'Hassan Mahmoud',
        observedName: 'Hassan',
        punchCount: 10,
        firstPunch: punchTime1,
        lastPunch: punchTime2,
        mapped: true,
      },
      {
        deviceUserId: 'DEV-002',
        employeeId: 'emp-2',
        employeeCode: 'EMP-002',
        employeeName: 'Tarek Zaki',
        observedName: 'Tarek',
        punchCount: 10,
        firstPunch: punchTime1,
        lastPunch: punchTime2,
        mapped: true,
      },
    ]);

    const fixture = TestBed.createComponent(AttendanceBrowserPage);
    const component = fixture.componentInstance;
    await component.load();

    component.search.set('Tarek');
    expect(component.filteredEmployees().length).toBe(1);
    expect(component.filteredEmployees()[0].employeeName).toBe('Tarek Zaki');
  });
});
