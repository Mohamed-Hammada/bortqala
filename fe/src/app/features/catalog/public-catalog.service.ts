import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  BrandSummary,
  CategorySummary,
  ProductPageResponse,
  PublicProductDetail,
} from './public-catalog.models';

@Injectable({ providedIn: 'root' })
export class PublicCatalogService {
  private readonly http = inject(HttpClient);

  searchProducts(params?: {
    categorySlug?: string;
    brandSlug?: string;
    minPrice?: number;
    maxPrice?: number;
    search?: string;
    page?: number;
    size?: number;
    sortBy?: string;
  }): Observable<ProductPageResponse> {
    let httpParams = new HttpParams();
    if (params?.categorySlug) httpParams = httpParams.set('categorySlug', params.categorySlug);
    if (params?.brandSlug) httpParams = httpParams.set('brandSlug', params.brandSlug);
    if (params?.minPrice != null) httpParams = httpParams.set('minPrice', params.minPrice.toString());
    if (params?.maxPrice != null) httpParams = httpParams.set('maxPrice', params.maxPrice.toString());
    if (params?.search) httpParams = httpParams.set('search', params.search);
    if (params?.page != null) httpParams = httpParams.set('page', params.page.toString());
    if (params?.size != null) httpParams = httpParams.set('size', params.size.toString());
    if (params?.sortBy) httpParams = httpParams.set('sortBy', params.sortBy);

    return this.http.get<ProductPageResponse>('/api/v1/public/catalog/products', { params: httpParams });
  }

  getProductBySlug(slug: string): Observable<PublicProductDetail> {
    return this.http.get<PublicProductDetail>(`/api/v1/public/catalog/products/${slug}`);
  }

  getCategories(): Observable<CategorySummary[]> {
    return this.http.get<CategorySummary[]>('/api/v1/public/catalog/categories');
  }

  getBrands(): Observable<BrandSummary[]> {
    return this.http.get<BrandSummary[]>('/api/v1/public/catalog/brands');
  }
}
