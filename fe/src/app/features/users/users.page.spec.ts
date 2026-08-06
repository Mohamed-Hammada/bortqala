import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { UsersPage } from './users.page';
import { AuthService } from '../../core/auth/auth.service';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { AccessCatalog, AccessRole } from './access.models';
import { UserPayload } from './users.store';

const ROLE_VIEWER: AccessRole = {
  code: 'VIEWER',
  nameKey: 'roles.access.viewer',
  descriptionKey: 'roles.access.viewer.description',
  sensitivity: 'LOW',
  kind: 'READ_ONLY',
  permissions: ['dashboard.view'],
  dependencies: [],
  sensitiveReasonKey: null,
};

const ROLE_HR_REVIEWER: AccessRole = {
  code: 'HR_REVIEWER',
  nameKey: 'roles.access.hrReviewer',
  descriptionKey: 'roles.access.hrReviewer.description',
  sensitivity: 'MEDIUM',
  kind: 'APPROVAL',
  permissions: ['reports.read', 'workers.read'],
  dependencies: [],
  sensitiveReasonKey: null,
};

const ROLE_WORKFORCE_MANAGER: AccessRole = {
  code: 'WORKFORCE_MANAGER',
  nameKey: 'roles.access.workforceManager',
  descriptionKey: 'roles.access.workforceManager.description',
  sensitivity: 'MEDIUM',
  kind: 'OPERATIONAL',
  permissions: ['workers.read', 'workers.create', 'workers.edit'],
  dependencies: [],
  sensitiveReasonKey: null,
};

const ROLE_ADMIN: AccessRole = {
  code: 'ADMIN',
  nameKey: 'roles.access.admin',
  descriptionKey: 'roles.access.admin.description',
  sensitivity: 'CRITICAL',
  kind: 'ADMINISTRATION',
  permissions: ['dashboard.view', 'reports.read', 'workers.read', 'workers.create', 'workers.edit'],
  dependencies: [],
  sensitiveReasonKey: null,
};

const CATALOG: AccessCatalog = {
  roles: [ROLE_VIEWER, ROLE_HR_REVIEWER, ROLE_WORKFORCE_MANAGER, ROLE_ADMIN],
  pages: [
    {
      code: 'DASHBOARD',
      module: 'DASHBOARD',
      route: '/dashboard',
      menuId: 'dashboard',
      titleKey: 'nav.dashboard',
      viewPermissions: ['dashboard.view'],
      roles: [],
      requiredFeature: null,
      actions: [],
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
  ],
  conflictRules: [],
  needs: [],
  sensitivePermissions: [],
};

describe('UsersPage', () => {
  let httpMock: HttpTestingController;
  let page: UsersPage;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UsersPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: {
            user: () => ({
              id: 'u1',
              username: 'admin',
              displayName: 'Admin',
              roles: ['ADMIN'],
              allowedMenus: [],
              activeFeatures: [],
              active: true,
              version: 1,
            }),
            appSettings: () =>
              of({
                minPasswordLength: 8,
                maxPasswordLength: 128,
                disallowSpaces: false,
                requireUppercase: false,
                requireLowercase: false,
                requireNumbers: false,
                requireSpecialChars: false,
              }),
            isSuperAdmin: () => true,
          },
        },
        {
          provide: I18nService,
          useValue: { t: (key: string) => key },
        },
        {
          provide: NotificationService,
          useValue: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
        },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(UsersPage);
    page = fixture.componentInstance;

    httpMock.expectOne('/api/v1/users').flush([]);
    httpMock.expectOne('/api/v1/auth/user-categories').flush([]);
    httpMock.expectOne('/api/v1/access/catalog').flush(CATALOG);
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    httpMock.verify();
  });

  it('reports a menu role mismatch when no selected role can open the page', () => {
    page.form.controls.roles.setValue(['VIEWER']);
    expect(page.menuRoleMismatch('workforce-workers')).toBe(true);
  });

  it('suppresses the mismatch banner while an admin role is selected', () => {
    page.form.controls.roles.setValue(['ADMIN']);
    expect(page.menuRoleMismatch('workforce-workers')).toBe(false);
  });

  it('lists route-guarded pages only for roles that can actually open them', () => {
    const pages = page.roleAccessiblePages('WORKFORCE_MANAGER');
    expect(pages.map((item) => item.code)).toEqual(['WORKERS']);
    expect(pages[0].level).toBe('EDIT');
  });

  it('excludes pages whose route guard rejects the role even when permissions match', () => {
    const pages = page.roleAccessiblePages('HR_REVIEWER');
    expect(pages.some((item) => item.code === 'WORKERS')).toBe(false);
  });

  it('shows every guarded page as REVIEW for an admin role', () => {
    const pages = page.roleAccessiblePages('ADMIN');
    expect(pages.map((item) => item.level)).toEqual(['REVIEW', 'REVIEW']);
  });

  it('carries the acknowledgment reason into the save payload', async () => {
    page.form.patchValue({
      username: 'worker',
      displayName: 'Worker',
      password: 'password123',
      roles: ['VIEWER'],
      allowedMenus: ['dashboard'],
    });
    page.ackReason.set('handled by admin');
    page.validationResult.set({ valid: true, warnings: [], conflicts: [], errors: [], sensitivePermissions: [] });
    const save = vi.spyOn(page.store, 'save').mockResolvedValue(true);

    await page.submit();

    expect(save).toHaveBeenCalledTimes(1);
    const payload = save.mock.calls[0][1] as UserPayload;
    expect(payload.accessChangeReason).toBe('handled by admin');
    expect(payload.password).toBe('password123');
  });

  it('omits the acknowledgment reason when it is blank', async () => {
    page.form.patchValue({
      username: 'worker',
      displayName: 'Worker',
      password: 'password123',
      roles: ['VIEWER'],
      allowedMenus: ['dashboard'],
    });
    page.validationResult.set({ valid: true, warnings: [], conflicts: [], errors: [], sensitivePermissions: [] });
    const save = vi.spyOn(page.store, 'save').mockResolvedValue(true);

    await page.submit();

    const payload = save.mock.calls[0][1] as UserPayload;
    expect(payload.accessChangeReason).toBeUndefined();
  });
});
