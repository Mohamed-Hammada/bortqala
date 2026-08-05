import { TestBed } from '@angular/core/testing';
import { CategoriesStore } from './categories.store';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('CategoriesStore', () => {
  let store: any;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [CategoriesStore, provideHttpClient(), provideHttpClientTesting()]
    });
    store = TestBed.inject(CategoriesStore);
  });

  it('should be created', () => {
    expect(store).toBeTruthy();
  });
});
