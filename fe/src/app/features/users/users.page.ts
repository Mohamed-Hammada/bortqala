import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthUser, RoleCode } from '../../core/auth/auth.models';
import { UserPayload, UsersStore } from './users.store';
import { TablePagination } from '../../shared/ui/table-pagination/pagination';
import { TablePaginationComponent } from '../../shared/ui/table-pagination/table-pagination.component';
import { I18nService } from '../../core/i18n.service';
@Component({
  selector: 'app-users-page',
  imports: [ReactiveFormsModule, TablePaginationComponent],
  providers: [UsersStore],
  templateUrl: './users.page.html',
  styleUrl: './users.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UsersPage {
  readonly store = inject(UsersStore);
  readonly i18n = inject(I18nService);
  readonly drawerOpen = signal(false);
  readonly submitted = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly pagination = new TablePagination();
  readonly paged = computed(() => this.pagination.slice(this.store.items()));
  readonly roles: Array<{ code: RoleCode; labelKey: string; descriptionKey: string }> = [
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
    { id: 'operations', labelKey: 'nav.operations' },
    { id: 'users', labelKey: 'nav.users' },
    { id: 'settings', labelKey: 'settings.title' },
  ];
  readonly form = new FormGroup({
    username: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    displayName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.minLength(10)] }),
    roles: new FormControl<RoleCode[]>([], {
      nonNullable: true,
      validators: [Validators.required],
    }),
    allowedMenus: new FormControl<string[]>([], { nonNullable: true }),
    active: new FormControl(true, { nonNullable: true }),
    version: new FormControl<number | null>(null),
  });
  constructor() {
    void this.store.load();
  }
  openNew() {
    this.submitted.set(false);
    this.editingId.set(null);
    this.form.reset({
      username: '',
      displayName: '',
      password: '',
      roles: ['VIEWER'],
      allowedMenus: ['dashboard', 'reports'],
      active: true,
      version: null,
    });
    this.drawerOpen.set(true);
  }
  openEdit(item: AuthUser) {
    this.submitted.set(false);
    this.editingId.set(item.id);
    this.form.reset({
      username: item.username,
      displayName: item.displayName,
      password: '',
      roles: item.roles,
      allowedMenus: item.allowedMenus ?? ['dashboard', 'categories', 'employees', 'imports', 'parties', 'reports', 'operations', 'users', 'settings'],
      active: item.active,
      version: item.version,
    });
    this.drawerOpen.set(true);
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
  async submit() {
    this.submitted.set(true);
    if (
      this.form.invalid ||
      !this.form.controls.roles.value.length ||
      (!this.editingId() && !this.form.controls.password.value)
    ) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const payload: UserPayload = { ...raw, password: raw.password || null };
    if (await this.store.save(this.editingId(), payload)) this.closeDrawer();
  }
  roleLabel(code: RoleCode) {
    const role = this.roles.find((item) => item.code === code);
    return role ? this.i18n.t(role.labelKey) : code;
  }
  closeDrawer(): void {
    this.drawerOpen.set(false);
    this.submitted.set(false);
  }
  @HostListener('document:keydown.escape') onEscape(): void {
    if (this.drawerOpen()) this.closeDrawer();
  }
}
