export interface ItemCategory {
  id: string;
  name: string;
  description: string | null;
  active: boolean;
  createdAt: number;
  updatedAt: number;
}
export interface UnitOfMeasure {
  id: string;
  name: string;
  abbreviation: string | null;
  description: string | null;
  active: boolean;
  createdAt: number;
  updatedAt: number;
}
export interface InventoryItem {
  id: string;
  code: string;
  name: string;
  itemType: string;
  unitCode: string;
  categoryId: string | null;
  categoryName: string | null;
  uomId: string | null;
  uomName: string | null;
  active: boolean;
  currentBalance: number;
  version: number;
}
export interface StockMovement {
  id: string;
  itemId: string;
  itemCode: string;
  itemName: string;
  partyId: string | null;
  partyName: string | null;
  operationType: string;
  documentType: string | null;
  quantityDelta: number;
  lossPercentage: number | null;
  referenceCode: string | null;
  note: string | null;
  reason: string | null;
  occurredAt: number;
  createdBy: string;
}
export interface PartyBalance {
  partyId: string;
  partyCode: string;
  partyName: string;
  partyType: string;
  balance: number;
}
export interface LedgerEntry {
  id: string;
  partyId: string;
  partyName: string;
  entryType: string;
  amountDelta: number;
  referenceCode: string | null;
  note: string | null;
  occurredAt: number;
  createdBy: string;
}
export interface EmployeeAdvance {
  id: string;
  employeeId: string;
  employeeCode: string;
  employeeName: string;
  amountDelta: number;
  currentBalance: number;
  entryType: string;
  note: string | null;
  occurredAt: number;
  createdBy: string;
}
export interface OperationsSnapshot {
  items: InventoryItem[];
  movements: StockMovement[];
  partyBalances: PartyBalance[];
  ledgerEntries: LedgerEntry[];
  employeeAdvances: EmployeeAdvance[];
}
export interface PartyOption {
  id: string;
  code: string;
  name: string;
  partyType: string;
}
export interface EmployeeOption {
  id: string;
  employeeCode: string;
  fullName: string;
}
export interface NegativeBalance {
  itemId: string;
  itemCode: string;
  itemName: string;
  currentBalance: number;
}
