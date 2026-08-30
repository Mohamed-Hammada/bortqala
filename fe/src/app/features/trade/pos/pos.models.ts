export type PosTerminalStatus = 'ACTIVE' | 'INACTIVE' | 'LOCKED' | 'MAINTENANCE';
export type PosSessionStatus = 'OPEN' | 'CLOSING' | 'CLOSED';
export type PosPaymentMethod = 'CASH' | 'CARD' | 'WALLET' | 'SPLIT' | 'CREDIT';
export type PosTransactionType = 'SALE' | 'RETURN' | 'EXCHANGE';
export type PosTransactionStatus = 'COMPLETED' | 'VOIDED' | 'REFUNDED';

export interface PosTerminal {
  id: string;
  terminalCode: string;
  terminalName: string;
  branchId: string | null;
  warehouseId: string | null;
  cashboxId: string | null;
  status: PosTerminalStatus;
  createdAt: number;
  updatedAt: number;
}

export interface SaveTerminalPayload {
  terminalCode: string;
  terminalName: string;
  branchId?: string | null;
  warehouseId?: string | null;
  cashboxId?: string | null;
  status?: PosTerminalStatus;
}

export interface PosSession {
  id: string;
  sessionNumber: string;
  terminalId: string;
  cashierUserId: string;
  openedAt: number;
  closedAt: number | null;
  openingFloat: number;
  closingActualCash: number | null;
  closingCalculatedCash: number | null;
  closingActualCard: number | null;
  closingCalculatedCard: number | null;
  cashVariance: number | null;
  cardVariance: number | null;
  status: PosSessionStatus;
  notes: string | null;
  createdAt: number;
  updatedAt: number;
}

export interface OpenSessionPayload {
  terminalId: string;
  openingFloat: number;
}

export interface CloseSessionPayload {
  closingActualCash: number;
  closingActualCard: number;
  notes?: string;
}

export interface PosLineItem {
  itemId: string;
  itemCode: string;
  itemName: string;
  quantity: number;
  unitPrice: number;
  discountRate: number;
  discountAmount: number;
  taxAmount: number;
  lineTotal: number;
  notes?: string | null;
}

export interface ProcessSalePayload {
  sessionId: string;
  customerId?: string | null;
  paymentMethod: PosPaymentMethod;
  cashTendered?: number | null;
  clientOfflineId?: string | null;
  lines: PosLineItem[];
}

export interface ProcessReturnPayload {
  originalTransactionId: string;
  sessionId: string;
  reason: string;
  returnLines?: PosLineItem[];
}

export interface PosTransaction {
  id: string;
  transactionNumber: string;
  sessionId: string;
  terminalId: string;
  cashierUserId: string;
  customerId: string | null;
  transactionType: PosTransactionType;
  paymentMethod: PosPaymentMethod;
  subtotal: number;
  discountAmount: number;
  taxAmount: number;
  totalAmount: number;
  cashTendered: number | null;
  changeAmount: number | null;
  status: PosTransactionStatus;
  originalTransactionId: string | null;
  clientOfflineId: string | null;
  createdAt: number;
  lines: PosLineItem[];
}

export interface PosSummary {
  todaySales: number;
  todayTransactionsCount: number;
  activeShiftsCount: number;
  totalVariance: number;
}
