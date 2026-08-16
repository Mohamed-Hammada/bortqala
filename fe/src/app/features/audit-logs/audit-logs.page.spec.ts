import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { AuditLogsPage } from './audit-logs.page';
import { I18nService } from '../../core/i18n.service';
import { AuditLog, AuditLogPage } from './audit-logs.models';

describe('AuditLogsPage', () => {
  let httpMock: HttpTestingController;
  let page: AuditLogsPage;

  const log = (overrides: Partial<AuditLog> = {}): AuditLog => ({
    id: 'log-1',
    action: 'CREATE',
    entityType: 'EMPLOYEE',
    entityId: 'QA-EMP-RETEST-0808',
    username: 'admin',
    detailsJson: '{"code":"QA-EMP-RETEST-0808"}',
    ipAddress: '10.0.0.1',
    occurredAt: 1700000000000,
    ...overrides,
  });

  const pageOf = (items: AuditLog[]): AuditLogPage => ({
    content: items,
    page: 0,
    pageSize: 20,
    totalElements: items.length,
    totalPages: 1,
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuditLogsPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: I18nService,
          useValue: { t: (key: string) => key },
        },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(AuditLogsPage);
    page = fixture.componentInstance;

    httpMock.expectOne((req) => req.url === '/api/v1/audit-logs').flush(pageOf([log()]));
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    httpMock.verify();
  });

  async function yieldMicrotasks(): Promise<void> {
    await Promise.resolve();
    await Promise.resolve();
  }

  function applyFilters(): Promise<void> {
    page.applyFilters();
    return yieldMicrotasks();
  }

  it('loads the first page without filters on init', () => {
    expect(page.logs()).toHaveLength(1);
    expect(page.totalElements()).toBe(1);
    expect(page.loading()).toBe(false);
  });

  it('sends entityType, action, username and search filters on apply', async () => {
    page.entityTypeFilter.set(' EMPLOYEE ');
    page.actionFilter.set('CREATE');
    page.usernameFilter.set('admin');
    page.searchFilter.set('QA-EMP-RETEST-0808');

    await applyFilters();

    const req = httpMock.expectOne((request) => request.url === '/api/v1/audit-logs');
    expect(req.request.params.get('entityType')).toBe('EMPLOYEE');
    expect(req.request.params.get('action')).toBe('CREATE');
    expect(req.request.params.get('username')).toBe('admin');
    expect(req.request.params.get('search')).toBe('QA-EMP-RETEST-0808');
    expect(req.request.params.get('page')).toBe('0');
    req.flush(pageOf([log()]));
  });

  it('sends date range as epoch millis with to-date at end of day', async () => {
    page.fromFilter.set('2026-08-01');
    page.toFilter.set('2026-08-15');

    await applyFilters();

    const req = httpMock.expectOne((request) => request.url === '/api/v1/audit-logs');
    const from = Number(req.request.params.get('from'));
    const to = Number(req.request.params.get('to'));
    expect(from).toBe(Date.parse('2026-08-01T00:00:00Z'));
    expect(to).toBe(Date.parse('2026-08-15T23:59:59.999Z'));
    req.flush(pageOf([log()]));
  });

  it('omits empty filters from the request', async () => {
    page.fromFilter.set('');
    page.toFilter.set('');
    await applyFilters();

    const req = httpMock.expectOne((request) => request.url === '/api/v1/audit-logs');
    expect(req.request.params.has('entityType')).toBe(false);
    expect(req.request.params.has('action')).toBe(false);
    expect(req.request.params.has('username')).toBe(false);
    expect(req.request.params.has('search')).toBe(false);
    expect(req.request.params.has('from')).toBe(false);
    expect(req.request.params.has('to')).toBe(false);
    req.flush(pageOf([log()]));
  });

  it('reset clears every filter and reloads', async () => {
    page.entityTypeFilter.set('EMPLOYEE');
    page.actionFilter.set('CREATE');
    page.usernameFilter.set('admin');
    page.searchFilter.set('QA-EMP');
    page.fromFilter.set('2026-08-01');
    page.toFilter.set('2026-08-15');

    page.resetFilters();
    await yieldMicrotasks();

    expect(page.entityTypeFilter()).toBe('');
    expect(page.actionFilter()).toBe('');
    expect(page.usernameFilter()).toBe('');
    expect(page.searchFilter()).toBe('');
    expect(page.fromFilter()).toBe((page as any).relativeDate(-1));
    expect(page.toFilter()).toBe((page as any).relativeDate(0));

    const req = httpMock.expectOne((request) => request.url === '/api/v1/audit-logs');
    expect(req.request.params.has('entityType')).toBe(false);
    expect(req.request.params.has('from')).toBe(true);
    req.flush(pageOf([log()]));
  });

  it('changing page size resets to page 1 and reloads with the new size', async () => {
    page.totalElements.set(99);
    page.pagination.changePage(2, 99);

    page.changePageSize(50);
    await yieldMicrotasks();

    expect(page.pagination.page()).toBe(1);
    expect(page.pagination.pageSize()).toBe(50);

    const req = httpMock.expectOne((request) => request.url === '/api/v1/audit-logs');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('50');
    req.flush(pageOf([log()]));
  });

  it('computes the visible range for page 2 at size 25', () => {
    page.totalElements.set(99);
    page.pagination.changePage(2, 99);

    const component = page.pagination;
    const from = (component.page() - 1) * component.pageSize() + 1;
    const to = Math.min(99, component.page() * component.pageSize());
    expect(from).toBe(26);
    expect(to).toBe(50);
  });

  it('shows the load-error state instead of the empty state when the API fails', async () => {
    const fixture = TestBed.createComponent(AuditLogsPage);
    const fixturePage = fixture.componentInstance;
    httpMock.expectOne((request) => request.url === '/api/v1/audit-logs').flush(pageOf([]));
    fixture.detectChanges();

    fixturePage.applyFilters();
    fixture.detectChanges();
    const failing = httpMock.expectOne((request) => request.url === '/api/v1/audit-logs');
    failing.flush({ message: 'boom' }, { status: 500, statusText: 'Server Error' });
    await yieldMicrotasks();
    fixture.detectChanges();

    expect(fixturePage.error()).not.toBeNull();
    expect(fixturePage.loading()).toBe(false);

    const host: HTMLElement = fixture.nativeElement;
    const loadError = host.querySelector('.load-error');
    expect(loadError).not.toBeNull();
    expect(loadError!.querySelector('strong')!.textContent?.trim()).toBe('audit.loadErrorTitle');
    const retryButton = loadError!.querySelector('button') as HTMLButtonElement;
    expect(retryButton).not.toBeNull();
    expect(retryButton.textContent?.trim()).toBe('common.retry');
    expect(host.querySelector('.table-card')).toBeNull();
    expect(host.textContent).not.toContain('audit.empty');
  });

  it('retry refires the audit-logs GET and clears the error on success', async () => {
    page.applyFilters();
    const failing = httpMock.expectOne((request) => request.url === '/api/v1/audit-logs');
    failing.flush({ message: 'boom' }, { status: 500, statusText: 'Server Error' });
    await yieldMicrotasks();
    expect(page.error()).not.toBeNull();

    page.retry();
    expect(page.error()).toBeNull();
    const retryReq = httpMock.expectOne((request) => request.url === '/api/v1/audit-logs');
    retryReq.flush(pageOf([log()]));
    await yieldMicrotasks();

    expect(page.error()).toBeNull();
    expect(page.logs()).toHaveLength(1);
  });
});
