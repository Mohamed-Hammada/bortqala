import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type WorkflowStepState = 'pending' | 'current' | 'completed' | 'rejected';

export interface WorkflowStep {
  key: string;
  label: string;
  state: WorkflowStepState;
}

@Component({
  selector: 'app-workflow-stepper',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="stepper-track" role="progressbar" aria-label="Transaction workflow progress">
      @for (step of steps; track step.key; let i = $index) {
        <div class="step-node" [class.completed]="step.state === 'completed'" [class.current]="step.state === 'current'" [class.rejected]="step.state === 'rejected'">
          <div class="node-badge">
            @if (step.state === 'completed') {
              <span>✓</span>
            } @else if (step.state === 'rejected') {
              <span>✕</span>
            } @else {
              <span>{{ i + 1 }}</span>
            }
          </div>
          <span class="node-label">{{ step.label }}</span>
        </div>
        @if (i < steps.length - 1) {
          <div class="step-connector" [class.filled]="step.state === 'completed'"></div>
        }
      }
    </div>
  `,
  styles: [`
    .stepper-track {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 1rem 0;
      width: 100%;
      overflow-x: auto;
    }
    .step-node {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.35rem;
      min-width: 80px;
      text-align: center;
    }
    .node-badge {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      background: var(--surface-secondary, #e2e8f0);
      color: var(--text-muted, #64748b);
      font-size: 0.8rem;
      font-weight: bold;
      display: flex;
      align-items: center;
      justify-content: center;
      border: 2px solid transparent;
      transition: all 0.2s ease;
    }
    .step-node.completed .node-badge {
      background: #10b981;
      color: #ffffff;
    }
    .step-node.current .node-badge {
      background: #3b82f6;
      color: #ffffff;
      box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
    }
    .step-node.rejected .node-badge {
      background: #ef4444;
      color: #ffffff;
    }
    .node-label {
      font-size: 0.75rem;
      font-weight: 500;
      color: var(--text-color, #1e293b);
    }
    .step-connector {
      flex: 1;
      height: 2px;
      background: var(--surface-secondary, #e2e8f0);
      margin: 0 0.5rem;
      margin-bottom: 1.25rem;
    }
    .step-connector.filled {
      background: #10b981;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkflowStepperComponent {
  @Input() steps: WorkflowStep[] = [
    { key: 'draft', label: 'Draft', state: 'completed' },
    { key: 'submitted', label: 'Submitted', state: 'current' },
    { key: 'approved', label: 'Approved', state: 'pending' },
    { key: 'posted', label: 'Posted', state: 'pending' },
    { key: 'settled', label: 'Settled', state: 'pending' },
  ];
}
