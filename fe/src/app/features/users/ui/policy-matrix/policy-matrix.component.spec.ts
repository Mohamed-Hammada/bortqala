import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PolicyMatrixComponent } from './policy-matrix.component';
import { I18nService } from '../../../../core/i18n.service';
import { PolicyCatalogResponse } from '../../../../core/auth/security-policy.models';

describe('PolicyMatrixComponent', () => {
  let component: PolicyMatrixComponent;
  let fixture: ComponentFixture<PolicyMatrixComponent>;

  const sampleCatalog: PolicyCatalogResponse = {
    totalPermissions: 3,
    modules: [
      {
        module: 'trade',
        permissions: [
          { id: '1', permissionKey: 'sales:so:create', module: 'trade', isSystem: true },
          { id: '2', permissionKey: 'sales:so:read', module: 'trade', isSystem: true },
        ],
      },
      {
        module: 'finance',
        permissions: [
          { id: '3', permissionKey: 'finance:journal:post', module: 'finance', isSystem: true },
        ],
      },
    ],
  };

  beforeEach(() => {
    const mockI18n = {
      t: (key: string) => key,
    };

    TestBed.configureTestingModule({
      imports: [PolicyMatrixComponent],
      providers: [{ provide: I18nService, useValue: mockI18n }],
    });

    fixture = TestBed.createComponent(PolicyMatrixComponent);
    component = fixture.componentInstance;
    component.catalog = sampleCatalog;
    fixture.detectChanges();
  });

  it('filters modules and permissions based on search term', () => {
    component.searchTerm.set('journal');
    fixture.detectChanges();

    const filtered = component.filteredModules();
    expect(filtered.length).toBe(1);
    expect(filtered[0].module).toBe('finance');
  });

  it('toggles permission selection and emits changes', () => {
    let emittedKeys: string[] | null = null;
    component.selectedPermissionKeysChange.subscribe((keys) => {
      emittedKeys = keys;
    });

    component.togglePermission('sales:so:create');

    expect(component.isPermissionSelected('sales:so:create')).toBe(true);
    expect(emittedKeys).toEqual(['sales:so:create']);
  });

  it('selects and deselects all permissions', () => {
    let emittedKeys: string[] = [];
    component.selectedPermissionKeysChange.subscribe((keys) => {
      emittedKeys = keys;
    });

    component.selectAll();
    expect(component.totalSelectedCount()).toBe(3);
    expect(emittedKeys.length).toBe(3);

    component.deselectAll();
    expect(component.totalSelectedCount()).toBe(0);
    expect(emittedKeys).toEqual([]);
  });

  it('applies VIEW_ONLY and MANAGE preset tiers properly', () => {
    let emittedKeys: string[] = [];
    component.selectedPermissionKeysChange.subscribe((keys) => {
      emittedKeys = keys;
    });

    component.applyPresetTier('VIEW_ONLY');
    expect(component.isPermissionSelected('sales:so:read')).toBe(true);
    expect(component.isPermissionSelected('sales:so:create')).toBe(false);

    component.applyPresetTier('MANAGE');
    expect(component.isPermissionSelected('sales:so:create')).toBe(true);
    expect(component.isPermissionSelected('sales:so:read')).toBe(true);
  });
});
