import '@angular/compiler';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { AuthService } from '../../core/auth/auth.service';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { TranslationManagementComponent } from './translation-management.component';

interface TestTranslationRow {
  key: string;
  defaultValue: string | null;
  overrideValue: string | null;
  effectiveValue: string | null;
  overridden: boolean;
}

const APPS = [
  { id: 'app-1', code: 'DEMO', name: 'Demo App', active: true },
  { id: 'app-2', code: 'CORE', name: 'Core ERP', active: true },
];

const DEFAULT_ROWS: TestTranslationRow[] = [
  { key: 'common.save', defaultValue: 'حفظ', overrideValue: null, effectiveValue: 'حفظ', overridden: false },
  { key: 'nav.title', defaultValue: 'العنوان العام', overrideValue: null, effectiveValue: 'العنوان العام', overridden: false },
];

const APP_ROWS: TestTranslationRow[] = [
  { key: 'common.save', defaultValue: 'حفظ', overrideValue: null, effectiveValue: 'حفظ', overridden: false },
  { key: 'nav.title', defaultValue: 'العنوان العام', overrideValue: 'عنوان العميل', effectiveValue: 'عنوان العميل', overridden: true },
];

describe('TranslationManagementComponent', () => {
  let component: TranslationManagementComponent;
  let fixture: ComponentFixture<TranslationManagementComponent>;
  let http: HttpTestingController;
  let notification: NotificationService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TranslationManagementComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { app: () => ({ id: 'app-1', code: 'DEMO', name: 'Demo App' }) } },
        {
          provide: I18nService,
          useValue: {
            locale: () => 'ar-EG',
            t: (key: string, _params?: unknown, fallback?: string) => fallback ?? key,
            invalidate: vi.fn(),
            use: vi.fn(() => Promise.resolve()),
          },
        },
        {
          provide: NotificationService,
          useValue: { success: vi.fn(), error: vi.fn() },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TranslationManagementComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    notification = TestBed.inject(NotificationService);

    await flushInitialLoad();
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
    vi.useRealTimers();
    http.verify();
  });

  function pageResponse(
    content = DEFAULT_ROWS,
    page = 0,
    size = 25,
    totalElements = content.length,
    overriddenCount = 0,
  ) {
    return {
      content,
      page,
      size,
      totalElements,
      totalPages: Math.max(1, Math.ceil(totalElements / size)),
      overriddenCount,
    };
  }

  function expectTranslationRequest(options: {
    locale?: string;
    page?: number;
    size?: number;
    appId?: string | null;
    search?: string | null;
  } = {}): TestRequest {
    const locale = options.locale ?? 'ar-EG';
    const page = options.page ?? 0;
    const size = options.size ?? 25;

    return http.expectOne((request) => {
      if (request.url !== '/api/v1/i18n/admin/translations') return false;
      if (request.params.get('locale') !== locale) return false;
      if (request.params.get('page') !== String(page)) return false;
      if (request.params.get('size') !== String(size)) return false;
      if ((request.params.get('appId') ?? null) !== (options.appId ?? null)) return false;
      if ((request.params.get('search') ?? null) !== (options.search ?? null)) return false;
      return true;
    });
  }

  async function flushInitialLoad(): Promise<void> {
    http.expectOne('/api/v1/i18n/admin/apps').flush(APPS);
    await Promise.resolve();
    expectTranslationRequest().flush(pageResponse());
    await Promise.resolve();
  }

  it('loads the first server page and total count on init', () => {
    expect(component.apps()).toHaveLength(2);
    expect(component.rows().map((row) => row.key)).toEqual(['common.save', 'nav.title']);
    expect(component.total()).toBe(2);
    expect(component.pagination.page()).toBe(1);
  });

  it('reloads page zero when the language changes', async () => {
    const promise = component.changeLocale('en-US');
    expectTranslationRequest({ locale: 'en-US' }).flush(pageResponse(
      DEFAULT_ROWS.map((row) => ({ ...row, effectiveValue: 'Save' })),
    ));
    await promise;

    expect(component.locale()).toBe('en-US');
    expect(component.pagination.page()).toBe(1);
  });

  it('reloads page zero with the selected application scope', async () => {
    const promise = component.changeScope('app-1');
    expectTranslationRequest({ appId: 'app-1' }).flush(pageResponse(APP_ROWS, 0, 25, 2, 1));
    await promise;

    expect(component.appId()).toBe('app-1');
    expect(component.overriddenCount()).toBe(1);
    expect(component.rows().find((row) => row.key === 'nav.title')?.overridden).toBe(true);
  });

  it('requests the selected page from the backend', async () => {
    component.total.set(60);

    const promise = component.changePage(2);
    expectTranslationRequest({ page: 1 }).flush(pageResponse(DEFAULT_ROWS, 1, 25, 60));
    await promise;

    expect(component.pagination.page()).toBe(2);
    expect(component.total()).toBe(60);
  });

  it('resets to page one and requests the new page size', async () => {
    component.total.set(60);
    component.pagination.page.set(2);

    const promise = component.changePageSize(50);
    expectTranslationRequest({ page: 0, size: 50 }).flush(pageResponse(DEFAULT_ROWS, 0, 50, 60));
    await promise;

    expect(component.pagination.page()).toBe(1);
    expect(component.pagination.pageSize()).toBe(50);
  });

  it('debounces search and sends it to the backend', async () => {
    vi.useFakeTimers();
    component.changeSearch('العنوان');

    vi.advanceTimersByTime(299);
    http.expectNone((request) => request.url === '/api/v1/i18n/admin/translations');
    vi.advanceTimersByTime(1);

    expectTranslationRequest({ search: 'العنوان' }).flush(pageResponse([DEFAULT_ROWS[1]], 0, 25, 1));
    await Promise.resolve();

    expect(component.total()).toBe(1);
    expect(component.rows().map((row) => row.key)).toEqual(['nav.title']);
  });

  it('uploads an excel file then refreshes the current page', async () => {
    const file = new File(['binary'], 'translations.xlsx', {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });
    component.onImportFileSelected({
      target: { files: [file] },
    } as unknown as Event);

    const promise = component.uploadImport();
    const request = http.expectOne('/api/v1/i18n/admin/translations/import');
    expect(request.request.method).toBe('POST');
    const body = request.request.body as FormData;
    expect(body.get('locale')).toBe('ar-EG');
    request.flush({ importedCount: 2, createdCount: 1, updatedCount: 1, unchangedCount: 0 });
    await new Promise((resolve) => setTimeout(resolve, 0));

    expectTranslationRequest().flush(pageResponse());
    await promise;

    expect(component.importFile()).toBeNull();
    expect(notification.success).toHaveBeenCalled();
  });

  it('surfaces server-page load errors through the notification service', async () => {
    const promise = component.changeLocale('en-US');
    expectTranslationRequest({ locale: 'en-US' })
      .error(new ProgressEvent('error'), { status: 500, statusText: 'Server Error' });
    await promise;

    expect(notification.error).toHaveBeenCalled();
  });
});
