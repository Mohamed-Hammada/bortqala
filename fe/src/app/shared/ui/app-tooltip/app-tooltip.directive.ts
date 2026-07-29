import { DOCUMENT } from '@angular/common';
import {
  Directive,
  ElementRef,
  HostListener,
  Input,
  OnDestroy,
  Renderer2,
  inject,
} from '@angular/core';

let nextTooltipId = 0;

@Directive({
  selector: '[appTooltip]',
  standalone: true,
})
export class AppTooltipDirective implements OnDestroy {
  private readonly elementRef = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly renderer = inject(Renderer2);
  private readonly document = inject(DOCUMENT);
  private tooltipElement: HTMLElement | null = null;
  private previousDescribedBy: string | null = null;
  private showTimer: ReturnType<typeof setTimeout> | null = null;

  @Input({ required: true }) appTooltip = '';

  @HostListener('mouseenter')
  scheduleShow(): void {
    if (!this.appTooltip.trim() || this.tooltipElement || this.showTimer) return;
    this.showTimer = setTimeout(() => {
      this.showTimer = null;
      this.show();
    }, 350);
  }

  private show(): void {
    if (!this.appTooltip.trim() || this.tooltipElement) return;

    const tooltip = this.renderer.createElement('div') as HTMLElement;
    const tooltipId = `app-tooltip-${++nextTooltipId}`;
    this.renderer.addClass(tooltip, 'app-tooltip-surface');
    this.renderer.setAttribute(tooltip, 'id', tooltipId);
    this.renderer.setAttribute(tooltip, 'role', 'tooltip');
    this.renderer.setAttribute(tooltip, 'dir', this.document.documentElement.dir || 'rtl');
    this.renderer.appendChild(tooltip, this.renderer.createText(this.appTooltip));
    this.renderer.appendChild(this.document.body, tooltip);

    const host = this.elementRef.nativeElement;
    this.previousDescribedBy = host.getAttribute('aria-describedby');
    this.renderer.setAttribute(
      host,
      'aria-describedby',
      [this.previousDescribedBy, tooltipId].filter(Boolean).join(' '),
    );
    this.tooltipElement = tooltip;
    this.positionTooltip();
  }

  @HostListener('focusin', ['$event'])
  onFocus(event: FocusEvent): void {
    if (event.target === this.elementRef.nativeElement) this.scheduleShow();
  }

  @HostListener('mouseleave')
  @HostListener('focusout')
  @HostListener('click')
  hide(): void {
    if (this.showTimer) {
      clearTimeout(this.showTimer);
      this.showTimer = null;
    }
    if (!this.tooltipElement) return;
    this.renderer.removeChild(this.document.body, this.tooltipElement);
    this.tooltipElement = null;

    const host = this.elementRef.nativeElement;
    if (this.previousDescribedBy) {
      this.renderer.setAttribute(host, 'aria-describedby', this.previousDescribedBy);
    } else {
      this.renderer.removeAttribute(host, 'aria-describedby');
    }
    this.previousDescribedBy = null;
  }

  @HostListener('document:keydown.escape')
  @HostListener('window:resize')
  @HostListener('window:scroll')
  onViewportChange(): void {
    this.hide();
  }

  ngOnDestroy(): void {
    this.hide();
  }

  private positionTooltip(): void {
    if (!this.tooltipElement) return;
    const hostRect = this.elementRef.nativeElement.getBoundingClientRect();
    const tooltipRect = this.tooltipElement.getBoundingClientRect();
    const viewportWidth = this.document.documentElement.clientWidth;
    const gap = 9;
    const edge = 8;

    let top = hostRect.top - tooltipRect.height - gap;
    if (top < edge) top = hostRect.bottom + gap;
    const centeredLeft = hostRect.left + hostRect.width / 2 - tooltipRect.width / 2;
    const left = Math.min(
      Math.max(centeredLeft, edge),
      Math.max(edge, viewportWidth - tooltipRect.width - edge),
    );

    this.renderer.setStyle(this.tooltipElement, 'top', `${Math.round(top)}px`);
    this.renderer.setStyle(this.tooltipElement, 'left', `${Math.round(left)}px`);
  }
}
