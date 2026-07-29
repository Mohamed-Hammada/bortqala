import { Component, Input, Output, EventEmitter, HostListener, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-modal-dialog',
  standalone: true,
  imports: [CommonModule],
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
          <button type="button" class="close-btn" aria-label="Close" (click)="onClose()">
            ✕
          </button>
        </header>

        <div class="modal-body">
          <ng-content></ng-content>
        </div>

        <footer class="modal-footer" *ngIf="showFooter">
          <ng-content select="[modal-actions]"></ng-content>
        </footer>
      </div>
    </div>
  `,
  styles: [`
    .modal-backdrop {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(15, 23, 42, 0.65);
      backdrop-filter: blur(4px);
      z-index: 9999;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1rem;
      animation: fadeIn 0.2s ease-out;
    }

    .modal-dialog-box {
      background: #ffffff;
      color: #0f172a;
      border-radius: 16px;
      box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
      width: 100%;
      max-width: 640px;
      max-height: 90vh;
      display: flex;
      flex-direction: column;
      overflow: hidden;
      animation: zoomIn 0.2s ease-out;
    }

    .modal-dialog-box.wide {
      max-width: 960px;
    }

    .modal-dialog-box.large {
      max-width: 1100px;
    }

    .modal-header {
      padding: 1.25rem 1.5rem;
      background: #f8fafc;
      border-bottom: 1px solid #e2e8f0;
      display: flex;
      align-items: center;
      justify-content: space-between;
      position: sticky;
      top: 0;
      z-index: 10;
    }

    .modal-title {
      font-size: 1.25rem;
      font-weight: 700;
      color: #1e293b;
      margin: 0;
    }

    .close-btn {
      background: transparent;
      border: none;
      font-size: 1.25rem;
      color: #64748b;
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
      background: #e2e8f0;
      color: #0f172a;
    }

    .modal-body {
      padding: 1.5rem;
      overflow-y: auto;
      flex: 1;
    }

    .modal-footer {
      padding: 1rem 1.5rem;
      background: #f8fafc;
      border-top: 1px solid #e2e8f0;
      display: flex;
      align-items: center;
      gap: 0.75rem;
      position: sticky;
      bottom: 0;
      z-index: 10;
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
        height: 100vh;
        max-height: 100vh;
        border-radius: 0;
      }
    }
  `]
})
export class ModalDialogComponent implements AfterViewInit {
  // Structural call sites create the component only while open; explicit bindings
  // still override this value for components that stay mounted.
  @Input() isOpen = true;
  @Input() title = '';
  @Input() titleId = 'modal-title-' + Math.random().toString(36).substring(2, 9);
  @Input() size: 'normal' | 'wide' | 'large' = 'normal';
  @Input() showFooter = true;
  @Input() preventOutsideClose = false;

  @Output() close = new EventEmitter<void>();
  @Output() closeModal = new EventEmitter<void>();
  @ViewChild('dialogBox') dialogBox?: ElementRef;

  ngAfterViewInit() {
    if (this.isOpen && this.dialogBox) {
      queueMicrotask(() => {
        const dialog = this.dialogBox?.nativeElement as HTMLElement | undefined;
        const firstControl = dialog?.querySelector<HTMLElement>(
          'input:not([disabled]):not([readonly]), select:not([disabled]), textarea:not([disabled]), button:not([disabled])',
        );
        (firstControl ?? dialog)?.focus();
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
