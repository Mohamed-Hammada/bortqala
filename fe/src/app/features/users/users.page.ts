import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthUser, RoleCode } from '../../core/auth/auth.models';
import { UserPayload, UsersStore } from './users.store';
@Component({
  selector: 'app-users-page',
  imports: [ReactiveFormsModule],
  providers: [UsersStore],
  templateUrl: './users.page.html',
  styleUrl: './users.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UsersPage {
  readonly store = inject(UsersStore);
  readonly drawerOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly roles: Array<{ code: RoleCode; label: string; description: string }> = [
    { code: 'ADMIN', label: 'مدير النظام', description: 'المستخدمون وكل الوظائف' },
    { code: 'HR_MANAGER', label: 'مدير HR', description: 'الإعدادات والاعتماد' },
    { code: 'HR_REVIEWER', label: 'مراجع', description: 'البصمة والاستثناءات' },
    { code: 'VIEWER', label: 'مشاهد', description: 'قراءة وتقارير فقط' },
  ];
  readonly form = new FormGroup({
    username: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    displayName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.minLength(10)] }),
    roles: new FormControl<RoleCode[]>([], {
      nonNullable: true,
      validators: [Validators.required],
    }),
    active: new FormControl(true, { nonNullable: true }),
    version: new FormControl<number | null>(null),
  });
  constructor() {
    void this.store.load();
  }
  openNew() {
    this.editingId.set(null);
    this.form.reset({
      username: '',
      displayName: '',
      password: '',
      roles: ['VIEWER'],
      active: true,
      version: null,
    });
    this.drawerOpen.set(true);
  }
  openEdit(item: AuthUser) {
    this.editingId.set(item.id);
    this.form.reset({
      username: item.username,
      displayName: item.displayName,
      password: '',
      roles: item.roles,
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
  async submit() {
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
    if (await this.store.save(this.editingId(), payload)) this.drawerOpen.set(false);
  }
  roleLabel(code: RoleCode) {
    return this.roles.find((role) => role.code === code)?.label ?? code;
  }
}
