import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { ProjectsPage } from './projects.page';
import { ProjectService } from '../data-access/project.service';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { ConfirmDialogService } from '../../../core/confirm-dialog.service';
import { ProjectResponse, ProjectSummaryResponse } from '../models/project.models';

describe('ProjectsPage', () => {
  let page: ProjectsPage;
  let httpMock: HttpTestingController;

  const mockProject: ProjectResponse = {
    id: 'prj-1',
    code: 'PRJ-001',
    name: 'برج النخيل',
    nameEn: 'Palm Tower',
    siteAddress: 'القاهرة الجديدة',
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
    totalProjects: 1,
    activeProjects: 1,
    onHoldProjects: 0,
    completedProjects: 0,
    closedProjects: 0,
    totalContractValue: 50000000,
    totalPlannedAmount: 45000000,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectsPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        ProjectService,
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
    const fixture = TestBed.createComponent(ProjectsPage);
    page = fixture.componentInstance;
    fixture.detectChanges();

    // Flush initial load calls
    httpMock.expectOne((r) => r.url === '/api/v1/projects').flush([mockProject]);
    httpMock.expectOne((r) => r.url === '/api/v1/projects/summary').flush(mockSummary);
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    httpMock.verify();
  });

  it('should create and load project data', () => {
    expect(page).toBeTruthy();
    expect(page.projectService.projects().length).toBe(1);
    expect(page.projectService.summary()?.totalProjects).toBe(1);
  });

  it('should filter projects by search query', () => {
    page.searchQuery.set('النخيل');
    expect(page.filteredProjects().length).toBe(1);

    page.searchQuery.set('NonExistent');
    expect(page.filteredProjects().length).toBe(0);
  });
});
