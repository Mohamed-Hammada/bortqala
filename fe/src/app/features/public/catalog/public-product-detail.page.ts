import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { I18nService } from '../../../core/i18n.service';
import { PublicCatalogService } from '../../catalog/public-catalog.service';
import { PublicProductDetail } from '../../catalog/public-catalog.models';

@Component({
  selector: 'app-public-product-detail-page',
  standalone: true,
  imports: [RouterLink, DecimalPipe],
  template: `
    <div class="detail-container">
      <a routerLink="/products" class="back-link">
        ← {{ i18n.t('catalog.backToCatalog') }}
      </a>

      @if (loading()) {
        <div class="loading-state">{{ i18n.t('common.loading') }}</div>
      } @else if (product()) {
        <div class="product-detail-layout">
          <div class="product-gallery">
            @if (product()!.imageUrl) {
              <img [src]="product()!.imageUrl" [alt]="product()!.name" class="main-img" />
            } @else {
              <div class="main-img-placeholder">💻</div>
            }
          </div>

          <div class="product-main-info">
            <div class="product-badge">{{ product()!.brandName }}</div>
            <h1 class="product-title">{{ product()!.name }}</h1>
            <div class="product-sku">{{ i18n.t('catalog.sku') }}: {{ product()!.sku }}</div>

            <div class="price-section">
              <span class="price">{{ product()!.price | number:'1.2-2' }} {{ product()!.currency }}</span>
              @if (product()!.compareAtPrice) {
                <span class="compare-price">{{ product()!.compareAtPrice | number:'1.2-2' }} {{ product()!.currency }}</span>
              }
            </div>

            <div class="stock-status" [class.in-stock]="product()!.stockStatus === 'IN_STOCK'">
              {{ product()!.stockStatus === 'IN_STOCK' ? i18n.t('catalog.inStock') : i18n.t('catalog.outOfStock') }}
            </div>

            @if (product()!.description) {
              <div class="description-section">
                <h3>{{ i18n.t('common.description') }}</h3>
                <p>{{ product()!.description }}</p>
              </div>
            }

            @if (specifications().length > 0) {
              <div class="specs-section">
                <h3>{{ i18n.t('catalog.specifications') }}</h3>
                <table class="specs-table">
                  <tbody>
                    @for (spec of specifications(); track spec.key) {
                      <tr>
                        <td class="spec-key">{{ spec.key }}</td>
                        <td class="spec-value">{{ spec.value }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          </div>
        </div>
      } @else {
        <div class="empty-state">{{ i18n.t('common.noData') }}</div>
      }
    </div>
  `,
  styles: [`
    .detail-container { max-width: 1100px; margin: 0 auto; padding: 2rem 1rem; }
    .back-link { display: inline-block; margin-bottom: 1.5rem; color: var(--gold); text-decoration: none; font-weight: 500; }
    .product-detail-layout { display: grid; grid-template-columns: 1fr 1fr; gap: 3rem; }
    @media (max-width: 768px) { .product-detail-layout { grid-template-columns: 1fr; gap: 1.5rem; } }
    .product-gallery { background: var(--surface-card); border-radius: 12px; border: 1px solid var(--border); overflow: hidden; display: flex; align-items: center; justify-content: center; min-height: 360px; }
    .main-img { width: 100%; max-height: 400px; object-fit: contain; }
    .main-img-placeholder { font-size: 6rem; color: var(--muted); }
    .product-badge { font-size: 0.85rem; color: var(--muted); text-transform: uppercase; font-weight: 600; }
    .product-title { font-size: 1.75rem; margin: 0.5rem 0; color: var(--ink); }
    .product-sku { font-size: 0.9rem; color: var(--muted); margin-bottom: 1.5rem; }
    .price-section { display: flex; align-items: baseline; gap: 1rem; margin-bottom: 1rem; }
    .price { font-size: 2rem; font-weight: 700; color: var(--gold); }
    .compare-price { font-size: 1.2rem; color: var(--muted); text-decoration: line-through; }
    .stock-status { font-size: 0.95rem; font-weight: 600; color: var(--danger); margin-bottom: 1.5rem; }
    .stock-status.in-stock { color: var(--success); }
    .description-section { margin-bottom: 1.5rem; border-top: 1px solid var(--border); padding-top: 1rem; }
    .specs-section { border-top: 1px solid var(--border); padding-top: 1rem; }
    .specs-table { width: 100%; border-collapse: collapse; margin-top: 0.75rem; }
    .specs-table td { padding: 0.6rem 0.5rem; border-bottom: 1px solid var(--border); font-size: 0.9rem; }
    .spec-key { color: var(--muted); width: 40%; }
    .spec-value { color: var(--ink); font-weight: 500; }
    .loading-state, .empty-state { text-align: center; padding: 4rem; color: var(--muted); }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicProductDetailPage implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly route = inject(ActivatedRoute);
  private readonly catalogService = inject(PublicCatalogService);

  readonly product = signal<PublicProductDetail | null>(null);
  readonly specifications = signal<{ key: string; value: string }[]>([]);
  readonly loading = signal<boolean>(true);

  ngOnInit() {
    const slug = this.route.snapshot.paramMap.get('slug') ?? '';
    if (!slug) {
      this.loading.set(false);
      return;
    }

    this.catalogService.getProductBySlug(slug).subscribe({
      next: (data) => {
        this.product.set(data);
        if (data.specificationsJson) {
          try {
            const parsed = JSON.parse(data.specificationsJson);
            const specs = Object.entries(parsed).map(([key, value]) => ({
              key,
              value: String(value),
            }));
            this.specifications.set(specs);
          } catch {
            this.specifications.set([]);
          }
        }
        this.loading.set(false);
      },
      error: () => {
        this.product.set(null);
        this.loading.set(false);
      },
    });
  }
}
