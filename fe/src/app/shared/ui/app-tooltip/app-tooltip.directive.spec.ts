import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AppTooltipDirective } from './app-tooltip.directive';

@Component({
  imports: [AppTooltipDirective],
  template: `<button appTooltip="Helpful details">Action</button>`,
})
class TooltipHostComponent {}

describe('AppTooltipDirective', () => {
  let fixture: ComponentFixture<TooltipHostComponent>;
  let button: HTMLButtonElement;

  beforeEach(async () => {
    vi.useFakeTimers();
    await TestBed.configureTestingModule({ imports: [TooltipHostComponent] }).compileComponents();
    fixture = TestBed.createComponent(TooltipHostComponent);
    fixture.detectChanges();
    button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
  });

  afterEach(() => {
    vi.runOnlyPendingTimers();
    vi.useRealTimers();
    document.body.querySelectorAll('[role="tooltip"]').forEach(element => element.remove());
  });

  it('shows an accessible tooltip after 350ms on focus and removes it on blur', () => {
    button.dispatchEvent(new FocusEvent('focusin', { bubbles: true }));
    fixture.detectChanges();
    expect(document.body.querySelector('[role="tooltip"]')).toBeNull();
    vi.advanceTimersByTime(350);

    const tooltip = document.body.querySelector('[role="tooltip"]');
    expect(tooltip?.textContent).toBe('Helpful details');
    expect(button.getAttribute('aria-describedby')).toBe(tooltip?.id);

    button.dispatchEvent(new FocusEvent('focusout', { bubbles: true }));
    expect(document.body.querySelector('[role="tooltip"]')).toBeNull();
    expect(button.hasAttribute('aria-describedby')).toBe(false);
  });

  it('cancels a pending tooltip and closes a visible tooltip with Escape', () => {
    button.dispatchEvent(new MouseEvent('mouseenter'));
    vi.advanceTimersByTime(200);
    button.dispatchEvent(new MouseEvent('mouseleave'));
    vi.advanceTimersByTime(200);
    expect(document.body.querySelector('[role="tooltip"]')).toBeNull();

    button.dispatchEvent(new MouseEvent('mouseenter'));
    vi.advanceTimersByTime(350);
    expect(document.body.querySelector('[role="tooltip"]')).not.toBeNull();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    expect(document.body.querySelector('[role="tooltip"]')).toBeNull();
  });
});
