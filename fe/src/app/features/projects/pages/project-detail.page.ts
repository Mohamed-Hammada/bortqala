import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';
import { formatDate } from '../../../core/date';
import { ConfirmDialogService } from '../../../core/confirm-dialog.service';
import { ModalDialogComponent } from '../../../shared/ui/modal-dialog/modal-dialog.component';
import { WbsTreeGridComponent } from '../ui/wbs-tree-grid.component';
import { DailyReportsListComponent } from '../ui/daily-reports-list.component';
import { ProjectScheduleGanttComponent } from '../ui/project-schedule-gantt.component';
import { TendersListComponent } from '../ui/tenders-list.component';
import { ClaimsListComponent } from '../ui/claims-list.component';
import { ProjectCostControlComponent } from '../ui/project-cost-control.component';
import { SiteCustodyListComponent } from '../ui/site-custody-list.component';
import { ProjectService } from '../data-access/project.service';
import {
  CostCodeCategory,
  CreateCostCodeRequest,
  CreateWbsNodeRequest,
  ProjectPartyRoleResponse,
  ProjectPartyRoleType,
  ProjectResponse,
  ProjectStatus,
  RepositionWbsNodeRequest,
  UpdateProjectRequest,
  UpdateWbsNodeRequest,
  WbsNodeResponse,
  WbsNodeStatus,
  WbsNodeType,
} from '../models/project.models';

@Component({
  selector: 'app-project-detail-page',
  standalone: true,
  imports: [
    CommonModule,
    DecimalPipe,
    ReactiveFormsModule,
    RouterLink,
    ModalDialogComponent,
    WbsTreeGridComponent,
    DailyReportsListComponent,
    ProjectScheduleGanttComponent,
    TendersListComponent,
    ClaimsListComponent,
    ProjectCostControlComponent,
    SiteCustodyListComponent,
  ],
  templateUrl: './project-detail.page.html',
  styleUrl: './project-detail.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectDetailPage implements OnInit {
  readonly i18n = inject(I18nService);
  readonly projectService = inject(ProjectService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly notification = inject(NotificationService);
  private readonly confirm = inject(ConfirmDialogService);

  readonly projectId = signal<string>('');
  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly activeTab = signal<'wbs' | 'overview' | 'costCodes' | 'roles' | 'dpr' | 'schedule' | 'tenders' | 'claims' | 'costControl' | 'custodies'>('wbs');

  // Edit Project Modal
  readonly editProjectOpen = signal(false);
  readonly projectForm = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(255)] }),
    nameEn: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(255)] }),
    description: new FormControl('', { nonNullable: true }),
    siteAddress: new FormControl('', { nonNullable: true }),
    contractNumber: new FormControl('', { nonNullable: true }),
    contractValue: new FormControl<number | null>(null, { nonNullable: true }),
    currencyCode: new FormControl('EGP', { nonNullable: true, validators: [Validators.required] }),
    startDate: new FormControl<string>('', { nonNullable: true }),
    endDate: new FormControl<string>('', { nonNullable: true }),
    budgetBlocking: new FormControl(true, { nonNullable: true }),
  });

  // WBS Add / Edit Modal
  readonly wbsModalOpen = signal(false);
  readonly isEditingWbs = signal(false);
  readonly editingWbsId = signal<string | null>(null);
  readonly parentWbsNode = signal<WbsNodeResponse | null>(null);

  readonly wbsForm = new FormGroup({
    wbsCode: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    nameEn: new FormControl('', { nonNullable: true }),
    description: new FormControl('', { nonNullable: true }),
    nodeType: new FormControl<WbsNodeType>('WORK_PACKAGE', { nonNullable: true }),
    unitOfMeasure: new FormControl('', { nonNullable: true }),
    plannedQuantity: new FormControl<number | null>(null, { nonNullable: true }),
    unitRate: new FormControl<number | null>(null, { nonNullable: true }),
    costCodeId: new FormControl<string | null>(null, { nonNullable: true }),
    startDate: new FormControl<string>('', { nonNullable: true }),
    endDate: new FormControl<string>('', { nonNullable: true }),
    status: new FormControl<WbsNodeStatus>('PLANNED', { nonNullable: true }),
  });

  // Reposition Modal
  readonly repositionModalOpen = signal(false);
  readonly repositionNodeTarget = signal<WbsNodeResponse | null>(null);
  readonly repositionParentId = signal<string>('');

  // Cost Code Modal
  readonly costCodeModalOpen = signal(false);
  readonly costCodeForm = new FormGroup({
    code: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    nameEn: new FormControl('', { nonNullable: true }),
    category: new FormControl<CostCodeCategory>('LABOR', { nonNullable: true }),
    description: new FormControl('', { nonNullable: true }),
  });

  // Role Assignment Modal
  readonly roleModalOpen = signal(false);
  readonly roleForm = new FormGroup({
    partyId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    roleType: new FormControl<ProjectPartyRoleType>('SUBCONTRACTOR', { nonNullable: true }),
    notes: new FormControl('', { nonNullable: true }),
  });

  async ngOnInit(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/projects']);
      return;
    }
    this.projectId.set(id);
    await this.loadProjectData(id);
  }

  async loadProjectData(id: string): Promise<void> {
    this.loading.set(true);
    try {
      await Promise.all([
        firstValueFrom(this.projectService.getProject(id)),
        firstValueFrom(this.projectService.loadWbsTree(id)),
        firstValueFrom(this.projectService.loadFlatWbs(id)),
        firstValueFrom(this.projectService.loadCostCodes()),
        firstValueFrom(this.projectService.loadProjectRoles(id)),
      ]);
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  // ─── Project Lifecycle Transitions ───────────────────────────────

  async activateProject(): Promise<void> {
    try {
      await firstValueFrom(this.projectService.activateProject(this.projectId()));
      this.notification.success(this.i18n.t('projects.activatedSuccess'));
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    }
  }

  async holdProject(): Promise<void> {
    try {
      await firstValueFrom(this.projectService.holdProject(this.projectId()));
      this.notification.success(this.i18n.t('projects.heldSuccess'));
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    }
  }

  async completeProject(): Promise<void> {
    try {
      await firstValueFrom(this.projectService.completeProject(this.projectId()));
      this.notification.success(this.i18n.t('projects.completedSuccess'));
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    }
  }

  async closeProject(): Promise<void> {
    const confirmed = await this.confirm.confirmOptions({
      titleKey: 'projects.closeConfirmTitle',
      messageKey: 'projects.closeConfirmMessage',
    });
    if (!confirmed) return;

    try {
      await firstValueFrom(this.projectService.closeProject(this.projectId()));
      this.notification.success(this.i18n.t('projects.closedSuccess'));
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    }
  }

  async reopenProject(): Promise<void> {
    try {
      await firstValueFrom(this.projectService.reopenProject(this.projectId()));
      this.notification.success(this.i18n.t('projects.reopenedSuccess'));
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    }
  }

  // ─── Edit Project ────────────────────────────────────────────────

  openEditProject(): void {
    const p = this.projectService.currentProject();
    if (!p) return;

    this.projectForm.reset({
      name: p.name,
      nameEn: p.nameEn || '',
      description: p.description || '',
      siteAddress: p.siteAddress || '',
      contractNumber: p.contractNumber || '',
      contractValue: p.contractValue,
      currencyCode: p.currencyCode,
      startDate: p.startDate ? new Date(p.startDate).toISOString().substring(0, 10) : '',
      endDate: p.endDate ? new Date(p.endDate).toISOString().substring(0, 10) : '',
      budgetBlocking: p.budgetBlocking,
    });
    this.editProjectOpen.set(true);
  }

  async saveEditProject(): Promise<void> {
    if (this.projectForm.invalid) {
      this.projectForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const val = this.projectForm.getRawValue();

    const req: UpdateProjectRequest = {
      name: val.name.trim(),
      nameEn: val.nameEn.trim() || null,
      description: val.description.trim() || null,
      siteAddress: val.siteAddress.trim() || null,
      contractNumber: val.contractNumber.trim() || null,
      contractValue: val.contractValue,
      currencyCode: val.currencyCode.trim() || 'EGP',
      startDate: val.startDate ? new Date(val.startDate).getTime() : null,
      endDate: val.endDate ? new Date(val.endDate).getTime() : null,
      budgetBlocking: val.budgetBlocking,
    };

    try {
      await firstValueFrom(this.projectService.updateProject(this.projectId(), req));
      this.notification.success(this.i18n.t('projects.updatedSuccess'));
      this.editProjectOpen.set(false);
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    } finally {
      this.submitting.set(false);
    }
  }

  // ─── WBS Node Operations ─────────────────────────────────────────

  openAddRootWbs(): void {
    this.parentWbsNode.set(null);
    this.isEditingWbs.set(false);
    this.editingWbsId.set(null);
    this.wbsForm.reset({
      wbsCode: '',
      name: '',
      nameEn: '',
      description: '',
      nodeType: 'PHASE',
      unitOfMeasure: '',
      plannedQuantity: null,
      unitRate: null,
      costCodeId: null,
      startDate: '',
      endDate: '',
      status: 'PLANNED',
    });
    this.wbsModalOpen.set(true);
  }

  openAddChildWbs(parent: WbsNodeResponse): void {
    this.parentWbsNode.set(parent);
    this.isEditingWbs.set(false);
    this.editingWbsId.set(null);
    this.wbsForm.reset({
      wbsCode: `${parent.wbsCode}.`,
      name: '',
      nameEn: '',
      description: '',
      nodeType: parent.nodeType === 'PHASE' ? 'SUB_PHASE' : 'WORK_PACKAGE',
      unitOfMeasure: '',
      plannedQuantity: null,
      unitRate: null,
      costCodeId: null,
      startDate: '',
      endDate: '',
      status: 'PLANNED',
    });
    this.wbsModalOpen.set(true);
  }

  openEditWbs(node: WbsNodeResponse): void {
    this.parentWbsNode.set(null);
    this.isEditingWbs.set(true);
    this.editingWbsId.set(node.id);
    this.wbsForm.reset({
      wbsCode: node.wbsCode,
      name: node.name,
      nameEn: node.nameEn || '',
      description: node.description || '',
      nodeType: node.nodeType,
      unitOfMeasure: node.unitOfMeasure || '',
      plannedQuantity: node.plannedQuantity,
      unitRate: node.unitRate,
      costCodeId: node.costCodeId || null,
      startDate: node.startDate ? new Date(node.startDate).toISOString().substring(0, 10) : '',
      endDate: node.endDate ? new Date(node.endDate).toISOString().substring(0, 10) : '',
      status: node.status,
    });
    this.wbsModalOpen.set(true);
  }

  async saveWbsNode(): Promise<void> {
    if (this.wbsForm.invalid) {
      this.wbsForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const val = this.wbsForm.getRawValue();

    try {
      if (this.isEditingWbs()) {
        const req: UpdateWbsNodeRequest = {
          name: val.name.trim(),
          nameEn: val.nameEn.trim() || null,
          description: val.description.trim() || null,
          nodeType: val.nodeType,
          unitOfMeasure: val.unitOfMeasure.trim() || null,
          plannedQuantity: val.plannedQuantity,
          unitRate: val.unitRate,
          costCodeId: val.costCodeId || null,
          startDate: val.startDate ? new Date(val.startDate).getTime() : null,
          endDate: val.endDate ? new Date(val.endDate).getTime() : null,
          status: val.status,
        };
        await firstValueFrom(this.projectService.updateWbsNode(this.projectId(), this.editingWbsId()!, req));
        this.notification.success(this.i18n.t('wbs.nodeUpdatedSuccess'));
      } else {
        const parent = this.parentWbsNode();
        const req: CreateWbsNodeRequest = {
          parentId: parent ? parent.id : null,
          wbsCode: val.wbsCode.trim(),
          name: val.name.trim(),
          nameEn: val.nameEn.trim() || null,
          description: val.description.trim() || null,
          nodeType: val.nodeType,
          unitOfMeasure: val.unitOfMeasure.trim() || null,
          plannedQuantity: val.plannedQuantity,
          unitRate: val.unitRate,
          costCodeId: val.costCodeId || null,
          startDate: val.startDate ? new Date(val.startDate).getTime() : null,
          endDate: val.endDate ? new Date(val.endDate).getTime() : null,
          status: val.status,
        };
        await firstValueFrom(this.projectService.createWbsNode(this.projectId(), req));
        this.notification.success(this.i18n.t('wbs.nodeCreatedSuccess'));
      }

      this.wbsModalOpen.set(false);
      await Promise.all([
        firstValueFrom(this.projectService.loadWbsTree(this.projectId())),
        firstValueFrom(this.projectService.loadFlatWbs(this.projectId())),
        firstValueFrom(this.projectService.getProject(this.projectId())),
      ]);
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    } finally {
      this.submitting.set(false);
    }
  }

  openRepositionWbs(node: WbsNodeResponse): void {
    this.repositionNodeTarget.set(node);
    this.repositionParentId.set(node.parentId || '');
    this.repositionModalOpen.set(true);
  }

  async saveRepositionWbs(): Promise<void> {
    const node = this.repositionNodeTarget();
    if (!node) return;

    this.submitting.set(true);
    const parentId = this.repositionParentId() || null;
    const req: RepositionWbsNodeRequest = {
      parentId: parentId,
      sortOrder: node.sortOrder,
    };

    try {
      await firstValueFrom(this.projectService.repositionWbsNode(this.projectId(), node.id, req));
      this.notification.success(this.i18n.t('wbs.repositionSuccess'));
      this.repositionModalOpen.set(false);
      await Promise.all([
        firstValueFrom(this.projectService.loadWbsTree(this.projectId())),
        firstValueFrom(this.projectService.loadFlatWbs(this.projectId())),
      ]);
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    } finally {
      this.submitting.set(false);
    }
  }

  async deleteWbs(node: WbsNodeResponse): Promise<void> {
    const confirmed = await this.confirm.confirmOptions({
      titleKey: 'wbs.deleteConfirmTitle',
      messageKey: 'wbs.deleteConfirmMessage',
    });
    if (!confirmed) return;

    try {
      await firstValueFrom(this.projectService.deleteWbsNode(this.projectId(), node.id));
      this.notification.success(this.i18n.t('wbs.deletedSuccess'));
      await Promise.all([
        firstValueFrom(this.projectService.loadWbsTree(this.projectId())),
        firstValueFrom(this.projectService.loadFlatWbs(this.projectId())),
        firstValueFrom(this.projectService.getProject(this.projectId())),
      ]);
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    }
  }

  // ─── Cost Code Library ───────────────────────────────────────────

  openAddCostCode(): void {
    this.costCodeForm.reset({
      code: '',
      name: '',
      nameEn: '',
      category: 'LABOR',
      description: '',
    });
    this.costCodeModalOpen.set(true);
  }

  async saveCostCode(): Promise<void> {
    if (this.costCodeForm.invalid) {
      this.costCodeForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const val = this.costCodeForm.getRawValue();
    const req: CreateCostCodeRequest = {
      code: val.code.trim(),
      name: val.name.trim(),
      nameEn: val.nameEn.trim() || null,
      category: val.category,
      description: val.description.trim() || null,
    };

    try {
      await firstValueFrom(this.projectService.createCostCode(req));
      this.notification.success(this.i18n.t('costCode.createdSuccess'));
      this.costCodeModalOpen.set(false);
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    } finally {
      this.submitting.set(false);
    }
  }

  // ─── Project Stakeholder Roles ───────────────────────────────────

  openAddRole(): void {
    this.roleForm.reset({
      partyId: '',
      roleType: 'SUBCONTRACTOR',
      notes: '',
    });
    this.roleModalOpen.set(true);
  }

  async saveRole(): Promise<void> {
    if (this.roleForm.invalid) {
      this.roleForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const val = this.roleForm.getRawValue();
    const req = {
      partyId: val.partyId.trim(),
      roleType: val.roleType,
      notes: val.notes.trim() || null,
    };

    try {
      await firstValueFrom(this.projectService.assignProjectRole(this.projectId(), req));
      this.notification.success(this.i18n.t('projectRoles.assignedSuccess'));
      this.roleModalOpen.set(false);
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    } finally {
      this.submitting.set(false);
    }
  }

  async removeRole(role: ProjectPartyRoleResponse): Promise<void> {
    const confirmed = await this.confirm.confirmOptions({
      titleKey: 'projectRoles.removeConfirmTitle',
      messageKey: 'projectRoles.removeConfirmMessage',
    });
    if (!confirmed) return;

    try {
      await firstValueFrom(this.projectService.removeProjectRole(this.projectId(), role.id));
      this.notification.success(this.i18n.t('projectRoles.removedSuccess'));
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    }
  }

  async onReportChanged(): Promise<void> {
    try {
      await firstValueFrom(this.projectService.loadWbsTree(this.projectId()));
    } catch {
      // ignore
    }
  }

  formatEpoch(epoch?: number | null): string {
    if (!epoch) return '—';
    return formatDate(epoch);
  }

  getStatusClass(status: ProjectStatus): string {
    switch (status) {
      case 'ACTIVE':
        return 'status-active';
      case 'DRAFT':
        return 'status-draft';
      case 'ON_HOLD':
        return 'status-hold';
      case 'COMPLETED':
        return 'status-completed';
      case 'CLOSED':
        return 'status-closed';
      default:
        return 'status-default';
    }
  }
}
