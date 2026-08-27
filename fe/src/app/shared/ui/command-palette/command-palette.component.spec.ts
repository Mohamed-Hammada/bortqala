import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { vi } from 'vitest';
import { CommandPaletteComponent, PaletteAction } from './command-palette.component';

describe('CommandPaletteComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommandPaletteComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('populates static nav items on init', () => {
    const fixture = TestBed.createComponent(CommandPaletteComponent);
    fixture.componentRef.setInput('open', true);
    fixture.detectChanges();

    expect(fixture.componentInstance.results().length).toBeGreaterThan(0);
    expect(fixture.componentInstance.results()[0].type).toBe('navigation');
  });

  it('filters and searches combining static and backend results', async () => {
    const fixture = TestBed.createComponent(CommandPaletteComponent);
    const http = TestBed.inject(HttpTestingController);
    fixture.componentRef.setInput('open', true);
    fixture.detectChanges();

    const promise = fixture.componentInstance.onQueryChange('dash');
    const req = http.expectOne('/api/v1/platform/search?q=dash');
    req.flush({ results: [] });

    await promise;
    const results = fixture.componentInstance.results();
    expect(results.some(r => r.title === 'Dashboard')).toBe(true);
  });

  it('executes navigation action and closes', () => {
    const fixture = TestBed.createComponent(CommandPaletteComponent);
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigateByUrl');
    const closedSpy = vi.spyOn(fixture.componentInstance.closed, 'emit');
    fixture.componentRef.setInput('open', true);
    fixture.detectChanges();

    const item: PaletteAction = { type: 'navigation', title: 'Dashboard', url: '/dashboard' };
    fixture.componentInstance.executeItem(item);

    expect(navigateSpy).toHaveBeenCalledWith('/dashboard');
    expect(closedSpy).toHaveBeenCalled();
  });

  it('arrow key navigation updates selected index', async () => {
    const fixture = TestBed.createComponent(CommandPaletteComponent);
    fixture.componentRef.setInput('open', true);
    fixture.detectChanges();

    const initialIndex = fixture.componentInstance.selectedIndex();
    fixture.componentInstance.onKeydown(new KeyboardEvent('keydown', { key: 'ArrowDown' }));
    expect(fixture.componentInstance.selectedIndex()).toBe(initialIndex + 1);

    fixture.componentInstance.onKeydown(new KeyboardEvent('keydown', { key: 'ArrowUp' }));
    expect(fixture.componentInstance.selectedIndex()).toBe(initialIndex);
  });

  it('Escape closes the palette', () => {
    const fixture = TestBed.createComponent(CommandPaletteComponent);
    const closedSpy = vi.spyOn(fixture.componentInstance.closed, 'emit');
    fixture.componentRef.setInput('open', true);
    fixture.detectChanges();

    fixture.componentInstance.onKeydown(new KeyboardEvent('keydown', { key: 'Escape' }));
    expect(closedSpy).toHaveBeenCalled();
  });
});
