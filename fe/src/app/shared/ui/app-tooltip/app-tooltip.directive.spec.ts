import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
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
    await TestBed.configureTestingModule({ imports: [TooltipHostComponent] }).compileComponents();
    fixture = TestBed.createComponent(TooltipHostComponent);
    fixture.detectChanges();
    button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
  });

  it('shows an accessible tooltip on focus and removes it on blur', () => {
    button.dispatchEvent(new FocusEvent('focusin', { bubbles: true }));
    fixture.detectChanges();

    const tooltip = document.body.querySelector('[role="tooltip"]');
    expect(tooltip?.textContent).toBe('Helpful details');
    expect(button.getAttribute('aria-describedby')).toBe(tooltip?.id);

    button.dispatchEvent(new FocusEvent('focusout', { bubbles: true }));
    expect(document.body.querySelector('[role="tooltip"]')).toBeNull();
    expect(button.hasAttribute('aria-describedby')).toBe(false);
  });
});
