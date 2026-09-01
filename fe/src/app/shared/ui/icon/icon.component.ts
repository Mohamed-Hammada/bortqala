import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type IconName =
  | 'dashboard'
  | 'categories'
  | 'employees'
  | 'imports'
  | 'reports'
  | 'users'
  | 'settings'
  | 'logout'
  | 'menu'
  | 'close'
  | 'panel-expand'
  | 'panel-collapse'
  | 'expand-all'
  | 'collapse-all'
  | 'arrow-up'
  | 'arrow-down'
  | 'eye'
  | 'eye-off'
  | 'star'
  | 'clock'
  | 'bell'
  | 'chat'
  | 'wallet'
  | 'cart'
  | 'boxes'
  | 'banknote'
  | 'building'
  | 'factory'
  | 'search';

@Component({
  selector: 'app-icon',
  templateUrl: './icon.component.html',
  styleUrl: './icon.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IconComponent {
  readonly name = input.required<IconName>();
}
