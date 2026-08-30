import { Component, EventEmitter, Input, Output, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../../core/i18n.service';
import {
  ProjectScheduleTask,
  CreateScheduleTaskRequest,
  UpdateScheduleTaskRequest,
  TaskConstraintType,
  TaskDependencyType,
  TaskResourceType,
  TaskDependency,
  TaskResourceAssignment,
} from '../models/schedule.models';

@Component({
  selector: 'app-task-editor-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './task-editor-modal.component.html',
  styleUrls: ['./task-editor-modal.component.scss'],
})
export class TaskEditorModalComponent implements OnInit {
  readonly i18n = inject(I18nService);

  @Input() task: ProjectScheduleTask | null = null;
  @Input() allTasks: ProjectScheduleTask[] = [];
  @Input() dependencies: TaskDependency[] = [];
  @Input() isSaving = false;

  @Output() saveTask = new EventEmitter<{
    isEdit: boolean;
    taskId?: string;
    createReq?: CreateScheduleTaskRequest;
    updateReq?: UpdateScheduleTaskRequest;
  }>();

  @Output() addDependency = new EventEmitter<{
    predecessorTaskId: string;
    successorTaskId: string;
    dependencyType: TaskDependencyType;
    lagDays: number;
  }>();

  @Output() removeDependency = new EventEmitter<string>();

  @Output() addResource = new EventEmitter<{
    taskId: string;
    resourceType: TaskResourceType;
    resourceName: string;
    quantityAllocated: number;
    startDate?: number | null;
    endDate?: number | null;
    notes?: string | null;
  }>();

  @Output() removeResource = new EventEmitter<string>();

  @Output() close = new EventEmitter<void>();

  activeTab: 'details' | 'dependencies' | 'resources' = 'details';

  // Form fields
  taskCode = '';
  name = '';
  nameEn = '';
  durationDays = 1;
  plannedStartDate = '';
  plannedEndDate = '';
  isMilestone = false;
  constraintType: TaskConstraintType = 'ASAP';
  constraintDate = '';
  sortOrder = 0;

  // New Dependency form
  newPredTaskId = '';
  newDepType: TaskDependencyType = 'FS';
  newLagDays = 0;

  // New Resource form
  newResType: TaskResourceType = 'LABOR';
  newResName = '';
  newResQty = 1;
  newResNotes = '';

  ngOnInit(): void {
    if (this.task) {
      this.taskCode = this.task.taskCode;
      this.name = this.task.name;
      this.nameEn = this.task.nameEn || '';
      this.durationDays = this.task.durationDays;
      this.plannedStartDate = this.formatDateInput(this.task.plannedStartDate);
      this.plannedEndDate = this.formatDateInput(this.task.plannedEndDate);
      this.isMilestone = this.task.isMilestone;
      this.constraintType = this.task.constraintType || 'ASAP';
      this.constraintDate = this.formatDateInput(this.task.constraintDate);
      this.sortOrder = this.task.sortOrder || 0;
    } else {
      this.taskCode = `TSK-${String(this.allTasks.length + 1).padStart(2, '0')}`;
      this.plannedStartDate = new Date().toISOString().substring(0, 10);
      this.sortOrder = this.allTasks.length + 1;
    }
  }

  get taskDependencies(): TaskDependency[] {
    if (!this.task) return [];
    return this.dependencies.filter((d) => d.successorTaskId === this.task?.id);
  }

  get availablePredecessors(): ProjectScheduleTask[] {
    if (!this.task) return this.allTasks;
    return this.allTasks.filter((t) => t.id !== this.task?.id);
  }

  getTaskName(taskId: string): string {
    const t = this.allTasks.find((item) => item.id === taskId);
    return t ? `${t.taskCode} - ${t.name}` : taskId;
  }

  onSave(): void {
    if (!this.taskCode.trim() || !this.name.trim()) return;

    const startEpoch = this.plannedStartDate ? new Date(this.plannedStartDate).getTime() : null;
    const endEpoch = this.plannedEndDate ? new Date(this.plannedEndDate).getTime() : null;
    const constrEpoch = this.constraintDate ? new Date(this.constraintDate).getTime() : null;

    if (this.task) {
      const updateReq: UpdateScheduleTaskRequest = {
        taskCode: this.taskCode.trim(),
        name: this.name.trim(),
        nameEn: this.nameEn.trim() || null,
        durationDays: this.isMilestone ? 0 : Math.max(1, this.durationDays),
        plannedStartDate: startEpoch,
        plannedEndDate: endEpoch,
        isMilestone: this.isMilestone,
        constraintType: this.constraintType,
        constraintDate: constrEpoch,
        sortOrder: this.sortOrder,
      };
      this.saveTask.emit({ isEdit: true, taskId: this.task.id, updateReq });
    } else {
      const createReq: CreateScheduleTaskRequest = {
        taskCode: this.taskCode.trim(),
        name: this.name.trim(),
        nameEn: this.nameEn.trim() || null,
        durationDays: this.isMilestone ? 0 : Math.max(1, this.durationDays),
        plannedStartDate: startEpoch,
        plannedEndDate: endEpoch,
        isMilestone: this.isMilestone,
        constraintType: this.constraintType,
        constraintDate: constrEpoch,
        sortOrder: this.sortOrder,
      };
      this.saveTask.emit({ isEdit: false, createReq });
    }
  }

  onAddDependency(): void {
    if (!this.task || !this.newPredTaskId) return;
    this.addDependency.emit({
      predecessorTaskId: this.newPredTaskId,
      successorTaskId: this.task.id,
      dependencyType: this.newDepType,
      lagDays: this.newLagDays || 0,
    });
    this.newPredTaskId = '';
    this.newLagDays = 0;
  }

  onRemoveDependency(depId: string): void {
    this.removeDependency.emit(depId);
  }

  onAddResource(): void {
    if (!this.task || !this.newResName.trim()) return;
    this.addResource.emit({
      taskId: this.task.id,
      resourceType: this.newResType,
      resourceName: this.newResName.trim(),
      quantityAllocated: Math.max(0.1, this.newResQty),
      notes: this.newResNotes.trim() || null,
    });
    this.newResName = '';
    this.newResQty = 1;
    this.newResNotes = '';
  }

  onRemoveResource(resId: string): void {
    this.removeResource.emit(resId);
  }

  private formatDateInput(epoch?: number | null): string {
    if (!epoch) return '';
    return new Date(epoch).toISOString().substring(0, 10);
  }
}
