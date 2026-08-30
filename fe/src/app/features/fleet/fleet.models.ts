export type VehicleType = 'SEDAN' | 'TRUCK' | 'VAN' | 'BUS' | 'HEAVY_EQUIPMENT';
export type VehicleStatus = 'ACTIVE' | 'MAINTENANCE' | 'RETIRED';
export type MaintenanceKind = 'OIL' | 'TIRES' | 'INSPECTION' | 'BRAKES' | 'CUSTOM';
export type DocumentType = 'LICENSE' | 'INSURANCE' | 'INSPECTION_PASS' | 'PERMIT';

export interface VehicleDto {
  id: string;
  plateNumber: string;
  make: string;
  model: string;
  year: number;
  vehicleType: VehicleType;
  assetId?: string;
  assetNetBookValue?: number;
  defaultDriverId?: string;
  defaultDriverName?: string;
  initialOdometer?: number;
  currentOdometer: number;
  status: VehicleStatus;

  notes?: string;
  createdAt: number;
  updatedAt: number;
}

export interface FuelLogDto {
  id: string;
  vehicleId: string;
  logDate: string;
  liters: number;
  odometer: number;
  totalCost: number;
  efficiencyKmPerLiter?: number;
  stationName?: string;
  driverName?: string;
  notes?: string;
  createdAt: number;
}

export interface MaintenanceScheduleDto {
  id: string;
  vehicleId: string;
  title: string;
  maintenanceKind: MaintenanceKind;
  intervalKm?: number;
  intervalDays?: number;
  lastDoneOdometer?: number;
  lastDoneDate?: string;
  isDue: boolean;
  dueReason?: string;
  active: boolean;
  createdAt: number;
}

export interface MaintenanceRecordDto {
  id: string;
  vehicleId: string;
  scheduleId?: string;
  title: string;
  performedDate: string;
  odometer: number;
  cost: number;
  vendorPartyId?: string;
  vendorName?: string;
  description?: string;
  createdAt: number;
}

export interface VehicleDocumentDto {
  id: string;
  vehicleId: string;
  documentType: DocumentType;
  documentNumber: string;
  issueDate?: string;
  expiryDate: string;
  issuer?: string;
  isExpired: boolean;
  isDueSoon: boolean;
  notes?: string;
  createdAt: number;
}

export interface FleetCostSummaryDto {
  totalVehicles: number;
  totalFuelCost: number;
  totalMaintenanceCost: number;
  grandTotalCost: number;
  totalKilometers: number;
  costPerKilometer: number;
}
