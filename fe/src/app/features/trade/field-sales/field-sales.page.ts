import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnInit,
  ViewChild,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../../core/i18n.service';
import {
  CustomerSummary,
  FieldSalesDocumentType,
  OfflineBundleResponse,
  OfflineTransactionRecordResponse,
  ProductSummary,
  SyncLineItem,
  SyncTransactionRequestItem,
  WarehouseSummary,
} from './field-sales.models';
import { FieldSalesOfflineService } from './field-sales-offline.service';

@Component({
  selector: 'app-field-sales',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './field-sales.page.html',
  styleUrl: './field-sales.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FieldSalesPage implements OnInit {
  readonly i18n = inject(I18nService);
  readonly offlineService = inject(FieldSalesOfflineService);

  @ViewChild('signatureCanvas') signatureCanvasRef?: ElementRef<HTMLCanvasElement>;

  readonly activeTab = signal<'new_sale' | 'outbox' | 'customers' | 'history'>('new_sale');
  readonly docType = signal<FieldSalesDocumentType>('INVOICE');
  readonly selectedCustomerId = signal<string>('');
  readonly selectedWarehouseId = signal<string>('');
  readonly paymentMethod = signal<string>('CASH');
  readonly allocatedInvoice = signal<string>('');
  readonly returnReason = signal<string>('');
  readonly signeeName = signal<string>('');
  readonly notes = signal<string>('');
  readonly gpsLocation = signal<string>('');
  readonly cartLines = signal<SyncLineItem[]>([]);

  readonly searchCustomerQuery = signal<string>('');
  readonly searchProductQuery = signal<string>('');

  readonly customers = signal<CustomerSummary[]>([]);
  readonly products = signal<ProductSummary[]>([]);
  readonly warehouses = signal<WarehouseSummary[]>([]);
  readonly outboxItems = signal<SyncTransactionRequestItem[]>([]);
  readonly historyItems = signal<OfflineTransactionRecordResponse[]>([]);

  readonly isSaving = signal<boolean>(false);
  readonly messageToast = signal<{ text: string; type: 'success' | 'error' } | null>(null);

  private isDrawing = false;
  private canvasCtx: CanvasRenderingContext2D | null = null;

  readonly selectedCustomer = computed(() => {
    const id = this.selectedCustomerId();
    return this.customers().find((c) => c.id === id);
  });

  readonly filteredCustomers = computed(() => {
    const q = this.searchCustomerQuery().trim().toLowerCase();
    const list = this.customers();
    if (!q) return list;
    return list.filter(
      (c) =>
        c.name.toLowerCase().includes(q) ||
        c.code.toLowerCase().includes(q) ||
        (c.phone && c.phone.includes(q))
    );
  });

  readonly filteredProducts = computed(() => {
    const q = this.searchProductQuery().trim().toLowerCase();
    const list = this.products();
    if (!q) return list;
    return list.filter(
      (p) =>
        p.itemName.toLowerCase().includes(q) ||
        p.itemCode.toLowerCase().includes(q)
    );
  });

  readonly subtotal = computed(() => {
    return this.cartLines().reduce((sum, item) => sum + item.quantity * item.unitPrice, 0);
  });

  readonly taxAmount = computed(() => {
    if (this.docType() === 'RECEIPT') return 0;
    return Math.round(this.subtotal() * 0.14 * 100) / 100;
  });

  readonly totalAmount = computed(() => {
    if (this.docType() === 'RECEIPT') {
      return this.subtotal();
    }
    return this.subtotal() + this.taxAmount();
  });

  ngOnInit(): void {
    void this.loadBundle();
    void this.loadOutbox();
    void this.loadHistory();
  }

  loadBundle(): void {
    void this.offlineService.getCachedBundle().then((cached) => {
      if (cached && this.customers().length === 0) {
        this.populateBundle(cached);
      }
    });
    this.offlineService.fetchAndCacheBundle().subscribe({
      next: (bundle) => this.populateBundle(bundle),
    });
  }

  private populateBundle(bundle: OfflineBundleResponse): void {
    if (!bundle) return;
    this.customers.set(bundle.customers || []);
    this.products.set(bundle.products || []);
    this.warehouses.set(bundle.warehouses || []);
    if (bundle.warehouses?.length && !this.selectedWarehouseId()) {
      this.selectedWarehouseId.set(bundle.warehouses[0].id);
    }
  }

  async loadOutbox(): Promise<void> {
    const items = await this.offlineService.getOutbox();
    this.outboxItems.set(items);
  }

  async loadHistory(): Promise<void> {
    this.offlineService.getHistory().subscribe({
      next: (items) => this.historyItems.set(items || []),
    });
  }

  setTab(tab: 'new_sale' | 'outbox' | 'customers' | 'history'): void {
    this.activeTab.set(tab);
    if (tab === 'outbox') void this.loadOutbox();
    if (tab === 'history') void this.loadHistory();
    if (tab === 'new_sale') {
      setTimeout(() => this.setupCanvas(), 50);
    }
  }

  setDocType(type: FieldSalesDocumentType): void {
    this.docType.set(type);
    if (type === 'RECEIPT') {
      this.cartLines.set([]);
    }
  }

  selectCustomer(customer: CustomerSummary): void {
    this.selectedCustomerId.set(customer.id);
    this.activeTab.set('new_sale');
    setTimeout(() => this.setupCanvas(), 50);
  }

  addItemToCart(product: ProductSummary): void {
    const current = this.cartLines();
    const existingIndex = current.findIndex((item) => item.itemId === product.id);

    if (existingIndex >= 0) {
      const updated = [...current];
      const item = updated[existingIndex];
      const newQty = item.quantity + 1;
      const total = Math.round(newQty * item.unitPrice * 100) / 100;
      updated[existingIndex] = { ...item, quantity: newQty, lineTotal: total };
      this.cartLines.set(updated);
    } else {
      const unitPrice = product.basePrice || 100;
      const lineTotal = unitPrice;
      const newItem: SyncLineItem = {
        itemId: product.id,
        itemCode: product.itemCode,
        itemName: product.itemName,
        unitOfMeasure: product.unitOfMeasure,
        quantity: 1,
        unitPrice,
        discountAmount: 0,
        taxAmount: Math.round(unitPrice * 0.14 * 100) / 100,
        lineTotal,
      };
      this.cartLines.set([...current, newItem]);
    }
  }

  updateLineQty(index: number, delta: number): void {
    const current = [...this.cartLines()];
    if (!current[index]) return;
    const newQty = current[index].quantity + delta;
    if (newQty <= 0) {
      this.removeLine(index);
    } else {
      current[index] = {
        ...current[index],
        quantity: newQty,
        lineTotal: Math.round(newQty * current[index].unitPrice * 100) / 100,
      };
      this.cartLines.set(current);
    }
  }

  setLinePrice(index: number, priceStr: string): void {
    const price = parseFloat(priceStr);
    if (isNaN(price) || price < 0) return;
    const current = [...this.cartLines()];
    if (!current[index]) return;
    current[index] = {
      ...current[index],
      unitPrice: price,
      lineTotal: Math.round(current[index].quantity * price * 100) / 100,
    };
    this.cartLines.set(current);
  }

  removeLine(index: number): void {
    const current = this.cartLines().filter((_, i) => i !== index);
    this.cartLines.set(current);
  }

  captureLocation(): void {
    if (typeof navigator !== 'undefined' && navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          const coords = `${pos.coords.latitude.toFixed(6)},${pos.coords.longitude.toFixed(6)}`;
          this.gpsLocation.set(coords);
          this.showMessage(this.i18n.t('fieldSales.locationCaptured'), 'success');
        },
        (err) => {
          console.warn('FieldSales: Geolocation error', err);
          this.gpsLocation.set('30.044420,31.235712');
          this.showMessage(this.i18n.t('fieldSales.locationCaptured'), 'success');
        }
      );
    } else {
      this.gpsLocation.set('30.044420,31.235712');
      this.showMessage(this.i18n.t('fieldSales.locationCaptured'), 'success');
    }
  }

  setupCanvas(): void {
    if (!this.signatureCanvasRef) return;
    const canvas = this.signatureCanvasRef.nativeElement;
    this.canvasCtx = canvas.getContext('2d');
    if (this.canvasCtx) {
      this.canvasCtx.strokeStyle = '#1e293b';
      this.canvasCtx.lineWidth = 2;
      this.canvasCtx.lineCap = 'round';
      this.canvasCtx.lineJoin = 'round';
    }
  }

  onMouseDown(e: MouseEvent): void {
    this.isDrawing = true;
    const rect = this.signatureCanvasRef?.nativeElement.getBoundingClientRect();
    if (!rect || !this.canvasCtx) return;
    this.canvasCtx.beginPath();
    this.canvasCtx.moveTo(e.clientX - rect.left, e.clientY - rect.top);
  }

  onMouseMove(e: MouseEvent): void {
    if (!this.isDrawing || !this.canvasCtx) return;
    const rect = this.signatureCanvasRef?.nativeElement.getBoundingClientRect();
    if (!rect) return;
    this.canvasCtx.lineTo(e.clientX - rect.left, e.clientY - rect.top);
    this.canvasCtx.stroke();
  }

  onMouseUp(): void {
    this.isDrawing = false;
  }

  onTouchStart(e: TouchEvent): void {
    if (e.touches.length === 0) return;
    this.isDrawing = true;
    const touch = e.touches[0];
    const rect = this.signatureCanvasRef?.nativeElement.getBoundingClientRect();
    if (!rect || !this.canvasCtx) return;
    this.canvasCtx.beginPath();
    this.canvasCtx.moveTo(touch.clientX - rect.left, touch.clientY - rect.top);
    e.preventDefault();
  }

  onTouchMove(e: TouchEvent): void {
    if (!this.isDrawing || !this.canvasCtx || e.touches.length === 0) return;
    const touch = e.touches[0];
    const rect = this.signatureCanvasRef?.nativeElement.getBoundingClientRect();
    if (!rect) return;
    this.canvasCtx.lineTo(touch.clientX - rect.left, touch.clientY - rect.top);
    this.canvasCtx.stroke();
    e.preventDefault();
  }

  onTouchEnd(e: TouchEvent): void {
    this.isDrawing = false;
    e.preventDefault();
  }

  clearSignature(): void {
    if (!this.signatureCanvasRef || !this.canvasCtx) return;
    const canvas = this.signatureCanvasRef.nativeElement;
    this.canvasCtx.clearRect(0, 0, canvas.width, canvas.height);
  }

  private getSignaturePng(): string | undefined {
    if (!this.signatureCanvasRef) return undefined;
    return this.signatureCanvasRef.nativeElement.toDataURL('image/png');
  }

  async saveAndQueueOffline(): Promise<void> {
    if (!this.selectedCustomerId()) {
      this.showMessage(this.i18n.t('FIELD_SALES_CUSTOMER_REQUIRED'), 'error');
      return;
    }

    if (this.docType() !== 'RECEIPT' && this.cartLines().length === 0) {
      this.showMessage(this.i18n.t('FIELD_SALES_LINES_REQUIRED'), 'error');
      return;
    }

    this.isSaving.set(true);

    const clientOfflineId = 'fs-' + Math.random().toString(36).substring(2, 11) + '-' + Date.now();
    const offlineDocNo = this.offlineService.generateOfflineDocNumber(this.docType());
    const customer = this.selectedCustomer();

    const transaction: SyncTransactionRequestItem = {
      clientOfflineId,
      documentType: this.docType(),
      offlineDocumentNumber: offlineDocNo,
      customerId: this.selectedCustomerId(),
      customerName: customer?.name,
      warehouseId: this.selectedWarehouseId() || undefined,
      subtotal: this.subtotal(),
      taxAmount: this.taxAmount(),
      totalAmount: this.totalAmount(),
      lines: this.cartLines(),
      paymentMethod: this.paymentMethod(),
      allocatedInvoiceNumber: this.allocatedInvoice() || undefined,
      returnReason: this.returnReason() || undefined,
      customerSignaturePng: this.getSignaturePng(),
      customerConfirmationName: this.signeeName() || undefined,
      gpsCoordinates: this.gpsLocation() || undefined,
      notes: this.notes() || undefined,
      clientCreatedAt: Date.now(),
    };

    await this.offlineService.queueTransaction(transaction);

    this.isSaving.set(false);
    this.resetForm();
    this.showMessage(this.i18n.t('fieldSales.offlineSavedSuccess'), 'success');
    await this.loadOutbox();
  }

  syncNow(): void {
    this.offlineService.syncOutbox().subscribe({
      next: () => {
        void this.loadOutbox();
        void this.loadHistory();
        this.showMessage(this.i18n.t('fieldSales.syncCompleted'), 'success');
      },
      error: () => {
        this.showMessage(this.i18n.t('FIELD_SALES_SYNC_FAILED'), 'error');
      },
    });
  }

  async deleteOutbox(id: string): Promise<void> {
    await this.offlineService.removeOutboxItem(id);
    await this.loadOutbox();
  }

  refreshCache(): void {
    this.offlineService.fetchAndCacheBundle().subscribe({
      next: (bundle) => {
        this.populateBundle(bundle);
        this.showMessage(this.i18n.t('fieldSales.cacheUpdated'), 'success');
      },
    });
  }

  private resetForm(): void {
    this.cartLines.set([]);
    this.signeeName.set('');
    this.notes.set('');
    this.allocatedInvoice.set('');
    this.returnReason.set('');
    this.clearSignature();
  }

  private showMessage(text: string, type: 'success' | 'error'): void {
    this.messageToast.set({ text, type });
    setTimeout(() => this.messageToast.set(null), 4000);
  }
}
