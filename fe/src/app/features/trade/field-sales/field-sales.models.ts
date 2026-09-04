export type FieldSalesDocumentType = 'INVOICE' | 'ORDER' | 'RECEIPT' | 'RETURN' | 'QUOTATION';

export type FieldSalesSyncStatus = 'SYNCED' | 'CONFLICT' | 'FAILED' | 'PENDING';

export interface CustomerSummary {
  id: string;
  code: string;
  name: string;
  phone?: string;
  address?: string;
  taxId?: string;
  creditLimit: number;
  currentBalance: number;
  creditHold: boolean;
  paymentTermsDays: number;
}

export interface ProductSummary {
  id: string;
  itemCode: string;
  itemName: string;
  unitOfMeasure: string;
  basePrice: number;
  taxRate: number;
  availableStock: number;
}

export interface WarehouseSummary {
  id: string;
  warehouseCode: string;
  warehouseName: string;
}

export interface OfflineBundleResponse {
  customers: CustomerSummary[];
  products: ProductSummary[];
  warehouses: WarehouseSummary[];
  salesRepUserId: string;
  salesRepName: string;
  serverTimestamp: number;
}

export interface SyncLineItem {
  itemId: string;
  itemCode?: string;
  itemName?: string;
  unitOfMeasure?: string;
  quantity: number;
  unitPrice: number;
  discountAmount?: number;
  taxAmount?: number;
  lineTotal: number;
}

export interface SyncTransactionRequestItem {
  clientOfflineId: string;
  documentType: FieldSalesDocumentType;
  offlineDocumentNumber: string;
  customerId: string;
  customerName?: string;
  warehouseId?: string;
  subtotal: number;
  discountAmount?: number;
  taxAmount?: number;
  totalAmount: number;
  lines?: SyncLineItem[];
  paymentMethod?: string;
  allocatedInvoiceNumber?: string;
  returnReason?: string;
  customerSignaturePng?: string;
  customerConfirmationName?: string;
  gpsCoordinates?: string;
  notes?: string;
  clientCreatedAt: number;
}

export interface SyncBatchRequest {
  transactions: SyncTransactionRequestItem[];
}

export interface SyncResultItem {
  clientOfflineId: string;
  serverDocumentId?: string;
  serverDocumentNumber?: string;
  status: FieldSalesSyncStatus;
  conflictReason?: string;
  message?: string;
}

export interface SyncBatchResponse {
  totalCount: number;
  syncedCount: number;
  conflictCount: number;
  results: SyncResultItem[];
}

export interface OfflineTransactionRecordResponse {
  id: string;
  clientOfflineId: string;
  documentType: FieldSalesDocumentType;
  offlineDocumentNumber: string;
  serverDocumentId?: string;
  serverDocumentNumber?: string;
  customerId: string;
  customerName?: string;
  salesRepUserId: string;
  totalAmount: number;
  status: FieldSalesSyncStatus;
  conflictReason?: string;
  customerConfirmationName?: string;
  gpsCoordinates?: string;
  clientCreatedAt: number;
  syncedAt: number;
}
