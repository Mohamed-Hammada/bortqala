import { LANDING_PAGE_ITEMS, NAV_ITEMS, WORKSPACE_ORDER } from './app-navigation';

describe('app navigation information architecture', () => {
  it('keeps menu ids unique', () => {
    const ids = NAV_ITEMS.map((item) => item.menuId);
    expect(new Set(ids).size).toBe(ids.length);
  });

  it('keeps routes unique', () => {
    const paths = NAV_ITEMS.map((item) => item.path);
    expect(new Set(paths).size).toBe(paths.length);
  });

  it('renders every declared workspace', () => {
    const workspaces = new Set(WORKSPACE_ORDER);
    for (const item of NAV_ITEMS) {
      expect(workspaces.has(item.workspace)).toBe(true);
    }
  });

  it('keeps approvals in the rendered workspace order', () => {
    expect(WORKSPACE_ORDER).toContain('workspace.approvalsWorkflow');
    expect(NAV_ITEMS.find((item) => item.menuId === 'approvals-my-tasks')?.workspace)
      .toBe('workspace.approvalsWorkflow');
  });

  it('uses the reviewed domain classifications', () => {
    expect(NAV_ITEMS.find((item) => item.menuId === 'dashboard')?.workspace).toBe('workspace.homeOverview');
    expect(NAV_ITEMS.find((item) => item.menuId === 'organization')?.workspace).toBe('workspace.peopleHr');
    expect(NAV_ITEMS.find((item) => item.menuId === 'payroll')?.workspace).toBe('workspace.peopleHr');
    expect(NAV_ITEMS.find((item) => item.menuId === 'operations')?.workspace).toBe('workspace.supplyChainInventory');
    expect(NAV_ITEMS.find((item) => item.menuId === 'procurement')?.workspace).toBe('workspace.supplyChainInventory');
    expect(NAV_ITEMS.find((item) => item.menuId === 'sales')?.workspace).toBe('workspace.salesCommercial');
    expect(NAV_ITEMS.find((item) => item.menuId === 'production')?.workspace).toBe('workspace.manufacturingDomain');
    expect(NAV_ITEMS.find((item) => item.menuId === 'quality')?.workspace).toBe('workspace.manufacturingDomain');
    expect(NAV_ITEMS.find((item) => item.menuId === 'parties')?.workspace).toBe('workspace.businessPartners');
  });


  it('preserves the current Workforce authorization menu ids', () => {
    const ids = NAV_ITEMS.map((item) => item.menuId);
    expect(ids).toContain('workforce-requests');
    expect(ids).toContain('workforce-settlements');
    expect(ids).toContain('workforce-accounts');
    expect(ids).toContain('workforce-reports');
    expect(ids).not.toContain('workforce-labor-requests');
    expect(ids).not.toContain('workforce-settlement-periods');
    expect(ids).not.toContain('workforce-contractor-accounts');
    expect(ids).not.toContain('workforce-reports-import');
  });

  it('does not offer administrative utility pages as default landing pages', () => {
    const ids = LANDING_PAGE_ITEMS.map((item) => item.menuId);
    expect(ids).not.toContain('settings');
    expect(ids).not.toContain('audit-logs');
    expect(ids).not.toContain('users');
    expect(ids).not.toContain('notifications-send');
  });
});
