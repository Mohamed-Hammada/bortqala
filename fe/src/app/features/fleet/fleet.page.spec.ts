import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { FleetPageComponent } from './fleet.page';
import { FleetService } from './fleet.service';
import { I18nService } from '../../core/i18n.service';

describe('FleetPageComponent', () => {
  let component: FleetPageComponent;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FleetPageComponent],
      providers: [
        FleetService,
        I18nService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(FleetPageComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should initialize and load vehicles by default', async () => {
    expect(component.activeTab()).toBe('vehicles');

    const loadPromise = component.loadData();

    const reqVehicles = httpMock.expectOne('/api/v1/fleet/vehicles');
    expect(reqVehicles.request.method).toBe('GET');
    reqVehicles.flush([
      {
        id: 'veh-1',
        plateNumber: 'ABC-123',
        make: 'Toyota',
        model: 'Hilux',
        year: 2024,
        vehicleType: 'TRUCK',
        currentOdometer: 10000,
        status: 'ACTIVE',
        createdAt: 1000,
        updatedAt: 1000,
      },
    ]);

    await loadPromise;
    expect(component.fleet.vehicles().length).toBe(1);
    expect(component.fleet.vehicles()[0].plateNumber).toBe('ABC-123');
  });

  it('should switch to fuel tab and load fuel logs', async () => {
    const tabPromise = component.setTab('fuel');
    expect(component.activeTab()).toBe('fuel');

    const reqVeh = httpMock.expectOne('/api/v1/fleet/vehicles');
    expect(reqVeh.request.method).toBe('GET');
    reqVeh.flush([]);

    const reqFuel = httpMock.expectOne('/api/v1/fleet/fuel-logs');
    expect(reqFuel.request.method).toBe('GET');
    reqFuel.flush([
      {
        id: 'f-1',
        vehicleId: 'veh-1',
        logDate: '2026-08-10',
        liters: 10,
        odometer: 10100,
        totalCost: 150,
        efficiencyKmPerLiter: 10.0,
        createdAt: 1000,
      },
    ]);

    await tabPromise;
    expect(component.fleet.fuelLogs().length).toBe(1);
    expect(component.fleet.fuelLogs()[0].efficiencyKmPerLiter).toBe(10.0);
  });

  it('should switch to costs tab and load cost summary', async () => {
    const tabPromise = component.setTab('costs');
    expect(component.activeTab()).toBe('costs');

    const reqCosts = httpMock.expectOne('/api/v1/fleet/cost-summary');
    expect(reqCosts.request.method).toBe('GET');
    reqCosts.flush({
      totalVehicles: 2,
      totalFuelCost: 1000,
      totalMaintenanceCost: 500,
      grandTotalCost: 1500,
      totalKilometers: 50000,
      costPerKilometer: 0.03,
    });

    await tabPromise;
    expect(component.fleet.costSummary()?.grandTotalCost).toBe(1500);
    expect(component.fleet.costSummary()?.costPerKilometer).toBe(0.03);
  });
});
