export interface SerializedDevice {
  id: string;
  serialNumber: string;
  brand: string;
  model: string;
  cpu: string;
  ramGb: number;
  storageGb: number;
  storageType: string;
  gpu?: string | null;
  conditionGrade: string;
  purchasePrice: number;
  sellingPrice: number;
  margin: number;
  status: string;
  customerName?: string | null;
  saleDate?: string | null;
  warrantyEndDate?: string | null;
  isWarrantyActive: boolean;
}

export interface RegisterDeviceRequest {
  serialNumber: string;
  brand: string;
  model: string;
  cpu: string;
  ramGb: number;
  storageGb: number;
  storageType: string;
  purchasePrice: number;
  sellingPrice: number;
  conditionGrade?: string;
  supplierId?: string;
  gpu?: string;
  screenSizeInch?: number;
}

export interface SellDeviceRequest {
  customerId: string;
  customerName: string;
  warrantyMonths: number;
  finalSellingPrice?: number;
}

export interface RepairTicket {
  id: string;
  ticketNumber: string;
  serialNumber: string;
  customerName: string;
  customerPhone: string;
  reportedIssue: string;
  diagnosis?: string | null;
  technicianNotes?: string | null;
  costAmount: number;
  chargedAmount: number;
  status: string;
  isUnderWarranty: boolean;
  createdAt: string;
}

export interface CreateRepairTicketRequest {
  serialNumber: string;
  customerName: string;
  customerPhone: string;
  reportedIssue: string;
}

export interface UpdateRepairStatusRequest {
  status?: string;
  diagnosis?: string;
  technicianNotes?: string;
  costAmount?: number;
  chargedAmount?: number;
}
