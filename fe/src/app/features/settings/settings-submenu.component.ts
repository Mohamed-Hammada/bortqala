import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';
import { I18nService } from '../../core/i18n.service';
import { SETTINGS_SUBMENU_GROUPS, SettingsTab } from './settings-navigation';

@Component({
  selector: 'app-settings-submenu',
  standalone: true,
  templateUrl: './settings-submenu.component.html',
  styleUrl: './settings-submenu.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SettingsSubmenuComponent {
  readonly auth = inject(AuthService);
  readonly i18n = inject(I18nService);
  readonly groups = SETTINGS_SUBMENU_GROUPS;

  @Input({ required: true }) activeTab!: SettingsTab;
  @Output() readonly tabChange = new EventEmitter<SettingsTab>();

  isAdmin(): boolean {
    return this.auth.hasAnyRole(['SUPER_ADMIN', 'ADMIN']);
  }

  select(tab: SettingsTab): void {
    this.tabChange.emit(tab);
  }
}
