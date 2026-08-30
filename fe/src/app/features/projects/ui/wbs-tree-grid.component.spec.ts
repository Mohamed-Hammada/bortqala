import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach } from 'vitest';
import { WbsTreeGridComponent } from './wbs-tree-grid.component';
import { I18nService } from '../../../core/i18n.service';
import { WbsNodeResponse } from '../models/project.models';

describe('WbsTreeGridComponent', () => {
  let component: WbsTreeGridComponent;
  let fixture: ComponentFixture<WbsTreeGridComponent>;

  const mockNodes: WbsNodeResponse[] = [
    {
      id: 'node-1',
      projectId: 'prj-1',
      wbsCode: '1',
      wbsPath: '/1',
      name: 'أعمال الأساسات',
      nodeType: 'PHASE',
      level: 1,
      sortOrder: 0,
      plannedQuantity: 0,
      unitRate: 0,
      plannedAmount: 500000,
      status: 'IN_PROGRESS',
      createdAt: 1700000000000,
      updatedAt: 1700000000000,
      version: 1,
      children: [
        {
          id: 'node-1-1',
          projectId: 'prj-1',
          parentId: 'node-1',
          wbsCode: '1.1',
          wbsPath: '/1/1.1',
          name: 'حفر الموقع',
          nodeType: 'BOQ_ITEM',
          level: 2,
          sortOrder: 0,
          unitOfMeasure: 'م3',
          plannedQuantity: 1000,
          unitRate: 150,
          plannedAmount: 150000,
          status: 'COMPLETED',
          createdAt: 1700000000000,
          updatedAt: 1700000000000,
          version: 1,
          children: [],
        },
      ],
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WbsTreeGridComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: I18nService,
          useValue: { t: (key: string) => key },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WbsTreeGridComponent);
    component = fixture.componentInstance;
    component.nodes = mockNodes;
    fixture.detectChanges();
  });

  it('should create and display nodes', () => {
    expect(component).toBeTruthy();
    expect(component.nodes.length).toBe(1);
  });

  it('should toggle node expansion', () => {
    expect(component.isExpanded('node-1')).toBe(false);
    component.toggleExpand('node-1');
    expect(component.isExpanded('node-1')).toBe(true);
    component.toggleExpand('node-1');
    expect(component.isExpanded('node-1')).toBe(false);
  });

  it('should expand and collapse all nodes', () => {
    component.expandAll();
    expect(component.isExpanded('node-1')).toBe(true);
    component.collapseAll();
    expect(component.isExpanded('node-1')).toBe(false);
  });
});
