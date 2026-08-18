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
});
