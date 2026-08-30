import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { ScheduleService } from '../data-access/schedule.service';
import { ProjectScheduleGanttComponent } from './project-schedule-gantt.component';
import { ProjectScheduleResponse } from '../models/schedule.models';

describe('ProjectScheduleGanttComponent', () => {
  let component: ProjectScheduleGanttComponent;
  let fixture: ComponentFixture<ProjectScheduleGanttComponent>;
  let scheduleService: ScheduleService;

  const mockSchedule: ProjectScheduleResponse = {
    id: 'sched-1',
    projectId: 'prj-1',
    name: 'Project Schedule',
    calendarCode: 'STANDARD_6DAY',
    startDate: 1772323200000,
    endDate: 1774915200000,
    status: 'DRAFT',
    currentBaselineVersion: 1,
    totalTasksCount: 2,
    criticalTasksCount: 1,
    overallProgress: 50,
    createdAt: 1772323200000,
    updatedAt: 1772323200000,
    version: 1,
    tasks: [
      {
        id: 't-1',
        scheduleId: 'sched-1',
        taskCode: 'TSK-01',
        name: 'Site Prep',
        durationDays: 5,
        plannedStartDate: 1772323200000,
        plannedEndDate: 1772755200000,
        earlyStartDate: 1772323200000,
        earlyEndDate: 1772755200000,
        lateStartDate: 1772323200000,
        lateEndDate: 1772755200000,
        freeFloatDays: 0,
        totalFloatDays: 0,
        isCritical: true,
        percentComplete: 100,
        isMilestone: false,
        constraintType: 'ASAP',
        sortOrder: 1,
        resourceAssignments: [],
      },
      {
        id: 't-2',
        scheduleId: 'sched-1',
        taskCode: 'TSK-02',
        name: 'Excavation',
        durationDays: 4,
        plannedStartDate: 1772841600000,
        plannedEndDate: 1773187200000,
        earlyStartDate: 1772841600000,
        earlyEndDate: 1773187200000,
        lateStartDate: 1772928000000,
        lateEndDate: 1773273600000,
        freeFloatDays: 1,
        totalFloatDays: 1,
        isCritical: false,
        percentComplete: 0,
        isMilestone: false,
        constraintType: 'ASAP',
        sortOrder: 2,
        resourceAssignments: [],
      },
    ],
    dependencies: [],
    baselines: [],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, ProjectScheduleGanttComponent],
      providers: [
        {
          provide: I18nService,
          useValue: {
            t: (k: string) => k,
            currentLocale: () => 'ar-EG',
          },
        },
        {
          provide: NotificationService,
          useValue: {
            success: vi.fn(),
            error: vi.fn(),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectScheduleGanttComponent);
    component = fixture.componentInstance;
    component.projectId = 'prj-1';
    scheduleService = TestBed.inject(ScheduleService);

    vi.spyOn(scheduleService, 'loadSchedule').mockReturnValue(of(mockSchedule));
    vi.spyOn(scheduleService, 'loadOverAllocations').mockReturnValue(of([]));

    scheduleService.schedule.set(mockSchedule);
    fixture.detectChanges();
  });

  it('should create and render schedule summary', () => {
    expect(component).toBeTruthy();
    expect(component.schedule()?.totalTasksCount).toBe(2);
  });

  it('should filter critical tasks when toggle is active', () => {
    component.filterCriticalOnly.set(true);
    expect(component.filteredTasks().length).toBe(1);
    expect(component.filteredTasks()[0].taskCode).toBe('TSK-01');
  });

  it('should switch tabs between gantt, table, baselines, and resources', () => {
    component.activeTab.set('table');
    expect(component.activeTab()).toBe('table');

    component.activeTab.set('baselines');
    expect(component.activeTab()).toBe('baselines');

    component.activeTab.set('resources');
    expect(component.activeTab()).toBe('resources');
  });
});
