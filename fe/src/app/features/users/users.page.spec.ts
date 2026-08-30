import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { provideRouter } from '@angular/router';
import { UsersPage, USER_MENU_OPTIONS } from './users.page';
import { AuthService } from '../../core/auth/auth.service';
import { AuthUser } from '../../core/auth/auth.models';
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

  /** WP-10: openNew() lazily fetches server menu options + job templates. */
  async function flushDialogOptions(): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve, 0));
    httpMock.expectOne('/api/v1/users/menu-options').flush([]);
    httpMock.expectOne('/api/v1/users/role-templates').flush([]);
    await new Promise((resolve) => setTimeout(resolve, 0));
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UsersPage],
      providers: [
        provideRouter([]),
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
    httpMock.expectOne('/api/v1/access/policy-groups').flush([]);
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    httpMock.verify();
  });

  it('filters the user list by display name, username, and role without changing backend data', () => {
    page.store.items.set([
      {
        id: 'u-search-1',
        username: 'ali.account',
        displayName: 'Ali Hassan',
        roles: ['VIEWER'],
        allowedMenus: ['dashboard'],
        active: true,
        version: 1,
      },
      {
        id: 'u-search-2',
        username: 'finance.user',
        displayName: 'Mona',
        roles: ['ACCOUNTANT'],
        allowedMenus: ['accounts'],
        active: true,
        version: 1,
      },
    ] as AuthUser[]);

    page.userSearch.set('ali');
    expect(page.filteredUsers().map((user) => user.id)).toEqual(['u-search-1']);

    page.userSearch.set('accountant');
    expect(page.filteredUsers().map((user) => user.id)).toEqual(['u-search-2']);

    page.userSearch.set('');
    expect(page.filteredUsers()).toHaveLength(2);
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

  it('uses route-accessible pages when calculating role summary counts', () => {
    expect(page.roleMeta(ROLE_HR_REVIEWER)).toEqual({ pages: 0, actions: 0 });
    expect(page.roleMeta(ROLE_WORKFORCE_MANAGER)).toEqual({ pages: 1, actions: 2 });
  });

  it('returns the route roles declared by the page instead of inferring them from permissions', () => {
    const workersPage = CATALOG.pages.find((item) => item.code === 'WORKERS');
    expect(workersPage).toBeDefined();
    expect(page.requiredRolesForPage(workersPage!.viewPermissions)).toEqual(
      [...workersPage!.roles].sort(),
    );
  });

  it('shows every guarded page as REVIEW for an admin role', () => {
    const pages = page.roleAccessiblePages('ADMIN');
    expect(pages.map((item) => item.level)).toEqual(['REVIEW', 'REVIEW']);
  });

  it('counts all menus for ADMIN because runtime admin access bypasses menu selection', () => {
    const user = {
      roles: ['ADMIN'],
      allowedMenus: ['dashboard'],
    } as unknown as AuthUser;

    expect(page.allowedMenuCount(user)).toBe(page.menuOptions().length);
  });

  it('derives menu access from selected roles for a new user until menus are customized', async () => {
    page.openNew();
    await flushDialogOptions();
    page.form.controls.roles.setValue([]);
    page.form.controls.allowedMenus.setValue([]);

    page.toggleRole(
      'WORKFORCE_MANAGER',
      { target: { checked: true } } as unknown as Event,
    );

    expect(page.form.controls.roles.value).toEqual(['WORKFORCE_MANAGER']);
    expect(page.form.controls.allowedMenus.value).toEqual(['workforce-workers']);
  });

  it('preserves manual menu overrides when roles change', async () => {
    page.openNew();
    await flushDialogOptions();
    page.toggleMenu('reports', { target: { checked: true } } as unknown as Event);

    page.toggleRole(
      'WORKFORCE_MANAGER',
      { target: { checked: true } } as unknown as Event,
    );

    expect(page.customMenuAccess()).toBe(true);
    expect(page.form.controls.allowedMenus.value).toContain('reports');
    expect(page.form.controls.allowedMenus.value).not.toContain('workforce-workers');
  });

  it('keeps every selected privilege when promoting an additional role', () => {
    page.form.controls.roles.setValue(['VIEWER', 'WORKFORCE_MANAGER', 'HR_REVIEWER']);

    page.setPrimaryRole(
      { target: { value: 'WORKFORCE_MANAGER' } } as unknown as Event,
    );

    expect(page.form.controls.roles.value).toEqual([
      'WORKFORCE_MANAGER',
      'VIEWER',
      'HR_REVIEWER',
    ]);
  });

  it('does not duplicate a role when a checked event is replayed', () => {
    page.form.controls.roles.setValue(['VIEWER', 'WORKFORCE_MANAGER']);

    page.toggleRole(
      'WORKFORCE_MANAGER',
      { target: { checked: true } } as unknown as Event,
    );

    expect(page.form.controls.roles.value).toEqual(['VIEWER', 'WORKFORCE_MANAGER']);
  });

  it('normalizes multiple privilege and menu IDs while preserving the category ID', async () => {
    page.form.patchValue({
      username: 'multi-role-user',
      displayName: 'Multi Role User',
      password: 'password123',
      roles: ['VIEWER', 'WORKFORCE_MANAGER', 'VIEWER'],
      allowedMenus: ['dashboard', 'workforce-workers', 'dashboard'],
      categoryId: 'category-both',
    });
    page.validationResult.set({ valid: true, warnings: [], conflicts: [], errors: [], sensitivePermissions: [] });
    const save = vi.spyOn(page.store, 'save').mockResolvedValue(true);

    await page.submit();

    const payload = save.mock.calls[0][1] as UserPayload;
    expect(payload.roles).toEqual(['VIEWER', 'WORKFORCE_MANAGER']);
    expect(payload.allowedMenus).toEqual(['dashboard', 'workforce-workers']);
    expect(payload.categoryId).toBe('category-both');
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

  it('initializes policy groups on new user and carries policy assignments with scopes into payload on save', async () => {
    page.availablePolicyGroups.set([
      {
        id: 'pg-1',
        groupName: 'Site Accountant',
        description: 'Accountant scoped to site',
        isSystem: false,
        permissionsCount: 5,
        assignedUsersCount: 0,
        createdAt: 0,
        updatedAt: 0,
        version: 1,
      },
    ]);

    page.openNew();
    await flushDialogOptions();
    expect(page.userPolicyAssignments()).toHaveLength(1);
    expect(page.userPolicyAssignments()[0].selected).toBe(false);

    page.togglePolicyAssignment('pg-1');
    page.updateBranchScope('pg-1', 'BRANCH-CAIRO');
    page.updateCostCenterScope('pg-1', 'CC-101');

    page.form.patchValue({
      username: 'site.user',
      displayName: 'Site User',
      password: 'Password123!',
      roles: ['VIEWER'],
      allowedMenus: ['dashboard'],
    });

    page.validationResult.set({ valid: true, warnings: [], conflicts: [], errors: [], sensitivePermissions: [] });
    const save = vi.spyOn(page.store, 'save').mockResolvedValue(true);

    await page.submit();

    expect(save).toHaveBeenCalledTimes(1);
    const payload = save.mock.calls[0][1] as UserPayload;
    expect(payload.policyAssignments).toEqual([
      {
        policyGroupId: 'pg-1',
        scopeBranchId: 'BRANCH-CAIRO',
        scopeCostCenterId: 'CC-101',
      },
    ]);
  });
});

describe('WP-10 job templates and server menu options', () => {
  let httpMock: HttpTestingController;
  let page: UsersPage;
  let fixture: ComponentFixture<UsersPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UsersPage],
      providers: [
        provideRouter([]),
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
        { provide: I18nService, useValue: { t: (key: string) => key } },
        {
          provide: NotificationService,
          useValue: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
        },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(UsersPage);
    page = fixture.componentInstance;

    httpMock.expectOne('/api/v1/users').flush([]);
    httpMock.expectOne('/api/v1/auth/user-categories').flush([]);
    httpMock.expectOne('/api/v1/access/catalog').flush(CATALOG);
    httpMock.expectOne('/api/v1/access/policy-groups').flush([]);
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    httpMock.verify();
  });

  it('renders a feature-locked menu grayed, unclickable, and with the vertical tooltip (AC-1)', async () => {
    page.openNew();
    await new Promise((resolve) => setTimeout(resolve, 0));
    httpMock.expectOne('/api/v1/users/menu-options').flush([
      { id: 'dashboard', labelKey: 'nav.dashboard', groupKey: 'DASHBOARD', verticalTags: ['GENERAL'], enabled: false },
      { id: 'reports', labelKey: 'nav.attendanceReports', groupKey: 'HR', verticalTags: ['GENERAL'], enabled: true },
    ]);
    httpMock.expectOne('/api/v1/users/role-templates').flush([]);
    await new Promise((resolve) => setTimeout(resolve, 0));
    fixture.detectChanges();

    // ModalDialogComponent teleports its content to document.body when open.
    const labels = Array.from(document.body.querySelectorAll('.menu-item-check')) as HTMLElement[];
    const lockedLabel = labels.find((el) => el.textContent.includes('nav.dashboard')) as HTMLElement;
    expect(lockedLabel).toBeDefined();
    expect(lockedLabel.classList.contains('feature-locked')).toBe(true);
    expect(lockedLabel.getAttribute('title')).toBe('users.menuNotEnabledForVertical');
    const lockedInput = lockedLabel.querySelector('input[type=checkbox]') as HTMLInputElement;
    expect(lockedInput.disabled).toBe(true);

    const openLabel = labels.find((el) => el.textContent.includes('nav.attendanceReports')) as HTMLElement;
    expect(openLabel).toBeDefined();
    expect((openLabel.querySelector('input[type=checkbox]') as HTMLInputElement).disabled).toBe(false);
  });

  it('applying a template pre-checks its menus and selects suggested policy groups', async () => {
    page.availablePolicyGroups.set([
      {
        id: 'g-pharmacy',
        groupName: 'Pharmacy',
        description: 'pharmacy',
        isSystem: true,
        permissionsCount: 3,
        assignedUsersCount: 0,
        createdAt: 0,
        updatedAt: 0,
        version: 1,
      } as never,
    ]);
    page.openNew();
    await new Promise((resolve) => setTimeout(resolve, 0));
    httpMock.expectOne('/api/v1/users/menu-options').flush([
      { id: 'sales', labelKey: 'nav.sales', groupKey: 'TRADE', verticalTags: ['MEDICAL'], enabled: true },
      { id: 'pos', labelKey: 'pos.title', groupKey: 'TRADE', verticalTags: ['RETAIL'], enabled: true },
    ]);
    httpMock.expectOne('/api/v1/users/role-templates').flush([
      {
        code: 'PHARMACIST',
        nameKey: 'users.template.pharmacist',
        vertical: 'MEDICAL',
        menuIds: ['sales', 'pos', 'reports'],
        permissionPrefixes: ['sales:so'],
        suggestedPolicyGroupIds: ['g-pharmacy'],
        sortOrder: 30,
      },
    ]);
    await new Promise((resolve) => setTimeout(resolve, 0));

    page.applyJobTemplate('PHARMACIST');

    const menus = page.form.controls.allowedMenus.value;
    expect(menus).toContain('sales');
    expect(menus).toContain('pos');
    expect(page.customMenuAccess()).toBe(true);
    expect(
      page.userPolicyAssignments().find((a) => a.policyGroupId === 'g-pharmacy')?.selected,
    ).toBe(true);
  });

  it('falls back to the static menu catalog when the endpoint fails (AC-3)', async () => {
    page.openNew();
    await new Promise((resolve) => setTimeout(resolve, 0));
    httpMock
      .expectOne('/api/v1/users/menu-options')
      .flush(null, { status: 500, statusText: 'Server Error' });
    httpMock.expectOne('/api/v1/users/role-templates').flush([]);
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(page.access.serverMenuOptions()).toBeNull();
    expect(page.menuOptions().length).toBe(USER_MENU_OPTIONS.length);
    expect(page.roleTemplates().length).toBe(0);
  });
});
