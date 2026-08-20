import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../../../core/i18n.service';
import { PolicyService } from '../../../../core/auth/policy.service';
import {
  PolicyCatalogResponse,
  PolicyGroupDetailDto,
  PolicyGroupSummaryDto,
} from '../../../../core/auth/security-policy.models';
import { PolicyMatrixComponent } from '../../ui/policy-matrix/policy-matrix.component';

@Component({
  selector: 'app-policy-groups-page',
  standalone: true,
  imports: [CommonModule, FormsModule, PolicyMatrixComponent],
  templateUrl: './policy-groups.page.html',
  styleUrls: ['./policy-groups.page.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PolicyGroupsPageComponent implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly policyService = inject(PolicyService);

  readonly loading = signal<boolean>(true);
  readonly saving = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly groups = signal<PolicyGroupSummaryDto[]>([]);
  readonly catalog = signal<PolicyCatalogResponse | null>(null);

  // Editor Drawer State
  readonly drawerOpen = signal<boolean>(false);
  readonly editingGroupId = signal<string | null>(null);
  readonly editingGroupName = signal<string>('');
  readonly editingDescription = signal<string>('');
  readonly editingPermissionKeys = signal<string[]>([]);
  readonly editingIsSystem = signal<boolean>(false);
  readonly editingVersion = signal<number | undefined>(undefined);

  ngOnInit(): void {
    this.loadCatalog();
    this.loadGroups();
  }

  loadCatalog(): void {
    this.policyService.getCatalog().subscribe({
      next: (res) => this.catalog.set(res),
      error: (err) => console.warn('Failed to load permission catalog', err),
    });
  }

  loadGroups(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.policyService.listPolicyGroups().subscribe({
      next: (res) => {
        this.groups.set(res);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.message || 'Failed to load policy groups');
        this.loading.set(false);
      },
    });
  }

  openCreateDrawer(): void {
    this.editingGroupId.set(null);
    this.editingGroupName.set('');
    this.editingDescription.set('');
    this.editingPermissionKeys.set([]);
    this.editingIsSystem.set(false);
    this.editingVersion.set(undefined);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.drawerOpen.set(true);
  }

  openEditDrawer(group: PolicyGroupSummaryDto): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.policyService.getPolicyGroup(group.id).subscribe({
      next: (detail: PolicyGroupDetailDto) => {
        this.editingGroupId.set(detail.id);
        this.editingGroupName.set(detail.groupName);
        this.editingDescription.set(detail.description || '');
        this.editingPermissionKeys.set(detail.permissionKeys || []);
        this.editingIsSystem.set(detail.isSystem);
        this.editingVersion.set(detail.version);
        this.drawerOpen.set(true);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.message || 'Failed to load policy group details');
        this.loading.set(false);
      },
    });
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
  }

  saveGroup(): void {
    const name = this.editingGroupName().trim();
    if (!name) return;

    this.saving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const id = this.editingGroupId();
    if (id) {
      this.policyService
        .updatePolicyGroup(id, {
          groupName: name,
          description: this.editingDescription().trim() || undefined,
          permissionKeys: this.editingPermissionKeys(),
          version: this.editingVersion(),
        })
        .subscribe({
          next: () => {
            this.saving.set(false);
            this.drawerOpen.set(false);
            this.successMessage.set(this.i18n.t('policy.savedSuccess'));
            this.loadGroups();
          },
          error: (err) => {
            this.errorMessage.set(err.message || 'Failed to update group');
            this.saving.set(false);
          },
        });
    } else {
      this.policyService
        .createPolicyGroup({
          groupName: name,
          description: this.editingDescription().trim() || undefined,
          permissionKeys: this.editingPermissionKeys(),
        })
        .subscribe({
          next: () => {
            this.saving.set(false);
            this.drawerOpen.set(false);
            this.successMessage.set(this.i18n.t('policy.savedSuccess'));
            this.loadGroups();
          },
          error: (err) => {
            this.errorMessage.set(err.message || 'Failed to create group');
            this.saving.set(false);
          },
        });
    }
  }

  deleteGroup(group: PolicyGroupSummaryDto): void {
    if (group.isSystem) return;
    if (!confirm(this.i18n.t('policy.confirmDelete'))) return;

    this.loading.set(true);
    this.errorMessage.set(null);

    this.policyService.deletePolicyGroup(group.id).subscribe({
      next: () => {
        this.successMessage.set(this.i18n.t('policy.deletedSuccess'));
        this.loadGroups();
      },
      error: (err) => {
        this.errorMessage.set(err.message || 'Failed to delete group');
        this.loading.set(false);
      },
    });
  }
}
