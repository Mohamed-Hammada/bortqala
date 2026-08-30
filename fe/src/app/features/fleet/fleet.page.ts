import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FleetService } from './fleet.service';
import { I18nService } from '../../core/i18n.service';
import {
  VehicleDto,
  FuelLogDto,
  MaintenanceScheduleDto,
  MaintenanceRecordDto,
  VehicleDocumentDto,
  VehicleStatus,
  VehicleType,
  MaintenanceKind,
  DocumentType,
} from './fleet.models';

@Component({
  selector: 'app-fleet-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './fleet.page.html',
  styleUrls: ['./fleet.page.scss'],
})
export class FleetPageComponent implements OnInit {
  readonly fleet = inject(FleetService);
  readonly i18n = inject(I18nService);

  readonly activeTab = signal<'vehicles' | 'fuel' | 'schedules' | 'docs' | 'costs'>('vehicles');

  // Modals
  readonly showVehicleModal = signal(false);
  readonly showFuelModal = signal(false);
  readonly showScheduleModal = signal(false);
  readonly showRecordModal = signal(false);
  readonly showDocModal = signal(false);

  // Forms
  readonly vehicleForm = signal<{
    plateNumber: string;
    make: string;
    model: string;
    year: number;
    vehicleType: VehicleType;
    assetId: string;
    defaultDriverName: string;
    initialOdometer: number;
    notes: string;
  }>({
    plateNumber: '',
    make: '',
    model: '',
    year: 2024,
    vehicleType: 'SEDAN',
    assetId: '',
    defaultDriverName: '',
    initialOdometer: 0,
    notes: '',
  });

  readonly fuelForm = signal<{
    vehicleId: string;
    logDate: string;
    liters: number;
    odometer: number;
    totalCost: number;
    stationName: string;
    driverName: string;
    notes: string;
  }>({
    vehicleId: '',
    logDate: '',
    liters: 0,
    odometer: 0,
    totalCost: 0,
    stationName: '',
    driverName: '',
    notes: '',
  });

  readonly scheduleForm = signal<{
    vehicleId: string;
    title: string;
    maintenanceKind: MaintenanceKind;
    intervalKm: number;
    intervalDays: number;
    lastDoneOdometer: number;
    lastDoneDate: string;
  }>({
    vehicleId: '',
    title: '',
    maintenanceKind: 'OIL',
    intervalKm: 5000,
    intervalDays: 90,
    lastDoneOdometer: 0,
    lastDoneDate: '',
  });

  readonly recordForm = signal<{
    vehicleId: string;
    scheduleId: string;
    title: string;
    performedDate: string;
    odometer: number;
    cost: number;
    vendorName: string;
    description: string;
  }>({
    vehicleId: '',
    scheduleId: '',
    title: '',
    performedDate: '',
    odometer: 0,
    cost: 0,
    vendorName: '',
    description: '',
  });

  readonly docForm = signal<{
    vehicleId: string;
    documentType: DocumentType;
    documentNumber: string;
    issueDate: string;
    expiryDate: string;
    issuer: string;
    notes: string;
  }>({
    vehicleId: '',
    documentType: 'LICENSE',
    documentNumber: '',
    issueDate: '',
    expiryDate: '',
    issuer: '',
    notes: '',
  });

  async ngOnInit(): Promise<void> {
    await this.loadData();
  }

  async setTab(tab: 'vehicles' | 'fuel' | 'schedules' | 'docs' | 'costs'): Promise<void> {
    this.activeTab.set(tab);
    await this.loadData();
  }

  async loadData(): Promise<void> {
    const tab = this.activeTab();
    if (tab === 'vehicles') {
      await this.fleet.loadVehicles();
    } else if (tab === 'fuel') {
      await Promise.all([this.fleet.loadVehicles(), this.fleet.loadFuelLogs()]);
    } else if (tab === 'schedules') {
      await Promise.all([this.fleet.loadVehicles(), this.fleet.loadSchedules(), this.fleet.loadRecords()]);
    } else if (tab === 'docs') {
      await Promise.all([this.fleet.loadVehicles(), this.fleet.loadDocuments()]);
    } else if (tab === 'costs') {
      await this.fleet.loadCostSummary();
    }
  }


  // --- Vehicle Actions ---

  openCreateVehicle(): void {
    this.vehicleForm.set({
      plateNumber: '',
      make: '',
      model: '',
      year: 2024,
      vehicleType: 'SEDAN',
      assetId: '',
      defaultDriverName: '',
      initialOdometer: 0,
      notes: '',
    });
    this.showVehicleModal.set(true);
  }

  async saveVehicle(): Promise<void> {
    const form = this.vehicleForm();
    await this.fleet.createVehicle({
      plateNumber: form.plateNumber,
      make: form.make,
      model: form.model,
      year: form.year,
      vehicleType: form.vehicleType,
      assetId: form.assetId ? form.assetId : undefined,
      defaultDriverName: form.defaultDriverName ? form.defaultDriverName : undefined,
      initialOdometer: form.initialOdometer,
      notes: form.notes ? form.notes : undefined,
    });
    this.showVehicleModal.set(false);
  }

  async toggleVehicleStatus(vehicle: VehicleDto): Promise<void> {
    const nextStatus: VehicleStatus = vehicle.status === 'ACTIVE' ? 'MAINTENANCE' : 'ACTIVE';
    await this.fleet.updateVehicleStatus(vehicle.id, nextStatus);
  }

  // --- Fuel Log Actions ---

  openFuelModal(vehicle?: VehicleDto): void {
    this.fuelForm.set({
      vehicleId: vehicle ? vehicle.id : (this.fleet.vehicles()[0]?.id ?? ''),
      logDate: new Date().toISOString().substring(0, 10),
      liters: 40,
      odometer: vehicle ? vehicle.currentOdometer + 400 : 1000,
      totalCost: 500,
      stationName: '',
      driverName: vehicle?.defaultDriverName ?? '',
      notes: '',
    });
    this.showFuelModal.set(true);
  }

  async saveFuelLog(): Promise<void> {
    const form = this.fuelForm();
    await this.fleet.logFuel(form);
    this.showFuelModal.set(false);
    await this.fleet.loadVehicles();
  }

  // --- Schedule Actions ---

  openScheduleModal(): void {
    this.scheduleForm.set({
      vehicleId: this.fleet.vehicles()[0]?.id ?? '',
      title: '',
      maintenanceKind: 'OIL',
      intervalKm: 5000,
      intervalDays: 90,
      lastDoneOdometer: 0,
      lastDoneDate: new Date().toISOString().substring(0, 10),
    });
    this.showScheduleModal.set(true);
  }

  async saveSchedule(): Promise<void> {
    const form = this.scheduleForm();
    await this.fleet.createSchedule(form);
    this.showScheduleModal.set(false);
  }

  // --- Maintenance Record Actions ---

  openRecordModal(schedule?: MaintenanceScheduleDto): void {
    const vehicleId = schedule ? schedule.vehicleId : (this.fleet.vehicles()[0]?.id ?? '');
    const vehicle = this.fleet.vehicles().find((v) => v.id === vehicleId);

    this.recordForm.set({
      vehicleId,
      scheduleId: schedule ? schedule.id : '',
      title: schedule ? schedule.title : '',
      performedDate: new Date().toISOString().substring(0, 10),
      odometer: vehicle ? vehicle.currentOdometer : 0,
      cost: 300,
      vendorName: '',
      description: '',
    });
    this.showRecordModal.set(true);
  }

  async saveRecord(): Promise<void> {
    const form = this.recordForm();
    await this.fleet.recordMaintenance({
      vehicleId: form.vehicleId,
      scheduleId: form.scheduleId ? form.scheduleId : undefined,
      title: form.title,
      performedDate: form.performedDate,
      odometer: form.odometer,
      cost: form.cost,
      vendorName: form.vendorName ? form.vendorName : undefined,
      description: form.description ? form.description : undefined,
    });
    this.showRecordModal.set(false);
    await this.fleet.loadSchedules();
    await this.fleet.loadVehicles();
  }

  // --- Document Actions ---

  openDocModal(): void {
    this.docForm.set({
      vehicleId: this.fleet.vehicles()[0]?.id ?? '',
      documentType: 'LICENSE',
      documentNumber: '',
      issueDate: new Date().toISOString().substring(0, 10),
      expiryDate: new Date(Date.now() + 365 * 86400000).toISOString().substring(0, 10),
      issuer: '',
      notes: '',
    });
    this.showDocModal.set(true);
  }

  async saveDoc(): Promise<void> {
    const form = this.docForm();
    await this.fleet.addDocument(form);
    this.showDocModal.set(false);
  }

  getVehiclePlate(id: string): string {
    const v = this.fleet.vehicles().find((veh) => veh.id === id);
    return v ? `${v.plateNumber} (${v.make} ${v.model})` : id;
  }
}
