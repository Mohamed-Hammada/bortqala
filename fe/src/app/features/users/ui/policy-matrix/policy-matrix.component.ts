import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../../../core/i18n.service';
import { ModulePermissionTreeDto, PermissionDto, PolicyCatalogResponse } from '../../../../core/auth/security-policy.models';

@Component({
  selector: 'app-policy-matrix',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './policy-matrix.component.html',
  styleUrls: ['./policy-matrix.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PolicyMatrixComponent {
  readonly i18n = inject(I18nService);

  @Input() set catalog(value: PolicyCatalogResponse | null) {
    this.catalogSignal.set(value);
  }

  @Input() set selectedPermissionKeys(value: string[] | null | undefined) {
    this.selectedKeysSignal.set(new Set(value || []));
  }

  @Input() readOnly = false;

  @Output() readonly selectedPermissionKeysChange = new EventEmitter<string[]>();

  readonly catalogSignal = signal<PolicyCatalogResponse | null>(null);
  readonly selectedKeysSignal = signal<Set<string>>(new Set());
  readonly searchTerm = signal<string>('');

  readonly filteredModules = computed(() => {
    const cat = this.catalogSignal();
    if (!cat) return [];
    const term = this.searchTerm().trim().toLowerCase();
    if (!term) return cat.modules;

    return cat.modules
      .map((mod) => {
        const matchingPerms = mod.permissions.filter(
          (p) =>
            p.permissionKey.toLowerCase().includes(term) ||
            p.module.toLowerCase().includes(term) ||
            (p.submodule && p.submodule.toLowerCase().includes(term)) ||
            (p.descriptionKey && this.i18n.t(p.descriptionKey).toLowerCase().includes(term))
        );
        return {
          ...mod,
          permissions: matchingPerms,
        };
      })
      .filter((mod) => mod.permissions.length > 0);
  });

  readonly totalSelectedCount = computed(() => this.selectedKeysSignal().size);

  isPermissionSelected(key: string): boolean {
    return this.selectedKeysSignal().has(key);
  }

  togglePermission(key: string): void {
    if (this.readOnly) return;
    const current = new Set(this.selectedKeysSignal());
    if (current.has(key)) {
      current.delete(key);
    } else {
      current.add(key);
    }
    this.selectedKeysSignal.set(current);
    this.selectedPermissionKeysChange.emit(Array.from(current));
  }

  isModuleAllSelected(mod: ModulePermissionTreeDto): boolean {
    if (mod.permissions.length === 0) return false;
    const current = this.selectedKeysSignal();
    return mod.permissions.every((p) => current.has(p.permissionKey));
  }

  isModulePartiallySelected(mod: ModulePermissionTreeDto): boolean {
    const current = this.selectedKeysSignal();
    const count = mod.permissions.filter((p) => current.has(p.permissionKey)).length;
    return count > 0 && count < mod.permissions.length;
  }

  toggleModule(mod: ModulePermissionTreeDto): void {
    if (this.readOnly) return;
    const current = new Set(this.selectedKeysSignal());
    const allSelected = this.isModuleAllSelected(mod);

    if (allSelected) {
      mod.permissions.forEach((p) => current.delete(p.permissionKey));
    } else {
      mod.permissions.forEach((p) => current.add(p.permissionKey));
    }

    this.selectedKeysSignal.set(current);
    this.selectedPermissionKeysChange.emit(Array.from(current));
  }

  selectAll(): void {
    if (this.readOnly) return;
    const cat = this.catalogSignal();
    if (!cat) return;
    const allKeys = new Set<string>();
    cat.modules.forEach((mod) => {
      mod.permissions.forEach((p) => allKeys.add(p.permissionKey));
    });
    this.selectedKeysSignal.set(allKeys);
    this.selectedPermissionKeysChange.emit(Array.from(allKeys));
  }

  deselectAll(): void {
    if (this.readOnly) return;
    this.selectedKeysSignal.set(new Set());
    this.selectedPermissionKeysChange.emit([]);
  }

  applyPresetTier(tier: 'VIEW_ONLY' | 'MANAGE' | 'APPROVER' | 'FULL_CONTROL' | 'CLEAR'): void {
    if (this.readOnly) return;
    const cat = this.catalogSignal();
    if (!cat) return;

    if (tier === 'CLEAR') {
      this.deselectAll();
      return;
    }
    if (tier === 'FULL_CONTROL') {
      this.selectAll();
      return;
    }

    const newKeys = new Set<string>();
    cat.modules.forEach((mod) => {
      mod.permissions.forEach((p) => {
        const action = (p.action || '').toLowerCase();
        const key = p.permissionKey.toLowerCase();
        if (tier === 'VIEW_ONLY') {
          if (action === 'read' || action === 'view' || action === 'list' || key.endsWith(':read') || key.endsWith(':view')) {
            newKeys.add(p.permissionKey);
          }
        } else if (tier === 'MANAGE') {
          if (['read', 'view', 'list', 'create', 'update', 'edit', 'write', 'calculate', 'import', 'export'].includes(action) ||
              !['approve', 'decide', 'delete', 'close'].includes(action)) {
            newKeys.add(p.permissionKey);
          }
        } else if (tier === 'APPROVER') {
          if (['read', 'view', 'list', 'approve', 'decide', 'review', 'verify', 'post'].includes(action) ||
              key.includes(':approve') || key.includes(':review') || key.includes(':post')) {
            newKeys.add(p.permissionKey);
          }
        }
      });
    });

    this.selectedKeysSignal.set(newKeys);
    this.selectedPermissionKeysChange.emit(Array.from(newKeys));
  }

  selectedInModuleCount(mod: ModulePermissionTreeDto): number {
    const current = this.selectedKeysSignal();
    return mod.permissions.filter((p) => current.has(p.permissionKey)).length;
  }

  getPermissionLabel(p: PermissionDto): string {
    if (p.descriptionKey) {
      const translated = this.i18n.t(p.descriptionKey);
      if (translated && translated !== p.descriptionKey) {
        return translated;
      }
    }
    return p.permissionKey;
  }
}
