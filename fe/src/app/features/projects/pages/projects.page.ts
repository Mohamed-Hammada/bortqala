import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';
import { formatDate } from '../../../core/date';
import { ConfirmDialogService } from '../../../core/confirm-dialog.service';
import { ModalDialogComponent } from '../../../shared/ui/modal-dialog/modal-dialog.component';
import { ProjectService } from '../data-access/project.service';
import { CreateProjectRequest, ProjectResponse, ProjectStatus } from '../models/project.models';

@Component({
  selector: 'app-projects-page',
  standalone: true,
  imports: [CommonModule, DecimalPipe, ReactiveFormsModule, RouterLink, ModalDialogComponent],
  templateUrl: './projects.page.html',
  styleUrl: './projects.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectsPage implements OnInit {
  readonly i18n = inject(I18nService);
  readonly projectService = inject(ProjectService);
  private readonly notification = inject(NotificationService);
  private readonly confirm = inject(ConfirmDialogService);
  private readonly router = inject(Router);

  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly searchQuery = signal('');
  readonly statusFilter = signal<string>('ALL');
  readonly drawerOpen = signal(false);

  readonly projectForm = new FormGroup({
    code: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(32)] }),
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

  readonly filteredProjects = computed(() => {
    const list = this.projectService.projects();
    const query = this.searchQuery().trim().toLowerCase();
    const status = this.statusFilter();

    return list.filter((p) => {
      const matchesQuery =
        !query ||
        p.code.toLowerCase().includes(query) ||
        p.name.toLowerCase().includes(query) ||
        (p.nameEn && p.nameEn.toLowerCase().includes(query)) ||
        (p.siteAddress && p.siteAddress.toLowerCase().includes(query));

      const matchesStatus = status === 'ALL' || p.status === status;

      return matchesQuery && matchesStatus;
    });
  });

  async ngOnInit(): Promise<void> {
    await this.loadData();
  }

  async loadData(): Promise<void> {
    this.loading.set(true);
    try {
      await Promise.all([
        firstValueFrom(this.projectService.loadProjects()),
        firstValueFrom(this.projectService.loadSummary()),
      ]);
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  openCreateDrawer(): void {
    this.projectForm.reset({
      code: '',
      name: '',
      nameEn: '',
      description: '',
      siteAddress: '',
      contractNumber: '',
      contractValue: null,
      currencyCode: 'EGP',
      startDate: '',
      endDate: '',
      budgetBlocking: true,
    });
    this.drawerOpen.set(true);
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
  }

  async saveProject(): Promise<void> {
    if (this.projectForm.invalid) {
      this.projectForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const formVal = this.projectForm.getRawValue();

    const req: CreateProjectRequest = {
      code: formVal.code.trim(),
      name: formVal.name.trim(),
      nameEn: formVal.nameEn.trim() || null,
      description: formVal.description.trim() || null,
      siteAddress: formVal.siteAddress.trim() || null,
      contractNumber: formVal.contractNumber.trim() || null,
      contractValue: formVal.contractValue,
      currencyCode: formVal.currencyCode.trim() || 'EGP',
      startDate: formVal.startDate ? new Date(formVal.startDate).getTime() : null,
      endDate: formVal.endDate ? new Date(formVal.endDate).getTime() : null,
      budgetBlocking: formVal.budgetBlocking,
    };

    try {
      const created = await firstValueFrom(this.projectService.createProject(req));
      this.notification.success(this.i18n.t('projects.createdSuccess'));
      this.drawerOpen.set(false);
      await firstValueFrom(this.projectService.loadSummary());
      this.router.navigate(['/projects', created.id]);
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    } finally {
      this.submitting.set(false);
    }
  }

  async deleteProject(project: ProjectResponse): Promise<void> {
    const confirmed = await this.confirm.confirmOptions({
      titleKey: 'projects.deleteConfirmTitle',
      messageKey: 'projects.deleteConfirmMessage',
    });

    if (!confirmed) return;

    try {
      await firstValueFrom(this.projectService.deleteProject(project.id));
      this.notification.success(this.i18n.t('projects.deletedSuccess'));
      await firstValueFrom(this.projectService.loadSummary());
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
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
