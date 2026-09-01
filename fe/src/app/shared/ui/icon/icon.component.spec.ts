import { describe, expect, it, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { IconComponent, IconName } from './icon.component';

const ALL_ICONS: IconName[] = [
  'dashboard',
  'categories',
  'employees',
  'imports',
  'reports',
  'users',
  'settings',
  'logout',
  'menu',
  'close',
  'panel-expand',
  'panel-collapse',
  'expand-all',
  'collapse-all',
  'arrow-up',
  'arrow-down',
  'eye',
  'eye-off',
  'star',
  'clock',
  'bell',
  'chat',
  'wallet',
  'cart',
  'boxes',
  'banknote',
  'building',
  'factory',
  'search',
];

describe('IconComponent', () => {
  let fixture: ComponentFixture<IconComponent>;
  let component: IconComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IconComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(IconComponent);
    component = fixture.componentInstance;
  });

  it('renders SVG with standard attributes (viewBox, aria-hidden, focusable=false)', () => {
    fixture.componentRef.setInput('name', 'dashboard');
    fixture.detectChanges();

    const svg = fixture.nativeElement.querySelector('svg');
    expect(svg).toBeTruthy();
    expect(svg.getAttribute('viewBox')).toBe('0 0 24 24');
    expect(svg.getAttribute('aria-hidden')).toBe('true');
    expect(svg.getAttribute('focusable')).toBe('false');
  });

  it('renders every declared IconName with valid SVG child geometry', () => {
    for (const iconName of ALL_ICONS) {
      fixture.componentRef.setInput('name', iconName);
      fixture.detectChanges();

      const svg = fixture.nativeElement.querySelector('svg');
      expect(svg).toBeTruthy();
      expect(svg.children.length).toBeGreaterThan(0);
    }
  });
});
