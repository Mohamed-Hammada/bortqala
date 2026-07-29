import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { AppTooltipDirective } from '../app-tooltip/app-tooltip.directive';

@Component({
  selector: 'app-icon-button',
  standalone: true,
  imports: [AppTooltipDirective],
  template: `
    <button
      type="button"
      class="app-icon-action"
      [class.danger]="variant === 'danger'"
      [disabled]="disabled"
      [attr.aria-label]="accessibleLabel"
      [appTooltip]="tooltipText"
      (click)="pressed.emit($event)"
    ><span aria-hidden="true">{{ icon }}</span></button>
  `,
  styles: [`
    :host { display: inline-flex; }
    .app-icon-action { width: 2rem; height: 2rem; display: inline-grid; place-items: center; border: 1px solid #cbd5e1; border-radius: .5rem; background: #fff; color: #334155; cursor: pointer; font: inherit; }
    .app-icon-action:hover, .app-icon-action:focus-visible { border-color: #d97706; color: #92400e; outline: 2px solid color-mix(in srgb, #d97706 25%, transparent); outline-offset: 2px; }
    .app-icon-action.danger { color: #b91c1c; }
    .app-icon-action:disabled { opacity: .55; cursor: not-allowed; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IconButtonComponent {
  @Input({ required: true }) icon = '';
  @Input({ required: true }) label = '';
  @Input() description = '';
  @Input() itemName = '';
  @Input() shortcut = '';
  @Input() variant: 'default' | 'danger' = 'default';
  @Input() disabled = false;
  @Output() pressed = new EventEmitter<MouseEvent>();

  get accessibleLabel(): string {
    return [this.label, this.itemName].filter(Boolean).join(' — ');
  }

  get tooltipText(): string {
    const action = this.accessibleLabel;
    const description = this.description ? ` — ${this.description}` : '';
    const shortcut = this.shortcut ? ` · ${this.shortcut}` : '';
    return `${action}${description}${shortcut}`;
  }
}
