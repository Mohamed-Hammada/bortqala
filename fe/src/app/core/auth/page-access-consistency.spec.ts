import { describe, expect, it } from 'vitest';
import { routes } from '../../app.routes';
import { WORKFORCE_ROUTES } from '../../features/workforce/workforce.routes';
import { NAV_ITEMS, SHELL_MENU_ROLES } from '../shell/app-shell.component';
import { USER_MENU_OPTIONS } from '../../features/users/users.page';
import { CATALOG_PAGE_CONTRACT } from './access-catalog-contract';

/**
 * Parity guards between the backend access catalog contract and the frontend
 * navigation. The backend {@code AccessCatalog} is the single source of truth;
 * these tests fail when the shell menu, route guards or the users page drift
 * from the {@link CATALOG_PAGE_CONTRACT} snapshot (which mirrors it).
 */

const ADMIN_BYPASS: string[] = ['ADMIN', 'SUPER_ADMIN'];

function navRoles(roles: string[] | undefined): string[] {
  const base = roles ?? [];
  if (base.length === 0) return [];
  if (base.length === 1 && base[0] === 'SUPER_ADMIN') return ['SUPER_ADMIN'];
  return [...new Set([...ADMIN_BYPASS, ...base])].sort();
}

describe('page access consistency', () => {
  const contractByMenu = new Map(CATALOG_PAGE_CONTRACT.map((item) => [item.menuId, item]));

  it('shell NAV_ITEMS cover exactly the catalog contract menu ids', () => {
    const navMenuIds = NAV_ITEMS.map((item) => item.menuId).sort();
    const contractMenuIds = CATALOG_PAGE_CONTRACT.map((item) => item.menuId).sort();
    expect(navMenuIds).toEqual(contractMenuIds);
  });

  it('shell paths match the catalog contract routes', () => {
    for (const item of NAV_ITEMS) {
      const contract = contractByMenu.get(item.menuId);
      expect(contract, `missing contract for ${item.menuId}`).toBeDefined();
      expect(item.path, `path mismatch for ${item.menuId}`).toBe(contract!.route);
    }
  });

  it('shell route-guard roles match the catalog contract', () => {
    for (const item of NAV_ITEMS) {
      const contract = contractByMenu.get(item.menuId)!;
      expect(navRoles(item.roles), `roles mismatch for ${item.menuId}`).toEqual([...contract.roles].sort());
    }
  });

  it('SHELL_MENU_ROLES export agrees with NAV_ITEMS', () => {
    for (const item of NAV_ITEMS) {
      expect(SHELL_MENU_ROLES[item.menuId], `missing SHELL_MENU_ROLES for ${item.menuId}`).toBeDefined();
      expect([...SHELL_MENU_ROLES[item.menuId]].sort()).toEqual([...(item.roles ?? [])].sort());
    }
  });

  it('users page menu options cover every manageable catalog contract menu id', () => {
    const optionIds = USER_MENU_OPTIONS.map((option) => option.id);
    const manageableNavItems = NAV_ITEMS.filter((item) => item.showInPermissionEditor !== false);
    for (const item of manageableNavItems) {
      expect(optionIds, `menu ${item.menuId} missing from users.page menuOptions`).toContain(item.menuId);
    }
  });

  it('feature-gated menus match the AuthService.hasMenuAccess toggle lists', () => {
    const gatedByFeature = new Map<string, string[]>();
    for (const contract of CATALOG_PAGE_CONTRACT) {
      if (!contract.requiredFeature) continue;
      const list = gatedByFeature.get(contract.requiredFeature) ?? [];
      list.push(contract.menuId);
      gatedByFeature.set(contract.requiredFeature, list);
    }
    expect([...gatedByFeature.keys()].sort()).toEqual([
      'finance.enabled',
      'manufacturing.enabled',
      'payroll.enabled',
      'quality.enabled',
      'sales.enabled',
      'workforce.contractorAccounts.enabled',
    ]);
    expect(gatedByFeature.get('finance.enabled')!.sort()).toEqual([
      'accounts', 'banks', 'budgets', 'fiscal-periods', 'journal-entries', 'tax-currency',
    ]);
    expect(gatedByFeature.get('payroll.enabled')).toEqual(['payroll']);
    expect(gatedByFeature.get('sales.enabled')).toEqual(['sales']);
    expect(gatedByFeature.get('manufacturing.enabled')).toEqual(['production']);
    expect(gatedByFeature.get('quality.enabled')).toEqual(['quality']);
    expect(gatedByFeature.get('workforce.contractorAccounts.enabled')!.sort()).toEqual([
      'workforce-accounts', 'workforce-settlements',
    ]);
  });
});

describe('route parity', () => {
  const routeData: Array<{ path: string; menuId?: string; roles?: string[] }> = [];
  const seenMenus = new Set<string>();
  const walk = (items: typeof routes, prefix = ''): void => {
    for (const route of items) {
      const path = `${prefix}/${route.path ?? ''}`.replace(/\/+/g, '/');
      const menuId = route.data?.['menuId'] as string | undefined;
      if (menuId && !seenMenus.has(menuId)) {
        seenMenus.add(menuId);
        routeData.push({ path, menuId, roles: route.data?.['roles'] as string[] | undefined });
      }
      if (route.children) walk(route.children as typeof routes, path);
    }
  };
  walk(routes);
  for (const route of WORKFORCE_ROUTES) {
    const menuId = route.data?.['menuId'] as string | undefined;
    if (menuId && !seenMenus.has(menuId)) {
      seenMenus.add(menuId);
      routeData.push({
        path: `/workforce/${route.path}`,
        menuId,
        roles: route.data?.['roles'] as string[] | undefined,
      });
    }
  }

  const contractByMenu = new Map(CATALOG_PAGE_CONTRACT.map((item) => [item.menuId, item]));

  it('every route with a menuId is in the catalog contract and uses matching guards', () => {
    for (const route of routeData) {
      const contract = contractByMenu.get(route.menuId!);
      expect(contract, `route for unknown menu ${route.menuId}`).toBeDefined();
      expect(route.path, `route path for ${route.menuId}`).toBe(contract!.route);
      expect(navRoles(route.roles), `route roles for ${route.menuId}`).toEqual([...contract!.roles].sort());
    }
  });

  it('every catalog contract menu has a route', () => {
    const routedMenus = routeData.map((route) => route.menuId).sort();
    const contractMenus = CATALOG_PAGE_CONTRACT.map((item) => item.menuId).sort();
    expect(routedMenus).toEqual(contractMenus);
  });
});
