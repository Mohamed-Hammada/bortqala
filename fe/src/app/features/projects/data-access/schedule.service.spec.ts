import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ScheduleService } from './schedule.service';
import { ProjectScheduleResponse, ProjectScheduleTask } from '../models/schedule.models';

describe('ScheduleService', () => {
  let service: ScheduleService;
  let httpMock: HttpTestingController;

  const mockSchedule: ProjectScheduleResponse = {
    id: 'sched-1',
    projectId: 'prj-1',
    name: 'Project Schedule',
    calendarCode: 'STANDARD_6DAY',
    startDate: 1772323200000,
    endDate: 1774915200000,
    status: 'DRAFT',
    currentBaselineVersion: 0,
    totalTasksCount: 1,
    criticalTasksCount: 1,
    overallProgress: 0,
    createdAt: 1772323200000,
    updatedAt: 1772323200000,
    version: 0,
    tasks: [
      {
        id: 't-1',
        scheduleId: 'sched-1',
        taskCode: 'TSK-01',
        name: 'Site Preparation',
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
        percentComplete: 0,
        isMilestone: false,
        constraintType: 'ASAP',
        sortOrder: 1,
        resourceAssignments: [],
      },
    ],
    dependencies: [],
    baselines: [],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ScheduleService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(ScheduleService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load project schedule and update signal', () => {
    service.loadSchedule('prj-1').subscribe((res) => {
      expect(res.id).toBe('sched-1');
      expect(service.schedule()?.totalTasksCount).toBe(1);
    });

    const req = httpMock.expectOne('/api/v1/projects/prj-1/schedule');
    expect(req.request.method).toBe('GET');
    req.flush(mockSchedule);
  });

  it('should recalculate CPM and update schedule', () => {
    service.recalculateCpm('prj-1').subscribe((res) => {
      expect(res.tasks[0].isCritical).toBe(true);
    });

    const req = httpMock.expectOne('/api/v1/projects/prj-1/schedule/recalculate-cpm');
    expect(req.request.method).toBe('POST');
    req.flush(mockSchedule);
  });

  it('should create task and reload schedule', () => {
    service
      .createTask('prj-1', {
        taskCode: 'TSK-02',
        name: 'Foundation Excavation',
        durationDays: 4,
        isMilestone: false,
        constraintType: 'ASAP',
        sortOrder: 2,
      })
      .subscribe();

    const postReq = httpMock.expectOne('/api/v1/projects/prj-1/schedule/tasks');
    expect(postReq.request.method).toBe('POST');
    postReq.flush(mockSchedule.tasks[0]);

    const getReq = httpMock.expectOne('/api/v1/projects/prj-1/schedule');
    expect(getReq.request.method).toBe('GET');
    getReq.flush(mockSchedule);
  });
});
