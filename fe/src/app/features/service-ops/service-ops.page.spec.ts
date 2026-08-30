import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ServiceOpsPageComponent } from './service-ops.page';
import { ServiceOpsService } from './service-ops.service';
import { I18nService } from '../../core/i18n.service';

describe('ServiceOpsPageComponent', () => {
  let component: ServiceOpsPageComponent;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ServiceOpsPageComponent],
      providers: [
        ServiceOpsService,
        I18nService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(ServiceOpsPageComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should initialize and load rentals by default', async () => {
    expect(component.activeTab()).toBe('rentals');

    const loadPromise = component.loadData();

    const reqItems = httpMock.expectOne('/api/v1/service-ops/rentals/items');
    expect(reqItems.request.method).toBe('GET');
    reqItems.flush([]);

    const reqContracts = httpMock.expectOne('/api/v1/service-ops/rentals/contracts');
    expect(reqContracts.request.method).toBe('GET');
    reqContracts.flush([]);

    const reqUtil = httpMock.expectOne('/api/v1/service-ops/rentals/utilization');
    expect(reqUtil.request.method).toBe('GET');
    reqUtil.flush({ totalItems: 5, rentedItems: 2, availableItems: 3, utilizationPercentage: 40 });

    await loadPromise;
    expect(component.serviceOps.utilization()?.utilizationPercentage).toBe(40);
  });

  it('should switch tabs and load work orders', async () => {
    const tabPromise = component.setTab('work-orders');
    expect(component.activeTab()).toBe('work-orders');

    const reqWO = httpMock.expectOne('/api/v1/service-ops/work-orders');
    expect(reqWO.request.method).toBe('GET');
    reqWO.flush([
      {
        id: 'wo-1',
        ticketNo: 'WO-101',
        title: 'Overhaul pump',
        priority: 'HIGH',
        status: 'OPEN',
        laborTotal: 100,
        partsTotal: 50,
        grandTotal: 150,
        laborLines: [],
        partsLines: [],
        createdAt: 1000,
        updatedAt: 1000,
      },
    ]);

    await tabPromise;
    expect(component.serviceOps.workOrders().length).toBe(1);
    expect(component.serviceOps.workOrders()[0].ticketNo).toBe('WO-101');
  });

  it('should switch tabs and load bookings', async () => {
    const tabPromise = component.setTab('bookings');
    expect(component.activeTab()).toBe('bookings');

    const reqRes = httpMock.expectOne('/api/v1/service-ops/bookings/resources');
    expect(reqRes.request.method).toBe('GET');
    reqRes.flush([{ id: 'res-1', code: 'ROOM-A', name: 'Room A', kind: 'ROOM', active: true, createdAt: 1, updatedAt: 1 }]);

    const reqBook = httpMock.expectOne('/api/v1/service-ops/bookings');
    expect(reqBook.request.method).toBe('GET');
    reqBook.flush([{ id: 'b-1', resourceId: 'res-1', title: 'Sprint Review', startTime: 1000, endTime: 2000, status: 'CONFIRMED', createdAt: 1, updatedAt: 1 }]);

    await tabPromise;
    expect(component.serviceOps.resources().length).toBe(1);
    expect(component.serviceOps.bookings().length).toBe(1);
  });
});
