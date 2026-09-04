import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthUser, AppSettings, RoleCode } from '../../core/auth/auth.models';
import { UserPayload, UsersStore } from './users.store';
import { TablePagination } from '../../shared/ui/table-pagination/pagination';
import { TablePaginationComponent } from '../../shared/ui/table-pagination/table-pagination.component';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';

import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { exportCsv } from '../../core/download';

import { ModalDialogComponent } from '../../shared/ui/modal-dialog/modal-dialog.component';
import { IconComponent } from '../../shared/ui/icon/icon.component';
import { AccessRole, AccessValidateResult, ACCESS_LEVEL_PRECEDENCE, MenuOption, RoleTemplate } from './access.models';
import { AccessService } from './access.service';


import { NAV_ITEMS, WORKSPACE_ORDER } from '../../core/navigation/app-navigation';

import { RouterLink } from '@angular/router';
import { UserPolicyAssignmentModalComponent } from './ui/user-policy-assignment-modal/user-policy-assignment-modal.component';
import { PolicyService } from '../../core/auth/policy.service';
import {
  PolicyGroupSummaryDto,
  UserPolicyAssignmentDto,
  UserPolicyAssignmentItem,
} from '../../core/auth/security-policy.models';

export interface EditablePolicyAssignment {
  policyGroupId: string;
  groupName: string;
  description?: string;
  permissionsCount: number;
  selected: boolean;
  scopeBranchId: string;
  scopeCostCenterId: string;
}

export const USER_MENU_OPTIONS: Array<{ id: string; labelKey: string }> = NAV_ITEMS
  .filter((item) => item.showInPermissionEditor !== false)
  .map((item) => ({
    id: item.menuId,
    labelKey: item.labelKey,
  }));

@Component({
  selector: 'app-users-page',
  imports: [ReactiveFormsModule, TablePaginationComponent, ModalDialogComponent, IconComponent, UserPolicyAssignmentModalComponent, RouterLink],
  providers: [UsersStore],
  templateUrl: './users.page.html',
  styleUrl: './users.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})


export class UsersPage {
  readonly policyModalUserId = signal<string | null>(null);
  readonly policyModalUserName = signal<string>('');

  openPolicyModal(user: AuthUser): void {
    this.policyModalUserId.set(user.id);
    this.policyModalUserName.set(user.displayName);
  }

  closePolicyModal(): void {
    this.policyModalUserId.set(null);
  }

  // BORTQALA_RUNTIME_20260816_V2_USER_REASON_FIELD
  readonly accessReasonEditing = signal(false);

  finishAccessReasonEditing(): void {
    // Defer until after the click that caused blur (for example Save) has fired.
    window.setTimeout(() => this.accessReasonEditing.set(false), 0);
  }

  readonly auth = inject(AuthService);
  readonly store = inject(UsersStore);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  readonly access = inject(AccessService);
  // WP-10: server-side menu catalog enriches the static fallback; on endpoint
  // failure the constant list is used verbatim (AC-3 fallback contract).
  readonly menuOptions = computed<Array<{ id: string; labelKey: string; enabled?: boolean }>>(() => {
    const server = this.access.serverMenuOptions();
    if (!server || server.length === 0) return USER_MENU_OPTIONS;
    const byId = new Map(server.map((option) => [option.id, option]));
    return USER_MENU_OPTIONS.map((option) => {
      const enriched = byId.get(option.id);
      return enriched ? { ...option, enabled: enriched.enabled } : option;
    });
  });
  readonly roleTemplates = signal<RoleTemplate[]>([]);
  readonly activeTemplateCode = signal('');
  readonly drawerOpen = signal(false);
  readonly wizardStep = signal(1);
  readonly advancedOpen = signal(false);
  readonly wizardSteps: Array<{ key: string }> = [
    { key: 'users.stepIdentity' },
    { key: 'users.stepCategory' },
    { key: 'users.stepRole' },
    { key: 'users.stepPermissions' },
    { key: 'users.stepConfirmation' },
  ];
  readonly submitted = signal(false);
  readonly showPassword = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly accessLoading = signal(false);
  readonly roleSearch = signal('');
  readonly pageSearch = signal('');
  readonly needCodes = signal<string[]>([]);
  readonly validationResult = signal<AccessValidateResult | null>(null);
  readonly validationRunning = signal(false);
  readonly validationError = signal<string | null>(null);
  readonly acknowledgedWarnings = signal(false);
  readonly ackReason = signal('');
  readonly expandedRole = signal<string | null>(null);
  readonly baselineRoles = signal<RoleCode[]>([]);
  readonly baselineMenus = signal<string[]>([]);
  readonly selectedRoles = signal<RoleCode[]>([]);
  readonly selectedMenus = signal<string[]>([]);
  readonly policyService = inject(PolicyService);
  readonly availablePolicyGroups = signal<PolicyGroupSummaryDto[]>([]);
  readonly userPolicyAssignments = signal<EditablePolicyAssignment[]>([]);
  readonly policySearch = signal<string>('');
  readonly activePolicyAssignmentsCount = computed(
    () => this.userPolicyAssignments().filter((a) => a.selected).length,
  );
  readonly filteredPolicyAssignments = computed(() => {
    const query = this.policySearch().trim().toLowerCase();
    const items = this.userPolicyAssignments();
    if (!query) return items;
    return items.filter(
      (item) =>
        item.groupName.toLowerCase().includes(query) ||
        (item.description && item.description.toLowerCase().includes(query)),
    );
  });
  // New users follow role-derived menu access until an admin manually customizes menus.
  readonly customMenuAccess = signal(false);
  readonly userSearch = signal('');
  readonly pagination = new TablePagination();

  readonly filteredUsers = computed(() => {
    const query = this.userSearch().trim().toLowerCase();
    const items = this.store.items();
    if (!query) return items;

    return items.filter((user) =>
      user.displayName.toLowerCase().includes(query) ||
      user.username.toLowerCase().includes(query) ||
      user.roles.some((role) => role.toLowerCase().includes(query) || this.roleLabel(role).toLowerCase().includes(query)),
    );
  });

  readonly paged = computed(() => this.pagination.slice(this.filteredUsers()));
  readonly activeUserCount = computed(() => this.store.items().filter((user) => user.active).length);
  readonly adminUserCount = computed(() =>
    this.store.items().filter((user) => user.roles.some((role) => role === 'ADMIN' || role === 'SUPER_ADMIN')).length,
  );
  readonly usedRoleCount = computed(() => new Set(this.store.items().flatMap((user) => user.roles)).size);
  readonly roles: Array<{ code: RoleCode; labelKey: string; descriptionKey: string }> = [
    { code: 'SUPER_ADMIN', labelKey: 'role.superAdmin', descriptionKey: 'role.superAdminHint' },
    { code: 'ADMIN', labelKey: 'role.admin', descriptionKey: 'role.adminHint' },
    { code: 'HR_MANAGER', labelKey: 'role.hrManager', descriptionKey: 'role.hrManagerHint' },
    { code: 'HR_REVIEWER', labelKey: 'role.hrReviewer', descriptionKey: 'role.hrReviewerHint' },
    { code: 'VIEWER', labelKey: 'role.viewer', descriptionKey: 'role.viewerHint' },
    { code: 'FINANCE_MANAGER', labelKey: 'role.financeManager', descriptionKey: 'role.financeManagerHint' },
    { code: 'ACCOUNTANT', labelKey: 'role.accountant', descriptionKey: 'role.accountantHint' },
    { code: 'TREASURY_USER', labelKey: 'role.treasuryUser', descriptionKey: 'role.treasuryUserHint' },
    { code: 'PROCUREMENT_MANAGER', labelKey: 'role.procurementManager', descriptionKey: 'role.procurementManagerHint' },
    { code: 'PROCUREMENT_USER', labelKey: 'role.procurementUser', descriptionKey: 'role.procurementUserHint' },
    { code: 'SALES_MANAGER', labelKey: 'role.salesManager', descriptionKey: 'role.salesManagerHint' },
    { code: 'INVENTORY_MANAGER', labelKey: 'role.inventoryManager', descriptionKey: 'role.inventoryManagerHint' },
    { code: 'MANUFACTURING_MANAGER', labelKey: 'role.manufacturingManager', descriptionKey: 'role.manufacturingManagerHint' },
    { code: 'QUALITY_MANAGER', labelKey: 'role.qualityManager', descriptionKey: 'role.qualityManagerHint' },
    { code: 'PAYROLL_MANAGER', labelKey: 'role.payrollManager', descriptionKey: 'role.payrollManagerHint' },
    { code: 'WORKFORCE_MANAGER', labelKey: 'role.workforceManager', descriptionKey: 'role.workforceManagerHint' },
    { code: 'WORKFORCE_REVIEWER', labelKey: 'role.workforceReviewer', descriptionKey: 'role.workforceReviewerHint' },
    { code: 'WORKFORCE_FINANCE', labelKey: 'role.workforceFinance', descriptionKey: 'role.workforceFinanceHint' },
    { code: 'AUDITOR', labelKey: 'role.auditor', descriptionKey: 'role.auditorHint' },
  ];

  readonly menuGroups = WORKSPACE_ORDER.map((workspace) => ({
    titleKey: workspace,
    ids: NAV_ITEMS.filter(
      (item) => item.workspace === workspace && item.showInPermissionEditor !== false,
    ).map((item) => item.menuId),
  })).filter((group) => group.ids.length > 0);

  readonly catalogRoles = computed(() => {
    const catalog = this.access.catalog();
    const q = this.roleSearch().trim().toLowerCase();
    if (!catalog) return [];
    if (!q) return catalog.roles;
    return catalog.roles.filter((role) =>
      role.code.toLowerCase().includes(q) ||
      this.i18n.t(role.nameKey).toLowerCase().includes(q) ||
      this.i18n.t(role.descriptionKey).toLowerCase().includes(q),
    );
  });

  readonly needs = computed(() => this.access.catalog()?.needs ?? []);

  readonly neededPermissions = computed(() => {
    const catalog = this.access.catalog();
    if (!catalog) return [] as string[];
    const codes = new Set(this.needCodes());
    return catalog.needs
      .filter((need) => codes.has(need.code))
      .flatMap((need) => need.permissions);
  });

  readonly suggestedRoles = computed(() => this.access.suggestRoles(this.neededPermissions()));

  readonly broaderRoles = computed(() => this.access.broaderRoles(this.suggestedRoles()));

  readonly preview = computed(() => this.access.preview(this.selectedRoles(), this.selectedMenus()));

  readonly previewPageResults = computed(() => {
    const q = this.pageSearch().trim().toLowerCase();
    const pages = this.access.pages();
    if (!q) return pages;
    return pages.filter((page) =>
      page.code.toLowerCase().includes(q) ||
      this.i18n.t(page.titleKey).toLowerCase().includes(q) ||
      page.module.toLowerCase().includes(q) ||
      page.route.toLowerCase().includes(q),
    );
  });

  readonly editMode = computed(() => this.editingId() !== null);

  /** True when at least one role is selected, allowing progression past the Role step. */
  readonly rolesSelected = () => this.form.controls.roles.value.length > 0;

  readonly selectedCategoryLabel = computed(() => {
    const id = this.form.controls.categoryId.value;
    if (!id) return this.i18n.t('common.none');
    return this.store.categories().find((cat) => cat.id === id)?.name ?? id;
  });

  readonly confirmationSummary = computed(() => {
    const v = this.form.getRawValue();
    return {
      displayName: v.displayName,
      username: v.username,
      categoryName: this.selectedCategoryLabel(),
      roles: [...new Set(v.roles)],
      menus: [...new Set(v.allowedMenus)],
      active: v.active,
    };
  });

  readonly rolesAdded = computed(() =>
    this.selectedRoles().filter((role) => !this.baselineRoles().includes(role)).sort(),
  );
  readonly rolesRemoved = computed(() =>
    this.baselineRoles().filter((role) => !this.selectedRoles().includes(role)).sort(),
  );
  readonly menusAdded = computed(() =>
    this.selectedMenus().filter((menu) => !this.baselineMenus().includes(menu)).sort(),
  );
  readonly menusRemoved = computed(() =>
    this.baselineMenus().filter((menu) => !this.selectedMenus().includes(menu)).sort(),
  );

  readonly changedPages = computed(() => {
    if (!this.editMode()) return [];
    const before = this.access.preview(this.baselineRoles(), this.baselineMenus());
    const byCode = new Map(this.access.pages().map((page) => [page.code, page.titleKey]));
    return this.preview()
      .pages
      .map((page) => ({
        page,
        titleKey: byCode.get(page.pageCode) ?? page.pageCode,
        before: before.pages.find((item) => item.pageCode === page.pageCode)?.access ?? 'NONE',
      }))
      .filter((row) => row.before !== row.page.access);
  });

  readonly selectedSensitiveReasons = computed(() => {
    const catalog = this.access.catalog();
    if (!catalog) return [] as string[];
    const selected = new Set(this.selectedRoles());
    return catalog.roles
      .filter((role) => selected.has(role.code) && role.sensitiveReasonKey)
      .map((role) => role.sensitiveReasonKey as string);
  });

  readonly validationNeedsAck = computed(() => {
    const result = this.validationResult();
    if (!result) return false;
    if (result.errors.some((error) => error.code === 'ACCESS_ACK_REASON_REQUIRED')) return true;
    return result.warnings.length > 0 || result.conflicts.length > 0;
  });

  readonly validationAckSatisfied = computed(
    () => !this.validationNeedsAck() || this.ackReason().trim().length > 0,
  );

  readonly validationOk = computed(() => {
    const result = this.validationResult();
    if (!result || result.valid !== true) return false;
    if (!this.validationAckSatisfied()) return false;
    return true;
  });

  readonly adminOverrideActive = computed(() =>
    this.selectedRoles().some((role) => role === 'SUPER_ADMIN' || role === 'ADMIN'),
  );

  readonly catalogUnavailable = computed(() => {
    const catalog = this.access.catalog();
    return !this.accessLoading() && (catalog === null || this.access.error() !== null);
  });

  /** Feature gate state for a menu: which feature disables it, if any. */
  menuFeature(menuId: string): string | null {
    // WP-10: when the server menu catalog loads successfully it is authoritative;
    // otherwise fall back to the local catalog+activeFeatures derivation.
    const server = this.access.serverMenuOptions();
    if (server && server.length > 0) {
      const option = server.find((item) => item.id === menuId);
      if (!option) return null;
      return option.enabled ? null : option.id;
    }
    const page = this.access.pages().find((item) => item.menuId === menuId);
    if (!page || page.requiredFeature === null || page.requiredFeature === undefined) return null;
    const activeFeatures = this.auth.user()?.activeFeatures ?? [];
    return activeFeatures.includes(page.requiredFeature) ? null : page.requiredFeature;
  }

  /** True when none of the selected roles can open the page behind this menu. */
  menuRoleMismatch(menuId: string): boolean {
    if (this.adminOverrideActive()) return false;
    const page = this.access.pages().find((item) => item.menuId === menuId);
    if (!page || page.roles.length === 0) return false;
    return this.selectedRoles().every((role) => !page.roles.includes(role));
  }

  /** Pages the given role can actually open, for the expandable role cards. */
  roleAccessiblePages(roleCode: string): Array<{ code: string; titleKey: string; level: string }> {
    const catalog = this.access.catalog();
    if (!catalog) return [];
    const role = catalog.roles.find((item) => item.code === roleCode);
    if (!role) return [];
    const granted = new Set(role.permissions);
    const menus = new Set(this.menuOptions().map((menu) => menu.id));
    const activeFeatures = this.auth.user()?.activeFeatures ?? [];
    const isAdmin = roleCode === 'ADMIN' || roleCode === 'SUPER_ADMIN';
    const result: Array<{ code: string; titleKey: string; level: string }> = [];
    for (const page of catalog.pages) {
      if (!isAdmin && !page.viewPermissions.some((permission) => granted.has(permission))) continue;
      if (!menus.has(page.menuId)) continue;
      if (page.requiredFeature && !activeFeatures.includes(page.requiredFeature)) continue;
      if (!isAdmin && page.roles.length > 0 && !page.roles.includes(roleCode)) continue;
      const grantedActions = page.actions.filter((action) => granted.has(action.permission)).map((action) => action.code);
      const level = isAdmin ? 'REVIEW' : ACCESS_LEVEL_PRECEDENCE.find((item) => grantedActions.includes(item)) ?? 'VIEW';
      result.push({ code: page.code, titleKey: page.titleKey, level });
    }
    return result;
  }

  toggleRoleDetails(code: string): void {
    this.expandedRole.set(this.expandedRole() === code ? null : code);
  }

  isModuleAllSelected(ids: string[]): boolean {
    const current = this.form.controls.allowedMenus.value;
    return ids.every((id) => current.includes(id));
  }

  isModulePartiallySelected(ids: string[]): boolean {
    const current = this.form.controls.allowedMenus.value;
    const count = ids.filter((id) => current.includes(id)).length;
    return count > 0 && count < ids.length;
  }

  toggleModule(ids: string[]): void {
    this.customMenuAccess.set(true);
    const current = new Set(this.form.controls.allowedMenus.value);
    if (this.isModuleAllSelected(ids)) {
      ids.forEach((id) => current.delete(id));
    } else {
      ids.forEach((id) => current.add(id));
    }
    this.form.controls.allowedMenus.setValue(Array.from(current));
  }

  getMenuLabel(id: string): string {
    const option = this.menuOptions().find((item) => item.id === id);
    return option ? this.i18n.t(option.labelKey) : id;
  }

  selectAllMenus(): void {
    this.customMenuAccess.set(true);
    const allIds = this.menuOptions().map((o) => o.id);
    this.form.controls.allowedMenus.setValue(allIds);
  }

  clearAllMenus(): void {
    this.customMenuAccess.set(true);
    this.form.controls.allowedMenus.setValue([]);
  }
  readonly passwordPolicy = signal<Partial<AppSettings>>({ minPasswordLength: 8 });
  readonly form = new FormGroup({
    username: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    displayName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    password: new FormControl('', { nonNullable: true }),
    roles: new FormControl<RoleCode[]>([], {
      nonNullable: true,
      validators: [Validators.required],
    }),
    allowedMenus: new FormControl<string[]>([], { nonNullable: true }),
    canViewSalary: new FormControl(true, { nonNullable: true }),
    dashboardCustomizationEnabled: new FormControl(true, { nonNullable: true }),
    active: new FormControl(true, { nonNullable: true }),
    version: new FormControl<number | null>(null),
    categoryId: new FormControl<string | null>(null),
  });

  constructor() {
    void this.store.load();
    void this.store.loadCategories();
    void this.loadPolicy();
    void this.loadAccessCatalog();
    void this.loadPolicyGroups();
    this.form.valueChanges.subscribe(() => {
      this.selectedRoles.set(this.form.controls.roles.value);
      this.selectedMenus.set(this.form.controls.allowedMenus.value);
      this.validationResult.set(null);
      this.validationError.set(null);
      this.acknowledgedWarnings.set(false);
      this.ackReason.set('');
    });
  }

  private async loadPolicy(): Promise<void> {
    try {
      const settings = await firstValueFrom(this.auth.appSettings());
      this.passwordPolicy.set(settings);
    } catch {}
  }

  async loadPolicyGroups(): Promise<void> {
    try {
      const groups = await firstValueFrom(this.policyService.listPolicyGroups());
      this.availablePolicyGroups.set(groups || []);
    } catch {
      this.availablePolicyGroups.set([]);
    }
  }

  async loadAccessCatalog(): Promise<void> {
    this.accessLoading.set(true);
    await this.access.loadCatalog();
    this.accessLoading.set(false);
    if (this.drawerOpen() && !this.editMode() && !this.customMenuAccess()) {
      this.syncMenusToRoles();
    }
  }

  roleUserCount(code: RoleCode): number {
    return this.store.items().filter((user) => user.roles.includes(code)).length;
  }

  openNew() {
    this.submitted.set(false);
    this.showPassword.set(false);
    this.editingId.set(null);
    this.customMenuAccess.set(false);
    this.baselineRoles.set([]);
    this.baselineMenus.set([]);
    this.needCodes.set([]);
    this.form.reset({
      username: '',
      displayName: '',
      password: '',
      roles: ['VIEWER'],
      allowedMenus: ['dashboard', 'reports'],
      canViewSalary: true,
      dashboardCustomizationEnabled: true,
      active: true,
      version: null,
      categoryId: null,
    });
    this.syncMenusToRoles();
    this.initPolicyAssignmentsForNew();
    this.activeTemplateCode.set('');
    this.roleTemplates.set([]);
    this.drawerOpen.set(true);
    this.wizardStep.set(1);
    void this.loadUserDialogOptions();
  }

  /** WP-10: best-effort load of server menu options + job templates (silent fallback). */
  private async loadUserDialogOptions(): Promise<void> {
    await this.access.loadCatalog();
    const [templates] = await Promise.all([
      this.access.loadRoleTemplates(),
      this.access.loadMenuOptions(),
    ]);
    this.roleTemplates.set(templates);
  }

  /**
   * WP-10: applying a template pre-checks its menus and selects its suggested
   * policy groups. Everything stays manually editable afterwards.
   */
  applyJobTemplate(code: string): void {
    this.activeTemplateCode.set(code);
    const template = this.roleTemplates().find((item) => item.code === code);
    if (!template) return;
    const merged = new Set(this.form.controls.allowedMenus.value);
    for (const menuId of template.menuIds) merged.add(menuId);
    this.form.controls.allowedMenus.setValue(Array.from(merged));
    this.customMenuAccess.set(true);
    if (template.suggestedPolicyGroupIds.length > 0) {
      const suggested = new Set(template.suggestedPolicyGroupIds);
      this.userPolicyAssignments.set(
        this.userPolicyAssignments().map((assignment) =>
          suggested.has(assignment.policyGroupId)
            ? { ...assignment, selected: true }
            : assignment,
        ),
      );
    }
  }

  openEdit(item: AuthUser) {
    this.submitted.set(false);
    this.showPassword.set(false);
    this.editingId.set(item.id);
    // Never overwrite an existing user's explicit menu configuration while editing.
    this.customMenuAccess.set(true);
    this.baselineRoles.set(item.roles);
    this.baselineMenus.set(item.allowedMenus ?? this.menuOptions().map((m) => m.id));
    this.needCodes.set([]);
    this.form.reset({
      username: item.username,
      displayName: item.displayName,
      password: '',
      roles: item.roles,
      allowedMenus: item.allowedMenus ?? this.menuOptions().map((m) => m.id),
      canViewSalary: item.canViewSalary ?? true,
      dashboardCustomizationEnabled: item.dashboardCustomizationEnabled ?? true,
      active: item.active,
      version: item.version,
      categoryId: item.categoryId ?? null,
    });
    void this.initPolicyAssignmentsForEdit(item);
    this.drawerOpen.set(true);
    this.wizardStep.set(1);
  }

  private initPolicyAssignmentsForNew(): void {
    const groups = this.availablePolicyGroups();
    this.userPolicyAssignments.set(
      groups.map((g) => ({
        policyGroupId: g.id,
        groupName: g.groupName,
        description: g.description,
        permissionsCount: g.permissionsCount,
        selected: false,
        scopeBranchId: '',
        scopeCostCenterId: '',
      })),
    );
  }

  private async initPolicyAssignmentsForEdit(item: AuthUser): Promise<void> {
    let groups = this.availablePolicyGroups();
    if (!groups.length) {
      try {
        groups = (await firstValueFrom(this.policyService.listPolicyGroups())) || [];
        this.availablePolicyGroups.set(groups);
      } catch {
        groups = [];
      }
    }
    let existing: UserPolicyAssignmentDto[] = item.policyAssignments || [];
    if (!existing.length && item.id) {
      try {
        existing = (await firstValueFrom(this.policyService.getUserPolicies(item.id))) || [];
      } catch {
        existing = [];
      }
    }
    const byGroupId = new Map(existing.map((a) => [a.policyGroupId, a]));
    this.userPolicyAssignments.set(
      groups.map((g) => {
        const match = byGroupId.get(g.id);
        return {
          policyGroupId: g.id,
          groupName: g.groupName,
          description: g.description,
          permissionsCount: g.permissionsCount,
          selected: !!match,
          scopeBranchId: match?.scopeBranchId || '',
          scopeCostCenterId: match?.scopeCostCenterId || '',
        };
      }),
    );
  }

  togglePolicyAssignment(policyGroupId: string): void {
    this.userPolicyAssignments.update((list) =>
      list.map((a) => (a.policyGroupId === policyGroupId ? { ...a, selected: !a.selected } : a)),
    );
  }

  updateBranchScope(policyGroupId: string, value: string): void {
    this.userPolicyAssignments.update((list) =>
      list.map((a) => (a.policyGroupId === policyGroupId ? { ...a, scopeBranchId: value } : a)),
    );
  }

  updateCostCenterScope(policyGroupId: string, value: string): void {
    this.userPolicyAssignments.update((list) =>
      list.map((a) => (a.policyGroupId === policyGroupId ? { ...a, scopeCostCenterId: value } : a)),
    );
  }

  selectAllPolicies(): void {
    this.userPolicyAssignments.update((list) => list.map((a) => ({ ...a, selected: true })));
  }

  clearAllPolicies(): void {
    this.userPolicyAssignments.update((list) =>
      list.map((a) => ({ ...a, selected: false, scopeBranchId: '', scopeCostCenterId: '' })),
    );
  }

  allowedMenuCount(item: AuthUser): number {
    if (item.roles.some((role) => role === 'SUPER_ADMIN' || role === 'ADMIN')) {
      return this.menuOptions().length;
    }
    return item.allowedMenus ? item.allowedMenus.length : this.menuOptions().length;
  }

  primaryRole(): RoleCode | '' {
    return this.form.controls.roles.value[0] ?? '';
  }

  primaryRoleInfo(): { code: RoleCode; labelKey: string; descriptionKey: string } | null {
    const code = this.primaryRole();
    return this.roles.find((role) => role.code === code) ?? null;
  }

  setPrimaryRole(event: Event): void {
    const code = (event.target as HTMLSelectElement).value as RoleCode;
    if (!code) return;

    const current = this.form.controls.roles.value;
    // Role order is presentation-only: the backend persists a set. Keep every
    // selected privilege when promoting a role so an edit cannot silently drop
    // whichever role happened to be returned first.
    const next: RoleCode[] = [code, ...current.filter((role) => role !== code)];
    this.form.controls.roles.setValue(next);

    if (!this.customMenuAccess()) {
      this.syncMenusToRoles(next);
    }
  }

  toggleRole(code: RoleCode, event: Event) {
    const checked = (event.target as HTMLInputElement).checked;
    const current = this.form.controls.roles.value;
    const next = checked
      ? [...new Set([...current, code])]
      : current.filter((item) => item !== code);
    this.form.controls.roles.setValue(next);
    if (!this.customMenuAccess()) {
      this.syncMenusToRoles(next);
    }
  }

  hasRole(code: RoleCode) {
    return this.form.controls.roles.value.includes(code);
  }

  toggleMenu(id: string, event: Event) {
    this.customMenuAccess.set(true);
    const checked = (event.target as HTMLInputElement).checked;
    const current = this.form.controls.allowedMenus.value;
    this.form.controls.allowedMenus.setValue(
      checked ? [...current, id] : current.filter((item) => item !== id),
    );
  }

  hasMenu(id: string) {
    return this.form.controls.allowedMenus.value.includes(id);
  }

  /**
   * Keep creation simple: menu access follows the selected role catalog until the
   * admin deliberately changes a menu. The backend catalog remains authoritative.
   */
  private syncMenusToRoles(roles: RoleCode[] = this.form.controls.roles.value): void {
    if (this.customMenuAccess()) return;
    const catalog = this.access.catalog();
    if (!catalog) return;

    const pageCodes = new Set<string>();
    for (const role of roles) {
      for (const page of this.roleAccessiblePages(role)) {
        pageCodes.add(page.code);
      }
    }

    const knownMenus = new Set(this.menuOptions().map((item) => item.id));
    const recommended = catalog.pages
      .filter((page) => pageCodes.has(page.code) && knownMenus.has(page.menuId))
      .map((page) => page.menuId);

    this.form.controls.allowedMenus.setValue([...new Set(recommended)]);
  }

  toggleNeed(code: string): void {
    const current = this.needCodes();
    this.needCodes.set(
      current.includes(code) ? current.filter((item) => item !== code) : [...current, code],
    );
  }

  hasNeed(code: string): boolean {
    return this.needCodes().includes(code);
  }

  applySuggestedRoles(): void {
    const suggested = this.suggestedRoles();
    if (suggested.length) {
      const merged = new Set(this.form.controls.roles.value);
      suggested.forEach((role) => merged.add(role as RoleCode));
      this.form.controls.roles.setValue(Array.from(merged) as RoleCode[]);
    }
    this.needCodes.set([]);
  }

  roleLabelFor(code: string): string {
    const role = this.access.roles().find((item) => item.code === code);
    return role ? this.i18n.t(role.nameKey) : code;
  }

  pageMeta(pageCode: string) {
    return this.access.pages().find((page) => page.code === pageCode);
  }

  moduleLabel(pageCode: string): string {
    return this.pageMeta(pageCode)?.module ?? pageCode;
  }

  pageTitle(pageCode: string): string {
    const page = this.pageMeta(pageCode);
    return page ? this.i18n.t(page.titleKey) : pageCode;
  }

  levelLabel(level: string): string {
    return this.i18n.t(`access.level.${level}`);
  }

  sensitivityLabel(sensitivity: string): string {
    return this.i18n.t(`access.sensitivity.${sensitivity}`);
  }

  kindLabel(kind: string): string {
    return this.i18n.t(`access.kind.${kind}`);
  }

  roleMeta(role: AccessRole): { pages: number; actions: number } {
    const catalog = this.access.catalog();
    if (!catalog) return { pages: 0, actions: 0 };

    const accessiblePages = this.roleAccessiblePages(role.code);
    const accessibleCodes = new Set(accessiblePages.map((page) => page.code));
    const granted = new Set(role.permissions);
    let actions = 0;

    for (const page of catalog.pages) {
      if (!accessibleCodes.has(page.code)) continue;
      actions += page.actions.filter((action) => granted.has(action.permission)).length;
    }

    return { pages: accessiblePages.length, actions };
  }

  requiredRolesForPage(viewPermissions: string[]): string[] {
    const pages = this.access.pages();
    const page =
      pages.find((item) => item.viewPermissions === viewPermissions) ??
      pages.find(
        (item) =>
          item.viewPermissions.length === viewPermissions.length &&
          item.viewPermissions.every(
            (permission, index) => permission === viewPermissions[index],
          ),
      );

    if (page?.roles?.length) {
      return [...page.roles].sort();
    }

    const all = new Set<string>();
    viewPermissions.forEach((permission) =>
      this.access.rolesGranting(permission).forEach((role) => all.add(role)),
    );
    return [...all].sort();
  }

  actionLabel(action: string): string {
    return this.i18n.t(`access.level.${action}`);
  }

  validatePassword(pwd: string): string | null {
    const policy = this.passwordPolicy();
    const minLen = policy.minPasswordLength ?? 8;
    const maxLen = policy.maxPasswordLength ?? 128;
    if (!pwd) return null;
    if (pwd.length < minLen) {
      return this.i18n.t('users.passwordHint', { min: minLen });
    }
    if (maxLen > 0 && pwd.length > maxLen) {
      return this.i18n.t('users.passwordMaxHint', { max: maxLen });
    }
    if (policy.disallowSpaces && pwd.includes(' ')) {
      return this.i18n.t('users.passwordNoSpaces');
    }
    if (policy.requireUppercase && !/[A-Z]/.test(pwd)) {
      return this.i18n.t('users.passwordNeedUppercase');
    }
    if (policy.requireLowercase && !/[a-z]/.test(pwd)) {
      return this.i18n.t('users.passwordNeedLowercase');
    }
    if (policy.requireNumbers && !/[0-9]/.test(pwd)) {
      return this.i18n.t('users.passwordNeedNumber');
    }
    if (policy.requireSpecialChars && !/[^A-Za-z0-9]/.test(pwd)) {
      return this.i18n.t('users.passwordNeedSpecial');
    }
    return null;
  }

  async runValidation(): Promise<AccessValidateResult | null> {
    this.validationRunning.set(true);
    this.validationError.set(null);
    try {
      const result = await this.access.validate(
        [...new Set(this.form.controls.roles.value)],
        [...new Set(this.form.controls.allowedMenus.value)],
        this.editingId(),
        this.ackReason().trim() || undefined,
      );
      this.validationResult.set(result);
      return result;
    } catch (error) {
      const message = (error as { error?: { message?: string } })?.error?.message;
      this.validationError.set(message || this.i18n.t('access.validateFailed'));
      return null;
    } finally {
      this.validationRunning.set(false);
    }
  }

  validateErrorLabel(code: string): string {
    return this.i18n.t(`access.validateError.${code}`);
  }

  async submit() {
    this.submitted.set(true);
    if (this.catalogUnavailable()) {
      this.notification.error(this.i18n.t('access.saveBlockedCatalog'));
      this.form.markAllAsTouched();
      return;
    }
    const pwd = this.form.controls.password.value;
    if (!this.editingId() && !pwd) {
      this.notification.error(this.i18n.t('users.passwordHint', { min: this.passwordPolicy().minPasswordLength ?? 8 }));
      this.form.markAllAsTouched();
      return;
    }
    const pwdError = this.validatePassword(pwd);
    if (pwdError) {
      this.notification.error(pwdError);
      this.form.markAllAsTouched();
      return;
    }
    if (this.form.invalid || !this.form.controls.roles.value.length) {
      this.form.markAllAsTouched();
      return;
    }

    let result = this.validationOk() ? this.validationResult() : await this.runValidation();
    if (!result) {
      this.form.markAllAsTouched();
      return;
    }
    if (result.valid !== true) {
      if (result.errors.length > 0) {
        const first = result.errors[0];
        this.notification.error(this.validateErrorLabel(first.code));
      } else {
        this.notification.error(this.i18n.t('access.saveBlocked'));
      }
      return;
    }
    if ((result.warnings.length > 0 || result.conflicts.length > 0) && !this.validationAckSatisfied()) {
      this.notification.error(this.i18n.t('access.saveWarningsUnacknowledged'));
      return;
    }

    const raw = this.form.getRawValue();
    const policyAssignments: UserPolicyAssignmentItem[] = this.userPolicyAssignments()
      .filter((a) => a.selected)
      .map((a) => ({
        policyGroupId: a.policyGroupId,
        scopeBranchId: a.scopeBranchId?.trim() || undefined,
        scopeCostCenterId: a.scopeCostCenterId?.trim() || undefined,
      }));

    const payload: UserPayload = {
      ...raw,
      password: raw.password || null,
      roles: [...new Set(raw.roles)],
      allowedMenus: [...new Set(raw.allowedMenus)],
      accessChangeReason: this.ackReason().trim() || undefined,
      policyAssignments,
    };
    if (await this.store.save(this.editingId(), payload)) {
      this.notification.success(this.i18n.t('users.userSaved')); 
      this.closeDrawer();
    } else if (this.store.error()) {
      this.notification.error(this.store.error()!);
    }
  }

  roleLabel(code: RoleCode) {
    const role = this.roles.find((item) => item.code === code);
    return role ? this.i18n.t(role.labelKey) : code;
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
    this.submitted.set(false);
    this.wizardStep.set(1);
  }

  goToStep(step: number): void {
    if (step >= 1 && step <= this.wizardSteps.length) {
      this.wizardStep.set(step);
    }
  }

  goNext(): void {
    if (this.wizardStep() < this.wizardSteps.length) {
      this.wizardStep.update((step) => step + 1);
    }
  }

  goBack(): void {
    if (this.wizardStep() > 1) {
      this.wizardStep.update((step) => step - 1);
    }
  }

  exportCsv(): void {
    const rows = this.paged().map((user) => ({
      username: user.username,
      displayName: user.displayName,
      roles: user.roles.join(', '),
      allowedMenus: user.allowedMenus?.length ?? 0,
      active: user.active ? this.i18n.t('common.active') : this.i18n.t('common.inactive'),
    }));
    exportCsv(
      rows,
      [
        { key: 'username', label: this.i18n.t('users.username') },
        { key: 'displayName', label: this.i18n.t('users.displayName') },
        { key: 'roles', label: this.i18n.t('users.roles') },
        { key: 'allowedMenus', label: this.i18n.t('users.allowedMenus') },
        { key: 'active', label: this.i18n.t('users.status') },
      ],
      `users-${new Date().toISOString().slice(0, 10)}.csv`,
    );
  }

  @HostListener('document:keydown', ['$event']) onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Escape' && this.drawerOpen()) {
      this.closeDrawer();
    } else if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') {
      if (this.drawerOpen()) {
        event.preventDefault();
        void this.submit();
      }
    }
  }
}
