import { Injectable, computed, signal } from '@angular/core';

/**
 * Central registry of page-level modal dialogs (BUG-1/BUG-5 of the shortcut audit).
 *
 * `ModalDialogComponent` reports its lifecycle here so the shell's global
 * keyboard handler can suppress shortcuts while any dialog is open. Shell-owned
 * panels (quick-nav, help, logout) keep their own signals — they are the shell's
 * own consumers, not blockers.
 */
@Injectable({ providedIn: 'root' })
export class DialogStateService {
  private readonly modalDepth = signal(0);

  /** True while at least one page-level modal is open anywhere in the app. */
  readonly modalOpen = computed(() => this.modalDepth() > 0);

  /** Number of currently open page-level modals (topmost tracking stays local to the component). */
  readonly openModalCount = computed(() => this.modalDepth());

  modalOpened(): void {
    this.modalDepth.update((depth) => depth + 1);
  }

  modalClosed(): void {
    this.modalDepth.update((depth) => Math.max(0, depth - 1));
  }
}
