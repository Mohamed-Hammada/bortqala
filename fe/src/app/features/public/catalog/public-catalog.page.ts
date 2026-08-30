import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { I18nService } from '../../../core/i18n.service';
import { PublicCatalogService } from '../../catalog/public-catalog.service';
import { BrandSummary, CategorySummary, PublicProductSummary } from '../../catalog/public-catalog.models';

@Component({
  selector: 'app-public-catalog-page',
  standalone: true,
  imports: [RouterLink, DecimalPipe],
  template: `
    <div class="catalog-container">
      <header class="catalog-header">
        <h1>{{ i18n.t('catalog.title') }}</h1>
        <div class="search-bar">
          <input
            type="text"
            [placeholder]="i18n.t('catalog.searchPlaceholder')"
            (input)="onSearchChange($event)"
          />
        </div>
      </header>

      <div class="catalog-body">
        <aside class="catalog-sidebar">
          <div class="filter-group">
            <h3>{{ i18n.t('catalog.categories') }}</h3>
            <button
              class="filter-btn"
              [class.active]="selectedCategory() === ''"
              (click)="selectCategory('')"
            >
              {{ i18n.t('catalog.allCategories') }}
            </button>
            @for (cat of categories(); track cat.slug) {
              <button
                class="filter-btn"
                [class.active]="selectedCategory() === cat.slug"
                (click)="selectCategory(cat.slug)"
              >
                {{ cat.name }} ({{ cat.productCount }})
              </button>
            }
          </div>

          <div class="filter-group">
            <h3>{{ i18n.t('catalog.brands') }}</h3>
            <button
              class="filter-btn"
              [class.active]="selectedBrand() === ''"
              (click)="selectBrand('')"
            >
              {{ i18n.t('catalog.allBrands') }}
            </button>
            @for (b of brands(); track b.slug) {
              <button
                class="filter-btn"
                [class.active]="selectedBrand() === b.slug"
                (click)="selectBrand(b.slug)"
              >
                {{ b.name }} ({{ b.productCount }})
              </button>
            }
          </div>
        </aside>

        <main class="catalog-products">
          @if (loading()) {
            <div class="loading-state">{{ i18n.t('common.loading') }}</div>
          } @else if (products().length === 0) {
            <div class="empty-state">{{ i18n.t('common.noData') }}</div>
          } @else {
            <div class="product-grid">
              @for (p of products(); track p.id) {
                <div class="product-card">
                  @if (p.imageUrl) {
                    <img [src]="p.imageUrl" [alt]="p.name" class="product-img" />
                  } @else {
                    <div class="product-img-placeholder">💻</div>
                  }
                  <div class="product-info">
                    <div class="product-brand">{{ p.brandName }}</div>
                    <h2 class="product-name">{{ p.name }}</h2>
                    <div class="product-price">
                      {{ p.price | number:'1.2-2' }} {{ p.currency }}
                    </div>
                    <div class="product-stock" [class.in-stock]="p.stockStatus === 'IN_STOCK'">
                      {{ p.stockStatus === 'IN_STOCK' ? i18n.t('catalog.inStock') : i18n.t('catalog.outOfStock') }}
                    </div>
                    <a [routerLink]="['/products', p.slug]" class="view-btn">
                      {{ i18n.t('catalog.viewDetails') }}
                    </a>
                  </div>
                </div>
              }
            </div>
          }
        </main>
      </div>
    </div>
  `,
  styles: [`
    .catalog-container { max-width: 1200px; margin: 0 auto; padding: 2rem 1rem; }
    .catalog-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; flex-wrap: wrap; gap: 1rem; }
    .search-bar input { padding: 0.75rem 1.25rem; border-radius: 8px; border: 1px solid var(--border); min-width: 280px; font-size: 1rem; }
    .catalog-body { display: grid; grid-template-columns: 240px 1fr; gap: 2rem; }
    @media (max-width: 768px) { .catalog-body { grid-template-columns: 1fr; } }
    .catalog-sidebar { background: var(--surface-card); padding: 1.5rem; border-radius: 12px; border: 1px solid var(--border); height: fit-content; }
    .filter-group { margin-bottom: 1.5rem; }
    .filter-group h3 { font-size: 1rem; margin-bottom: 0.75rem; color: var(--ink); }
    .filter-btn { display: block; width: 100%; text-align: left; padding: 0.5rem 0.75rem; margin-bottom: 0.25rem; border: none; background: transparent; border-radius: 6px; cursor: pointer; color: var(--muted); font-size: 0.9rem; }
    .filter-btn:hover { background: var(--surface-hover); color: var(--ink); }
    .filter-btn.active { background: var(--gold-soft); color: var(--gold); font-weight: 600; }
    .product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 1.5rem; }
    .product-card { background: var(--surface-card); border-radius: 12px; border: 1px solid var(--border); overflow: hidden; display: flex; flex-direction: column; transition: transform 0.2s; }
    .product-card:hover { transform: translateY(-4px); }
    .product-img { width: 100%; height: 180px; object-fit: cover; }
    .product-img-placeholder { width: 100%; height: 180px; display: flex; align-items: center; justify-content: center; font-size: 3rem; background: var(--surface); }
    .product-info { padding: 1.25rem; flex: 1; display: flex; flex-direction: column; }
    .product-brand { font-size: 0.8rem; color: var(--muted); text-transform: uppercase; letter-spacing: 0.5px; }
    .product-name { font-size: 1.1rem; margin: 0.25rem 0 0.75rem; color: var(--ink); flex: 1; }
    .product-price { font-size: 1.25rem; font-weight: 700; color: var(--gold); margin-bottom: 0.5rem; }
    .product-stock { font-size: 0.85rem; color: var(--danger); margin-bottom: 1rem; }
    .product-stock.in-stock { color: var(--success); }
    .view-btn { display: block; text-align: center; padding: 0.6rem; border-radius: 8px; background: var(--gold); color: var(--ink); text-decoration: none; font-weight: 600; }
    .loading-state, .empty-state { text-align: center; padding: 3rem; color: var(--muted); }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicCatalogPage implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly catalogService = inject(PublicCatalogService);

  readonly products = signal<PublicProductSummary[]>([]);
  readonly categories = signal<CategorySummary[]>([]);
  readonly brands = signal<BrandSummary[]>([]);
  readonly selectedCategory = signal<string>('');
  readonly selectedBrand = signal<string>('');
  readonly searchTerm = signal<string>('');
  readonly loading = signal<boolean>(true);

  ngOnInit() {
    this.loadMetadata();
    this.loadProducts();
  }

  loadMetadata() {
    this.catalogService.getCategories().subscribe({
      next: (data) => this.categories.set(data),
      error: () => this.categories.set([]),
    });
    this.catalogService.getBrands().subscribe({
      next: (data) => this.brands.set(data),
      error: () => this.brands.set([]),
    });
  }

  loadProducts() {
    this.loading.set(true);
    this.catalogService.searchProducts({
      categorySlug: this.selectedCategory() || undefined,
      brandSlug: this.selectedBrand() || undefined,
      search: this.searchTerm() || undefined,
    }).subscribe({
      next: (res) => {
        this.products.set(res.items);
        this.loading.set(false);
      },
      error: () => {
        this.products.set([]);
        this.loading.set(false);
      },
    });
  }

  selectCategory(slug: string) {
    this.selectedCategory.set(slug);
    this.loadProducts();
  }

  selectBrand(slug: string) {
    this.selectedBrand.set(slug);
    this.loadProducts();
  }

  onSearchChange(event: Event) {
    const target = event.target as HTMLInputElement;
    this.searchTerm.set(target.value.trim());
    this.loadProducts();
  }
}
