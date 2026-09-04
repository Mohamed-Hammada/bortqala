import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy, HostListener, inject, signal, computed } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import {
  PosLineItem,
  PosPaymentMethod,
  PosSession,
  PosSummary,
  PosTerminal,
  PosTransaction,
  SavePrinterPayload,
  ThermalPrinter,
} from './pos.models';
import { PosDataService } from './pos.service';
import { ThermalPrinterService } from '../../../core/native/thermal-printer.service';

@Component({
  selector: 'app-pos-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './pos.page.html',
  styleUrls: ['./pos.page.scss'],
})
export class PosPage implements OnInit, OnDestroy {
  // Keyboard shortcut ref for cleanup
  private readonly boundKeyHandler = this.handleKeyboard.bind(this);
  readonly i18n = inject(I18nService);
  private readonly posService = inject(PosDataService);
  private readonly thermalPrinter = inject(ThermalPrinterService);
  private readonly fb = inject(FormBuilder);
  private readonly notification = inject(NotificationService);

  readonly activeTab = signal<'register' | 'sessions' | 'terminals' | 'printers' | 'history'>('register');
  readonly loading = signal(false);
  readonly summary = signal<PosSummary | null>(null);
  readonly terminals = signal<PosTerminal[]>([]);
  readonly sessions = signal<PosSession[]>([]);
  readonly transactions = signal<PosTransaction[]>([]);
  readonly selectedTerminal = signal<PosTerminal | null>(null);
  readonly activeSession = signal<PosSession | null>(null);

  // Cart state
  readonly cartLines = signal<PosLineItem[]>([]);
  readonly selectedPaymentMethod = signal<PosPaymentMethod>('CASH');
  readonly cashTendered = signal<number>(0);
  readonly barcodeQuery = signal<string>('');

  // Sample catalog for fast POS touchscreen touch
  readonly sampleCatalog = [
    { itemId: 'item-101', itemCode: 'ITM-COF-01', itemName: 'Espresso Roast 1KG', unitPrice: 350.0 },
    { itemId: 'item-102', itemCode: 'ITM-COF-02', itemName: 'Arabica Blend 500G', unitPrice: 220.0 },
    { itemId: 'item-103', itemCode: 'ITM-TEA-01', itemName: 'Ceylon Black Tea', unitPrice: 85.0 },
    { itemId: 'item-104', itemCode: 'ITM-BAK-01', itemName: 'Butter Croissant', unitPrice: 45.0 },
    { itemId: 'item-105', itemCode: 'ITM-BAK-02', itemName: 'Chocolate Muffin', unitPrice: 55.0 },
    { itemId: 'item-106', itemCode: 'ITM-BEV-01', itemName: 'Fresh Orange Juice', unitPrice: 60.0 },
  ];

  // Modals
  readonly showOpenShiftModal = signal(false);
  readonly showCloseShiftModal = signal(false);
  readonly showTerminalModal = signal(false);
  readonly showReceiptModal = signal(false);
  readonly showPrinterModal = signal(false);
  readonly showReprintModal = signal(false);
  readonly activeReceipt = signal<PosTransaction | null>(null);
  readonly reprintReason = signal<string>('');

  // Printers state
  readonly printers = signal<ThermalPrinter[]>([]);
  readonly selectedPrinter = signal<ThermalPrinter | null>(null);
  readonly isPrintingThermal = signal<boolean>(false);

  openShiftForm!: FormGroup;
  closeShiftForm!: FormGroup;
  terminalForm!: FormGroup;
  printerForm!: FormGroup;

  // Cart calculations
  readonly cartSubtotal = computed(() => {
    return this.cartLines().reduce((sum, line) => sum + line.quantity * line.unitPrice, 0);
  });

  readonly cartDiscount = computed(() => {
    return this.cartLines().reduce((sum, line) => sum + line.discountAmount, 0);
  });

  readonly cartTax = computed(() => {
    const net = this.cartSubtotal() - this.cartDiscount();
    return Math.round(net * 0.14 * 100) / 100;
  });

  readonly cartTotal = computed(() => {
    return Math.round((this.cartSubtotal() - this.cartDiscount() + this.cartTax()) * 100) / 100;
  });

  readonly changeDue = computed(() => {
    if (this.selectedPaymentMethod() === 'CASH' && this.cashTendered() > this.cartTotal()) {
      return Math.round((this.cashTendered() - this.cartTotal()) * 100) / 100;
    }
    return 0;
  });

  ngOnInit(): void {
    this.initForms();
    this.loadAll();
    window.addEventListener('keydown', this.boundKeyHandler);
  }

  ngOnDestroy(): void {
    window.removeEventListener('keydown', this.boundKeyHandler);
  }

  /**
   * POS Keyboard shortcuts (F1-F12):
   * F1: Focus barcode scanner / item search
   * F2: Focus quantity input for selected item
   * F4: Apply line discount
   * F5: Refresh cart/session
   * F8: Split payment
   * F9: Quick cash & complete sale
   * F12: Hold / park cart (clear)
   * Esc: Close any open modal
   */
  handleKeyboard(event: KeyboardEvent): void {
    // Esc closes any open modal
    if (event.key === 'Escape') {
      if (this.showReceiptModal()) { this.showReceiptModal.set(false); return; }
      if (this.showOpenShiftModal()) { this.showOpenShiftModal.set(false); return; }
      if (this.showCloseShiftModal()) { this.showCloseShiftModal.set(false); return; }
      if (this.showTerminalModal()) { this.showTerminalModal.set(false); return; }
      if (this.showPrinterModal()) { this.showPrinterModal.set(false); return; }
      if (this.showReprintModal()) { this.showReprintModal.set(false); return; }
      return;
    }

    // Only handle F-keys on the register tab
    if (this.activeTab() !== 'register' && !event.key.startsWith('F')) return;

    switch (event.key) {
      case 'F1':
        event.preventDefault();
        this.focusBarcodeScanner();
        break;
      case 'F2':
        event.preventDefault();
        this.focusQuantityInput();
        break;
      case 'F4':
        event.preventDefault();
        this.applyDiscountToLastItem();
        break;
      case 'F5':
        event.preventDefault();
        this.clearCart();
        this.loadAll();
        break;
      case 'F8':
        event.preventDefault();
        this.selectedPaymentMethod.set('CARD');
        this.notification.info(this.i18n.t('pos.splitPayment') || 'Card / split payment selected');
        break;
      case 'F9':
        event.preventDefault();
        this.quickCashSale();
        break;
      case 'F12':
        event.preventDefault();
        this.holdCart();
        break;
    }
  }

  private focusBarcodeScanner(): void {
    const el = document.querySelector('.barcode-input') as HTMLInputElement | null;
    if (el) { el.focus(); el.select(); }
  }

  private focusQuantityInput(): void {
    const el = document.querySelector('.cart-line-row .qty-val') as HTMLElement | null;
    if (el) el.click();
  }

  private applyDiscountToLastItem(): void {
    const lines = this.cartLines();
    if (lines.length === 0) return;
    const last = lines[lines.length - 1];
    const discountRate = prompt(this.i18n.t('pos.discountPrompt') || 'Discount %', '10');
    if (discountRate === null) return;
    const rate = parseFloat(discountRate);
    if (isNaN(rate) || rate < 0 || rate > 100) return;
    this.updateLineDiscount(last.itemId, rate);
  }

  updateLineDiscount(itemId: string, discountRate: number): void {
    const updated = this.cartLines().map((line) => {
      if (line.itemId === itemId) {
        const sub = line.quantity * line.unitPrice;
        const disc = (sub * discountRate) / 100;
        const net = sub - disc;
        const tax = Math.round(net * 0.14 * 100) / 100;
        return {
          ...line,
          discountRate,
          discountAmount: disc,
          taxAmount: tax,
          lineTotal: Math.round((net + tax) * 100) / 100,
        };
      }
      return line;
    });
    this.cartLines.set(updated);
  }

  private quickCashSale(): void {
    if (this.cartLines().length === 0 || !this.activeSession()) return;
    this.selectedPaymentMethod.set('CASH');
    this.completeSale();
  }

  private holdCart(): void {
    if (this.cartLines().length === 0) return;
    // Store cart in sessionStorage for later retrieval
    try {
      sessionStorage.setItem('pos-held-cart', JSON.stringify(this.cartLines()));
      this.notification.info(this.i18n.t('pos.cartHeld') || 'Cart held for later');
      this.clearCart();
    } catch { /* ignore storage errors */ }
  }

  private initForms(): void {
    this.openShiftForm = this.fb.group({
      terminalId: ['', Validators.required],
      openingFloat: [500, [Validators.required, Validators.min(0)]],
    });

    this.closeShiftForm = this.fb.group({
      closingActualCash: [0, [Validators.required, Validators.min(0)]],
      closingActualCard: [0, [Validators.required, Validators.min(0)]],
      notes: [''],
    });

    this.terminalForm = this.fb.group({
      terminalCode: ['', Validators.required],
      terminalName: ['', Validators.required],
      branchId: [''],
      warehouseId: [''],
      cashboxId: [''],
      status: ['ACTIVE', Validators.required],
    });

    this.printerForm = this.fb.group({
      id: [''],
      name: ['', Validators.required],
      connectionType: ['NETWORK', Validators.required],
      ipAddress: [''],
      port: [9100],
      bluetoothMac: [''],
      paperWidth: ['MM_80', Validators.required],
      openDrawer: [true],
      cutPaper: [true],
      printQrCode: [true],
      isDefault: [false],
      active: [true],
      headerText: [''],
      footerText: [''],
    });
  }

  loadAll(): void {
    this.loading.set(true);
    this.loadSummary();
    this.loadTerminals();
    this.loadSessions();
    this.loadTransactions();
    this.loadPrinters();
  }

  loadSummary(): void {
    this.posService.getSummary().subscribe({
      next: (data) => this.summary.set(data),
      error: () => this.loading.set(false),
    });
  }

  loadTerminals(): void {
    this.posService.getTerminals().subscribe({
      next: (data) => {
        this.terminals.set(data);
        if (data.length > 0 && !this.selectedTerminal()) {
          this.selectTerminal(data[0]);
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  loadSessions(): void {
    this.posService.getSessions().subscribe({
      next: (data) => this.sessions.set(data),
    });
  }

  loadTransactions(): void {
    this.posService.getTransactions().subscribe({
      next: (data) => this.transactions.set(data),
    });
  }

  selectTerminal(terminal: PosTerminal): void {
    this.selectedTerminal.set(terminal);
    this.posService.getActiveSession(terminal.id).subscribe({
      next: (session) => {
        this.activeSession.set(session);
        if (session) {
          this.openShiftForm.patchValue({ terminalId: terminal.id });
        }
      },
    });
  }

  // Cart operations
  addItemToCart(item: { itemId: string; itemCode: string; itemName: string; unitPrice: number }): void {
    const current = this.cartLines();
    const existing = current.find((l) => l.itemId === item.itemId);
    if (existing) {
      this.updateQuantity(item.itemId, existing.quantity + 1);
    } else {
      const newLine: PosLineItem = {
        itemId: item.itemId,
        itemCode: item.itemCode,
        itemName: item.itemName,
        quantity: 1,
        unitPrice: item.unitPrice,
        discountRate: 0,
        discountAmount: 0,
        taxAmount: Math.round(item.unitPrice * 0.14 * 100) / 100,
        lineTotal: Math.round(item.unitPrice * 1.14 * 100) / 100,
      };
      this.cartLines.set([...current, newLine]);
    }
  }

  updateQuantity(itemId: string, qty: number): void {
    if (qty <= 0) {
      this.removeItem(itemId);
      return;
    }
    const updated = this.cartLines().map((line) => {
      if (line.itemId === itemId) {
        const sub = qty * line.unitPrice;
        const disc = (sub * line.discountRate) / 100;
        const net = sub - disc;
        const tax = Math.round(net * 0.14 * 100) / 100;
        return {
          ...line,
          quantity: qty,
          discountAmount: disc,
          taxAmount: tax,
          lineTotal: Math.round((net + tax) * 100) / 100,
        };
      }
      return line;
    });
    this.cartLines.set(updated);
  }

  removeItem(itemId: string): void {
    this.cartLines.set(this.cartLines().filter((l) => l.itemId !== itemId));
  }

  clearCart(): void {
    this.cartLines.set([]);
    this.cashTendered.set(0);
  }

  onBarcodeEnter(event: Event): void {
    event.preventDefault();
    const q = this.barcodeQuery().trim();
    if (!q) return;
    const found = this.sampleCatalog.find(
      (i) => i.itemCode.toLowerCase() === q.toLowerCase() || i.itemName.toLowerCase().includes(q.toLowerCase())
    );
    if (found) {
      this.addItemToCart(found);
      this.barcodeQuery.set('');
    }
  }

  // Sale Checkout
  completeSale(): void {
    const session = this.activeSession();
    if (!session) {
      this.notification.error(this.i18n.t('pos.noActiveShift'));
      return;
    }
    if (this.cartLines().length === 0) {
      return;
    }

    const payload = {
      sessionId: session.id,
      paymentMethod: this.selectedPaymentMethod(),
      cashTendered: this.selectedPaymentMethod() === 'CASH' ? (this.cashTendered() || this.cartTotal()) : this.cartTotal(),
      clientOfflineId: `off-${Date.now()}`,
      lines: this.cartLines(),
    };

    this.posService.processSale(payload).subscribe({
      next: (txn) => {
        this.notification.success(this.i18n.t('pos.saleSuccess'));
        this.activeReceipt.set(txn);
        this.showReceiptModal.set(true);
        this.clearCart();
        this.loadSummary();
        this.loadTransactions();
      },
    });
  }

  // Shifts / Sessions
  openOpenShiftModal(): void {
    const term = this.selectedTerminal();
    if (term) {
      this.openShiftForm.patchValue({ terminalId: term.id });
    }
    this.showOpenShiftModal.set(true);
  }

  submitOpenShift(): void {
    if (this.openShiftForm.invalid) return;
    this.posService.openSession(this.openShiftForm.value).subscribe({
      next: (session) => {
        this.notification.success(this.i18n.t('pos.shiftOpened'));
        this.activeSession.set(session);
        this.showOpenShiftModal.set(false);
        this.loadSummary();
        this.loadSessions();
      },
    });
  }

  openCloseShiftModal(): void {
    const session = this.activeSession();
    if (session) {
      this.closeShiftForm.patchValue({
        closingActualCash: session.closingCalculatedCash || session.openingFloat,
        closingActualCard: session.closingCalculatedCard || 0,
        notes: '',
      });
      this.showCloseShiftModal.set(true);
    }
  }

  submitCloseShift(): void {
    const session = this.activeSession();
    if (!session || this.closeShiftForm.invalid) return;
    this.posService.closeSession(session.id, this.closeShiftForm.value).subscribe({
      next: () => {
        this.notification.success(this.i18n.t('pos.shiftClosed'));
        this.activeSession.set(null);
        this.showCloseShiftModal.set(false);
        this.loadSummary();
        this.loadSessions();
      },
    });
  }

  // Terminals
  openTerminalModal(): void {
    this.terminalForm.reset({ status: 'ACTIVE' });
    this.showTerminalModal.set(true);
  }

  saveTerminal(): void {
    if (this.terminalForm.invalid) return;
    this.posService.saveTerminal(this.terminalForm.value).subscribe({
      next: () => {
        this.notification.success(this.i18n.t('pos.terminalSaved'));
        this.showTerminalModal.set(false);
        this.loadTerminals();
      },
    });
  }

  viewReceipt(txn: PosTransaction): void {
    this.activeReceipt.set(txn);
    this.showReceiptModal.set(true);
  }

  returnReceipt(txn: PosTransaction): void {
    const session = this.activeSession();
    if (!session) {
      this.notification.error(this.i18n.t('pos.noActiveShift'));
      return;
    }
    this.posService.processReturn({
      originalTransactionId: txn.id,
      sessionId: session.id,
      reason: 'Customer return request',
    }).subscribe({
      next: () => {
        this.notification.success(this.i18n.t('pos.returnSuccess'));
        this.loadSummary();
        this.loadTransactions();
      },
    });
  }

  printReceipt(): void {
    window.print();
  }

  // Thermal Printers
  loadPrinters(): void {
    this.posService.getPrinters().subscribe({
      next: (data) => {
        this.printers.set(data);
        if (data.length > 0 && !this.selectedPrinter()) {
          const defaultPrinter = data.find((p) => p.isDefault) || data[0];
          this.selectedPrinter.set(defaultPrinter);
        }
      },
    });
  }

  selectPrinter(printer: ThermalPrinter): void {
    this.selectedPrinter.set(printer);
  }

  openAddPrinterModal(): void {
    this.printerForm.reset({
      connectionType: 'NETWORK',
      port: 9100,
      paperWidth: 'MM_80',
      openDrawer: true,
      cutPaper: true,
      printQrCode: true,
      isDefault: false,
      active: true,
    });
    this.showPrinterModal.set(true);
  }

  openEditPrinterModal(printer: ThermalPrinter): void {
    this.printerForm.patchValue({
      id: printer.id,
      name: printer.name,
      connectionType: printer.connectionType,
      ipAddress: printer.ipAddress || '',
      port: printer.port || 9100,
      bluetoothMac: printer.bluetoothMac || '',
      paperWidth: printer.paperWidth,
      openDrawer: printer.openDrawer,
      cutPaper: printer.cutPaper,
      printQrCode: printer.printQrCode,
      isDefault: printer.isDefault,
      active: printer.active,
      headerText: printer.headerText || '',
      footerText: printer.footerText || '',
    });
    this.showPrinterModal.set(true);
  }

  savePrinter(): void {
    if (this.printerForm.invalid) return;
    const formVal = this.printerForm.value;
    const payload: SavePrinterPayload = {
      id: formVal.id || undefined,
      name: formVal.name,
      connectionType: formVal.connectionType,
      ipAddress: formVal.ipAddress || undefined,
      port: formVal.port ? Number(formVal.port) : undefined,
      bluetoothMac: formVal.bluetoothMac || undefined,
      paperWidth: formVal.paperWidth,
      openDrawer: formVal.openDrawer !== false,
      cutPaper: formVal.cutPaper !== false,
      printQrCode: formVal.printQrCode !== false,
      isDefault: !!formVal.isDefault,
      active: formVal.active !== false,
      headerText: formVal.headerText || undefined,
      footerText: formVal.footerText || undefined,
    };

    this.posService.savePrinter(payload).subscribe({
      next: () => {
        this.notification.success(this.i18n.t('pos.printerSaved'));
        this.showPrinterModal.set(false);
        this.loadPrinters();
      },
    });
  }

  deletePrinter(printer: ThermalPrinter): void {
    this.posService.deletePrinter(printer.id).subscribe({
      next: () => {
        this.notification.success(this.i18n.t('pos.printerDeleted'));
        if (this.selectedPrinter()?.id === printer.id) {
          this.selectedPrinter.set(null);
        }
        this.loadPrinters();
      },
    });
  }

  testPrint(printer: ThermalPrinter): void {
    this.posService.testPrint(printer.id).subscribe({
      next: (res) => {
        if (res.sentToPrinter) {
          this.notification.success(res.message);
        } else {
          this.notification.info(res.message);
        }
      },
    });
  }

  printThermalReceiptDirect(txn: PosTransaction): void {
    const printer = this.selectedPrinter();
    const printerId = printer ? printer.id : undefined;

    this.isPrintingThermal.set(true);
    this.posService.getReceiptEscPos(txn.id, printerId).subscribe({
      next: async (res) => {
        await this.thermalPrinter.printReceipt(res);
        this.isPrintingThermal.set(false);
      },
      error: () => {
        this.isPrintingThermal.set(false);
      },
    });
  }

  openReprintDialog(txn: PosTransaction): void {
    this.activeReceipt.set(txn);
    this.reprintReason.set('');
    this.showReprintModal.set(true);
  }

  submitReprint(): void {
    const txn = this.activeReceipt();
    if (!txn) return;
    const reason = this.reprintReason().trim();
    if (!reason) {
      this.notification.warning(this.i18n.t('POS_REPRINT_REASON_REQUIRED'));
      return;
    }
    const printer = this.selectedPrinter();
    this.posService.reprintReceipt(txn.id, reason, printer?.id).subscribe({
      next: async (res) => {
        this.notification.success(this.i18n.t('pos.reprintSuccess'));
        this.showReprintModal.set(false);
        this.loadTransactions();
        await this.thermalPrinter.printReceipt(res);
      },
    });
  }
}
