import { TestBed } from '@angular/core/testing';
import { CategoriesPage } from './categories.page';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('CategoriesPage', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CategoriesPage],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(CategoriesPage);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});
