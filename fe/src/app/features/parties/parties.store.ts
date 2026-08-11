import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { downloadBlob, timestampedExcelFileName } from '../../core/download';
import { I18nService } from '../../core/i18n.service';
import { BusinessParty, BusinessPartyPayload, Supplier360, SupplierDuplicateResponse } from './parties.models';

@Injectable()
export class PartiesStore {
  private readonly httpClient = inject(HttpClient);
  private readonly i18n = inject(I18nService);
  readonly items = signal<BusinessParty[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly supplier360 = signal<Supplier360 | null>(null);

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.items.set(await firstValueFrom(this.httpClient.get<BusinessParty[]>('/api/v1/parties')));
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async save(id: string | null, payload: BusinessPartyPayload): Promise<boolean> {
    this.loading.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(
        id
          ? this.httpClient.put<BusinessParty>(`/api/v1/parties/${id}`, payload)
          : payload.partyType === 'SUPPLIER'
            ? this.httpClient.post<BusinessParty>('/api/v1/parties/supplier-requests', {
                code: payload.code, name: payload.name, nameEn: payload.nameEn,
                contactPerson: payload.contactPerson, phone: payload.phone, email: payload.email,
                address: payload.address, taxId: payload.taxId, currencyCode: payload.currencyCode,
                paymentTerms: payload.paymentTerms, supplierCategory: payload.supplierCategory,
                riskLevel: payload.riskLevel, ownerUserId: payload.ownerUserId, notes: payload.notes,
              })
            : this.httpClient.post<BusinessParty>('/api/v1/parties', payload),
      );
      await this.load();
      return true;
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
      return false;
    } finally {
      this.loading.set(false);
    }
  }

  async checkDuplicates(taxId?: string | null, iban?: string | null, excludeSupplierId?: string | null): Promise<SupplierDuplicateResponse> {
    return firstValueFrom(this.httpClient.get<SupplierDuplicateResponse>('/api/v1/parties/supplier-duplicates', {
      params: { ...(taxId ? { taxId } : {}), ...(iban ? { iban } : {}), ...(excludeSupplierId ? { excludeSupplierId } : {}) },
    }));
  }

  async loadSupplier360(id: string): Promise<boolean> {
    this.error.set(null);
    try {
      this.supplier360.set(await firstValueFrom(this.httpClient.get<Supplier360>(`/api/v1/parties/${id}/supplier-360`)));
      return true;
    } catch (error) { this.error.set(apiErrorMessage(error, this.i18n)); return false; }
  }

  async addDocument(supplierId: string, metadata: object, file: File): Promise<boolean> {
    const form = new FormData();
    form.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));
    form.append('file', file, file.name);
    return this.mutateSupplier(supplierId, this.httpClient.post(`/api/v1/parties/${supplierId}/documents`, form));
  }

  async verifyDocument(supplierId: string, documentId: string): Promise<boolean> {
    return this.mutateSupplier(supplierId, this.httpClient.post(`/api/v1/parties/${supplierId}/documents/${documentId}/verify`, {}));
  }

  async downloadDocument(supplierId: string, documentId: string, fileName: string): Promise<void> {
    try {
      downloadBlob(await firstValueFrom(this.httpClient.get(`/api/v1/parties/${supplierId}/documents/${documentId}/download`,
        { responseType: 'blob' })), fileName);
    } catch (error) { this.error.set(apiErrorMessage(error, this.i18n)); }
  }

  async addBankAccount(supplierId: string, payload: object): Promise<boolean> {
    return this.mutateSupplier(supplierId, this.httpClient.post(`/api/v1/parties/${supplierId}/bank-accounts`, payload));
  }

  async verifyBankAccount(supplierId: string, accountId: string): Promise<boolean> {
    return this.mutateSupplier(supplierId, this.httpClient.post(`/api/v1/parties/${supplierId}/bank-accounts/${accountId}/verify`, {}));
  }

  async transition(supplierId: string, action: 'submit' | 'approve' | 'activate' | 'suspend' | 'blacklist', reason = ''): Promise<boolean> {
    return this.mutateSupplier(supplierId, this.httpClient.post(`/api/v1/parties/${supplierId}/onboarding/${action}`, { reason }), true);
  }

  private async mutateSupplier(supplierId: string, request: import('rxjs').Observable<unknown>, reloadList = false): Promise<boolean> {
    this.error.set(null);
    try {
      await firstValueFrom(request);
      await this.loadSupplier360(supplierId);
      if (reloadList) await this.load();
      return true;
    } catch (error) { this.error.set(apiErrorMessage(error, this.i18n)); return false; }
  }

  async deactivate(id: string): Promise<void> {
    try {
      await firstValueFrom(this.httpClient.delete<void>(`/api/v1/parties/${id}`));
      await this.load();
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
    }
  }

  async export(): Promise<void> {
    try {
      downloadBlob(
        await firstValueFrom(
          this.httpClient.get('/api/v1/exports/parties.xlsx', { responseType: 'blob' }),
        ),
        timestampedExcelFileName('جهات-التعامل', 'business-parties', this.i18n.locale()),
      );
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
    }
  }
}
