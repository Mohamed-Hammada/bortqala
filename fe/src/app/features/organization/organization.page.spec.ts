import { TestBed } from '@angular/core/testing';
import { OrganizationPage } from './organization.page';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('OrganizationPage', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrganizationPage],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(OrganizationPage);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});
