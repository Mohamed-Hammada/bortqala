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
  templateUrl: './modal-dialog.component.html',
  styleUrls: ['./modal-dialog.component.scss']
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
