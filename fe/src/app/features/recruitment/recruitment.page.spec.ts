import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { RecruitmentPage } from './recruitment.page';
import { JobOpening } from './recruitment.models';

describe('RecruitmentPage (WP-50)', () => {
  let fixture: ComponentFixture<RecruitmentPage>;
  let component: RecruitmentPage;
  let http: HttpTestingController;

  const opening: JobOpening = {
    id: 'open-1',
    titleAr: 'محاسب',
    titleEn: 'Accountant',
    departmentId: 'dep-1',
    headcount: 2,
    status: 'OPEN',
    description: null,
    published: true,
    applicationCount: 0,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    version: 0,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RecruitmentPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: I18nService, useValue: { t: (key: string) => key, locale: vi.fn(() => 'en-US') } },
        { provide: NotificationService, useValue: { success: vi.fn(), error: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RecruitmentPage);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    flushLoad();
    await fixture.whenStable();
  });

  afterEach(() => {
    try {
      http.verify();
    } finally {
      TestBed.resetTestingModule();
    }
  });

  function flushLoad(): void {
    http.expectOne('/api/v1/recruitment/openings').flush([opening]);
    http.expectOne('/api/v1/recruitment/applications').flush([]);
    http.expectOne('/api/v1/organization/departments').flush([
      { id: 'dep-1', code: 'ACC', name: 'Accounting', active: true },
    ]);
  }

  it('AC-3: surfaces a non-blocking duplicate warning banner listing the prior applicant', async () => {
    component.openNewApplication();
    component.applicationForm.patchValue({ openingId: 'open-1', fullName: 'New Applicant' });
    component.applicationForm.patchValue({ phone: '01000000000' });
    await new Promise((r) => setTimeout(r, 500));
    const duplicates = http.expectOne(
      (req) => req.method === 'GET' && req.url === '/api/v1/recruitment/applications/duplicates' && req.params.get('phone') === '01000000000',
    );
    duplicates.flush([{ applicationId: 'app-old', fullName: 'Prior Applicant', matchedBy: 'phone' }]);
    await Promise.resolve();
    expect(component.warnings()).toHaveLength(1);
    expect(component.warnings()[0]).toContain('Prior Applicant');
  });

  it('AC-4: rejects an oversized CV and clears it', () => {
    component.openNewApplication();
    const file = new File([new ArrayBuffer(5 * 1024 * 1024 + 1)], 'big.pdf', { type: 'application/pdf' });
    component.onCvSelected({ target: { files: [file] } } as unknown as Event);
    expect(component.cvError()).not.toBeNull();
    expect(component.cvFile.value).toBeNull();
  });

  it('AC-4: rejects an unsupported CV type', () => {
    component.openNewApplication();
    const file = new File(['x'], 'evil.exe', { type: 'application/x-msdownload' });
    component.onCvSelected({ target: { files: [file] } } as unknown as Event);
    expect(component.cvError()).not.toBeNull();
    expect(component.cvFile.value).toBeNull();
  });

  it('AC-4: accepts a valid CV and uploads it after the application is created', async () => {
    component.openNewApplication();
    const file = new File(['%PDF-1.4'], 'cv.pdf', { type: 'application/pdf' });
    component.onCvSelected({ target: { files: [file] } } as unknown as Event);
    expect(component.cvFile.value).not.toBeNull();
    expect(component.cvError()).toBeNull();

    component.applicationForm.patchValue({ openingId: 'open-1', fullName: 'Hired Person' });
    const promise = component.submitApplication();
    const create = http.expectOne((req) => req.method === 'POST' && req.url === '/api/v1/recruitment/applications');
    expect(create.request.body.fullName).toBe('Hired Person');
    create.flush({
      id: 'app-1', openingId: 'open-1', fullName: 'Hired Person', phone: null, email: null,
      source: null, cvAttachmentId: null, stage: 'NEW', rating: null, notes: null,
      convertedEmployeeId: null, createdAt: Date.now(), updatedAt: Date.now(), version: 0,
    });
    await Promise.resolve();
    const cv = http.expectOne((req) => req.method === 'POST' && req.url === '/api/v1/recruitment/applications/app-1/cv');
    expect(cv.request.body instanceof FormData).toBe(true);
    cv.flush({ id: 'cv-1', originalName: 'cv.pdf', contentType: 'application/pdf', sizeBytes: 8 });
    await new Promise((r) => setTimeout(r, 0));
    flushLoad();
    await promise;
    expect(component.appDrawerOpen()).toBe(false);
    expect(component.cvFile.value).toBeNull();
  });

  it('AC-4: removeCv clears the selected file', () => {
    component.openNewApplication();
    const file = new File(['x'], 'cv.pdf', { type: 'application/pdf' });
    component.cvFile.setValue(file);
    component.removeCv();
    expect(component.cvFile.value).toBeNull();
    expect(component.cvError()).toBeNull();
  });
});