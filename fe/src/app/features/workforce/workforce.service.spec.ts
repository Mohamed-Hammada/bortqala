import { TestBed } from '@angular/core/testing';
import { WorkforceService } from './workforce.service';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('WorkforceService', () => {
  let service: WorkforceService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(WorkforceService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
