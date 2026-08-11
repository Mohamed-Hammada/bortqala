#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import subprocess
import sys
from pathlib import Path

ROOT = Path.cwd()
FILES = {
    "ts": ROOT / "fe/src/app/features/users/users.page.ts",
    "html": ROOT / "fe/src/app/features/users/users.page.html",
    "scss": ROOT / "fe/src/app/features/users/users.page.scss",
    "spec": ROOT / "fe/src/app/features/users/users.page.spec.ts",
    "readme": ROOT / "fe/src/app/features/users/README.md",
}
EXPECTED_BLOB_SHAS = {
    "ts": "4f17d69a7f5b8bae06b314a48b6d1b6a54391c51",
    "html": "3ff447084b78c618e2f8a9769dc5c1b21b25e60e",
    "scss": "21c326b1ba8bb1e044deca216045c2409d925325",
    "spec": "12465c83c80e6c401d7758d9d3eeba1d00e83069",
    "readme": "c22157b2903f50efcffafb8f70250a4da8460264",
}


def git_blob_sha(path: Path) -> str:
    result = subprocess.run(
        ["git", "hash-object", str(path)],
        check=True,
        capture_output=True,
        text=True,
    )
    return result.stdout.strip()


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


def preflight() -> None:
    missing = [str(path) for path in FILES.values() if not path.exists()]
    if missing:
        raise RuntimeError(
            "Run this script from the bortqala repository root. Missing: " + ", ".join(missing)
        )

    try:
        branch = subprocess.run(
            ["git", "branch", "--show-current"],
            check=True,
            capture_output=True,
            text=True,
        ).stdout.strip()
    except Exception:
        branch = ""

    if branch and branch != "fm_bemo_consolidated":
        print(f"WARNING: current branch is {branch!r}; source was verified against 'fm_bemo_consolidated'.")

    mismatches = []
    for key, path in FILES.items():
        actual = git_blob_sha(path)
        expected = EXPECTED_BLOB_SHAS[key]
        if actual != expected:
            mismatches.append((path, expected, actual))

    if mismatches:
        details = "\n".join(
            f"  {path}: expected {expected}, got {actual}" for path, expected, actual in mismatches
        )
        raise RuntimeError(
            "Preflight stopped because one or more source files no longer match the exact "
            "fm_bemo_consolidated version reviewed on 2026-08-10.\n" + details
        )


def patch_typescript(text: str) -> str:
    text = replace_once(
        text,
        "  readonly selectedMenus = signal<string[]>([]);\n  readonly pagination = new TablePagination();",
        "  readonly selectedMenus = signal<string[]>([]);\n"
        "  // New users follow role-derived menu access until an admin manually customizes menus.\n"
        "  readonly customMenuAccess = signal(false);\n"
        "  readonly pagination = new TablePagination();",
        "add customMenuAccess signal",
    )

    text = replace_once(
        text,
        "  toggleModule(ids: string[]): void {\n    const current = new Set(this.form.controls.allowedMenus.value);",
        "  toggleModule(ids: string[]): void {\n"
        "    this.customMenuAccess.set(true);\n"
        "    const current = new Set(this.form.controls.allowedMenus.value);",
        "mark module override as custom",
    )

    text = replace_once(
        text,
        "  selectAllMenus(): void {\n    const allIds = this.menuOptions.map((o) => o.id);",
        "  selectAllMenus(): void {\n"
        "    this.customMenuAccess.set(true);\n"
        "    const allIds = this.menuOptions.map((o) => o.id);",
        "mark select-all as custom",
    )

    text = replace_once(
        text,
        "  clearAllMenus(): void {\n    this.form.controls.allowedMenus.setValue([]);",
        "  clearAllMenus(): void {\n"
        "    this.customMenuAccess.set(true);\n"
        "    this.form.controls.allowedMenus.setValue([]);",
        "mark clear-all as custom",
    )

    text = replace_once(
        text,
        "  async loadAccessCatalog(): Promise<void> {\n    this.accessLoading.set(true);\n    await this.access.loadCatalog();\n    this.accessLoading.set(false);\n  }",
        "  async loadAccessCatalog(): Promise<void> {\n"
        "    this.accessLoading.set(true);\n"
        "    await this.access.loadCatalog();\n"
        "    this.accessLoading.set(false);\n"
        "    if (this.drawerOpen() && !this.editMode() && !this.customMenuAccess()) {\n"
        "      this.syncMenusToRoles();\n"
        "    }\n"
        "  }",
        "sync menus after catalog load",
    )

    text = replace_once(
        text,
        "    this.editingId.set(null);\n    this.baselineRoles.set([]);",
        "    this.editingId.set(null);\n"
        "    this.customMenuAccess.set(false);\n"
        "    this.baselineRoles.set([]);",
        "reset custom access on create",
    )

    text = replace_once(
        text,
        "      categoryId: null,\n    });\n    this.drawerOpen.set(true);\n  }\n\n  openEdit(item: AuthUser) {",
        "      categoryId: null,\n"
        "    });\n"
        "    this.syncMenusToRoles();\n"
        "    this.drawerOpen.set(true);\n"
        "  }\n\n"
        "  openEdit(item: AuthUser) {",
        "sync create defaults",
    )

    text = replace_once(
        text,
        "    this.editingId.set(item.id);\n    this.baselineRoles.set(item.roles);",
        "    this.editingId.set(item.id);\n"
        "    // Never overwrite an existing user's explicit menu configuration while editing.\n"
        "    this.customMenuAccess.set(true);\n"
        "    this.baselineRoles.set(item.roles);",
        "preserve edit menu access",
    )

    text = replace_once(
        text,
        "  toggleRole(code: RoleCode, event: Event) {\n    const checked = (event.target as HTMLInputElement).checked;\n    const current = this.form.controls.roles.value;\n    this.form.controls.roles.setValue(\n      checked ? [...current, code] : current.filter((item) => item !== code),\n    );\n  }",
        "  toggleRole(code: RoleCode, event: Event) {\n"
        "    const checked = (event.target as HTMLInputElement).checked;\n"
        "    const current = this.form.controls.roles.value;\n"
        "    const next = checked ? [...current, code] : current.filter((item) => item !== code);\n"
        "    this.form.controls.roles.setValue(next);\n"
        "    if (!this.customMenuAccess()) {\n"
        "      this.syncMenusToRoles(next);\n"
        "    }\n"
        "  }",
        "sync roles to menus",
    )

    text = replace_once(
        text,
        "  toggleMenu(id: string, event: Event) {\n    const checked = (event.target as HTMLInputElement).checked;",
        "  toggleMenu(id: string, event: Event) {\n"
        "    this.customMenuAccess.set(true);\n"
        "    const checked = (event.target as HTMLInputElement).checked;",
        "mark single menu override as custom",
    )

    anchor = "  hasMenu(id: string) {\n    return this.form.controls.allowedMenus.value.includes(id);\n  }\n"
    helper = """  hasMenu(id: string) {
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

    const knownMenus = new Set(this.menuOptions.map((item) => item.id));
    const recommended = catalog.pages
      .filter((page) => pageCodes.has(page.code) && knownMenus.has(page.menuId))
      .map((page) => page.menuId);

    this.form.controls.allowedMenus.setValue([...new Set(recommended)]);
  }
"""
    text = replace_once(text, anchor, helper, "add role-to-menu helper")
    return text


NEW_MODAL = r'''  <!-- Simplified User Form: simple by default, detailed access on demand -->
  <app-modal-dialog
    class="users-dialog"
    [isOpen]="drawerOpen()"
    [title]="i18n.t(editingId() ? 'users.editTitle' : 'users.newTitle')"
    size="large"
    [preventOutsideClose]="true"
    (close)="closeDrawer()">

    <form [formGroup]="form" (ngSubmit)="submit()" class="modal-form simplified-user-form">
      @if (submitted() && form.invalid) {
        <div class="alert error" role="alert">{{ i18n.t('users.validation') }}</div>
      }

      <div class="user-form-stack">
        <!-- 1. Identity and account basics -->
        <section class="user-form-panel">
          <header class="panel-heading">
            <span class="panel-icon" aria-hidden="true">👤</span>
            <div>
              <strong>{{ i18n.t('users.sectionBasic') }}</strong>
            </div>
          </header>

          <div class="user-profile-grid">
            <div class="field">
              <label for="user-display-name"><span class="required">*</span> {{ i18n.t('users.displayName') }}</label>
              <input
                id="user-display-name"
                formControlName="displayName"
                autocomplete="name"
                placeholder="محمد أحمد"
              />
            </div>

            <div class="field">
              <label for="user-username"><span class="required">*</span> {{ i18n.t('users.username') }}</label>
              <input
                id="user-username"
                class="ltr"
                formControlName="username"
                autocomplete="off"
                placeholder="admin"
              />
            </div>

            <div class="field span-2 password-field">
              <label for="user-password">
                @if (!editingId()) { <span class="required">*</span> }
                {{ i18n.t('users.password') }}
              </label>
              <div class="password-input-wrap">
                <input
                  id="user-password"
                  [type]="showPassword() ? 'text' : 'password'"
                  class="ltr"
                  formControlName="password"
                  autocomplete="new-password"
                  placeholder="••••••••"
                />
                <button
                  type="button"
                  class="toggle-pass-btn"
                  (click)="showPassword.set(!showPassword())"
                  [attr.aria-label]="i18n.t(showPassword() ? 'users.hidePassword' : 'users.showPassword')"
                  [attr.title]="i18n.t(showPassword() ? 'users.hidePassword' : 'users.showPassword')"
                >
                  {{ showPassword() ? '👁️' : '🙈' }}
                </button>
              </div>
              @if (editingId()) {
                <small class="hint">{{ i18n.t('users.passwordEditHint') }}</small>
              } @else {
                <small class="hint">
                  {{ i18n.t('users.passwordMinLength', { min: passwordPolicy().minPasswordLength ?? 8 }) }}
                </small>
              }
            </div>

            <div class="field">
              <label for="user-category">{{ i18n.t('users.category') }}</label>
              <select id="user-category" formControlName="categoryId">
                <option [ngValue]="null">— {{ i18n.t('common.none') }} —</option>
                @for (cat of store.categories(); track cat.id) {
                  <option [ngValue]="cat.id">{{ cat.name }}</option>
                }
              </select>
            </div>

            <div class="field account-status-field">
              <span class="field-label">{{ i18n.t('users.sectionStatus') }}</span>
              <label class="selectable-card active-card compact-setting-card">
                <input type="checkbox" formControlName="active" />
                <span>
                  <strong>{{ i18n.t('users.activeCheck') }}</strong>
                  <small>{{ i18n.t('users.activeCheckHint') }}</small>
                </span>
              </label>
            </div>
          </div>
        </section>

        <!-- 2. Role selection: human-readable, without technical access metadata -->
        <section class="user-form-panel">
          <header class="panel-heading panel-heading-with-count">
            <span class="panel-icon" aria-hidden="true">🔑</span>
            <div>
              <strong>{{ i18n.t('users.sectionRoles') }}</strong>
              <small>{{ i18n.t('access.sectionRolesHint') }}</small>
            </div>
            <span class="selection-count">{{ form.controls.roles.value.length }}</span>
          </header>

          <div class="access-catalog-wrap">
            @if (accessLoading()) {
              <div class="loading"></div>
            } @else if (!access.catalog()) {
              <div class="alert error">{{ i18n.t('access.catalogError') }}</div>
              <button type="button" class="button secondary" (click)="loadAccessCatalog()">
                {{ i18n.t('access.catalogRetry') }}
              </button>
            } @else {
              <input
                class="search-input"
                [value]="roleSearch()"
                (input)="roleSearch.set($any($event.target).value)"
                type="search"
                [placeholder]="i18n.t('access.roleSearchPlaceholder')"
              />

              <div class="access-role-grid simple-role-grid">
                @for (role of catalogRoles(); track role.code) {
                  <label
                    class="access-role-card simple-role-card"
                    [class.role-selected]="hasRole(role.code)"
                    [class.disabled-card]="role.code === 'SUPER_ADMIN' && !auth.isSuperAdmin()"
                  >
                    <input
                      type="checkbox"
                      [checked]="hasRole(role.code)"
                      [disabled]="role.code === 'SUPER_ADMIN' && !auth.isSuperAdmin()"
                      (change)="toggleRole(role.code, $event)"
                    />
                    <span class="role-choice-copy">
                      <span class="role-choice-title">
                        <strong>{{ i18n.t(role.nameKey) }}</strong>
                        @if (hasRole(role.code)) {
                          <span class="selected-check" aria-hidden="true">✓</span>
                        }
                      </span>
                      <small class="role-desc">{{ i18n.t(role.descriptionKey) }}</small>
                      @if (hasRole(role.code) && role.sensitiveReasonKey) {
                        <small class="role-sensitive-reason">{{ i18n.t(role.sensitiveReasonKey) }}</small>
                      }
                    </span>
                  </label>
                }
              </div>

              @if (catalogRoles().length === 0) {
                <p class="hint">{{ i18n.t('access.roleSearchNoResults') }}</p>
              }
            }
          </div>
        </section>

        <!-- 3. Small account-level switches -->
        <section class="user-form-panel secondary-settings-panel">
          <header class="panel-heading">
            <span class="panel-icon" aria-hidden="true">🔒</span>
            <div>
              <strong>{{ i18n.t('users.sectionSpecialPermissions') }}</strong>
            </div>
          </header>

          <div class="user-settings-grid">
            <label class="selectable-card menu-card compact-setting-card">
              <input type="checkbox" formControlName="canViewSalary" />
              <span>💵 {{ i18n.t('users.canViewSalary') }}</span>
            </label>

            <label class="selectable-card menu-card compact-setting-card">
              <input type="checkbox" formControlName="dashboardCustomizationEnabled" />
              <span>
                <strong>📊 {{ i18n.t('users.dashboardCustomization') }}</strong>
                <small>{{ i18n.t('users.dashboardCustomizationHint') }}</small>
              </span>
            </label>
          </div>
        </section>

        <!-- Advanced menu-level access remains available without blocking the normal flow. -->
        <details class="advanced-access-panel">
          <summary>
            <span class="advanced-summary-icon" aria-hidden="true">🖥️</span>
            <span class="advanced-summary-copy">
              <strong>{{ i18n.t('users.sectionMenus') }}</strong>
              <small>{{ i18n.t('users.menuGroupsHint') }}</small>
            </span>
            <span class="advanced-summary-meta ltr">
              {{ form.controls.allowedMenus.value.length }} / {{ menuOptions.length }}
            </span>
          </summary>

          <div class="advanced-access-content">
            <div class="quick-actions advanced-quick-actions">
              <button
                type="button"
                class="permission-action"
                (click)="selectAllMenus()"
                [disabled]="adminOverrideActive()"
                [attr.title]="adminOverrideActive() ? i18n.t('access.adminOverrideNotice') : ''"
              >
                {{ i18n.t('users.selectAllMenus') }}
              </button>
              <button
                type="button"
                class="permission-action"
                (click)="clearAllMenus()"
                [disabled]="adminOverrideActive()"
                [attr.title]="adminOverrideActive() ? i18n.t('access.adminOverrideNotice') : ''"
              >
                {{ i18n.t('users.clearAllMenus') }}
              </button>
            </div>

            @if (adminOverrideActive()) {
              <div class="alert info admin-override-notice">
                {{ i18n.t('access.adminOverrideNotice') }}
              </div>
            }

            <div class="module-groups-grid">
              @for (group of menuGroups; track group.titleKey) {
                <div class="module-group-card">
                  <label class="module-group-header">
                    <input
                      type="checkbox"
                      [checked]="isModuleAllSelected(group.ids)"
                      [indeterminate]="isModulePartiallySelected(group.ids)"
                      [disabled]="adminOverrideActive()"
                      (change)="toggleModule(group.ids)"
                    />
                    <strong>{{ i18n.t(group.titleKey) }}</strong>
                    <span class="count-badge">
                      {{ i18n.t(group.ids.length === 1 ? 'users.permissionOne' : 'users.permissionCount', { count: group.ids.length }) }}
                    </span>
                  </label>
                  <div class="module-group-items">
                    @for (id of group.ids; track id) {
                      <label class="menu-item-check" [class.menu-disabled]="adminOverrideActive()">
                        <input
                          type="checkbox"
                          [checked]="hasMenu(id)"
                          [disabled]="adminOverrideActive()"
                          (change)="toggleMenu(id, $event)"
                        />
                        <span>{{ getMenuLabel(id) }}</span>
                        @if (menuFeature(id)) {
                          <span class="chip chip-warn" [attr.title]="menuFeature(id)">{{ i18n.t('access.menuFeatureDisabled') }}</span>
                        } @else if (menuRoleMismatch(id)) {
                          <span class="chip chip-warn">{{ i18n.t('access.menuRoleMismatch') }}</span>
                        }
                      </label>
                    }
                  </div>
                </div>
              }
            </div>

            <details class="effective-access-panel">
              <summary>
                <span>🧮 {{ i18n.t('access.sectionEffective') }}</span>
                <small>{{ i18n.t('access.effectiveHint') }}</small>
              </summary>

              <div class="effective-access-content">
                @if (editMode()) {
                  <div class="diff-box">
                    <strong>{{ i18n.t('access.diffTitle') }}</strong>
                    @if (rolesAdded().length || rolesRemoved().length || menusAdded().length || menusRemoved().length) {
                      <div class="diff-chips">
                        @for (role of rolesAdded(); track role) {
                          <span class="chip chip-added">＋ {{ roleLabelFor(role) }}</span>
                        }
                        @for (role of rolesRemoved(); track role) {
                          <span class="chip chip-removed">－ {{ roleLabelFor(role) }}</span>
                        }
                        @for (menu of menusAdded(); track menu) {
                          <span class="chip chip-added">＋ {{ getMenuLabel(menu) }}</span>
                        }
                        @for (menu of menusRemoved(); track menu) {
                          <span class="chip chip-removed">－ {{ getMenuLabel(menu) }}</span>
                        }
                      </div>
                      @if (changedPages().length) {
                        <div class="changed-pages">
                          @for (row of changedPages(); track row.page.pageCode) {
                            <div class="changed-page-row">
                              <span>{{ i18n.t(row.titleKey) }}</span>
                              <code class="ltr diff-before">{{ row.before }}</code>
                              <span class="diff-arrow">→</span>
                              <code class="ltr diff-after">{{ row.page.access }}</code>
                            </div>
                          }
                        </div>
                      }
                    } @else {
                      <p class="hint">{{ i18n.t('access.diffUnchanged') }}</p>
                    }
                  </div>
                }

                <div class="preview-table-wrap">
                  <table class="preview-table">
                    <thead>
                      <tr>
                        <th>{{ i18n.t('access.previewModule') }}</th>
                        <th>{{ i18n.t('access.previewPage') }}</th>
                        <th>{{ i18n.t('access.previewAccess') }}</th>
                        <th>{{ i18n.t('access.previewGrantedBy') }}</th>
                        <th>{{ i18n.t('access.previewActions') }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      @for (row of preview().pages; track row.pageCode) {
                        <tr>
                          <td class="ltr">{{ moduleLabel(row.pageCode) }}</td>
                          <td>{{ pageTitle(row.pageCode) }}</td>
                          <td>
                            <span class="access-badge" [class]="row.access.toLowerCase()">{{ levelLabel(row.access) }}</span>
                          </td>
                          <td>
                            <div class="chip-row inline">
                              @for (role of row.grantedByRoles; track role) {
                                <span class="chip">{{ roleLabelFor(role) }}</span>
                              } @empty {
                                <span class="hint">—</span>
                              }
                            </div>
                          </td>
                          <td>
                            <div class="chip-row inline">
                              @for (action of row.grantedActions; track action) {
                                <span class="chip chip-action">{{ actionLabel(action) }}</span>
                              } @empty {
                                <span class="hint">—</span>
                              }
                            </div>
                          </td>
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>

                @if (preview().warnings.length || preview().conflicts.length) {
                  <div class="warnings-box">
                    <strong>⚠️ {{ i18n.t('access.sectionWarnings') }}</strong>
                    @for (warning of preview().warnings; track warning.code) {
                      <p class="hint">{{ i18n.t(warning.messageKey) }}</p>
                    }
                    @for (conflict of preview().conflicts; track conflict.code) {
                      <p class="hint conflict-text">{{ i18n.t(conflict.reasonKey) }}</p>
                    }
                  </div>
                }
              </div>
            </details>
          </div>
        </details>

        @if (validationRunning() || validationError() || validationResult()) {
          <div class="validation-box compact-validation-box">
            @if (validationRunning()) {
              <p class="hint">{{ i18n.t('access.validateRunning') }}</p>
            } @else if (validationError()) {
              <div class="alert error">{{ validationError() }}</div>
            } @else if (validationResult()) {
              @if (validationOk()) {
                <p class="validation-ok">✅ {{ i18n.t('access.validateOk') }}</p>
              } @else {
                @if (validationResult()!.errors.length) {
                  <div class="warnings-box">
                    @for (error of validationResult()!.errors; track error.code) {
                      <p class="hint conflict-text">{{ validateErrorLabel(error.code) }}</p>
                    }
                  </div>
                }
                @if (validationResult()!.conflicts.length) {
                  <div class="warnings-box">
                    @for (conflict of validationResult()!.conflicts; track conflict.code) {
                      <p class="hint conflict-text">{{ i18n.t(conflict.reasonKey) }}</p>
                    }
                  </div>
                }
                @if (validationResult()!.warnings.length) {
                  <div class="warnings-box">
                    @for (warning of validationResult()!.warnings; track warning.code) {
                      <p class="hint">{{ i18n.t(warning.messageKey) }}</p>
                    }
                  </div>
                }
                @if (validationNeedsAck()) {
                  <div class="ack-reason-box">
                    <label for="ack-reason" class="ack-reason-label">
                      {{ i18n.t('access.ackReasonLabel') }}
                    </label>
                    <input
                      id="ack-reason"
                      type="text"
                      [value]="ackReason()"
                      (input)="ackReason.set($any($event.target).value)"
                      [placeholder]="i18n.t('access.ackReasonPlaceholder')"
                    />
                    @if (!validationAckSatisfied()) {
                      <small class="hint warning-text">{{ i18n.t('access.ackReasonRequired') }}</small>
                    }
                  </div>
                }
              }
            }
          </div>
        }
      </div>
    </form>

    <div modal-actions class="user-modal-actions">
      <button
        class="button primary"
        type="button"
        (click)="submit()"
        [disabled]="store.loading() || validationRunning() || catalogUnavailable()"
      >
        {{ i18n.t('users.save') }}
      </button>
      <button class="button secondary" type="button" (click)="closeDrawer()">
        {{ i18n.t('common.cancel') }}
      </button>
    </div>
  </app-modal-dialog>
'''


def patch_html(text: str) -> str:
    start_marker = "  <!-- User Form Central Modal Dialog -->"
    end_marker = "\n</section>"
    start = text.find(start_marker)
    end = text.rfind(end_marker)
    if start == -1 or end == -1 or end <= start:
        raise RuntimeError("users.page.html: could not locate modal replacement boundaries")
    return text[:start] + NEW_MODAL + text[end:]


SCSS_APPEND = r'''

/* ------------------------------------------------------------------ */
/* Simplified user create/edit experience — 2026-08-10               */
/* ------------------------------------------------------------------ */

.users-dialog {
  --modal-wide-max-width: 880px;
  --modal-body-padding: 16px 20px 20px;
}

.simplified-user-form {
  width: 100%;
}

.user-form-stack {
  display: grid;
  gap: 14px;
}

.user-form-panel,
.advanced-access-panel {
  border: 1px solid var(--line);
  border-radius: 14px;
  background: var(--surface);
  box-shadow: 0 1px 0 color-mix(in srgb, var(--line) 55%, transparent);
}

.user-form-panel {
  padding: 16px;
}

.panel-heading {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.panel-heading-with-count {
  grid-template-columns: 34px minmax(0, 1fr) auto;
}

.panel-heading strong,
.panel-heading small {
  display: block;
}

.panel-heading strong {
  color: var(--ink);
  font-size: 14px;
}

.panel-heading small {
  margin-top: 2px;
  color: var(--muted);
  font-size: 11.5px;
  line-height: 1.4;
}

.panel-icon {
  display: inline-flex;
  width: 34px;
  height: 34px;
  align-items: center;
  justify-content: center;
  border: 1px solid color-mix(in srgb, var(--gold) 40%, var(--line));
  border-radius: 10px;
  background: var(--gold-glow);
}

.selection-count {
  display: inline-flex;
  min-width: 28px;
  height: 28px;
  align-items: center;
  justify-content: center;
  padding: 0 8px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--surface-muted);
  color: var(--secondary-text);
  font-size: 12px;
  font-weight: 700;
}

.user-profile-grid,
.user-settings-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
}

.user-profile-grid .field,
.user-settings-grid > * {
  min-width: 0;
}

.field-label {
  display: block;
  margin-bottom: 6px;
  color: var(--secondary-text);
  font-size: 12.5px;
  font-weight: 700;
}

.account-status-field {
  align-self: end;
}

.compact-setting-card {
  min-height: 46px;
  margin: 0;
}

.simple-role-grid {
  gap: 10px;
}

.simple-role-card {
  min-height: 88px;
  padding: 12px;
  border-radius: 12px;
}

.simple-role-card:hover:not(.disabled-card) {
  border-color: color-mix(in srgb, var(--gold) 48%, var(--line));
}

.simple-role-card.role-selected {
  border-color: var(--gold);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--gold) 45%, transparent);
}

.role-choice-copy {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.role-choice-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.role-choice-title strong {
  color: var(--ink);
  font-size: 13px;
}

.selected-check {
  display: inline-flex;
  width: 22px;
  height: 22px;
  flex: 0 0 22px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--gold);
  color: #0f172a;
  font-size: 12px;
  font-weight: 900;
}

.advanced-access-panel {
  overflow: hidden;
}

.advanced-access-panel > summary,
.effective-access-panel > summary {
  cursor: pointer;
  list-style: none;
}

.advanced-access-panel > summary::-webkit-details-marker,
.effective-access-panel > summary::-webkit-details-marker {
  display: none;
}

.advanced-access-panel > summary {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 64px;
  padding: 12px 16px;
  background: var(--surface-muted);
}

.advanced-access-panel > summary::after,
.effective-access-panel > summary::after {
  content: '⌄';
  color: var(--muted);
  font-size: 18px;
  transition: transform 0.15s ease;
}

.advanced-access-panel[open] > summary::after,
.effective-access-panel[open] > summary::after {
  transform: rotate(180deg);
}

.advanced-summary-icon {
  display: inline-flex;
  width: 34px;
  height: 34px;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: var(--surface);
}

.advanced-summary-copy {
  min-width: 0;
}

.advanced-summary-copy strong,
.advanced-summary-copy small {
  display: block;
}

.advanced-summary-copy strong {
  color: var(--ink);
  font-size: 13.5px;
}

.advanced-summary-copy small {
  margin-top: 2px;
  color: var(--muted);
  font-size: 11.5px;
  line-height: 1.35;
}

.advanced-summary-meta {
  grid-column: 3;
  grid-row: 1;
  margin-inline-end: 30px;
  padding: 3px 8px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--surface);
  color: var(--secondary-text);
  font-size: 11px;
  font-weight: 700;
}

.advanced-access-panel > summary::after {
  grid-column: 3;
  grid-row: 1;
  justify-self: end;
}

.advanced-access-content {
  display: grid;
  gap: 12px;
  padding: 14px;
  border-top: 1px solid var(--line);
}

.advanced-quick-actions {
  margin-inline-start: 0;
}

.effective-access-panel {
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--surface-muted);
}

.effective-access-panel > summary {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
}

.effective-access-panel > summary > span,
.effective-access-panel > summary > small {
  display: block;
}

.effective-access-panel > summary > span {
  color: var(--ink);
  font-size: 12.5px;
  font-weight: 700;
}

.effective-access-panel > summary > small {
  grid-column: 1;
  color: var(--muted);
  font-size: 11px;
}

.effective-access-panel > summary::after {
  grid-column: 2;
  grid-row: 1 / span 2;
}

.effective-access-content {
  padding: 12px;
  border-top: 1px solid var(--line);
  background: var(--surface);
}

.compact-validation-box {
  margin-top: 0;
}

@media (max-width: 700px) {
  .users-dialog {
    --modal-body-padding: 12px;
  }

  .user-form-panel {
    padding: 12px;
  }

  .user-profile-grid,
  .user-settings-grid,
  .simple-role-grid {
    grid-template-columns: 1fr;
  }

  .user-profile-grid .span-2 {
    grid-column: auto;
  }

  .advanced-access-panel > summary {
    grid-template-columns: 34px minmax(0, 1fr) auto;
    padding: 10px 12px;
  }

  .advanced-summary-meta {
    display: none;
  }

  .advanced-access-content {
    padding: 10px;
  }
}
'''


def patch_scss(text: str) -> str:
    marker = "/* Simplified user create/edit experience — 2026-08-10 */"
    if marker in text:
        raise RuntimeError("users.page.scss already appears to contain this redesign")
    return text.rstrip() + SCSS_APPEND + "\n"


def patch_spec(text: str) -> str:
    anchor = """  it('counts all menus for ADMIN because runtime admin access bypasses menu selection', () => {
    const user = {
      roles: ['ADMIN'],
      allowedMenus: ['dashboard'],
    } as unknown as AuthUser;

    expect(page.allowedMenuCount(user)).toBe(page.menuOptions.length);
  });

"""
    addition = anchor + """  it('derives menu access from selected roles for a new user until menus are customized', () => {
    page.openNew();
    page.form.controls.roles.setValue([]);
    page.form.controls.allowedMenus.setValue([]);

    page.toggleRole(
      'WORKFORCE_MANAGER',
      { target: { checked: true } } as unknown as Event,
    );

    expect(page.form.controls.roles.value).toEqual(['WORKFORCE_MANAGER']);
    expect(page.form.controls.allowedMenus.value).toEqual(['workforce-workers']);
  });

  it('preserves manual menu overrides when roles change', () => {
    page.openNew();
    page.toggleMenu('reports', { target: { checked: true } } as unknown as Event);

    page.toggleRole(
      'WORKFORCE_MANAGER',
      { target: { checked: true } } as unknown as Event,
    );

    expect(page.customMenuAccess()).toBe(true);
    expect(page.form.controls.allowedMenus.value).toContain('reports');
    expect(page.form.controls.allowedMenus.value).not.toContain('workforce-workers');
  });

"""
    return replace_once(text, anchor, addition, "add role-driven menu tests")


def patch_readme(text: str) -> str:
    old = """**EN:** The create/edit user dialog uses a compact responsive two-column layout. Roles and menu permissions are grouped into accessible selection cards, with translated labels, module-level selection, select/clear-all actions, a fixed action footer, and keyboard-first focus.

**AR:** تستخدم نافذة إنشاء وتعديل المستخدم تخطيطاً مدمجاً ومتجاوباً من عمودين. جُمعت الأدوار وصلاحيات القوائم في بطاقات اختيار واضحة ومتاحة بلوحة المفاتيح، مع تسميات مترجمة واختيار على مستوى الوحدة وأوامر تحديد/إلغاء الكل وتذييل ثابت للحفظ.
"""
    new = """**EN:** The create/edit user dialog is now simple by default: identity/account fields first, then human-readable role cards without technical role codes, sensitivity tags, guided-role duplication, or page-search noise. For newly created users, allowed menus follow the selected backend role catalog automatically until an administrator explicitly changes a menu. Existing users keep their saved menu configuration when edited.

**AR:** أصبحت نافذة إنشاء وتعديل المستخدم بسيطة بشكل افتراضي: بيانات الحساب أولاً، ثم بطاقات أدوار مفهومة بدون أكواد تقنية أو مستويات حساسية أو تكرار وضع الإرشاد أو البحث بالصفحات. عند إنشاء مستخدم جديد تتبع القوائم المسموحة الأدوار المختارة تلقائياً من كتالوج الصلاحيات في الخادم إلى أن يغيّر المدير صلاحية قائمة يدوياً. أما المستخدمون الحاليون فتظل إعدادات القوائم المحفوظة لديهم كما هي عند التعديل.

**EN:** Menu-by-menu permissions, effective-access preview, role/menu mismatch indicators, sensitive-access warnings, and access-change acknowledgment remain available inside a collapsed advanced-access section. Backend access validation still runs before save, so the UX is simpler without weakening authorization controls.

**AR:** تظل صلاحيات القوائم التفصيلية ومعاينة الصلاحيات الفعلية وتنبيهات تعارض الدور مع القائمة وتحذيرات الصلاحيات الحساسة وسبب الإقرار بالتغيير متاحة داخل قسم صلاحيات متقدمة قابل للفتح. ويستمر التحقق من الصلاحيات في الخادم قبل الحفظ، لذلك تم تبسيط تجربة الاستخدام بدون إضعاف ضوابط الأمان.
"""
    return replace_once(text, old, new, "update feature README")


def main() -> int:
    try:
        preflight()
        originals = {key: path.read_text(encoding="utf-8") for key, path in FILES.items()}
        patched = {
            "ts": patch_typescript(originals["ts"]),
            "html": patch_html(originals["html"]),
            "scss": patch_scss(originals["scss"]),
            "spec": patch_spec(originals["spec"]),
            "readme": patch_readme(originals["readme"]),
        }

        for key, path in FILES.items():
            path.write_text(patched[key], encoding="utf-8", newline="\n")

        print("Applied Add/Edit User UX redesign successfully.")
        print("Changed files:")
        for path in FILES.values():
            print(f"  - {path.relative_to(ROOT)}")
        print("\nRecommended verification:")
        print("  cd fe")
        print("  npm run check:hardcoded")
        print("  npm run check:i18n")
        print("  npm test -- --watch=false")
        print("  npm run build")
        print("\nReview with: git diff -- fe/src/app/features/users")
        return 0
    except Exception as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
