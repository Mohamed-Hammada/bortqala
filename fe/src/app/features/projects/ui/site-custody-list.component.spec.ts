import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { SiteCustodyListComponent } from './site-custody-list.component';
import { ProjectService } from '../data-access/project.service';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { of } from 'rxjs';

describe('SiteCustodyListComponent', () => {
  let component: SiteCustodyListComponent;
  let fixture: ComponentFixture<SiteCustodyListComponent>;
  let projectService: ProjectService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SiteCustodyListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ProjectService,
        I18nService,
        NotificationService,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SiteCustodyListComponent);
    component = fixture.componentInstance;
    component.projectId = 'PRJ-01';
    projectService = TestBed.inject(ProjectService);
  });

  it('should create and load project custodies on init', () => {
    vi.spyOn(projectService, 'loadProjectCustodies').mockReturnValue(of([
      {
        id: 'cust-1',
        projectId: 'PRJ-01',
        custodyCode: 'CUST-001',
        custodianName: 'Eng. Karim Adel',
        custodyType: 'CASH',
        initialAmount: 10000,
        remainingBalance: 10000,
        status: 'ACTIVE',
        issuedAt: Date.now(),
        version: 0,
        createdAt: Date.now(),
        updatedAt: Date.now(),
        expenses: [],
        returns: [],
      },
    ]));

    component.ngOnInit();
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.custodies().length).toBe(1);
    expect(component.selectedCustody()?.custodianName).toBe('Eng. Karim Adel');
  });

  it('should open issue modal and set form', () => {
    component.openIssueModal();
    expect(component.showIssueModal()).toBe(true);
    expect(component.issueForm.initialAmount).toBe(10000);
  });
});
