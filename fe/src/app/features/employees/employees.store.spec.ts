import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { EmployeesStore } from './employees.store';
import { Employee, EmployeePayload } from './employees.models';

describe('EmployeesStore', () => {
  let store: EmployeesStore;
  let httpMock: HttpTestingController;

  const employee: Employee = {
    id: 'emp-1',
    employeeCode: 'EMP-001',
    fullName: 'Ahmed Hassan',
    deviceUserId: '1001',
    categoryId: 'cat-1',
    categoryName: 'General',
    employmentType: 'FIXED',
    baseSalary: 1000,
    activeFrom: 0,
    activeTo: null,
    active: false,
    version: 1,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [EmployeesStore, provideHttpClient(), provideHttpClientTesting()],
    });
    store = TestBed.inject(EmployeesStore);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  function flushLoad(employees: Employee[]) {
    httpMock.expectOne((req) => req.method === 'GET' && req.url === '/api/v1/employees').flush(employees);
    httpMock.expectOne((req) => req.method === 'GET' && req.url === '/api/v1/categories').flush([]);
  }

  async function nextTick(): Promise<void> {
    await Promise.resolve();
    await Promise.resolve();
  }

  describe('deactivate', () => {
    it('deletes the employee and reloads the list on success', async () => {
      store.items.set([employee]);
      const promise = store.deactivate('emp-1');

      httpMock.expectOne((req) => req.method === 'DELETE' && req.url === '/api/v1/employees/emp-1').flush(null);
      await nextTick();
      flushLoad([]);

      await promise;
      expect(store.items()).toEqual([]);
    });

    it('propagates the failure to the caller', async () => {
      const promise = store.deactivate('emp-1');
      httpMock
        .expectOne((req) => req.method === 'DELETE' && req.url === '/api/v1/employees/emp-1')
        .flush({ message: 'deactivation failed' }, { status: 500, statusText: 'Server Error' });

      await expect(promise).rejects.toBeTruthy();
    });
  });

  describe('reactivate', () => {
    const payload: EmployeePayload = {
      employeeCode: 'EMP-001',
      fullName: 'Ahmed Hassan',
      deviceUserId: '1001',
      categoryId: 'cat-1',
      employmentType: 'FIXED',
      baseSalary: 1000,
      activeFrom: 0,
      activeTo: null,
      active: true,
      version: 1,
    };

    it('sends the active payload and reloads the list on success', async () => {
      store.items.set([]);
      const promise = store.reactivate('emp-1', payload);

      const putReq = httpMock.expectOne((req) => req.method === 'PUT' && req.url === '/api/v1/employees/emp-1');
      expect(putReq.request.body).toEqual({ ...payload, active: true });
      putReq.flush({ ...employee, active: true });
      await nextTick();
      flushLoad([{ ...employee, active: true }]);

      await promise;
      expect(store.items()[0].active).toBe(true);
    });

    it('propagates the failure to the caller', async () => {
      const promise = store.reactivate('emp-1', payload);
      httpMock
        .expectOne((req) => req.method === 'PUT' && req.url === '/api/v1/employees/emp-1')
        .flush({ message: 'reactivation failed' }, { status: 409, statusText: 'Conflict' });

      await expect(promise).rejects.toBeTruthy();
    });
  });
});
