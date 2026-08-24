import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';
import { formatDate } from '../../../core/date';
import { downloadBlob } from '../../../core/download';
import { ModalDialogComponent } from '../../../shared/ui/modal-dialog/modal-dialog.component';
import {
  ASSET_CATEGORIES,
  AssetCategory,
  DepreciationRunResponse,
  FixedAssetPayload,
  FixedAssetResponse,
} from './fixed-assets.models';

@Component({
  selector: 'app-fixed-assets-page',
  imports: [ReactiveFormsModule, ModalDialogComponent],
  templateUrl: './fixed-assets.page.html',
  styleUrl: './fixed-assets.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FixedAssetsPage {
  readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);

  private static readonly BASE = '/api/v1/fixed-assets';

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly drawerOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly disposingAsset = signal<FixedAssetResponse | null>(null);
  readonly runOpen = signal(false);
  readonly runResult = signal<DepreciationRunResponse | null>(null);
  readonly categories = ASSET_CATEGORIES;

  readonly assets = signal<FixedAssetResponse[]>([]);

  readonly assetForm = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(200)] }),
    category: new FormControl<AssetCategory>('VEHICLE', { nonNullable: true, validators: [Validators.required] }),
    acquisitionDate: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    acquisitionCost: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0.01)] }),
    salvageValue: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
    usefulLifeMonths: new FormControl(60, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1), Validators.max(480)],
    }),
  });

  readonly disposeForm = new FormGroup({
    disposalDate: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    proceeds: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
  });

  readonly runForm = new FormGroup({
    yearMonth: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/^\d{4}-\d{2}$/)],
    }),
  });

  readonly openAssetsCount = computed(
    () => this.assets().filter((asset) => asset.status !== 'DISPOSED').length,
  );

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const assets = await firstValueFrom(this.http.get<FixedAssetResponse[]>(FixedAssetsPage.BASE));
      this.assets.set(assets ?? []);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  openNew(): void {
    this.editingId.set(null);
    this.assetForm.reset({
      name: '',
      category: 'VEHICLE',
      acquisitionDate: new Date().toISOString().slice(0, 10),
      acquisitionCost: 0,
      salvageValue: 0,
      usefulLifeMonths: 60,
    });
    this.drawerOpen.set(true);
  }

  openEdit(asset: FixedAssetResponse): void {
    if (asset.status === 'DISPOSED') return;
    this.editingId.set(asset.id);
    this.assetForm.reset({
      name: asset.name,
      category: asset.category,
      acquisitionDate: new Date(asset.acquisitionDate).toISOString().slice(0, 10),
      acquisitionCost: asset.acquisitionCost,
      salvageValue: asset.salvageValue,
      usefulLifeMonths: asset.usefulLifeMonths,
    });
    this.drawerOpen.set(true);
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
  }

  async submitAsset(): Promise<void> {
    if (this.submitting() || this.assetForm.invalid) {
      this.assetForm.markAllAsTouched();
      return;
    }
    const value = this.assetForm.getRawValue();
    const payload: FixedAssetPayload = {
      name: value.name.trim(),
      category: value.category,
      acquisitionDate: this.isoToUtcMs(value.acquisitionDate),
      acquisitionCost: value.acquisitionCost,
      salvageValue: value.salvageValue,
      usefulLifeMonths: value.usefulLifeMonths,
    };
    this.submitting.set(true);
    try {
      const editingId = this.editingId();
      if (editingId) {
        await firstValueFrom(
          this.http.put<FixedAssetResponse>(`${FixedAssetsPage.BASE}/${editingId}`, payload),
        );
      } else {
        await firstValueFrom(this.http.post<FixedAssetResponse>(FixedAssetsPage.BASE, payload));
      }
      this.notification.success(this.i18n.t('asset.saved'));
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.submitting.set(false);
    }
  }

  openDispose(asset: FixedAssetResponse): void {
    if (asset.status === 'DISPOSED') return;
    this.disposingAsset.set(asset);
    this.disposeForm.reset({
      disposalDate: new Date().toISOString().slice(0, 10),
      proceeds: 0,
    });
  }

  closeDispose(): void {
    this.disposingAsset.set(null);
  }

  async submitDispose(): Promise<void> {
    const asset = this.disposingAsset();
    if (!asset || this.submitting() || this.disposeForm.invalid) {
      this.disposeForm.markAllAsTouched();
      return;
    }
    const value = this.disposeForm.getRawValue();
    this.submitting.set(true);
    try {
      await firstValueFrom(this.http.post<FixedAssetResponse>(
        `${FixedAssetsPage.BASE}/${asset.id}/dispose`,
        {
          disposalDate: this.isoToUtcMs(value.disposalDate),
          proceeds: value.proceeds,
        },
      ));
      this.notification.success(this.i18n.t('asset.disposed'));
      this.closeDispose();
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.submitting.set(false);
    }
  }

  openRun(): void {
    const now = new Date();
    const lastMonth = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth() - 1, 1));
    this.runForm.reset({ yearMonth: this.toYearMonth(lastMonth) });
    this.runResult.set(null);
    this.runOpen.set(true);
  }

  closeRun(): void {
    this.runOpen.set(false);
  }

  async submitRun(): Promise<void> {
    if (this.submitting() || this.runForm.invalid) {
      this.runForm.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    try {
      const yearMonth = this.runForm.getRawValue().yearMonth;
      const result = await firstValueFrom(this.http.post<DepreciationRunResponse>(
        `${FixedAssetsPage.BASE}/run-depreciation?yearMonth=${yearMonth}`,
        {},
      ));
      this.runResult.set(result);
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.submitting.set(false);
    }
  }

  outcomeLabel(outcome: string): string {
    const known = ['POSTED', 'ALREADY_POSTED', 'SKIPPED_MISSING_ACCOUNT', 'DEPRECIATION_PERIOD_LOCKED'];
    return known.includes(outcome)
      ? this.i18n.t(`outcome.${outcome}`)
      : outcome;
  }

  statusLabel(status: string): string {
    switch (status) {
      case 'ACTIVE': return this.i18n.t('status.active');
      case 'FULLY_DEPRECIATED': return this.i18n.t('status.fullyDepreciated');
      default: return this.i18n.t('status.disposed');
    }
  }

  statusClass(status: string): string {
    return status === 'ACTIVE' ? 'success' : status === 'DISPOSED' ? '' : 'warn';
  }

  categoryLabel(category: string): string {
    return this.i18n.t(`category.${category.toLowerCase()}`);
  }

  async exportExcel(): Promise<void> {
    try {
      const blob = await firstValueFrom(
        this.http.get('/api/v1/exports/fixed-assets.xlsx', { responseType: 'blob' }),
      );
      downloadBlob(blob, `fixed-assets-${new Date().toISOString().slice(0, 10)}.xlsx`);
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  date(ms: number | null): string {
    return ms ? formatDate(ms) : '—';
  }

  formatAmount(value: number): string {
    return new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value);
  }

  private isoToUtcMs(value: string): number {
    const parts = value.trim().split('-').map(Number);
    const [year, month, day] = parts;
    if (!year || !month || !day || Number.isNaN(year) || Number.isNaN(month) || Number.isNaN(day)) return 0;
    return Date.UTC(year, month - 1, day);
  }

  private toYearMonth(date: Date): string {
    return `${date.getUTCFullYear()}-${String(date.getUTCMonth() + 1).padStart(2, '0')}`;
  }
}
