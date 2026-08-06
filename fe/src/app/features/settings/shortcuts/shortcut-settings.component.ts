import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ScreenShortcutService } from '../../../core/shortcuts/screen-shortcut.service';
import {
  ScreenShortcutDestination,
  ShortcutAvailability,
} from '../../../core/shortcuts/screen-shortcut.models';
import { displayShortcutKey } from '../../../core/shortcuts/shortcut-key.util';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { ConfirmDialogService } from '../../../core/confirm-dialog.service';
import { apiErrorMessage } from '../../../core/api-error';
import { IconComponent } from '../../../shared/ui/icon/icon.component';
import { AppTooltipDirective } from '../../../shared/ui/app-tooltip/app-tooltip.directive';

export interface ShortcutDraft {
  pageCode: string;
  secondKeyCode: string;
  enabled: boolean;
}

@Component({
  selector: 'app-shortcut-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, AppTooltipDirective],
  templateUrl: './shortcut-settings.component.html',
  styleUrl: './shortcut-settings.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShortcutSettingsComponent implements OnInit {
  readonly shortcutService = inject(ScreenShortcutService);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  readonly confirm = inject(ConfirmDialogService);

  readonly drafts = signal<ShortcutDraft[]>([]);
  readonly captureIndex = signal<number | null>(null);
  readonly liveAnnouncement = signal<string>('');

  readonly profile = computed(() => this.shortcutService.profile());
  readonly loading = computed(() => this.shortcutService.loading());
  readonly saving = computed(() => this.shortcutService.saving());
  readonly error = computed(() => this.shortcutService.error());

  readonly availableDestinations = computed<ScreenShortcutDestination[]>(
    () => this.profile()?.availableDestinations ?? [],
  );

  async ngOnInit(): Promise<void> {
    const profile =
      this.profile() ?? (await this.shortcutService.load());
    if (profile) {
      this.loadDraftsFromProfile();
    }
  }

  loadDraftsFromProfile(): void {
    const profile = this.profile();
    if (!profile) return;
    this.drafts.set(
      profile.shortcuts.map((item) => ({
        pageCode: item.pageCode,
        secondKeyCode: item.secondKeyCode,
        enabled: item.enabled,
      })),
    );
  }

  displayKey(code: string): string {
    return displayShortcutKey(code);
  }

  captureKey(index: number, event: KeyboardEvent): void {
    event.preventDefault();
    event.stopPropagation();

    if (event.key === 'Escape') {
      this.captureIndex.set(null);
      return;
    }

    const valid =
      /^Key[A-Z]$/.test(event.code) || /^Digit[0-9]$/.test(event.code);

    if (!valid) {
      const msg = this.i18n.t('shortcuts.invalidKey');
      this.notification.warning(msg);
      this.liveAnnouncement.set(msg);
      return;
    }

    const duplicate = this.drafts().some(
      (item, itemIndex) =>
        itemIndex !== index && item.secondKeyCode === event.code,
    );

    if (duplicate) {
      const msg = this.i18n.t('shortcuts.duplicateKey');
      this.notification.warning(msg);
      this.liveAnnouncement.set(msg);
      return;
    }

    const keyLabel = this.displayKey(event.code);
    this.drafts.update((items) =>
      items.map((item, itemIndex) =>
        itemIndex === index
          ? { ...item, secondKeyCode: event.code }
          : item,
      ),
    );

    this.captureIndex.set(null);
    this.liveAnnouncement.set(
      this.i18n.t('shortcuts.saved', undefined, `Key ${keyLabel} captured`),
    );
  }

  changeDestination(index: number, newPageCode: string): void {
    const duplicate = this.drafts().some(
      (item, itemIndex) =>
        itemIndex !== index && item.pageCode === newPageCode,
    );

    if (duplicate) {
      this.notification.warning(
        this.i18n.t('shortcuts.duplicateDestination'),
      );
      return;
    }

    this.drafts.update((items) =>
      items.map((item, itemIndex) =>
        itemIndex === index ? { ...item, pageCode: newPageCode } : item,
      ),
    );
  }

  toggleEnabled(index: number, enabled: boolean): void {
    this.drafts.update((items) =>
      items.map((item, itemIndex) =>
        itemIndex === index ? { ...item, enabled } : item,
      ),
    );
  }

  remove(index: number): void {
    this.drafts.update((items) => items.filter((_, i) => i !== index));
  }

  addShortcut(): void {
    const currentDrafts = this.drafts();
    if (currentDrafts.length >= 20) {
      this.notification.warning(this.i18n.t('shortcuts.limitExceeded'));
      return;
    }

    const assignedPages = new Set(currentDrafts.map((d) => d.pageCode));
    const available = this.availableDestinations().find(
      (d) => !assignedPages.has(d.pageCode),
    );

    if (!available) {
      this.notification.warning(
        this.i18n.t('shortcuts.noMoreDestinations'),
      );
      return;
    }

    const assignedKeys = new Set(currentDrafts.map((d) => d.secondKeyCode));
    let nextKey = 'KeyA';
    for (let c = 65; c <= 90; c++) {
      const candidate = `Key${String.fromCharCode(c)}`;
      if (!assignedKeys.has(candidate)) {
        nextKey = candidate;
        break;
      }
    }

    this.drafts.update((items) => [
      ...items,
      { pageCode: available.pageCode, secondKeyCode: nextKey, enabled: true },
    ]);
  }

  async save(): Promise<void> {
    const profile = this.profile();
    if (!profile) return;

    const currentDrafts = this.drafts();

    // Check duplicate keys
    const keys = new Set<string>();
    for (const d of currentDrafts) {
      if (keys.has(d.secondKeyCode)) {
        this.notification.error(this.i18n.t('shortcuts.duplicateKey'));
        return;
      }
      keys.add(d.secondKeyCode);
    }

    // Check duplicate destinations
    const pages = new Set<string>();
    for (const d of currentDrafts) {
      if (pages.has(d.pageCode)) {
        this.notification.error(
          this.i18n.t('shortcuts.duplicateDestination'),
        );
        return;
      }
      pages.add(d.pageCode);
    }

    try {
      await this.shortcutService.replace({
        expectedVersion: profile.version,
        shortcuts: currentDrafts.map((item) => ({
          secondKeyCode: item.secondKeyCode,
          pageCode: item.pageCode,
          enabled: item.enabled,
        })),
      });
      const successMsg = this.i18n.t('shortcuts.saved');
      this.notification.success(successMsg);
      this.liveAnnouncement.set(successMsg);
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    }
  }

  async reset(): Promise<void> {
    const confirmed = await this.confirm.confirmOptions({
      titleKey: 'shortcuts.resetTitle',
      messageKey: 'shortcuts.resetMessage',
      confirmKey: 'shortcuts.resetConfirm',
      danger: true,
    });

    if (!confirmed) return;

    try {
      await this.shortcutService.reset();
      this.loadDraftsFromProfile();
      const successMsg = this.i18n.t('shortcuts.resetSuccess');
      this.notification.success(successMsg);
      this.liveAnnouncement.set(successMsg);
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    }
  }

  getAvailabilityStatus(pageCode: string): { statusKey: string; cssClass: string } {
    const profileItem = this.profile()?.shortcuts.find(
      (s) => s.pageCode === pageCode,
    );
    if (!profileItem) {
      return { statusKey: 'shortcuts.available', cssClass: 'status-available' };
    }

    switch (profileItem.availabilityStatus) {
      case 'AVAILABLE':
        return { statusKey: 'shortcuts.available', cssClass: 'status-available' };
      case 'NO_ROLE':
        return { statusKey: 'shortcuts.noRole', cssClass: 'status-unavailable' };
      case 'MENU_NOT_ALLOWED':
        return { statusKey: 'shortcuts.menuNotAllowed', cssClass: 'status-unavailable' };
      case 'FEATURE_DISABLED':
        return { statusKey: 'shortcuts.featureDisabled', cssClass: 'status-unavailable' };
      case 'PAGE_REMOVED':
        return { statusKey: 'shortcuts.pageRemoved', cssClass: 'status-unavailable' };
      case 'DISABLED':
        return { statusKey: 'shortcuts.disabled', cssClass: 'status-warning' };
      default:
        return { statusKey: 'shortcuts.available', cssClass: 'status-available' };
    }
  }

  getDestinationTitle(pageCode: string): string {
    const dest = this.availableDestinations().find((d) => d.pageCode === pageCode);
    return dest ? this.i18n.t(dest.titleKey) : pageCode;
  }
}
