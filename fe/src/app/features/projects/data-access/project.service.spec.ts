import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { ProjectService } from './project.service';
import { ProjectResponse, ProjectSummaryResponse, WbsNodeResponse } from '../models/project.models';

describe('ProjectService', () => {
  let service: ProjectService;
  let httpMock: HttpTestingController;

  const mockProject: ProjectResponse = {
    id: 'prj-1',
    code: 'PRJ-001',
    name: 'برج النخيل',
    contractValue: 50000000,
    currencyCode: 'EGP',
    status: 'ACTIVE',
    budgetBlocking: true,
    active: true,
    createdAt: 1700000000000,
    updatedAt: 1700000000000,
    version: 1,
    totalPlannedAmount: 45000000,
    wbsCount: 12,
  };

  const mockSummary: ProjectSummaryResponse = {
    totalProjects: 5,
    activeProjects: 3,
    onHoldProjects: 1,
    completedProjects: 1,
    closedProjects: 0,
    totalContractValue: 120000000,
    totalPlannedAmount: 105000000,
  };

  const mockWbsNode: WbsNodeResponse = {
    id: 'wbs-1',
    projectId: 'prj-1',
    wbsCode: '1',
    wbsPath: '/1',
    name: 'أعمال الأساسات',
    nodeType: 'PHASE',
    level: 1,
    sortOrder: 0,
    plannedQuantity: 0,
    unitRate: 0,
    plannedAmount: 0,
    status: 'PLANNED',
    createdAt: 1700000000000,
    updatedAt: 1700000000000,
    version: 1,
    children: [],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ProjectService,
      ],
    });

    service = TestBed.inject(ProjectService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('loadProjects should fetch projects and update signal', () => {
    service.loadProjects().subscribe((data) => {
      expect(data.length).toBe(1);
      expect(data[0].code).toBe('PRJ-001');
    });

    const req = httpMock.expectOne((r) => r.url === '/api/v1/projects');
    expect(req.request.method).toBe('GET');
    req.flush([mockProject]);

    expect(service.projects().length).toBe(1);
  });

  it('loadSummary should fetch summary and update signal', () => {
    service.loadSummary().subscribe((summary) => {
      expect(summary.totalProjects).toBe(5);
      expect(summary.activeProjects).toBe(3);
    });

    const req = httpMock.expectOne((r) => r.url === '/api/v1/projects/summary');
    expect(req.request.method).toBe('GET');
    req.flush(mockSummary);

    expect(service.summary()?.totalProjects).toBe(5);
  });

  it('loadWbsTree should fetch hierarchical tree and update signal', () => {
    service.loadWbsTree('prj-1').subscribe((tree) => {
      expect(tree.length).toBe(1);
      expect(tree[0].wbsCode).toBe('1');
    });

    const req = httpMock.expectOne((r) => r.url === '/api/v1/projects/prj-1/wbs');
    expect(req.request.method).toBe('GET');
    req.flush([mockWbsNode]);

    expect(service.wbsTree().length).toBe(1);
  });
});
