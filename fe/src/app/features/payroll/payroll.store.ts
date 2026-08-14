import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { downloadBlob } from '../../core/download';
import { I18nService } from '../../core/i18n.service';
import { BulkPaymentRequest, PaymentRequest, PayrollGlPosting, ReversePaymentRequest, SheetResponse, SalaryPaymentExplanation, StatusTransitionRequest } from './payroll.models';

@Injectable()
export class PayrollStore {
  private readonly httpClient = inject(HttpClient);
  private readonly i18n = inject(I18nService);

  async getExplanation(paymentId: string): Promise<SalaryPaymentExplanation[]> {
    try {
      return await firstValueFrom(
        this.httpClient.get<SalaryPaymentExplanation[]>(`/api/v1/payroll/payments/${paymentId}/explanation`),
      );
    } catch {
      return [];
    }
  }

  readonly data = signal<SheetResponse | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly glPosting = signal<PayrollGlPosting | null>(null);

  async load(year: number, month: number, categoryId?: string): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const params: Record<string, string | number> = { year, month };
      if (categoryId) params['categoryId'] = categoryId;
      const sheet = await firstValueFrom(
        this.httpClient.get<SheetResponse>('/api/v1/payroll', { params }),
      );
      this.data.set(sheet);
      if (sheet.periodStatus === 'POSTED' || sheet.periodStatus === 'PAID') {
        await this.loadGlPosting(year, month);
      } else {
        this.glPosting.set(null);
      }
    } catch (err) {
      this.error.set(apiErrorMessage(err, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async loadGlPosting(year: number, month: number): Promise<void> {
    try {
      this.glPosting.set(await firstValueFrom(
        this.httpClient.get<PayrollGlPosting>(`/api/v1/payroll/gl-posting/${year}/${month}`),
      ));
    } catch {
      this.glPosting.set(null);
    }
  }

  async recordPayment(payload: PaymentRequest): Promise<boolean> {
    this.saving.set(true);
    this.error.set(null);
    try {
      this.data.set(
        await firstValueFrom(
          this.httpClient.post<SheetResponse>('/api/v1/payroll/pay', payload),
        ),
      );
      return true;
    } catch (err) {
      this.error.set(apiErrorMessage(err, this.i18n));
      return false;
    } finally {
      this.saving.set(false);
    }
  }

  async payBulk(payload: BulkPaymentRequest): Promise<boolean> {
    this.saving.set(true);
    this.error.set(null);
    try {
      this.data.set(
        await firstValueFrom(
          this.httpClient.post<SheetResponse>('/api/v1/payroll/pay-bulk', payload),
        ),
      );
      return true;
    } catch (err) {
      this.error.set(apiErrorMessage(err, this.i18n));
      return false;
    } finally {
      this.saving.set(false);
    }
  }

  async transitionStatus(payload: StatusTransitionRequest): Promise<boolean> {
    this.saving.set(true);
    this.error.set(null);
    try {
      this.data.set(
        await firstValueFrom(
          this.httpClient.post<SheetResponse>('/api/v1/payroll/transition', payload),
        ),
      );
      return true;
    } catch (err) {
      this.error.set(apiErrorMessage(err, this.i18n));
      return false;
    } finally {
      this.saving.set(false);
    }
  }

  async reversePayment(payload: ReversePaymentRequest): Promise<boolean> {
    this.saving.set(true);
    this.error.set(null);
    try {
      this.data.set(
        await firstValueFrom(
          this.httpClient.post<SheetResponse>('/api/v1/payroll/reverse', payload),
        ),
      );
      return true;
    } catch (err) {
      this.error.set(apiErrorMessage(err, this.i18n));
      return false;
    } finally {
      this.saving.set(false);
    }
  }

  async exportExcel(year: number, month: number, categoryId?: string): Promise<void> {
    const params: Record<string, string | number> = { year, month };
    if (categoryId) params['categoryId'] = categoryId;
    const blob = await firstValueFrom(
      this.httpClient.get('/api/v1/payroll/export', { params, responseType: 'blob' }),
    );
    downloadBlob(blob, `payroll-${year}-${String(month).padStart(2, '0')}.xlsx`);
  }
}
