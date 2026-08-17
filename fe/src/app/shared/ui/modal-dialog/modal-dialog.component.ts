import {
  AfterViewChecked,
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { I18nService } from '../../../core/i18n.service';
import { AppTooltipDirective } from '../app-tooltip/app-tooltip.directive';

@Component({
  selector: 'app-modal-dialog',
  standalone: true,
  imports: [CommonModule, AppTooltipDirective],
  template: `
    <div
      *ngIf="isOpen"
      class="modal-backdrop"
      [class.dismissible]="!preventOutsideClose"
      (click)="onBackdropClick($event)"
    >
      <section
        #dialogBox
        class="modal-dialog-box"
        [class.compact]="size === 'compact'"
        [class.wide]="size === 'wide'"
        [class.large]="size === 'large'"
        role="dialog"
        aria-modal="true"
        [attr.aria-labelledby]="titleId"
        tabindex="-1"
      >
        <header class="modal-header">
          <h2 [id]="titleId" class="modal-title">{{ title }}</h2>
          <button
            type="button"
            class="close-btn"
            [attr.aria-label]="i18n.t('common.close')"
            [appTooltip]="i18n.t('modal.closeTooltip')"
            (click)="onClose()"
          >
            ✕
          </button>
        </header>

        <div #modalBody class="modal-body">
          <ng-content></ng-content>
        </div>

        <footer class="modal-actions" *ngIf="showFooter">
          <ng-content select="[modal-actions]"></ng-content>
        </footer>
      </section>
    </div>
  `,
  styles: [`
    :host {
      display: contents;
    }

    .modal-backdrop {
      position: fixed;
      inset: 0;
      z-index: var(--z-modal, 10000);
      display: grid;
      place-items: center;
      width: 100%;
      height: 100dvh;
      padding:
        max(12px, env(safe-area-inset-top))
        max(12px, env(safe-area-inset-right))
        max(12px, env(safe-area-inset-bottom))
        max(12px, env(safe-area-inset-left));
      overflow: hidden;
      box-sizing: border-box;
      background: rgb(15 23 42 / 62%);
      backdrop-filter: blur(4px);
      cursor: default;
      animation: fadeIn 0.16s ease-out;
    }

    .modal-backdrop.dismissible {
      cursor: pointer;
    }

    .modal-dialog-box {
      width: min(100%, 720px);
      max-height: calc(100dvh - 24px);
      display: flex;
      flex-direction: column;
      min-width: 0;
      min-height: 0;
      overflow: hidden;
      box-sizing: border-box;
      color: var(--ink, #0f172a);
      background: var(--surface, #ffffff);
      border: 1px solid var(--line, #e2e8f0);
      border-radius: var(--radius-lg, 16px);
      box-shadow: var(--shadow-modal, 0 24px 70px rgba(0, 0, 0, 0.38));
      cursor: default;
      animation: zoomIn 0.16s ease-out;
    }

    .modal-dialog-box.compact {
      width: min(100%, var(--modal-compact-max-width, 560px));
    }

    .modal-dialog-box.wide {
      width: min(100%, var(--modal-wide-max-width, 900px));
    }

    .modal-dialog-box.large {
      width: min(100%, var(--modal-large-max-width, 1120px));
    }

    .modal-header,
    .modal-actions {
      flex: 0 0 auto;
    }

    .modal-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      padding: 18px 20px;
      background: var(--surface-muted, #f8fafc);
      border-bottom: 1px solid var(--line, #e2e8f0);
    }

    .modal-title {
      min-width: 0;
      margin: 0;
      color: var(--ink, #1e293b);
      font-size: 1.2rem;
      font-weight: 700;
      line-height: 1.35;
      overflow-wrap: anywhere;
    }

    .close-btn {
      display: inline-grid;
      place-items: center;
      flex: 0 0 auto;
      width: 40px;
      height: 40px;
      padding: 0;
      border: 1px solid transparent;
      border-radius: 9px;
      background: transparent;
      color: var(--muted, #64748b);
      font-size: 1.15rem;
      line-height: 1;
      cursor: pointer;
      transition: background-color 0.15s ease, color 0.15s ease, border-color 0.15s ease;
    }

    .close-btn:hover {
      background: var(--surface-hover, #e2e8f0);
      border-color: var(--line, #e2e8f0);
      color: var(--ink, #0f172a);
    }

    .modal-body {
      flex: 1 1 auto;
      min-width: 0;
      min-height: 0;
      overflow-x: hidden;
      overflow-y: auto;
      overscroll-behavior: contain;
      scrollbar-gutter: stable;
      padding: var(--modal-body-padding, 20px);
    }

    .modal-actions {
      position: static;
      inset: auto;
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: flex-end;
      gap: 10px;
      margin: 0;
      padding: 14px 20px;
      background: var(--surface);
      border-top: 1px solid var(--line, #e2e8f0);
    }

    .modal-actions:empty {
      display: none;
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    @keyframes zoomIn {
      from { transform: translateY(6px) scale(0.985); opacity: 0; }
      to { transform: translateY(0) scale(1); opacity: 1; }
    }

    @media (max-width: 640px) {
      .modal-backdrop {
        padding:
          max(8px, env(safe-area-inset-top))
          max(8px, env(safe-area-inset-right))
          max(8px, env(safe-area-inset-bottom))
          max(8px, env(safe-area-inset-left));
      }

      .modal-dialog-box {
        width: 100% !important;
        max-height: calc(100dvh - 16px);
        border-radius: 12px;
      }

      .modal-header {
        padding: 14px 16px;
      }

      .modal-body {
        padding: 16px;
      }

      .modal-actions {
        padding: 12px 16px;
      }
    }

    @media (prefers-reduced-motion: reduce) {
      .modal-backdrop,
      .modal-dialog-box {
        animation: none;
      }
    }
  `]
})
export class ModalDialogComponent implements OnInit, OnChanges, OnDestroy, AfterViewChecked {
  @Input() isOpen = true;
  @Input() title = '';
  @Input() titleId = 'modal-title-' + Math.random().toString(36).substring(2, 9);
  @Input() size: 'compact' | 'normal' | 'wide' | 'large' = 'normal';
  @Input() showFooter = true;
  @Input() preventOutsideClose = false;
  @Input() preventEscapeClose = false;

  @Output() close = new EventEmitter<void>();
  @Output() closeModal = new EventEmitter<void>();

  @ViewChild('dialogBox')
  private dialogBox?: ElementRef<HTMLElement>;

  @ViewChild('modalBody')
  private modalBody?: ElementRef<HTMLElement>;

  readonly i18n = inject(I18nService);

  private wasOpen = false;
  private isLocked = false;
  private isTeleported = false;
  private focusOrigin: HTMLElement | null = null;

  private static openModals: ModalDialogComponent[] = [];
  private static previousBodyOverflow = '';
  private static previousBodyPaddingInlineEnd = '';

  constructor(private elementRef: ElementRef) {}

  ngOnInit(): void {
    if (this.isOpen) {
      this.activateModal();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['isOpen']) {
      return;
    }

    if (changes['isOpen'].currentValue === true) {
      this.activateModal();
    } else if (changes['isOpen'].previousValue === true) {
      this.deactivateModal();
    }
  }

  ngOnDestroy(): void {
    this.deactivateModal();
    if (this.isTeleported && typeof document !== 'undefined' && this.elementRef?.nativeElement?.parentNode) {
      this.elementRef.nativeElement.parentNode.removeChild(this.elementRef.nativeElement);
    }
  }

  ngAfterViewChecked(): void {
    if (this.isOpen && !this.wasOpen) {
      this.wasOpen = true;

      queueMicrotask(() => {
        const dialog = this.dialogBox?.nativeElement;
        const body = this.modalBody?.nativeElement ?? dialog?.querySelector<HTMLElement>('.modal-body');

        body?.scrollTo({ top: 0, left: 0, behavior: 'auto' });

        const firstControl = this.getFocusableElements(dialog)[0];
        (firstControl ?? dialog)?.focus({ preventScroll: true });

        body?.scrollTo({ top: 0, left: 0, behavior: 'auto' });
      });
    }

    if (!this.isOpen) {
      this.wasOpen = false;
    }
  }

  @HostListener('document:keydown', ['$event'])
  onDocumentKeydown(event: KeyboardEvent): void {
    if (!this.isOpen || !this.isTopMostModal()) {
      return;
    }

    if (event.key === 'Escape') {
      if (!this.preventEscapeClose) {
        event.preventDefault();
        this.onClose();
      }
      return;
    }

    if (event.key === 'Tab') {
      this.trapFocus(event);
    }
  }

  onBackdropClick(event: MouseEvent): void {
    if (
      this.isTopMostModal() &&
      !this.preventOutsideClose &&
      event.target === event.currentTarget
    ) {
      this.onClose();
    }
  }

  onClose(): void {
    this.close.emit();
    this.closeModal.emit();
  }

  private activateModal(): void {
    if (typeof document !== 'undefined' && !this.isLocked) {
      const activeElement = document.activeElement;
      this.focusOrigin = activeElement instanceof HTMLElement ? activeElement : null;
    }

    this.teleportToBody();
    this.lockBodyScroll();
  }

  private deactivateModal(): void {
    this.unlockBodyScroll();
    this.restoreFocus();
  }

  private teleportToBody(): void {
    if (!this.isTeleported && typeof document !== 'undefined' && document.body) {
      document.body.appendChild(this.elementRef.nativeElement);
      this.isTeleported = true;
    }
  }

  private lockBodyScroll(): void {
    if (this.isLocked || typeof document === 'undefined') {
      return;
    }

    this.isLocked = true;

    if (ModalDialogComponent.openModals.length === 0) {
      const body = document.body;
      const view = document.defaultView;

      ModalDialogComponent.previousBodyOverflow = body.style.overflow;
      ModalDialogComponent.previousBodyPaddingInlineEnd = body.style.paddingInlineEnd;

      const scrollbarWidth = view
        ? Math.max(0, view.innerWidth - document.documentElement.clientWidth)
        : 0;

      if (scrollbarWidth > 0 && view) {
        const currentPadding = Number.parseFloat(view.getComputedStyle(body).paddingInlineEnd) || 0;
        body.style.paddingInlineEnd = `${currentPadding + scrollbarWidth}px`;
      }

      body.style.overflow = 'hidden';
    }

    ModalDialogComponent.openModals.push(this);
  }

  private unlockBodyScroll(): void {
    if (!this.isLocked || typeof document === 'undefined') {
      return;
    }

    this.isLocked = false;
    ModalDialogComponent.openModals = ModalDialogComponent.openModals.filter((modal) => modal !== this);

    if (ModalDialogComponent.openModals.length === 0) {
      document.body.style.overflow = ModalDialogComponent.previousBodyOverflow;
      document.body.style.paddingInlineEnd = ModalDialogComponent.previousBodyPaddingInlineEnd;
    }
  }

  private restoreFocus(): void {
    const origin = this.focusOrigin;
    this.focusOrigin = null;

    if (!origin) {
      return;
    }

    queueMicrotask(() => {
      if (origin.isConnected) {
        origin.focus({ preventScroll: true });
      }
    });
  }

  private trapFocus(event: KeyboardEvent): void {
    const dialog = this.dialogBox?.nativeElement;
    if (!dialog) {
      return;
    }

    const focusable = this.getFocusableElements(dialog);
    if (focusable.length === 0) {
      event.preventDefault();
      dialog.focus({ preventScroll: true });
      return;
    }

    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    const activeElement = document.activeElement;
    const activeInside = activeElement instanceof Node && dialog.contains(activeElement);

    if (event.shiftKey && (!activeInside || activeElement === first)) {
      event.preventDefault();
      last.focus({ preventScroll: true });
      return;
    }

    if (!event.shiftKey && (!activeInside || activeElement === last)) {
      event.preventDefault();
      first.focus({ preventScroll: true });
    }
  }

  private getFocusableElements(container?: HTMLElement): HTMLElement[] {
    if (!container) {
      return [];
    }

    const selector = [
      'a[href]',
      'area[href]',
      'button:not([disabled])',
      'input:not([disabled]):not([type="hidden"])',
      'select:not([disabled])',
      'textarea:not([disabled])',
      '[contenteditable="true"]',
      '[tabindex]:not([tabindex="-1"])',
    ].join(',');

    return Array.from(container.querySelectorAll<HTMLElement>(selector)).filter((element) => {
      return element.getClientRects().length > 0 && element.getAttribute('aria-hidden') !== 'true';
    });
  }

  private isTopMostModal(): boolean {
    const stack = ModalDialogComponent.openModals;
    return stack.length === 0 || stack[stack.length - 1] === this;
  }
}
