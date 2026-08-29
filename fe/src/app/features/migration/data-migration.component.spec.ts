import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DataMigrationComponent } from './data-migration.component';
import { I18nService } from '../../core/i18n.service';

describe('DataMigrationComponent', () => {
  let component: DataMigrationComponent;
  let fixture: ComponentFixture<DataMigrationComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DataMigrationComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: I18nService,
          useValue: {
            t: (key: string) => key,
            locale: () => 'ar-EG',
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DataMigrationComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('initializes and loads batch list', async () => {
    fixture.detectChanges();
    const req = httpMock.expectOne('/api/v1/migration/batches');
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        id: 'b-1',
        entityType: 'CUSTOMERS',
        status: 'COMMITTED',
        fileName: 'customers.csv',
        totalRecords: 10,
        importedRecords: 10,
        rejectedRecords: 0,
        duplicateRecords: 0,
        totalAmount: 50000,
        glAccountCode: '110300',
        glBalanceMatch: true,
        createdBy: 'admin',
        startedAt: '2026-08-30T00:00:00Z',
      },
    ]);

    await fixture.whenStable();

    expect(component.batches().length).toBe(1);
    expect(component.batches()[0].id).toBe('b-1');
  });
});
