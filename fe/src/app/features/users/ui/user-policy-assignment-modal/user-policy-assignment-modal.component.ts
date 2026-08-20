import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../../../core/i18n.service';
import { PolicyService } from '../../../../core/auth/policy.service';
import {
  PolicyGroupSummaryDto,
  UserPolicyAssignmentItem,
} from '../../../../core/auth/security-policy.models';

interface EditableAssignment {
  policyGroupId: string;
  groupName: string;
  selected: boolean;
  scopeBranchId: string;
  scopeCostCenterId: string;
}

@Component({
  selector: 'app-user-policy-assignment-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-policy-assignment-modal.component.html',
  styleUrls: ['./user-policy-assignment-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserPolicyAssignmentModalComponent implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly policyService = inject(PolicyService);

  @Input() userId = '';
  @Input() userName = '';

  @Output() readonly closed = new EventEmitter<void>();
  @Output() readonly saved = new EventEmitter<void>();

  readonly loading = signal<boolean>(true);
  readonly saving = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly assignments = signal<EditableAssignment[]>([]);

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.policyService.listPolicyGroups().subscribe({
      next: (groups) => {
        this.policyService.getUserPolicies(this.userId).subscribe({
          next: (userAssignments) => {
            const assignmentMap = new Map(
              userAssignments.map((a) => [a.policyGroupId, a])
            );

            const list: EditableAssignment[] = groups.map((g) => {
              const existing = assignmentMap.get(g.id);
              return {
                policyGroupId: g.id,
                groupName: g.groupName,
                selected: !!existing,
                scopeBranchId: existing?.scopeBranchId || '',
                scopeCostCenterId: existing?.scopeCostCenterId || '',
              };
            });

            this.assignments.set(list);
            this.loading.set(false);
          },
          error: (err) => {
            this.errorMessage.set(err.message || 'Failed to load user policies');
            this.loading.set(false);
          },
        });
      },
      error: (err) => {
        this.errorMessage.set(err.message || 'Failed to load policy groups');
        this.loading.set(false);
      },
    });
  }

  toggleGroup(item: EditableAssignment): void {
    item.selected = !item.selected;
    this.assignments.set([...this.assignments()]);
  }

  onSave(): void {
    this.saving.set(true);
    this.errorMessage.set(null);

    const payload: UserPolicyAssignmentItem[] = this.assignments()
      .filter((a) => a.selected)
      .map((a) => ({
        policyGroupId: a.policyGroupId,
        scopeBranchId: a.scopeBranchId?.trim() || undefined,
        scopeCostCenterId: a.scopeCostCenterId?.trim() || undefined,
      }));

    this.policyService.assignUserPolicies(this.userId, { assignments: payload }).subscribe({
      next: () => {
        this.saving.set(false);
        this.saved.emit();
      },
      error: (err) => {
        this.errorMessage.set(err.message || 'Failed to save assignments');
        this.saving.set(false);
      },
    });
  }

  onCancel(): void {
    this.closed.emit();
  }
}
