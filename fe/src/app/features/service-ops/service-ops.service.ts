import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { I18nService } from '../../core/i18n.service';
import {
  BookableResource,
  RentalContract,
  RentalItem,
  RentalUtilizationSummary,
  ResourceBooking,
  WorkOrder,
  WorkOrderStatus,
} from './service-ops.models';

@Injectable({ providedIn: 'root' })
export class ServiceOpsService {
  private readonly http = inject(HttpClient);
  private readonly i18n = inject(I18nService);

  readonly rentalItems = signal<RentalItem[]>([]);
  readonly rentalContracts = signal<RentalContract[]>([]);
  readonly utilization = signal<RentalUtilizationSummary | null>(null);

  readonly workOrders = signal<WorkOrder[]>([]);
  readonly resources = signal<BookableResource[]>([]);
  readonly bookings = signal<ResourceBooking[]>([]);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  // --- Rentals ---

  async loadRentals(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const [items, contracts, util] = await Promise.all([
        firstValueFrom(this.http.get<RentalItem[]>('/api/v1/service-ops/rentals/items')),
        firstValueFrom(this.http.get<RentalContract[]>('/api/v1/service-ops/rentals/contracts')),
        firstValueFrom(this.http.get<RentalUtilizationSummary>('/api/v1/service-ops/rentals/utilization')),
      ]);
      this.rentalItems.set(items);
      this.rentalContracts.set(contracts);
      this.utilization.set(util);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async createRentalItem(payload: Partial<RentalItem>): Promise<RentalItem | null> {
    try {
      const created = await firstValueFrom(
        this.http.post<RentalItem>('/api/v1/service-ops/rentals/items', payload)
      );
      await this.loadRentals();
      return created;
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
      return null;
    }
  }

  async createRentalContract(payload: any): Promise<RentalContract | null> {
    try {
      const created = await firstValueFrom(
        this.http.post<RentalContract>('/api/v1/service-ops/rentals/contracts', payload)
      );
      await this.loadRentals();
      return created;
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
      return null;
    }
  }

  async activateContract(id: string): Promise<boolean> {
    try {
      await firstValueFrom(
        this.http.post<RentalContract>(`/api/v1/service-ops/rentals/contracts/${id}/activate`, {})
      );
      await this.loadRentals();
      return true;
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
      return false;
    }
  }

  async returnAndCloseContract(id: string, payload: any): Promise<boolean> {
    try {
      await firstValueFrom(
        this.http.post<RentalContract>(`/api/v1/service-ops/rentals/contracts/${id}/close`, payload)
      );
      await this.loadRentals();
      return true;
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
      return false;
    }
  }

  // --- Work Orders ---

  async loadWorkOrders(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const orders = await firstValueFrom(
        this.http.get<WorkOrder[]>('/api/v1/service-ops/work-orders')
      );
      this.workOrders.set(orders);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async createWorkOrder(payload: any): Promise<WorkOrder | null> {
    try {
      const created = await firstValueFrom(
        this.http.post<WorkOrder>('/api/v1/service-ops/work-orders', payload)
      );
      await this.loadWorkOrders();
      return created;
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
      return null;
    }
  }

  async addLaborLine(id: string, payload: any): Promise<boolean> {
    try {
      await firstValueFrom(
        this.http.post<WorkOrder>(`/api/v1/service-ops/work-orders/${id}/labor`, payload)
      );
      await this.loadWorkOrders();
      return true;
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
      return false;
    }
  }

  async addPartsLine(id: string, payload: any): Promise<boolean> {
    try {
      await firstValueFrom(
        this.http.post<WorkOrder>(`/api/v1/service-ops/work-orders/${id}/parts`, payload)
      );
      await this.loadWorkOrders();
      return true;
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
      return false;
    }
  }

  async updateWorkOrderStatus(id: string, status: WorkOrderStatus, overrideNote?: string): Promise<boolean> {
    try {
      await firstValueFrom(
        this.http.post<WorkOrder>(`/api/v1/service-ops/work-orders/${id}/status`, { status, overrideNote })
      );
      await this.loadWorkOrders();
      return true;
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
      return false;
    }
  }

  async deliverWorkOrder(id: string): Promise<boolean> {
    try {
      await firstValueFrom(
        this.http.post<WorkOrder>(`/api/v1/service-ops/work-orders/${id}/deliver`, {})
      );
      await this.loadWorkOrders();
      return true;
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
      return false;
    }
  }

  // --- Bookings ---

  async loadBookings(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const [res, books] = await Promise.all([
        firstValueFrom(this.http.get<BookableResource[]>('/api/v1/service-ops/bookings/resources')),
        firstValueFrom(this.http.get<ResourceBooking[]>('/api/v1/service-ops/bookings')),
      ]);
      this.resources.set(res);
      this.bookings.set(books);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async createResource(payload: any): Promise<BookableResource | null> {
    try {
      const created = await firstValueFrom(
        this.http.post<BookableResource>('/api/v1/service-ops/bookings/resources', payload)
      );
      await this.loadBookings();
      return created;
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
      return null;
    }
  }

  async createBooking(payload: any): Promise<ResourceBooking | null> {
    try {
      const created = await firstValueFrom(
        this.http.post<ResourceBooking>('/api/v1/service-ops/bookings', payload)
      );
      await this.loadBookings();
      return created;
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
      return null;
    }
  }

  async cancelBooking(id: string): Promise<boolean> {
    try {
      await firstValueFrom(
        this.http.post<ResourceBooking>(`/api/v1/service-ops/bookings/${id}/cancel`, {})
      );
      await this.loadBookings();
      return true;
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
      return false;
    }
  }
}
