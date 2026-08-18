import { LANDING_PAGE_ITEMS, NAV_ITEMS, WORKSPACE_ORDER, canAccessNavigationItem } from './app-navigation';

describe('settings navigation refactor', () => {
  it('moves non-settings capabilities to their owning workspaces', () => {
    const risk = NAV_ITEMS.find((item) => item.menuId === 'partner-risk');
    const setup = NAV_ITEMS.find((item) => item.menuId === 'admin-setup-readiness');
    const insights = NAV_ITEMS.find((item) => item.menuId === 'admin-product-insights');
    const platform = NAV_ITEMS.find((item) => item.menuId === 'platform-admin');

    expect(risk?.workspace).toBe('workspace.businessPartners');
    expect(risk?.permissionMenuId).toBe('settings');
    expect(setup?.workspace).toBe('workspace.administration');
    expect(insights?.workspace).toBe('workspace.administration');
    expect(platform?.workspace).toBe('workspace.platformAdministration');
    expect(platform?.permissionMenuId).toBe('settings');
    expect(platform?.strictRoles).toBe(true);
    expect(WORKSPACE_ORDER).toContain('workspace.platformAdministration');
  });

  it('does not offer moved administrative pages as default landing pages', () => {
    const ids = LANDING_PAGE_ITEMS.map((item) => item.menuId);
    expect(ids).not.toContain('partner-risk');
    expect(ids).not.toContain('admin-setup-readiness');
    expect(ids).not.toContain('admin-product-insights');
    expect(ids).not.toContain('platform-admin');
  });

  it('does not let tenant ADMIN bypass the platform-only navigation role', () => {
    const platform = NAV_ITEMS.find((item) => item.menuId === 'platform-admin');
    expect(platform).toBeDefined();
    expect(canAccessNavigationItem(platform!, ['ADMIN'], () => true)).toBe(false);
    expect(canAccessNavigationItem(platform!, ['SUPER_ADMIN'], () => true)).toBe(true);
  });

  it('enforces feature-flag checks for tenant ADMIN via hasMenuAccess', () => {
    const sales = NAV_ITEMS.find((item) => item.menuId === 'sales')!;
    const payroll = NAV_ITEMS.find((item) => item.menuId === 'payroll')!;
    expect(sales).toBeDefined();
    expect(payroll).toBeDefined();

    // Feature disabled -> ADMIN cannot access
    expect(canAccessNavigationItem(sales, ['ADMIN'], (menuId) => menuId !== 'sales')).toBe(false);
    expect(canAccessNavigationItem(payroll, ['ADMIN'], (menuId) => menuId !== 'payroll')).toBe(false);

    // Feature enabled -> ADMIN can access
    expect(canAccessNavigationItem(sales, ['ADMIN'], () => true)).toBe(true);
    expect(canAccessNavigationItem(payroll, ['ADMIN'], () => true)).toBe(true);
  });

  it('excludes synthetic navigation aliases from permission editor', () => {
    const hiddenIds = NAV_ITEMS
      .filter((item) => item.showInPermissionEditor === false)
      .map((item) => item.menuId);

    expect(hiddenIds).toContain('partner-risk');
    expect(hiddenIds).toContain('admin-setup-readiness');
    expect(hiddenIds).toContain('admin-product-insights');
    expect(hiddenIds).toContain('platform-admin');

    const visibleIds = NAV_ITEMS
      .filter((item) => item.showInPermissionEditor !== false)
      .map((item) => item.menuId);

    expect(visibleIds).toContain('settings');
    expect(visibleIds).toContain('users');
    expect(visibleIds).toContain('sales');
    expect(visibleIds).toContain('payroll');
  });

  it('enforces role requirements for non-admin users', () => {
    const sales = NAV_ITEMS.find((item) => item.menuId === 'sales')!;
    expect(canAccessNavigationItem(sales, ['VIEWER'], () => true)).toBe(false);
    expect(canAccessNavigationItem(sales, ['SALES_MANAGER'], () => true)).toBe(true);
    expect(canAccessNavigationItem(sales, ['SALES_MANAGER'], () => false)).toBe(false);
  });
});
