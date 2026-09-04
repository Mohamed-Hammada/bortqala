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
  reprintCount?: number;
  lastReprintedAt?: number | null;
  createdAt: number;
  lines: PosLineItem[];
}

export interface PosSummary {
  todaySales: number;
  todayTransactionsCount: number;
  activeShiftsCount: number;
  totalVariance: number;
}

export type ThermalPrinterConnectionType = 'NETWORK' | 'BLUETOOTH' | 'USB';
export type ThermalPaperWidth = 'MM_58' | 'MM_80';

export interface ThermalPrinter {
  id: string;
  name: string;
  branchId?: string | null;
  terminalId?: string | null;
  connectionType: ThermalPrinterConnectionType;
  ipAddress?: string | null;
  port?: number | null;
  bluetoothMac?: string | null;
  paperWidth: ThermalPaperWidth;
  characterCodePage: string;
  headerText?: string | null;
  footerText?: string | null;
  openDrawer: boolean;
  cutPaper: boolean;
  printQrCode: boolean;
  isDefault: boolean;
  active: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface SavePrinterPayload {
  id?: string | null;
  name: string;
  branchId?: string | null;
  terminalId?: string | null;
  connectionType: ThermalPrinterConnectionType;
  ipAddress?: string | null;
  port?: number | null;
  bluetoothMac?: string | null;
  paperWidth: ThermalPaperWidth;
  characterCodePage?: string;
  headerText?: string | null;
  footerText?: string | null;
  openDrawer: boolean;
  cutPaper: boolean;
  printQrCode: boolean;
  isDefault: boolean;
  active: boolean;
}

export interface ReceiptPrintData {
  transactionId: string;
  transactionNumber: string;
  printerId: string;
  printerName: string;
  connectionType: ThermalPrinterConnectionType;
  ipAddress?: string | null;
  port?: number | null;
  paperWidth: ThermalPaperWidth;
  base64Bytes: string;
  reprintCount: number;
  lastReprintedAt?: number | null;
  sentToPrinter: boolean;
  statusMessage: string;
}

export interface TestPrintResponse {
  printerId: string;
  printerName: string;
  base64Bytes: string;
  sentToPrinter: boolean;
  message: string;
}
