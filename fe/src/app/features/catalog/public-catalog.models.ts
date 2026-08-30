export interface PublicProductSummary {
  id: string;
  sku: string;
  slug: string;
  name: string;
  nameAr?: string;
  categorySlug: string;
  categoryName: string;
  brandSlug: string;
  brandName: string;
  price: number;
  compareAtPrice?: number | null;
  currency: string;
  stockStatus: string;
  imageUrl?: string | null;
  isFeatured: boolean;
}

export interface PublicProductDetail {
  id: string;
  sku: string;
  slug: string;
  name: string;
  nameAr?: string;
  description?: string;
  categorySlug: string;
  categoryName: string;
  brandSlug: string;
  brandName: string;
  price: number;
  compareAtPrice?: number | null;
  currency: string;
  stockStatus: string;
  imageUrl?: string | null;
  specificationsJson?: string;
  isFeatured: boolean;
}

export interface CategorySummary {
  slug: string;
  name: string;
  productCount: number;
}

export interface BrandSummary {
  slug: string;
  name: string;
  productCount: number;
}

export interface ProductPageResponse {
  items: PublicProductSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
