import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { AppTooltipDirective } from '../app-tooltip/app-tooltip.directive';

@Component({
  selector: 'app-icon-button',
  standalone: true,
  imports: [AppTooltipDirective],
  templateUrl: './icon-button.component.html',
  styleUrls: ['./icon-button.component.scss'],
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
