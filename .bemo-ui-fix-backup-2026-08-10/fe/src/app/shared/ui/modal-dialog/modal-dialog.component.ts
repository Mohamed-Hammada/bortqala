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
    <div *ngIf="isOpen" class="modal-backdrop" (click)="onBackdropClick($event)">
      <section
        #dialogBox
        class="modal-dialog-box"
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
      padding: 16px;

      overflow: hidden;
      box-sizing: border-box;
      background: rgb(15 23 42 / 58%);
      backdrop-filter: blur(4px);
      animation: fadeIn 0.2s ease-out;
    }

    .modal-dialog-box {
      width: min(100%, 720px);
      max-height: calc(100dvh - 32px);

      display: flex;
      flex-direction: column;

      min-height: 0;
      overflow: hidden;
      box-sizing: border-box;

      color: var(--ink, #0f172a);
      background: var(--surface, #ffffff);
      border: 1px solid var(--line, #e2e8f0);
      border-radius: var(--radius-lg, 16px);
      box-shadow: var(--shadow-modal, 0 24px 70px rgba(0, 0, 0, 0.38));
      animation: zoomIn 0.2s ease-out;
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
      padding: 1.25rem 1.5rem;
      background: var(--surface-muted, #f8fafc);
      border-bottom: 1px solid var(--line, #e2e8f0);
    }

    .modal-title {
      margin: 0;
      font-size: 1.25rem;
      font-weight: 700;
      color: var(--ink, #1e293b);
    }

    .close-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      flex: 0 0 auto;
      width: 32px;
      height: 32px;
      border: none;
      border-radius: 8px;
      background: transparent;
      color: var(--muted, #64748b);
      font-size: 1.25rem;
      cursor: pointer;
      transition: all 0.15s ease;
    }

    .close-btn:hover {
      background: var(--surface-hover, #e2e8f0);
      color: var(--ink, #0f172a);
    }

    .modal-body {
      flex: 1 1 auto;
      min-height: 0;

      overflow-x: hidden;
      overflow-y: auto;
      overscroll-behavior: contain;

      padding: var(--modal-body-padding, 20px);
    }

    .modal-actions {
      position: static;
      inset: auto;

      display: flex;
      flex-wrap: wrap;
      gap: 10px;

      margin: 0;
      padding: 16px 20px;

      background: var(--surface);
      border-top: 1px solid var(--line, #e2e8f0);
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    @keyframes zoomIn {
      from { transform: scale(0.95); opacity: 0; }
      to { transform: scale(1); opacity: 1; }
    }

    @media (max-width: 640px) {
      .modal-dialog-box {
        width: 100% !important;
        max-height: calc(100dvh - 32px);
        border-radius: 12px;
      }

      .modal-header {
        padding: 1rem 1.25rem;
      }

      .modal-actions {
        padding: 12px 16px;
      }
    }
  `]
})
export class ModalDialogComponent implements OnInit, OnChanges, OnDestroy, AfterViewChecked {
  @Input() isOpen = true;
  @Input() title = '';
  @Input() titleId = 'modal-title-' + Math.random().toString(36).substring(2, 9);
  @Input() size: 'normal' | 'wide' | 'large' = 'normal';
  @Input() showFooter = true;
  @Input() preventOutsideClose = false;

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
  private static openModalsCount = 0;

  constructor(private elementRef: ElementRef) {}

  ngOnInit(): void {
    if (this.isOpen) {
      this.teleportToBody();
      this.lockBodyScroll();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen']) {
      if (changes['isOpen'].currentValue === true) {
        this.teleportToBody();
        this.lockBodyScroll();
      } else if (changes['isOpen'].previousValue === true) {
        this.unlockBodyScroll();
      }
    }
  }

  ngOnDestroy(): void {
    this.unlockBodyScroll();
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

        if (body) {
          body.scrollTo({ top: 0, left: 0, behavior: 'auto' });
        }

        const firstControl = dialog?.querySelector<HTMLElement>(
          '.modal-body input:not([disabled]):not([readonly]), .modal-body select:not([disabled]), .modal-body textarea:not([disabled]), .modal-body button:not([disabled])',
        ) ?? dialog?.querySelector<HTMLElement>(
          'input:not([disabled]):not([readonly]), select:not([disabled]), textarea:not([disabled]), button:not([disabled])',
        );

        if (firstControl) {
          firstControl.focus({ preventScroll: true });
        } else if (dialog) {
          dialog.focus({ preventScroll: true });
        }

        if (body) {
          body.scrollTo({ top: 0, left: 0, behavior: 'auto' });
        }
      });
    }

    if (!this.isOpen) {
      this.wasOpen = false;
    }
  }

  @HostListener('document:keydown.escape', ['$event'])
  onEscapeKey(event: Event) {
    if (this.isOpen) {
      event.preventDefault();
      this.onClose();
    }
  }

  onBackdropClick(event: MouseEvent) {
    if (!this.preventOutsideClose && event.target === event.currentTarget) {
      this.onClose();
    }
  }

  onClose() {
    this.close.emit();
    this.closeModal.emit();
  }

  private teleportToBody(): void {
    if (!this.isTeleported && typeof document !== 'undefined' && document.body) {
      document.body.appendChild(this.elementRef.nativeElement);
      this.isTeleported = true;
    }
  }

  private lockBodyScroll() {
    if (!this.isLocked && typeof document !== 'undefined') {
      this.isLocked = true;
      ModalDialogComponent.openModalsCount++;
      document.body.style.overflow = 'hidden';
    }
  }

  private unlockBodyScroll() {
    if (this.isLocked && typeof document !== 'undefined') {
      this.isLocked = false;
      ModalDialogComponent.openModalsCount = Math.max(0, ModalDialogComponent.openModalsCount - 1);
      if (ModalDialogComponent.openModalsCount === 0) {
        document.body.style.overflow = '';
      }
    }
  }
}
