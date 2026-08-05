import '@angular/compiler';
import { describe, it, expect } from 'vitest';
import { Router, UrlTree } from '@angular/router';
import { routes } from './app.routes';
import { roleGuardDecision } from './core/auth/auth.guard';

describe('app routes', () => {
  it('exposes dedicated forbidden and not-found pages inside the shell', () => {
    const shell = routes.find((r) => r.path === '');
    const paths = (shell?.children ?? []).map((c) => c.path);
    expect(paths).toContain('forbidden');
    expect(paths).toContain('not-found');
  });

  it('redirects unknown routes to the not-found page instead of the dashboard', () => {
    const shell = routes.find((r) => r.path === '')!;
    const childWildcard = shell.children?.find((c) => c.path === '**') as { redirectTo?: string };
    expect(childWildcard?.redirectTo).toBe('not-found');
    const rootWildcard = routes.find((r) => r.path === '**') as { redirectTo?: string } | undefined;
    expect(rootWildcard?.redirectTo).toBe('not-found');
  });
});

describe('roleGuardDecision', () => {
  it('allows navigation when the user has the required role', () => {
    expect(roleGuardDecision(true, {} as Router)).toBe(true);
  });

  it('redirects to /forbidden when the user lacks the required role', () => {
    let redirectTarget: unknown;
    const router = {
      createUrlTree: (commands: unknown[]) => {
        redirectTarget = commands;
        return {} as UrlTree;
      },
    } as unknown as Router;
    const result = roleGuardDecision(false, router);
    expect(result).not.toBe(true);
    expect(redirectTarget).toEqual(['/forbidden']);
  });
});
