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

@Component({
  selector: 'app-users-page',
  imports: [ReactiveFormsModule, TablePaginationComponent, ModalDialogComponent],
  providers: [UsersStore],
  templateUrl: './users.page.html',
  styleUrl: './users.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UsersPage {
  readonly auth = inject(AuthService);
  readonly store = inject(UsersStore);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  readonly drawerOpen = signal(false);
  readonly submitted = signal(false);
  readonly showPassword = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly pagination = new TablePagination();
  readonly paged = computed(() => this.pagination.slice(this.store.items()));
  readonly roles: Array<{ code: RoleCode; labelKey: string; descriptionKey: string }> = [
    { code: 'SUPER_ADMIN', labelKey: 'role.superAdmin', descriptionKey: 'role.superAdminHint' },
    { code: 'ADMIN', labelKey: 'role.admin', descriptionKey: 'role.adminHint' },
    { code: 'HR_MANAGER', labelKey: 'role.hrManager', descriptionKey: 'role.hrManagerHint' },
    { code: 'HR_REVIEWER', labelKey: 'role.hrReviewer', descriptionKey: 'role.hrReviewerHint' },
    { code: 'VIEWER', labelKey: 'role.viewer', descriptionKey: 'role.viewerHint' },
  ];
  readonly menuOptions: Array<{ id: string; labelKey: string }> = [
    { id: 'dashboard', labelKey: 'nav.dashboard' },
    { id: 'categories', labelKey: 'nav.categories' },
    { id: 'employees', labelKey: 'nav.employees' },
    { id: 'imports', labelKey: 'nav.imports' },
    { id: 'parties', labelKey: 'nav.parties' },
    { id: 'reports', labelKey: 'nav.reports' },
    { id: 'workforce-dashboard', labelKey: 'workforce.dashboard.title' },
    { id: 'workforce-contractors', labelKey: 'workforce.contractors.title' },
    { id: 'workforce-workers', labelKey: 'workforce.workers.title' },
    { id: 'workforce-categories', labelKey: 'workforce.categories.title' },
    { id: 'workforce-requests', labelKey: 'workforce.laborRequests.title' },
    { id: 'workforce-attendance', labelKey: 'workforce.attendance.title' },
    { id: 'workforce-settlements', labelKey: 'workforce.settlements.title' },
    { id: 'workforce-advances', labelKey: 'workforce.advances.title' },
    { id: 'workforce-accounts', labelKey: 'workforce.accounts.title' },
    { id: 'workforce-reports', labelKey: 'workforce.reports.title' },
    { id: 'operations', labelKey: 'nav.operations' },
    { id: 'procurement', labelKey: 'nav.procurement' },
    { id: 'sales', labelKey: 'nav.sales' },
    { id: 'production', labelKey: 'nav.production' },
    { id: 'quality', labelKey: 'nav.quality' },
    { id: 'payroll', labelKey: 'nav.payroll' },
    { id: 'accounts', labelKey: 'nav.accounts' },
    { id: 'journal-entries', labelKey: 'nav.journalEntries' },
    { id: 'banks', labelKey: 'nav.banks' },
    { id: 'tax-currency', labelKey: 'nav.taxCurrency' },
    { id: 'fiscal-periods', labelKey: 'nav.fiscalPeriods' },
    { id: 'organization', labelKey: 'nav.organization' },
    { id: 'audit-logs', labelKey: 'nav.auditLogs' },
    { id: 'users', labelKey: 'nav.users' },
    { id: 'settings', labelKey: 'settings.title' },
  ];
  readonly menuGroups = [
    {
      titleKey: 'users.groupPeople',
      ids: ['employees', 'categories', 'imports', 'organization']
    },
    {
      titleKey: 'users.groupWorkforce',
      ids: [
        'workforce-dashboard', 'workforce-contractors', 'workforce-workers',
        'workforce-categories', 'workforce-requests', 'workforce-attendance',
        'workforce-settlements', 'workforce-advances', 'workforce-accounts', 'workforce-reports'
      ]
    },
    {
      titleKey: 'users.groupOperations',
      ids: ['operations', 'production', 'quality']
    },
    {
      titleKey: 'users.groupTrade',
      ids: ['procurement', 'sales', 'parties']
    },
    {
      titleKey: 'users.groupPayroll',
      ids: ['payroll']
    },
    {
      titleKey: 'users.groupFinance',
      ids: ['accounts', 'journal-entries', 'banks', 'tax-currency', 'fiscal-periods']
    },
    {
      titleKey: 'users.groupAdministration',
      ids: ['dashboard', 'reports', 'audit-logs', 'users', 'settings']
    }
  ];

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
    const current = new Set(this.form.controls.allowedMenus.value);
    if (this.isModuleAllSelected(ids)) {
      ids.forEach((id) => current.delete(id));
    } else {
      ids.forEach((id) => current.add(id));
    }
    this.form.controls.allowedMenus.setValue(Array.from(current));
  }

  getMenuLabel(id: string): string {
    const option = this.menuOptions.find((item) => item.id === id);
    return option ? this.i18n.t(option.labelKey) : id;
  }

  selectAllMenus(): void {
    const allIds = this.menuOptions.map((o) => o.id);
    this.form.controls.allowedMenus.setValue(allIds);
  }

  clearAllMenus(): void {
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
  }

  private async loadPolicy(): Promise<void> {
    try {
      const settings = await firstValueFrom(this.auth.appSettings());
      this.passwordPolicy.set(settings);
    } catch {}
  }

  roleUserCount(code: RoleCode): number {
    return this.store.items().filter((user) => user.roles.includes(code)).length;
  }

  openNew() {
    this.submitted.set(false);
    this.showPassword.set(false);
    this.editingId.set(null);
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
    this.drawerOpen.set(true);
  }

  openEdit(item: AuthUser) {
    this.submitted.set(false);
    this.showPassword.set(false);
    this.editingId.set(item.id);
    this.form.reset({
      username: item.username,
      displayName: item.displayName,
      password: '',
      roles: item.roles,
      allowedMenus: item.allowedMenus ?? this.menuOptions.map((m) => m.id),
      canViewSalary: item.canViewSalary ?? true,
      dashboardCustomizationEnabled: item.dashboardCustomizationEnabled ?? true,
      active: item.active,
      version: item.version,
      categoryId: item.categoryId ?? null,
    });
    this.drawerOpen.set(true);
  }

  allowedMenuCount(item: AuthUser): number {
    if (item.roles.includes('SUPER_ADMIN')) {
      return this.menuOptions.length;
    }
    return item.allowedMenus ? item.allowedMenus.length : this.menuOptions.length;
  }

  toggleRole(code: RoleCode, event: Event) {
    const checked = (event.target as HTMLInputElement).checked;
    const current = this.form.controls.roles.value;
    this.form.controls.roles.setValue(
      checked ? [...current, code] : current.filter((item) => item !== code),
    );
  }

  hasRole(code: RoleCode) {
    return this.form.controls.roles.value.includes(code);
  }

  toggleMenu(id: string, event: Event) {
    const checked = (event.target as HTMLInputElement).checked;
    const current = this.form.controls.allowedMenus.value;
    this.form.controls.allowedMenus.setValue(
      checked ? [...current, id] : current.filter((item) => item !== id),
    );
  }

  hasMenu(id: string) {
    return this.form.controls.allowedMenus.value.includes(id);
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

  async submit() {
    this.submitted.set(true);
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
    const raw = this.form.getRawValue();
    const payload: UserPayload = { ...raw, password: raw.password || null };
    if (await this.store.save(this.editingId(), payload)) {
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.closeDrawer();
    }
  }

  roleLabel(code: RoleCode) {
    const role = this.roles.find((item) => item.code === code);
    return role ? this.i18n.t(role.labelKey) : code;
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
    this.submitted.set(false);
  }

  exportCsv(): void {
    const rows = this.paged().map((user) => ({
      username: user.username,
      displayName: user.displayName,
      roles: user.roles.join(', '),
      allowedMenus: user.allowedMenus?.length ?? 0,
      active: user.active ? 'نشط' : 'غير نشط',
    }));
    exportCsv(
      rows,
      [
        { key: 'username', label: 'اسم المستخدم' },
        { key: 'displayName', label: 'الاسم المعروض' },
        { key: 'roles', label: 'الأدوار' },
        { key: 'allowedMenus', label: 'عدد الصلاحيات' },
        { key: 'active', label: 'الحالة' },
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
