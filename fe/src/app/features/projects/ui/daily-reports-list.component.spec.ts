import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { ConfirmDialogService } from '../../../core/confirm-dialog.service';
import { DprService } from '../data-access/dpr.service';
import { DailyReportsListComponent } from './daily-reports-list.component';
import { DailyReportResponse } from '../models/dpr.models';

describe('DailyReportsListComponent', () => {
  let component: DailyReportsListComponent;
  let fixture: ComponentFixture<DailyReportsListComponent>;
  let dprService: DprService;

  const mockReports: DailyReportResponse[] = [
    {
      id: 'dpr-1',
      projectId: 'p-1',
      reportNumber: 'DPR-PRJ-001-2026-03-01-DAY',
      reportDate: 1772323200000,
      shift: 'DAY',
      weatherCondition: 'SUNNY',
      temperatureCelsius: 28,
      status: 'APPROVED',
      siteEngineerUserId: 'u-1',
      totalWorkforceCount: 15,
      totalEquipmentCount: 4,
      totalManHours: 120,
      createdAt: 1772323200000,
      updatedAt: 1772323200000,
      version: 1,
      progressLines: [],
      laborSnapshots: [],
      equipmentLogs: [],
      materialConsumptions: [],
    },
    {
      id: 'dpr-2',
      projectId: 'p-1',
      reportNumber: 'DPR-PRJ-001-2026-03-02-DAY',
      reportDate: 1772409600000,
      shift: 'DAY',
      weatherCondition: 'CLEAR',
      temperatureCelsius: 29,
      status: 'DRAFT',
      siteEngineerUserId: 'u-1',
      totalWorkforceCount: 18,
      totalEquipmentCount: 5,
      totalManHours: 144,
      createdAt: 1772409600000,
      updatedAt: 1772409600000,
      version: 0,
      progressLines: [],
      laborSnapshots: [],
      equipmentLogs: [],
      materialConsumptions: [],
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, DailyReportsListComponent],
      providers: [
        {
          provide: I18nService,
          useValue: {
            t: (key: string) => key,
            currentLocale: () => 'ar-EG',
            isRtl: () => true,
          },
        },
        {
          provide: NotificationService,
          useValue: {
            success: vi.fn(),
            error: vi.fn(),
            info: vi.fn(),
          },
        },
        {
          provide: ConfirmDialogService,
          useValue: {
            confirmOptions: vi.fn().mockResolvedValue(true),
          },
        },
      ],
    }).compileComponents();

    dprService = TestBed.inject(DprService);
    dprService.reports.set(mockReports);
    vi.spyOn(dprService, 'loadReports').mockImplementation(() => {
      dprService.reports.set(mockReports);
      return of(mockReports);
    });

    fixture = TestBed.createComponent(DailyReportsListComponent);
    component = fixture.componentInstance;
    component.projectId = 'p-1';
    component.availableWbsNodes = [];
    fixture.detectChanges();
  });

  it('renders and computes KPI metrics', () => {
    expect(component).toBeTruthy();
    expect(component.totalReports()).toBe(2);
    expect(component.approvedReports()).toBe(1);
    expect(component.draftReports()).toBe(1);
  });

  it('filters reports by status and search query', () => {
    expect(component.filteredReports()).toHaveLength(2);

    component.statusFilter.set('APPROVED');
    expect(component.filteredReports()).toHaveLength(1);
    expect(component.filteredReports()[0].id).toBe('dpr-1');

    component.statusFilter.set('ALL');
    component.searchQuery.set('2026-03-02');
    expect(component.filteredReports()).toHaveLength(1);
    expect(component.filteredReports()[0].id).toBe('dpr-2');
  });

  it('opens and closes the daily report editor', () => {
    expect(component.editorOpen()).toBe(false);

    component.openCreate();
    expect(component.editorOpen()).toBe(true);
    expect(component.selectedReport()).toBeNull();

    component.onEditorClosed(false);
    expect(component.editorOpen()).toBe(false);
  });
});
