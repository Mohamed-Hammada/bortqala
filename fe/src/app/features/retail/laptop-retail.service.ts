import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreateRepairTicketRequest,
  RegisterDeviceRequest,
  RepairTicket,
  SellDeviceRequest,
  SerializedDevice,
  UpdateRepairStatusRequest,
} from './laptop-retail.models';

@Injectable({ providedIn: 'root' })
export class LaptopRetailService {
  private readonly http = inject(HttpClient);

  listDevices(params?: { status?: string; brand?: string }): Observable<SerializedDevice[]> {
    let httpParams = new HttpParams();
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.brand) httpParams = httpParams.set('brand', params.brand);

    return this.http.get<SerializedDevice[]>('/api/v1/retail/laptops/devices', { params: httpParams });
  }

  getDeviceBySerial(serialNumber: string): Observable<SerializedDevice> {
    return this.http.get<SerializedDevice>(`/api/v1/retail/laptops/devices/${serialNumber}`);
  }

  registerDevice(request: RegisterDeviceRequest): Observable<SerializedDevice> {
    return this.http.post<SerializedDevice>('/api/v1/retail/laptops/devices', request);
  }

  sellDevice(id: string, request: SellDeviceRequest): Observable<SerializedDevice> {
    return this.http.post<SerializedDevice>(`/api/v1/retail/laptops/devices/${id}/sell`, request);
  }

  returnDevice(id: string, reason?: string): Observable<SerializedDevice> {
    return this.http.post<SerializedDevice>(`/api/v1/retail/laptops/devices/${id}/return`, { reason });
  }

  createRepairTicket(request: CreateRepairTicketRequest): Observable<RepairTicket> {
    return this.http.post<RepairTicket>('/api/v1/retail/laptops/repairs', request);
  }

  listRepairTickets(params?: { status?: string; serialNumber?: string }): Observable<RepairTicket[]> {
    let httpParams = new HttpParams();
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.serialNumber) httpParams = httpParams.set('serialNumber', params.serialNumber);

    return this.http.get<RepairTicket[]>('/api/v1/retail/laptops/repairs', { params: httpParams });
  }

  updateRepairStatus(id: string, request: UpdateRepairStatusRequest): Observable<RepairTicket> {
    return this.http.put<RepairTicket>(`/api/v1/retail/laptops/repairs/${id}/status`, request);
  }
}
