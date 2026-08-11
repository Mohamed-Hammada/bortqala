import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AccessService } from './access.service';
import { AccessCatalog, AccessRole } from './access.models';

const ROLE_VIEWER: AccessRole = {
  code: 'VIEWER',
  nameKey: 'roles.access.viewer',
  descriptionKey: 'roles.access.viewer.description',
  sensitivity: 'LOW',
  kind: 'READ_ONLY',
  permissions: ['dashboard.view', 'reports.read'],
  dependencies: [],
  sensitiveReasonKey: null,
};

const ROLE_HR_REVIEWER: AccessRole = {
  code: 'HR_REVIEWER',
  nameKey: 'roles.access.hrReviewer',
  descriptionKey: 'roles.access.hrReviewer.description',
  sensitivity: 'MEDIUM',
  kind: 'APPROVAL',
  permissions: ['reports.read', 'reports.decide', 'workers.read', 'workers.edit'],
  dependencies: [],
  sensitiveReasonKey: null,
};

const ROLE_WORKFORCE_MANAGER: AccessRole = {
  code: 'WORKFORCE_MANAGER',
  nameKey: 'roles.access.workforceManager',
  descriptionKey: 'roles.access.workforceManager.description',
  sensitivity: 'MEDIUM',
  kind: 'OPERATIONAL',
  permissions: ['workers.read', 'workers.create', 'workers.edit', 'attendance.read'],
  dependencies: [],
  sensitiveReasonKey: null,
};

const ROLE_WORKFORCE_FINANCE: AccessRole = {
  code: 'WORKFORCE_FINANCE',
  nameKey: 'roles.access.workforceFinance',
  descriptionKey: 'roles.access.workforceFinance.description',
  sensitivity: 'MEDIUM',
  kind: 'FINANCE',
  permissions: ['workers.read', 'workers.create', 'workers.edit', 'attendance.read', 'reports.read'],
  dependencies: [],
  sensitiveReasonKey: null,
};

const ROLE_FINANCE_MANAGER: AccessRole = {
  code: 'FINANCE_MANAGER',
  nameKey: 'roles.access.financeManager',
  descriptionKey: 'roles.access.financeManager.description',
  sensitivity: 'HIGH',
  kind: 'FINANCE',
  permissions: ['journal.read', 'journal.create', 'journal.post'],
  dependencies: [],
  sensitiveReasonKey: 'access.sensitive.financeManager',
};

const CATALOG: AccessCatalog = {
  roles: [ROLE_VIEWER, ROLE_HR_REVIEWER, ROLE_WORKFORCE_MANAGER, ROLE_WORKFORCE_FINANCE, ROLE_FINANCE_MANAGER],
  pages: [
    {
      code: 'DASHBOARD',
      module: 'DASHBOARD',
      route: '/dashboard',
      menuId: 'dashboard',
      titleKey: 'nav.dashboard',
      viewPermissions: ['dashboard.view'],
      roles: ['ADMIN', 'SUPER_ADMIN', 'VIEWER'],
      requiredFeature: null,
      actions: [],
    },
    {
      code: 'REPORTS',
      module: 'HR',
      route: '/reports',
      menuId: 'reports',
      titleKey: 'nav.reports',
      viewPermissions: ['reports.read'],
      roles: ['ADMIN', 'SUPER_ADMIN', 'HR_MANAGER', 'HR_REVIEWER', 'VIEWER'],
      requiredFeature: null,
      actions: [{ code: 'DECIDE', permission: 'reports.decide', sensitive: false }],
    },
    {
      code: 'WORKERS',
      module: 'WORKFORCE',
      route: '/workforce/workers',
      menuId: 'workforce-workers',
      titleKey: 'workforce.workers.title',
      viewPermissions: ['workers.read'],
      roles: ['ADMIN', 'SUPER_ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE'],
      requiredFeature: null,
      actions: [
        { code: 'CREATE', permission: 'workers.create', sensitive: false },
        { code: 'EDIT', permission: 'workers.edit', sensitive: false },
      ],
    },
    {
      code: 'JOURNAL',
      module: 'FINANCE',
      route: '/finance/journal-entries',
      menuId: 'journal-entries',
      titleKey: 'nav.journalEntries',
      viewPermissions: ['journal.read'],
      roles: ['ADMIN', 'SUPER_ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'AUDITOR'],
      requiredFeature: 'finance.enabled',
      actions: [
        { code: 'CREATE', permission: 'journal.create', sensitive: false },
        { code: 'POST', permission: 'journal.post', sensitive: true },
      ],
    },
  ],
  conflictRules: [
    { code: 'JOURNAL_CREATE_AND_POST', permissions: ['journal.create', 'journal.post'], severity: 'WARNING', reasonKey: 'access.conflicts.journalCreateAndPost' },
  ],
  needs: [
    { code: 'VIEW_DASHBOARD', labelKey: 'access.pages.needs.view-dashboard', permissions: ['dashboard.view'] },
    { code: 'MANAGE_WORKERS', labelKey: 'access.pages.needs.manage-workers', permissions: ['workers.read', 'workers.create', 'workers.edit'] },
  ],
  sensitivePermissions: ['journal.post'],
};

describe('AccessService', () => {
  let service: AccessService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem(
      'bemo-erp-session',
      JSON.stringify({
        expiresAt: Date.now() + 3_600_000,
        mustChangePassword: false,
        app: { id: 'test-app', name: 'Test', code: 'test' },
        user: {
          id: 'u1',
          username: 'tester',
          roles: ['FINANCE_MANAGER'],
          allowedMenus: [],
          activeFeatures: ['finance.enabled'],
        },
        preferences: {},
      }),
    );
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AccessService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('loads the catalog once and reuses it', async () => {
    const promise = service.loadCatalog();
    const req = http.expectOne('/api/v1/access/catalog');
    expect(req.request.method).toBe('GET');
    req.flush(CATALOG);
    await promise;

    expect(service.catalog()).toEqual(CATALOG);
    expect(service.roles().length).toBe(5);
    expect(service.pages().length).toBe(4);

    const again = service.loadCatalog();
    http.expectNone('/api/v1/access/catalog');
    await expect(again).resolves.toEqual(CATALOG);
  });

  it('surfaces a catalog error instead of throwing', async () => {
    service.catalog.set(null);
    const promise = service.loadCatalog();
    const req = http.expectOne('/api/v1/access/catalog');
    req.flush('boom', { status: 500, statusText: 'Internal Server Error' });
    const result = await promise;

    expect(result).toBeNull();
    expect(service.error()).toBe('access.catalogError');
  });

  it('preview hides pages whose menu is not granted', () => {
    service.catalog.set(CATALOG);
    const preview = service.preview(['VIEWER'], ['dashboard', 'reports']);

    const dashboard = preview.pages.find((p) => p.pageCode === 'DASHBOARD')!;
    expect(dashboard.access).toBe('VIEW');
    expect(dashboard.grantedByRoles).toEqual(['VIEWER']);

    const workers = preview.pages.find((p) => p.pageCode === 'WORKERS')!;
    expect(workers.access).toBe('HIDDEN');

    const reports = preview.pages.find((p) => p.pageCode === 'REPORTS')!;
    expect(reports.access).toBe('VIEW');
  });

  it('preview marks restricted when the role is missing for a visible menu', () => {
    service.catalog.set(CATALOG);
    const preview = service.preview(['HR_REVIEWER'], ['dashboard']);

    const dashboard = preview.pages.find((p) => p.pageCode === 'DASHBOARD')!;
    expect(dashboard.access).toBe('RESTRICTED');
    expect(dashboard.missingPermissions).toContain('dashboard.view');
  });

  it('preview marks MODULE_UNAVAILABLE when the tenant lacks the required feature', () => {
    localStorage.clear();
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const freshService = TestBed.inject(AccessService);
    freshService.catalog.set(CATALOG);

    const preview = freshService.preview(['FINANCE_MANAGER'], ['journal-entries']);
    const journal = preview.pages.find((p) => p.pageCode === 'JOURNAL')!;
    expect(journal.access).toBe('MODULE_UNAVAILABLE');
  });

  it('preview derives action levels and reports conflicts and warnings', () => {
    service.catalog.set(CATALOG);
    const preview = service.preview(['FINANCE_MANAGER'], ['journal-entries']);

    const journal = preview.pages.find((p) => p.pageCode === 'JOURNAL')!;
    expect(journal.access).toBe('POST');
    expect(journal.grantedActions).toEqual(['CREATE', 'POST']);

    expect(preview.conflicts.map((c) => c.code)).toEqual(['JOURNAL_CREATE_AND_POST']);
    expect(preview.conflicts[0].roles).toEqual(['FINANCE_MANAGER']);
    expect(preview.warnings.map((w) => w.code)).toEqual(['journal.post']);
    expect(preview.warnings[0].messageKey).toBe('access.warnings.journal-post');
    expect(preview.sensitivePermissions).toEqual(['journal.post']);
  });

  it('suggestRoles picks the minimal covering role and excludes administration roles', () => {
    service.catalog.set(CATALOG);
    expect(service.suggestRoles(['workers.read', 'workers.create', 'workers.edit'])).toEqual(['WORKFORCE_MANAGER']);
    expect(service.suggestRoles([])).toEqual([]);
  });

  it('broaderRoles returns roles strictly containing the suggestion', () => {
    service.catalog.set(CATALOG);
    expect(service.broaderRoles(['WORKFORCE_MANAGER'])).toEqual(['WORKFORCE_FINANCE']);
    expect(service.broaderRoles([])).toEqual([]);
  });

  it('rolesGranting resolves roles for a permission', () => {
    service.catalog.set(CATALOG);
    expect(service.rolesGranting('workers.read')).toEqual(['HR_REVIEWER', 'WORKFORCE_FINANCE', 'WORKFORCE_MANAGER']);
    expect(service.rolesGranting('missing.permission')).toEqual([]);
  });

  it('previewRemote posts to the preview endpoint', async () => {
    const promise = service.previewRemote(['VIEWER'], ['dashboard']);
    const req = http.expectOne('/api/v1/access/preview');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ roleCodes: ['VIEWER'], menuCodes: ['dashboard'] });
    req.flush({ pages: [], warnings: [], conflicts: [], sensitivePermissions: [] });
    await expect(promise).resolves.toEqual({ pages: [], warnings: [], conflicts: [], sensitivePermissions: [] });
  });

  it('validate posts target and reason to the authoritative endpoint', async () => {
    const promise = service.validate(['VIEWER'], ['dashboard'], 'user-9', 'acknowledged');
    const req = http.expectOne('/api/v1/users/access/validate');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ roleCodes: ['VIEWER'], menuCodes: ['dashboard'], targetUserId: 'user-9', reason: 'acknowledged' });
    req.flush({ valid: true, conflicts: [], warnings: [], sensitivePermissions: [] });
    await expect(promise).resolves.toEqual({ valid: true, conflicts: [], warnings: [], sensitivePermissions: [] });
  });
});
