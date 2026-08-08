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
import { ScreenShortcutDestination } from '../../../core/shortcuts/screen-shortcut.models';
import { displayShortcutKey } from '../../../core/shortcuts/shortcut-key.util';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { ConfirmDialogService } from '../../../core/confirm-dialog.service';
import { apiErrorMessage } from '../../../core/api-error';
import { AppTooltipDirective } from '../../../shared/ui/app-tooltip/app-tooltip.directive';

export interface ShortcutDraft {
  clientId: string;
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

  readonly maxShortcuts = 20;
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

  private draftSequence = 0;

  destinationOptions(index: number): {
    pageCode: string;
    title: string;
    unavailable: boolean;
  }[] {
    const currentDraft = this.drafts()[index];
    const assignedElsewhere = new Set(
      this.drafts()
        .filter((_, itemIndex) => itemIndex !== index)
        .map((item) => item.pageCode),
    );

    const options = this.availableDestinations().map((dest) => ({
      pageCode: dest.pageCode,
      title: this.i18n.t(dest.titleKey),
      unavailable: assignedElsewhere.has(dest.pageCode),
    }));

    if (!currentDraft) return options;

    const known = new Set(options.map((option) => option.pageCode));
    if (!known.has(currentDraft.pageCode)) {
      const status = this.getAvailabilityStatus(currentDraft.pageCode);
      options.unshift({
        pageCode: currentDraft.pageCode,
        title: `${this.getDestinationTitle(currentDraft.pageCode)} (${this.i18n.t(status.statusKey)})`,
        unavailable: true,
      });
    }

    return options;
  }

  async ngOnInit(): Promise<void> {
    const profile = this.profile() ?? (await this.shortcutService.load());
    if (profile) {
      this.loadDraftsFromProfile();
    }
  }

  loadDraftsFromProfile(): void {
    const profile = this.profile();
    if (!profile) return;

    this.captureIndex.set(null);
    this.drafts.set(
      profile.shortcuts.map((item) => ({
        clientId: item.id ? `saved-${item.id}` : this.nextDraftId('saved'),
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
      this.warn('shortcuts.invalidKey');
      return;
    }

    const duplicate = this.drafts().some(
      (item, itemIndex) =>
        itemIndex !== index && item.secondKeyCode === event.code,
    );

    if (duplicate) {
      this.warn('shortcuts.duplicateKey');
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

  changeDestination(index: number, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const newPageCode = select.value;
    const previousPageCode = this.drafts()[index]?.pageCode;

    if (!previousPageCode || newPageCode === previousPageCode) return;

    const duplicate = this.drafts().some(
      (item, itemIndex) =>
        itemIndex !== index && item.pageCode === newPageCode,
    );

    if (duplicate) {
      this.warn('shortcuts.duplicateDestination');
      select.value = previousPageCode;
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
    this.captureIndex.set(null);
  }

  addShortcut(): void {
    const currentDrafts = this.drafts();
    if (currentDrafts.length >= this.maxShortcuts) {
      this.warn('shortcuts.limitExceeded');
      return;
    }

    const assignedPages = new Set(currentDrafts.map((d) => d.pageCode));
    const available = this.availableDestinations().find(
      (d) => !assignedPages.has(d.pageCode),
    );

    if (!available) {
      this.warn('shortcuts.noMoreDestinations');
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
      {
        clientId: this.nextDraftId('new'),
        pageCode: available.pageCode,
        secondKeyCode: nextKey,
        enabled: true,
      },
    ]);
  }

  async save(): Promise<void> {
    const profile = this.profile();
    if (!profile) {
      this.errorMessage(this.i18n.t(this.error() ?? 'shortcuts.loadFailed'));
      return;
    }

    const currentDrafts = this.drafts();

    const keys = new Set<string>();
    for (const d of currentDrafts) {
      if (keys.has(d.secondKeyCode)) {
        this.errorMessage(this.i18n.t('shortcuts.duplicateKey'));
        return;
      }
      keys.add(d.secondKeyCode);
    }

    const pages = new Set<string>();
    for (const d of currentDrafts) {
      if (pages.has(d.pageCode)) {
        this.errorMessage(this.i18n.t('shortcuts.duplicateDestination'));
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
      this.loadDraftsFromProfile();
      const successMsg = this.i18n.t('shortcuts.saved');
      this.notification.success(successMsg);
      this.liveAnnouncement.set(successMsg);
    } catch (err) {
      this.errorMessage(apiErrorMessage(err, this.i18n));
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
      this.errorMessage(apiErrorMessage(err, this.i18n));
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

  private nextDraftId(prefix: string): string {
    this.draftSequence += 1;
    return `${prefix}-${this.draftSequence}`;
  }

  private warn(key: string): void {
    const message = this.i18n.t(key);
    this.notification.warning(message);
    this.liveAnnouncement.set(message);
  }

  private errorMessage(message: string): void {
    this.notification.error(message);
    this.liveAnnouncement.set(message);
  }
}
