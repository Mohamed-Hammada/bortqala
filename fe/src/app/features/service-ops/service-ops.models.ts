export type RentalItemStatus = 'AVAILABLE' | 'RENTED' | 'MAINTENANCE' | 'RETIRED';
export type RentalContractStatus = 'DRAFT' | 'ACTIVE' | 'CLOSED' | 'CANCELLED';
export type RentalRateUnit = 'DAY' | 'WEEK' | 'MONTH';

export type WorkOrderPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
export type WorkOrderStatus = 'OPEN' | 'IN_PROGRESS' | 'WAITING_PARTS' | 'DONE' | 'DELIVERED' | 'CANCELLED';

export type ResourceKind = 'ROOM' | 'TRAINER' | 'EQUIPMENT' | 'VEHICLE';
export type BookingStatus = 'CONFIRMED' | 'CANCELLED' | 'COMPLETED';

export interface RentalItem {
  id: string;
  code: string;
  name: string;
  nameEn?: string;
  category?: string;
  rateDaily: number;
  rateWeekly: number;
  rateMonthly: number;
  depositAmount: number;
  status: RentalItemStatus;
  createdAt: number;
  updatedAt: number;
}

export interface RentalContractLine {
  id: string;
  rentalItemId: string;
  quantity: number;
  unitRate: number;
  totalAmount: number;
}

export interface RentalContract {
  id: string;
  contractNo: string;
  customerPartyId: string;
  startDate: string;
  expectedEndDate: string;
  actualEndDate?: string;
  rateUnit: RentalRateUnit;
  rateAmount: number;
  depositAmount: number;
  damageFee: number;
  totalAmount: number;
  status: RentalContractStatus;
  invoiceId?: string;
  notes?: string;
  lines: RentalContractLine[];
  createdAt: number;
  updatedAt: number;
}

export interface RentalUtilizationSummary {
  totalItems: number;
  rentedItems: number;
  availableItems: number;
  utilizationPercentage: number;
}

export interface WorkOrderLaborLine {
  id: string;
  description: string;
  hours: number;
  hourlyRate: number;
  totalAmount: number;
}

export interface WorkOrderPartsLine {
  id: string;
  itemCode: string;
  itemName: string;
  quantity: number;
  unitPrice: number;
  totalAmount: number;
}

export interface WorkOrder {
  id: string;
  ticketNo: string;
  customerPartyId?: string;
  customerName?: string;
  title: string;
  description?: string;
  assignedEmployeeId?: string;
  priority: WorkOrderPriority;
  status: WorkOrderStatus;
  promisedAt?: string;
  laborTotal: number;
  partsTotal: number;
  grandTotal: number;
  invoiceId?: string;
  overrideNote?: string;
  laborLines: WorkOrderLaborLine[];
  partsLines: WorkOrderPartsLine[];
  createdAt: number;
  updatedAt: number;
}

export interface BookableResource {
  id: string;
  code: string;
  name: string;
  nameEn?: string;
  kind: ResourceKind;
  capacity?: number;
  location?: string;
  active: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface ResourceBooking {
  id: string;
  resourceId: string;
  title: string;
  customerPartyId?: string;
  customerName?: string;
  startTime: number;
  endTime: number;
  status: BookingStatus;
  notes?: string;
  createdAt: number;
  updatedAt: number;
}
