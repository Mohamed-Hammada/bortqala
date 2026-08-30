import { Component, Input, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { ScheduleService } from '../data-access/schedule.service';
import {
  ProjectScheduleTask,
  TaskDependency,
  ScheduleBaseline,
  ScheduleBaselineComparison,
  ResourceOverAllocation,
  CreateScheduleTaskRequest,
  UpdateScheduleTaskRequest,
  TaskDependencyType,
  TaskResourceType,
} from '../models/schedule.models';
import { TaskEditorModalComponent } from './task-editor-modal.component';

@Component({
  selector: 'app-project-schedule-gantt',
  standalone: true,
  imports: [CommonModule, FormsModule, TaskEditorModalComponent],
  templateUrl: './project-schedule-gantt.component.html',
  styleUrls: ['./project-schedule-gantt.component.scss'],
})
export class ProjectScheduleGanttComponent implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly notification = inject(NotificationService);
  readonly scheduleService = inject(ScheduleService);

  @Input({ required: true }) projectId!: string;

  activeTab = signal<'gantt' | 'table' | 'baselines' | 'resources'>('gantt');
  zoomLevel = signal<'days' | 'weeks' | 'months'>('days');
  selectedBaselineId = signal<string>('');
  filterCriticalOnly = signal<boolean>(false);

  // Modal states
  showTaskEditor = signal<boolean>(false);
  editingTask = signal<ProjectScheduleTask | null>(null);
  isSavingTask = signal<boolean>(false);

  // Baseline prompt modal
  showBaselineModal = signal<boolean>(false);
  baselineName = signal<string>('');
  baselineNotes = signal<string>('');

  // Delete confirmation
  showDeleteModal = signal<boolean>(false);
  deletingTaskId = signal<string | null>(null);

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.scheduleService.loadSchedule(this.projectId).subscribe({
      error: () => this.notification.error(this.i18n.t('common.error')),
    });
    this.scheduleService.loadOverAllocations(this.projectId).subscribe();
  }

  readonly schedule = computed(() => this.scheduleService.schedule());

  readonly filteredTasks = computed(() => {
    const s = this.schedule();
    if (!s || !s.tasks) return [];
    if (this.filterCriticalOnly()) {
      return s.tasks.filter((t) => t.isCritical);
    }
    return s.tasks;
  });

  // ─── Gantt Timeline Computations ─────────────────────────────────

  readonly timelineRange = computed(() => {
    const s = this.schedule();
    const tasks = s?.tasks || [];
    if (tasks.length === 0) {
      const now = new Date();
      return {
        startDate: new Date(now.getFullYear(), now.getMonth(), 1),
        endDate: new Date(now.getFullYear(), now.getMonth() + 1, 0),
        totalDays: 30,
      };
    }

    let minEpoch = s?.startDate || tasks[0].earlyStartDate || tasks[0].plannedStartDate || Date.now();
    let maxEpoch = s?.endDate || tasks[0].earlyEndDate || tasks[0].plannedEndDate || Date.now();

    for (const t of tasks) {
      const start = t.earlyStartDate || t.plannedStartDate;
      const end = t.earlyEndDate || t.plannedEndDate;
      if (start && start < minEpoch) minEpoch = start;
      if (end && end > maxEpoch) maxEpoch = end;
    }

    const startDate = new Date(minEpoch);
    const endDate = new Date(maxEpoch);
    // Add 3 days buffer on each side
    startDate.setDate(startDate.getDate() - 3);
    endDate.setDate(endDate.getDate() + 7);

    const totalDays = Math.max(10, Math.ceil((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24)));

    return { startDate, endDate, totalDays };
  });

  readonly timelineDays = computed(() => {
    const { startDate, totalDays } = this.timelineRange();
    const days: { date: Date; label: string; isWeekend: boolean; isToday: boolean }[] = [];
    const todayStr = new Date().toDateString();

    for (let i = 0; i < totalDays; i++) {
      const d = new Date(startDate);
      d.setDate(startDate.getDate() + i);
      const isWeekend = d.getDay() === 5 || d.getDay() === 6; // Fri / Sat weekend
      const isToday = d.toDateString() === todayStr;
      days.push({
        date: d,
        label: `${d.getDate()}/${d.getMonth() + 1}`,
        isWeekend,
        isToday,
      });
    }
    return days;
  });

  getDayLeftPercent(epoch?: number | null): number {
    if (!epoch) return 0;
    const { startDate, totalDays } = this.timelineRange();
    const diffDays = (epoch - startDate.getTime()) / (1000 * 60 * 60 * 24);
    return Math.max(0, Math.min(100, (diffDays / totalDays) * 100));
  }

  getBarWidthPercent(startEpoch?: number | null, endEpoch?: number | null, durationDays = 1): number {
    const { totalDays } = this.timelineRange();
    if (!startEpoch || !endEpoch) {
      return Math.max(1, (durationDays / totalDays) * 100);
    }
    const diffDays = Math.max(1, Math.ceil((endEpoch - startEpoch) / (1000 * 60 * 60 * 24)) + 1);
    return Math.max(1, (diffDays / totalDays) * 100);
  }

  // ─── Actions ─────────────────────────────────────────────────────

  onOpenCreateTask(): void {
    this.editingTask.set(null);
    this.showTaskEditor.set(true);
  }

  onOpenEditTask(task: ProjectScheduleTask): void {
    this.editingTask.set(task);
    this.showTaskEditor.set(true);
  }

  onSaveTask(payload: {
    isEdit: boolean;
    taskId?: string;
    createReq?: CreateScheduleTaskRequest;
    updateReq?: UpdateScheduleTaskRequest;
  }): void {
    this.isSavingTask.set(true);
    if (payload.isEdit && payload.taskId && payload.updateReq) {
      this.scheduleService.updateTask(this.projectId, payload.taskId, payload.updateReq).subscribe({
        next: () => {
          this.isSavingTask.set(false);
          this.showTaskEditor.set(false);
          this.notification.success(this.i18n.t('schedule.taskUpdatedSuccess'));
        },
        error: () => {
          this.isSavingTask.set(false);
          this.notification.error(this.i18n.t('common.error'));
        },
      });
    } else if (!payload.isEdit && payload.createReq) {
      this.scheduleService.createTask(this.projectId, payload.createReq).subscribe({
        next: () => {
          this.isSavingTask.set(false);
          this.showTaskEditor.set(false);
          this.notification.success(this.i18n.t('schedule.taskCreatedSuccess'));
        },
        error: () => {
          this.isSavingTask.set(false);
          this.notification.error(this.i18n.t('common.error'));
        },
      });
    }
  }

  onPromptDelete(taskId: string): void {
    this.deletingTaskId.set(taskId);
    this.showDeleteModal.set(true);
  }

  onConfirmDelete(): void {
    const taskId = this.deletingTaskId();
    if (!taskId) return;
    this.scheduleService.deleteTask(this.projectId, taskId).subscribe({
      next: () => {
        this.showDeleteModal.set(false);
        this.deletingTaskId.set(null);
        this.notification.success(this.i18n.t('schedule.taskDeletedSuccess'));
      },
      error: () => this.notification.error(this.i18n.t('common.error')),
    });
  }

  onAddDependency(payload: {
    predecessorTaskId: string;
    successorTaskId: string;
    dependencyType: TaskDependencyType;
    lagDays: number;
  }): void {
    this.scheduleService.addDependency(this.projectId, payload).subscribe({
      next: () => this.notification.success(this.i18n.t('schedule.dependencyAddedSuccess')),
      error: (err) => {
        const msg = err.error?.message === 'SCHEDULE_CYCLE_DETECTED'
          ? this.i18n.t('schedule.cycleDetected')
          : this.i18n.t('common.error');
        this.notification.error(msg);
      },
    });
  }

  onRemoveDependency(dependencyId: string): void {
    this.scheduleService.removeDependency(this.projectId, dependencyId).subscribe({
      next: () => this.notification.success(this.i18n.t('schedule.dependencyRemovedSuccess')),
      error: () => this.notification.error(this.i18n.t('common.error')),
    });
  }

  onRecalculateCpm(): void {
    this.scheduleService.recalculateCpm(this.projectId).subscribe({
      next: () => this.notification.success(this.i18n.t('schedule.cpmSuccess')),
      error: (err) => {
        const msg = err.error?.message === 'SCHEDULE_CYCLE_DETECTED'
          ? this.i18n.t('schedule.cycleDetected')
          : this.i18n.t('common.error');
        this.notification.error(msg);
      },
    });
  }

  onOpenBaselineModal(): void {
    const s = this.schedule();
    const ver = (s?.currentBaselineVersion || 0) + 1;
    this.baselineName.set(`Baseline v${ver}`);
    this.baselineNotes.set('');
    this.showBaselineModal.set(true);
  }

  onCreateBaseline(): void {
    if (!this.baselineName().trim()) return;
    this.scheduleService
      .createBaseline(this.projectId, {
        name: this.baselineName().trim(),
        notes: this.baselineNotes().trim() || null,
      })
      .subscribe({
        next: () => {
          this.showBaselineModal.set(false);
          this.notification.success(this.i18n.t('schedule.baselineCreatedSuccess'));
        },
        error: () => this.notification.error(this.i18n.t('common.error')),
      });
  }

  onBaselineSelect(baselineId: string): void {
    this.selectedBaselineId.set(baselineId);
    if (baselineId) {
      this.scheduleService.loadBaselineComparison(this.projectId, baselineId).subscribe();
    }
  }

  onAssignResource(payload: {
    taskId: string;
    resourceType: TaskResourceType;
    resourceName: string;
    quantityAllocated: number;
    notes?: string | null;
  }): void {
    this.scheduleService.assignResource(this.projectId, payload.taskId, payload).subscribe({
      next: () => this.notification.success(this.i18n.t('schedule.resourceAssignedSuccess')),
      error: () => this.notification.error(this.i18n.t('common.error')),
    });
  }

  onRemoveResource(assignmentId: string): void {
    this.scheduleService.removeResourceAssignment(this.projectId, assignmentId).subscribe({
      next: () => this.notification.success(this.i18n.t('schedule.resourceRemovedSuccess')),
      error: () => this.notification.error(this.i18n.t('common.error')),
    });
  }

  onImportWbs(): void {
    this.scheduleService.importFromWbs(this.projectId).subscribe({
      next: () => this.notification.success(this.i18n.t('schedule.wbsImportSuccess')),
      error: () => this.notification.error(this.i18n.t('common.error')),
    });
  }

  onSyncDpr(): void {
    this.scheduleService.syncFromDpr(this.projectId).subscribe({
      next: () => this.notification.success(this.i18n.t('schedule.syncDprSuccess')),
      error: () => this.notification.error(this.i18n.t('common.error')),
    });
  }

  formatDate(epoch?: number | null): string {
    if (!epoch) return '—';
    return new Date(epoch).toLocaleDateString();
  }
}
