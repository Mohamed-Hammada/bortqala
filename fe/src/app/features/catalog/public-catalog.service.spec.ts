import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { PublicCatalogService } from './public-catalog.service';

describe('PublicCatalogService', () => {
  let service: PublicCatalogService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), PublicCatalogService],
    });
    service = TestBed.inject(PublicCatalogService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('searchProducts builds query params and fetches items', () => {
    service.searchProducts({ categorySlug: 'laptops', brandSlug: 'dell', minPrice: 10000 }).subscribe((res) => {
      expect(res.items.length).toBe(1);
      expect(res.items[0].slug).toBe('dell-xps-15');
    });

    const req = httpMock.expectOne((r) =>
      r.url === '/api/v1/public/catalog/products' &&
      r.params.get('categorySlug') === 'laptops' &&
      r.params.get('brandSlug') === 'dell' &&
      r.params.get('minPrice') === '10000'
    );
    expect(req.request.method).toBe('GET');

    req.flush({
      items: [{
        id: 'p1',
        sku: 'SKU-1',
        slug: 'dell-xps-15',
        name: 'Dell XPS 15',
        categorySlug: 'laptops',
        categoryName: 'Laptops',
        brandSlug: 'dell',
        brandName: 'Dell',
        price: 45000,
        currency: 'EGP',
        stockStatus: 'IN_STOCK',
        isFeatured: true,
      }],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
  });

  it('getProductBySlug fetches product detail', () => {
    service.getProductBySlug('dell-xps-15').subscribe((product) => {
      expect(product.slug).toBe('dell-xps-15');
      expect(product.name).toBe('Dell XPS 15');
    });

    const req = httpMock.expectOne('/api/v1/public/catalog/products/dell-xps-15');
    expect(req.request.method).toBe('GET');
    req.flush({
      id: 'p1',
      sku: 'SKU-1',
      slug: 'dell-xps-15',
      name: 'Dell XPS 15',
      categorySlug: 'laptops',
      categoryName: 'Laptops',
      brandSlug: 'dell',
      brandName: 'Dell',
      price: 45000,
      currency: 'EGP',
      stockStatus: 'IN_STOCK',
      isFeatured: true,
    });
  });

  it('getCategories and getBrands fetch summaries', () => {
    service.getCategories().subscribe((cats) => {
      expect(cats.length).toBe(1);
      expect(cats[0].slug).toBe('laptops');
    });

    const reqCat = httpMock.expectOne('/api/v1/public/catalog/categories');
    expect(reqCat.request.method).toBe('GET');
    reqCat.flush([{ slug: 'laptops', name: 'Laptops', productCount: 5 }]);

    service.getBrands().subscribe((brands) => {
      expect(brands.length).toBe(1);
      expect(brands[0].slug).toBe('dell');
    });

    const reqBrand = httpMock.expectOne('/api/v1/public/catalog/brands');
    expect(reqBrand.request.method).toBe('GET');
    reqBrand.flush([{ slug: 'dell', name: 'Dell', productCount: 3 }]);
  });
});
