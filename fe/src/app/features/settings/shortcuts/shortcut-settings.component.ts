import {
  AfterViewChecked,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
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
export class ShortcutSettingsComponent implements OnInit, AfterViewChecked {
  readonly shortcutService = inject(ScreenShortcutService);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  readonly confirm = inject(ConfirmDialogService);
  private readonly host = inject(ElementRef<HTMLElement>);

  readonly maxShortcuts = 20;
  readonly drafts = signal<ShortcutDraft[]>([]);
  readonly captureIndex = signal<number | null>(null);
  readonly lastAddedClientId = signal<string | null>(null);
  readonly liveAnnouncement = signal<string>('');
  readonly searchQuery = signal('');
  readonly editingClientId = signal<string | null>(null);
  private readonly editingSnapshot = signal<ShortcutDraft | null>(null);

  readonly profile = computed(() => this.shortcutService.profile());
  readonly loading = computed(() => this.shortcutService.loading());
  readonly saving = computed(() => this.shortcutService.saving());
  readonly error = computed(() => this.shortcutService.error());

  readonly availableDestinations = computed<ScreenShortcutDestination[]>(
    () => this.profile()?.availableDestinations ?? [],
  );

  readonly hasUnsavedChanges = computed<boolean>(() => {
    const profile = this.profile();
    if (!profile) return false;
    const original = profile.shortcuts;
    const current = this.drafts();
    if (original.length !== current.length) return true;
    for (let i = 0; i < original.length; i++) {
      if (
        original[i].pageCode !== current[i].pageCode ||
        original[i].secondKeyCode !== current[i].secondKeyCode ||
        original[i].enabled !== current[i].enabled
      ) {
        return true;
      }
    }
    return false;
  });

  // Source of truth for duplicate prevention is the CURRENT UI list.
  // This includes saved rows, edited rows, and newly-added unsaved rows.
  readonly usedTargetCodes = computed<Set<string>>(
    () => new Set(this.drafts().map((draft) => draft.pageCode)),
  );

  readonly usedShortcutKeys = computed<Set<string>>(
    () => new Set(this.drafts().map((draft) => draft.secondKeyCode)),
  );

  readonly remainingDestinations = computed<ScreenShortcutDestination[]>(() =>
    this.availableDestinations().filter(
      (destination) => !this.usedTargetCodes().has(destination.pageCode),
    ),
  );

  readonly remainingShortcutKeys = computed<string[]>(() => {
    const letters = Array.from({ length: 26 }, (_, index) =>
      `Key${String.fromCharCode(65 + index)}`,
    );
    const digits = Array.from({ length: 10 }, (_, index) =>
      `Digit${index}`,
    );
    return [...letters, ...digits].filter(
      (keyCode) => !this.usedShortcutKeys().has(keyCode),
    );
  });

  readonly canAddShortcut = computed(
    () =>
      !this.loading() &&
      !this.saving() &&
      this.drafts().length < this.maxShortcuts &&
      this.remainingDestinations().length > 0 &&
      this.remainingShortcutKeys().length > 0,
  );

  readonly filteredDrafts = computed<{ draft: ShortcutDraft; index: number }[]>(() => {
    const query = this.searchQuery().trim().toLowerCase();
    if (!query) {
      return this.drafts().map((draft, index) => ({ draft, index }));
    }
    return this.drafts()
      .map((draft, index) => ({ draft, index }))
      .filter(({ draft }) => {
        const target = this.getDestinationTitle(draft.pageCode).toLowerCase();
        return (
          target.includes(query) ||
          draft.pageCode.toLowerCase().includes(query) ||
          draft.secondKeyCode.toLowerCase().includes(query)
        );
      });
  });

  private draftSequence = 0;

  destinationOptions(index: number): {
    pageCode: string;
    title: string;
    unavailable: boolean;
  }[] {
    const currentDraft = this.drafts()[index];
    // Keep the selector focused: show only this row's current page plus
    // destinations that are not already assigned to another shortcut.
    const remainingPageCodes = new Set(
      this.remainingDestinations().map((destination) => destination.pageCode),
    );
    const options = this.availableDestinations()
      .filter(
        (dest) =>
          dest.pageCode === currentDraft?.pageCode ||
          remainingPageCodes.has(dest.pageCode),
      )
      .map((dest) => ({
        pageCode: dest.pageCode,
        title: this.i18n.t(dest.titleKey),
        unavailable: false,
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
    this.lastAddedClientId.set(null);
    this.editingClientId.set(null);
    this.editingSnapshot.set(null);
    this.searchQuery.set('');
    this.drafts.set(
      profile.shortcuts.map((item) => ({
        clientId: item.id ? `saved-${item.id}` : this.nextDraftId('saved'),
        pageCode: item.pageCode,
        secondKeyCode: item.secondKeyCode,
        enabled: item.enabled,
      })),
    );
  }

  discardChanges(): void {
    this.loadDraftsFromProfile();
    this.liveAnnouncement.set(
      this.i18n.t('shortcuts.discardChangesSuccess'),
    );
  }

  onSearchChange(): void {
    const editingId = this.editingClientId();
    if (!editingId) return;
    const stillVisible = this.filteredDrafts().some(
      (entry) => entry.draft.clientId === editingId,
    );
    if (!stillVisible) {
      this.cancelEdit();
    }
  }

  beginEdit(index: number): void {
    const draft = this.drafts()[index];
    if (!draft || this.saving()) return;
    this.editingSnapshot.set(draft);
    this.editingClientId.set(draft.clientId);
    this.captureIndex.set(null);
    setTimeout(() => {
      const row = this.host.nativeElement.querySelector(
        `[data-edit-client-id="${draft.clientId}"]`,
      ) as HTMLElement | null;
      row?.focus({ preventScroll: true });
    });
  }

  cancelEdit(): void {
    const clientId = this.editingClientId();
    if (!clientId) return;
    const snapshot = this.editingSnapshot();
    this.drafts.update((items) =>
      items.map((item) =>
        item.clientId === clientId && snapshot ? snapshot : item,
      ),
    );
    this.editingClientId.set(null);
    this.editingSnapshot.set(null);
    this.captureIndex.set(null);
  }

  saveEdit(index: number): void {
    const draft = this.drafts()[index];
    if (!draft || draft.clientId !== this.editingClientId()) return;

    if (!draft.secondKeyCode) {
      this.warn('shortcuts.invalidKey');
      return;
    }
    const duplicateKey = this.drafts().some(
      (item, itemIndex) =>
        itemIndex !== index && item.secondKeyCode === draft.secondKeyCode,
    );
    if (duplicateKey) {
      this.warn('shortcuts.duplicateKey');
      return;
    }
    const duplicateDestination = this.drafts().some(
      (item, itemIndex) =>
        itemIndex !== index && item.pageCode === draft.pageCode,
    );
    if (duplicateDestination) {
      this.warn('shortcuts.duplicateDestination');
      return;
    }

    this.editingClientId.set(null);
    this.editingSnapshot.set(null);
    this.captureIndex.set(null);
    const savedMsg = this.i18n.t('shortcuts.rowSaved');
    this.liveAnnouncement.set(savedMsg);
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

    if (!previousPageCode || !newPageCode || newPageCode === previousPageCode) {
      if (previousPageCode) select.value = previousPageCode;
      return;
    }

    const offered = this.destinationOptions(index).some(
      (option) => option.pageCode === newPageCode,
    );
    if (!offered) {
      select.value = previousPageCode;
      return;
    }

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

  ngAfterViewChecked(): void {
    const selects = this.host.nativeElement.querySelectorAll?.(
      'select.shortcut-destination-select',
    );
    if (!selects || selects.length === 0) return;

    const drafts = this.drafts();
    selects.forEach((element: Element) => {
      const select = element as HTMLSelectElement;
      const clientId = select.getAttribute('data-edit-client-id');
      const wanted = drafts.find((draft) => draft.clientId === clientId)?.pageCode;
      if (wanted && select.value !== wanted) select.value = wanted;
    });
  }

  toggleEnabled(index: number, enabled: boolean): void {
    this.drafts.update((items) =>
      items.map((item, itemIndex) =>
        itemIndex === index ? { ...item, enabled } : item,
      ),
    );
  }

  remove(index: number): void {
    const removed = this.drafts()[index];
    const removedClientId = removed?.clientId;
    this.drafts.update((items) => items.filter((_, i) => i !== index));
    this.captureIndex.set(null);

    if (removedClientId === this.editingClientId()) {
      this.editingClientId.set(null);
      this.editingSnapshot.set(null);
    }

    if (removedClientId && this.lastAddedClientId() === removedClientId) {
      this.lastAddedClientId.set(null);
    }
  }

  addShortcut(): void {
    const currentDrafts = this.drafts();

    if (this.loading() || this.saving()) return;

    if (currentDrafts.length >= this.maxShortcuts) {
      this.warn('shortcuts.limitExceeded');
      return;
    }

    // Recalculate from the CURRENT UI rows on every click. This includes
    // unsaved shortcuts added just before this one.
    const available = this.remainingDestinations()[0];
    const nextKey = this.remainingShortcutKeys()[0];

    if (!available) {
      this.warn('shortcuts.noMoreDestinations');
      return;
    }

    if (!nextKey) {
      this.warn('shortcuts.limitExceeded');
      return;
    }

    const clientId = this.nextDraftId('new');
    const newDraft: ShortcutDraft = {
      clientId,
      pageCode: available.pageCode,
      secondKeyCode: nextKey,
      enabled: true,
    };
    this.drafts.update((items) => [newDraft, ...items]);

    this.captureIndex.set(null);
    this.lastAddedClientId.set(clientId);
    this.editingSnapshot.set(newDraft);
    this.editingClientId.set(clientId);
    this.liveAnnouncement.set(
      `${this.i18n.t('shortcuts.add')}: ${this.getDestinationTitle(available.pageCode)}`,
    );
    this.revealAddedShortcut(clientId);
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

  private revealAddedShortcut(clientId: string): void {
    setTimeout(() => {
      const row = this.host.nativeElement.querySelector(
        `[data-shortcut-client-id="${clientId}"]`,
      ) as HTMLElement | null;
      if (!row) return;

      if (typeof row.scrollIntoView === 'function') {
        row.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      }

      const destinationSelect = row.querySelector(
        '.shortcut-destination-select',
      ) as HTMLSelectElement | null;
      destinationSelect?.focus({ preventScroll: true });
    });
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
