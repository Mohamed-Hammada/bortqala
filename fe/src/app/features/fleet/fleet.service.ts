import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import {
  VehicleDto,
  FuelLogDto,
  MaintenanceScheduleDto,
  MaintenanceRecordDto,
  VehicleDocumentDto,
  FleetCostSummaryDto,
  VehicleStatus,
} from './fleet.models';

@Injectable({
  providedIn: 'root',
})
export class FleetService {
  private readonly http = inject(HttpClient);

  readonly vehicles = signal<VehicleDto[]>([]);
  readonly fuelLogs = signal<FuelLogDto[]>([]);
  readonly schedules = signal<MaintenanceScheduleDto[]>([]);
  readonly records = signal<MaintenanceRecordDto[]>([]);
  readonly documents = signal<VehicleDocumentDto[]>([]);
  readonly costSummary = signal<FleetCostSummaryDto | null>(null);
  readonly loading = signal(false);

  // --- Vehicles ---

  async loadVehicles(): Promise<VehicleDto[]> {
    this.loading.set(true);
    try {
      const items = await firstValueFrom(this.http.get<VehicleDto[]>('/api/v1/fleet/vehicles'));
      this.vehicles.set(items);
      return items;
    } finally {
      this.loading.set(false);
    }
  }

  async createVehicle(payload: Partial<VehicleDto>): Promise<VehicleDto> {
    const created = await firstValueFrom(this.http.post<VehicleDto>('/api/v1/fleet/vehicles', payload));
    this.vehicles.update((list) => [created, ...list]);
    return created;
  }

  async updateVehicleStatus(id: string, status: VehicleStatus): Promise<VehicleDto> {
    const updated = await firstValueFrom(this.http.put<VehicleDto>(`/api/v1/fleet/vehicles/${id}/status?status=${status}`, {}));
    this.vehicles.update((list) => list.map((v) => (v.id === id ? updated : v)));
    return updated;
  }

  // --- Fuel Logs ---

  async loadFuelLogs(vehicleId?: string): Promise<FuelLogDto[]> {
    const url = vehicleId ? `/api/v1/fleet/fuel-logs?vehicleId=${vehicleId}` : '/api/v1/fleet/fuel-logs';
    const logs = await firstValueFrom(this.http.get<FuelLogDto[]>(url));
    this.fuelLogs.set(logs);
    return logs;
  }

  async logFuel(payload: Partial<FuelLogDto>): Promise<FuelLogDto> {
    const created = await firstValueFrom(this.http.post<FuelLogDto>('/api/v1/fleet/fuel-logs', payload));
    this.fuelLogs.update((list) => [created, ...list]);
    return created;
  }

  // --- Maintenance Schedules ---

  async loadSchedules(vehicleId?: string): Promise<MaintenanceScheduleDto[]> {
    const url = vehicleId ? `/api/v1/fleet/schedules?vehicleId=${vehicleId}` : '/api/v1/fleet/schedules';
    const list = await firstValueFrom(this.http.get<MaintenanceScheduleDto[]>(url));
    this.schedules.set(list);
    return list;
  }

  async createSchedule(payload: Partial<MaintenanceScheduleDto>): Promise<MaintenanceScheduleDto> {
    const created = await firstValueFrom(this.http.post<MaintenanceScheduleDto>('/api/v1/fleet/schedules', payload));
    this.schedules.update((list) => [created, ...list]);
    return created;
  }

  // --- Maintenance Records ---

  async loadRecords(vehicleId?: string): Promise<MaintenanceRecordDto[]> {
    const url = vehicleId ? `/api/v1/fleet/records?vehicleId=${vehicleId}` : '/api/v1/fleet/records';
    const list = await firstValueFrom(this.http.get<MaintenanceRecordDto[]>(url));
    this.records.set(list);
    return list;
  }

  async recordMaintenance(payload: Partial<MaintenanceRecordDto>): Promise<MaintenanceRecordDto> {
    const created = await firstValueFrom(this.http.post<MaintenanceRecordDto>('/api/v1/fleet/records', payload));
    this.records.update((list) => [created, ...list]);
    return created;
  }

  // --- Vehicle Documents ---

  async loadDocuments(vehicleId?: string): Promise<VehicleDocumentDto[]> {
    const url = vehicleId ? `/api/v1/fleet/documents?vehicleId=${vehicleId}` : '/api/v1/fleet/documents';
    const list = await firstValueFrom(this.http.get<VehicleDocumentDto[]>(url));
    this.documents.set(list);
    return list;
  }

  async addDocument(payload: Partial<VehicleDocumentDto>): Promise<VehicleDocumentDto> {
    const created = await firstValueFrom(this.http.post<VehicleDocumentDto>('/api/v1/fleet/documents', payload));
    this.documents.update((list) => [created, ...list]);
    return created;
  }

  // --- Cost Summary ---

  async loadCostSummary(): Promise<FleetCostSummaryDto> {
    const summary = await firstValueFrom(this.http.get<FleetCostSummaryDto>('/api/v1/fleet/cost-summary'));
    this.costSummary.set(summary);
    return summary;
  }
}
