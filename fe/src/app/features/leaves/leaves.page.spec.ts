import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { LeavesPage } from './leaves.page';
import { LeavesService } from './leaves.service';

describe('LeavesPage', () => {
  let leavesService: LeavesService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LeavesPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        LeavesService,
      ],
    }).compileComponents();

    leavesService = TestBed.inject(LeavesService);
  });

  it('initializes and loads requests, balances and types', async () => {
    vi.spyOn(leavesService, 'listRequests').mockResolvedValue([
      {
        id: 'req-1',
        requestNumber: 'LR-2026-001',
        employeeId: 'emp-1',
        employeeName: 'Mohamed Ahmed',
        leaveTypeId: 'type-1',
        leaveTypeCode: 'ANNUAL',
        leaveTypeName: 'Annual Leave',
        startDate: '2026-08-01',
        endDate: '2026-08-05',
        totalDays: 5,
        status: 'PENDING_APPROVAL',
        createdAt: 1000,
        updatedAt: 1000,
        version: 1,
      },
    ]);

    vi.spyOn(leavesService, 'listBalances').mockResolvedValue([
      {
        id: 'bal-1',
        employeeId: 'emp-1',
        employeeName: 'Mohamed Ahmed',
        leaveTypeId: 'type-1',
        leaveTypeCode: 'ANNUAL',
        leaveTypeName: 'Annual Leave',
        year: 2026,
        entitledDays: 21,
        carriedOverDays: 0,
        usedDays: 0,
        pendingDays: 5,
        remainingDays: 16,
      },
    ]);

    vi.spyOn(leavesService, 'listTypes').mockResolvedValue([
      {
        id: 'type-1',
        code: 'ANNUAL',
        nameAr: 'إجازة اعتيادية',
        nameEn: 'Annual Leave',
        paid: true,
        requiresAttachment: false,
        maxConsecutiveDays: 30,
        createdAt: 1000,
      },
    ]);

    vi.spyOn(leavesService, 'listEmployees').mockResolvedValue([
      {
        id: 'emp-1',
        fullName: 'Mohamed Ahmed',
        employeeCode: 'EMP-001',
      },
    ]);

    const fixture = TestBed.createComponent(LeavesPage);
    const component = fixture.componentInstance;
    await component.init();

    expect(component.requests().length).toBe(1);
    expect(component.balances().length).toBe(1);
    expect(component.types().length).toBe(1);
    expect(component.activeTab()).toBe('REQUESTS');
  });

  it('switches tabs correctly', () => {
    const fixture = TestBed.createComponent(LeavesPage);
    const component = fixture.componentInstance;

    component.setTab('BALANCES');
    expect(component.activeTab()).toBe('BALANCES');

    component.setTab('TYPES');
    expect(component.activeTab()).toBe('TYPES');
  });
});
