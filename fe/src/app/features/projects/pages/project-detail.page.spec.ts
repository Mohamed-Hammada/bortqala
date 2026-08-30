import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { ProjectDetailPage } from './project-detail.page';
import { ProjectService } from '../data-access/project.service';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { ConfirmDialogService } from '../../../core/confirm-dialog.service';
import { ProjectResponse, WbsNodeResponse } from '../models/project.models';

describe('ProjectDetailPage', () => {
  let page: ProjectDetailPage;
  let httpMock: HttpTestingController;

  const mockProject: ProjectResponse = {
    id: 'prj-100',
    code: 'PRJ-100',
    name: 'مشروع كوبري تحيا مصر',
    nameEn: 'Tahya Misr Bridge Project',
    siteAddress: 'شبرا - المظلات',
    contractValue: 120000000,
    currencyCode: 'EGP',
    status: 'ACTIVE',
    budgetBlocking: true,
    active: true,
    createdAt: 1700000000000,
    updatedAt: 1700000000000,
    version: 1,
    totalPlannedAmount: 110000000,
    wbsCount: 5,
  };

  const mockWbsNode: WbsNodeResponse = {
    id: 'wbs-1',
    projectId: 'prj-100',
    parentId: null,
    wbsCode: '1',
    wbsPath: '/1',
    name: 'أعمال الخوازيق والأساسات',
    nameEn: 'Piling & Foundation',
    nodeType: 'PHASE',
    level: 1,
    sortOrder: 1,
    plannedQuantity: 0,
    unitRate: 0,
    plannedAmount: 30000000,
    status: 'IN_PROGRESS',
    createdAt: 1700000000000,
    updatedAt: 1700000000000,
    version: 1,
    children: [],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectDetailPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        ProjectService,
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => (key === 'id' ? 'prj-100' : null),
              },
            },
          },
        },
        {
          provide: I18nService,
          useValue: { t: (key: string) => key },
        },
        {
          provide: NotificationService,
          useValue: { success: () => undefined, error: () => undefined, warning: () => undefined },
        },
        ConfirmDialogService,
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(ProjectDetailPage);
    page = fixture.componentInstance;
    fixture.detectChanges();

    // Flush initial project data calls
    httpMock.expectOne((r) => r.url === '/api/v1/projects/prj-100').flush(mockProject);
    httpMock.expectOne((r) => r.url === '/api/v1/projects/prj-100/wbs').flush([mockWbsNode]);
    httpMock.expectOne((r) => r.url === '/api/v1/projects/prj-100/wbs/flat').flush([mockWbsNode]);
    httpMock.expectOne((r) => r.url === '/api/v1/projects/cost-codes').flush([]);
    httpMock.expectOne((r) => r.url === '/api/v1/projects/prj-100/roles').flush([]);
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    httpMock.verify();
  });

  it('should initialize and load project details and WBS tree', () => {
    expect(page).toBeTruthy();
    expect(page.projectId()).toBe('prj-100');
    expect(page.projectService.currentProject()?.code).toBe('PRJ-100');
    expect(page.projectService.wbsTree().length).toBe(1);
    expect(page.loading()).toBe(false);
  });

  it('should switch tabs properly', () => {
    expect(page.activeTab()).toBe('wbs');

    page.activeTab.set('costControl');
    expect(page.activeTab()).toBe('costControl');

    page.activeTab.set('schedule');
    expect(page.activeTab()).toBe('schedule');

    page.activeTab.set('roles');
    expect(page.activeTab()).toBe('roles');
  });

  it('should open and populate the project edit modal', () => {
    page.openEditProject();
    expect(page.editProjectOpen()).toBe(true);
    expect(page.projectForm.controls.name.value).toBe('مشروع كوبري تحيا مصر');
    expect(page.projectForm.controls.nameEn.value).toBe('Tahya Misr Bridge Project');
    expect(page.projectForm.controls.currencyCode.value).toBe('EGP');
  });

  it('should open WBS modal for creating child node', () => {
    page.openAddChildWbs(mockWbsNode);
    expect(page.wbsModalOpen()).toBe(true);
    expect(page.isEditingWbs()).toBe(false);
    expect(page.parentWbsNode()?.id).toBe('wbs-1');
  });

  it('should open reposition modal for a WBS node', () => {
    page.openRepositionWbs(mockWbsNode);
    expect(page.repositionModalOpen()).toBe(true);
    expect(page.repositionNodeTarget()?.id).toBe('wbs-1');
  });
});
