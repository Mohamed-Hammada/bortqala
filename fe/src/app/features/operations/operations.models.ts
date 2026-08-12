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
  reorderPoint: number;
  reorderQuantity: number;
  currentBalance: number;
  version: number;
}
export interface ReorderAlert {
  itemId: string; itemCode: string; itemName: string; currentBalance: number;
  reorderPoint: number; reorderQuantity: number; shortage: number;
}
export interface CycleCount {
  id: string; countNumber: string; warehouseId: string; countDate: number; status: string; itemId: string;
  systemQuantity: number; countedQuantity: number; variance: number;
}
export interface WarehouseOption { id: string; code: string; name: string; }
export interface StockTransferLine {
  id: string; itemId: string; itemCode: string; itemName: string; quantity: number;
}
export interface StockTransfer {
  id: string; transferNumber: string; sourceWarehouseId: string; sourceWarehouseName: string | null;
  targetWarehouseId: string; targetWarehouseName: string | null; transferDate: number;
  status: 'DRAFT' | 'SHIPPED' | 'RECEIVED' | 'CANCELLED'; version: number; lines: StockTransferLine[];
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
  purchaseOrderNo: string | null;
  receiptNo: string | null;
  deliveryNoteNo: string | null;
  invoiceNo: string | null;
  voucherNo: string | null;
  externalRef: string | null;
  warehouse: string | null;
  attachmentName: string | null;
  attachmentContentType: string | null;
  attachmentSize: number | null;
  occurredAt: number;
  createdBy: string;
  createdAt?: number;
}
export interface TransactionPayload {
  itemId: string | null;
  partyId: string | null;
  operationType: string;
  quantityDelta: number;
  amountDelta: number;
  lossPercentage: number | null;
  referenceCode: string | null;
  note: string | null;
  reason: string | null;
  documentType: string | null;
  purchaseOrderNo: string | null;
  receiptNo: string | null;
  deliveryNoteNo: string | null;
  invoiceNo: string | null;
  voucherNo: string | null;
  externalRef: string | null;
  warehouse: string | null;
  attachmentName: string | null;
  attachmentContentType: string | null;
  attachmentSize: number | null;
  occurredAt: number;
  unitCost: number | null;
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
export interface UnitConversion {
  id: string;
  fromUomId: string;
  fromUomName: string;
  toUomId: string;
  toUomName: string;
  factor: number;
  createdAt: number;
}

export type ValuationMethod = 'FIFO' | 'WEIGHTED_AVERAGE';
export interface ValuationPolicy {
  id: string | null;
  valuationMethod: ValuationMethod;
  inventoryAccountId: string | null;
  receiptOffsetAccountId: string | null;
  cogsAccountId: string | null;
  adjustmentAccountId: string | null;
  glPostingEnabled: boolean;
  allowBackdatedPosting: boolean;
  version: number;
  createdAt: number | null;
  updatedAt: number | null;
}
export interface MovementCost {
  id: string;
  movementId: string;
  itemId: string;
  itemCode: string;
  itemName: string;
  valuationMethod: ValuationMethod;
  quantityEffect: number;
  unitCost: number;
  valueEffect: number;
  journalEntryId: string | null;
  explanation: string;
  occurredAt: number;
  createdAt: number;
}
export interface ItemValuation {
  itemId: string;
  itemCode: string;
  itemName: string;
  quantityOnHand: number;
  valuedQuantity: number;
  inventoryValue: number;
  averageUnitCost: number;
  openingQuantityGap: number;
}
export interface ValuationReport {
  policy: ValuationPolicy;
  totalInventoryValue: number;
  items: ItemValuation[];
  movementCosts: MovementCost[];
}
export interface AccountOption {
  id: string;
  code: string;
  name: string;
  isHeader: boolean;
  active: boolean;
}
