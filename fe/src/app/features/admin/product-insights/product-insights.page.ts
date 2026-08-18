import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ProductAnalyticsSettingsComponent } from '../../settings/product-analytics-settings.component';

@Component({
  selector: 'app-product-insights-page',
  standalone: true,
  imports: [ProductAnalyticsSettingsComponent],
  template: '<section class="page"><app-product-analytics-settings /></section>',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductInsightsPage {}
