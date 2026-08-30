import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { I18nService } from '../../../core/i18n.service';
import { WbsNodeResponse } from '../models/project.models';

@Component({
  selector: 'app-wbs-tree-grid',
  standalone: true,
  imports: [CommonModule, DecimalPipe],
  templateUrl: './wbs-tree-grid.component.html',
  styleUrl: './wbs-tree-grid.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WbsTreeGridComponent {
  readonly i18n = inject(I18nService);

  @Input() nodes: WbsNodeResponse[] = [];
  @Input() readOnly = false;

  @Output() addChild = new EventEmitter<WbsNodeResponse>();
  @Output() editNode = new EventEmitter<WbsNodeResponse>();
  @Output() repositionNode = new EventEmitter<WbsNodeResponse>();
  @Output() deleteNode = new EventEmitter<WbsNodeResponse>();

  readonly expandedNodeIds = signal<Set<string>>(new Set());

  isExpanded(nodeId: string): boolean {
    return this.expandedNodeIds().has(nodeId);
  }

  toggleExpand(nodeId: string): void {
    this.expandedNodeIds.update((set) => {
      const next = new Set(set);
      if (next.has(nodeId)) {
        next.delete(nodeId);
      } else {
        next.add(nodeId);
      }
      return next;
    });
  }

  expandAll(): void {
    const allIds = new Set<string>();
    const collect = (list: WbsNodeResponse[]) => {
      for (const node of list) {
        if (node.children && node.children.length > 0) {
          allIds.add(node.id);
          collect(node.children);
        }
      }
    };
    collect(this.nodes);
    this.expandedNodeIds.set(allIds);
  }

  collapseAll(): void {
    this.expandedNodeIds.set(new Set());
  }

  getTypeBadgeClass(type: string): string {
    switch (type) {
      case 'PHASE':
        return 'badge-phase';
      case 'SUB_PHASE':
        return 'badge-subphase';
      case 'WORK_PACKAGE':
        return 'badge-wp';
      case 'BOQ_ITEM':
        return 'badge-boq';
      case 'MILESTONE':
        return 'badge-milestone';
      default:
        return 'badge-default';
    }
  }

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'PLANNED':
        return 'status-planned';
      case 'IN_PROGRESS':
        return 'status-progress';
      case 'COMPLETED':
        return 'status-completed';
      case 'ON_HOLD':
        return 'status-hold';
      case 'CANCELLED':
        return 'status-cancelled';
      default:
        return 'status-default';
    }
  }
}
