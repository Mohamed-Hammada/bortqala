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
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppTooltipDirective } from '../app-tooltip/app-tooltip.directive';

@Component({
  selector: 'app-modal-dialog',
  standalone: true,
  imports: [CommonModule, AppTooltipDirective],
  template: `
    <div *ngIf="isOpen" class="modal-backdrop" (click)="onBackdropClick($event)">
      <div 
        #dialogBox
        class="modal-dialog-box" 
        [class.wide]="size === 'wide'"
        [class.large]="size === 'large'"
        role="dialog" 
        aria-modal="true" 
        [attr.aria-labelledby]="titleId"
        tabindex="-1"
        (click)="$event.stopPropagation()">
        
        <header class="modal-header">
          <h2 [id]="titleId" class="modal-title">{{ title }}</h2>
          <button type="button" class="close-btn" aria-label="إغلاق النافذة" appTooltip="إغلاق — إلغاء وإغلاق النافذة · Esc" (click)="onClose()">
            ✕
          </button>
        </header>

        <div class="modal-body" #modalBodyRef>
          <ng-content></ng-content>
        </div>

        <footer class="modal-footer" *ngIf="showFooter">
          <ng-content select="[modal-actions]"></ng-content>
        </footer>
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: contents;
    }

    .modal-backdrop {
      position: fixed;
      inset: 0;
      width: 100vw;
      height: 100dvh;
      background: rgba(15, 23, 42, 0.65);
      backdrop-filter: blur(4px);
      z-index: 9999;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 16px;
      box-sizing: border-box;
      overflow: hidden;
      animation: fadeIn 0.2s ease-out;
    }

    .modal-dialog-box {
      background: var(--surface, #ffffff);
      color: var(--ink, #0f172a);
      border-radius: 16px;
      box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
      width: 100%;
      max-width: 640px;
      max-height: calc(100dvh - 32px);
      display: flex;
      flex-direction: column;
      box-sizing: border-box;
      overflow: hidden;
      margin: auto;
      animation: zoomIn 0.2s ease-out;
    }

    .modal-dialog-box.wide {
      max-width: var(--modal-wide-max-width, 960px);
    }

    .modal-dialog-box.large {
      max-width: 1100px;
    }

    .modal-header {
      padding: 1.25rem 1.5rem;
      background: var(--surface-muted, #f8fafc);
      border-bottom: 1px solid var(--line, #e2e8f0);
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex: 0 0 auto;
    }

    .modal-title {
      font-size: 1.25rem;
      font-weight: 700;
      color: var(--ink, #1e293b);
      margin: 0;
    }

    .close-btn {
      background: transparent;
      border: none;
      font-size: 1.25rem;
      color: var(--muted, #64748b);
      cursor: pointer;
      width: 32px;
      height: 32px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.15s ease;
    }

    .close-btn:hover {
      background: var(--surface-hover, #e2e8f0);
      color: var(--ink, #0f172a);
    }

    .modal-body {
      padding: var(--modal-body-padding, 1.5rem);
      overflow-y: auto;
      flex: 1 1 auto;
      min-height: 0;
    }

    .modal-footer {
      padding: 1rem 1.5rem;
      background: var(--surface-muted, #f8fafc);
      border-top: 1px solid var(--line, #e2e8f0);
      display: flex;
      align-items: center;
      gap: 0.75rem;
      justify-content: flex-end;
      flex: 0 0 auto;
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
        max-width: 100% !important;
        height: calc(100dvh - 16px);
        max-height: calc(100dvh - 16px);
        border-radius: 12px;
        margin: auto;
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
  @ViewChild('dialogBox') dialogBox?: ElementRef;
  @ViewChild('modalBodyRef') modalBodyRef?: ElementRef;
  private focusPending = false;
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
        this.focusPending = true;
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

  ngAfterViewChecked(): void {
    if (this.focusPending && this.dialogBox) {
      this.focusPending = false;
      queueMicrotask(() => {
        const dialog = this.dialogBox?.nativeElement as HTMLElement | undefined;
        const body = (this.modalBodyRef?.nativeElement as HTMLElement | undefined) ?? dialog?.querySelector<HTMLElement>('.modal-body');
        if (body) {
          body.scrollTop = 0;
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
          body.scrollTop = 0;
        }
      });
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
    if (!this.preventOutsideClose) {
      this.onClose();
    }
  }

  onClose() {
    this.close.emit();
    this.closeModal.emit();
  }
}
