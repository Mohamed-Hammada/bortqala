import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';
import { DecimalPipe } from '@angular/common';

interface ExportShipmentLine {
  id: string;
  lineOrder: number;
  itemName: string;
  itemCode?: string;
  lotReference?: string;
  quantity: number;
  unitOfMeasure?: string;
  netWeightKg?: number;
  grossWeightKg?: number;
  packagesCount?: number;
}

interface ExportShipment {
  id: string;
  shipmentNumber: string;
  customerPartyId: string;
  customerPartyName?: string;
  contractRef?: string;
  containerNo?: string;
  bookingNo?: string;
  acidNo?: string;
  portOfLoading?: string;
  portOfDischarge?: string;
  etbDate?: number;
  etaDate?: number;
  status: string;
  notes?: string;
  expectedFxAmount?: number;
  expectedFxCurrency?: string;
  realizedFxAmount?: number;
  daysOutstanding: number;
  lines: ExportShipmentLine[];
  createdAt: number;
  updatedAt: number;
}

interface TreatmentLog {
  id: string;
  lotReference: string;
  chemical: string;
  dose?: string;
  treatmentDate: number;
  preHarvestIntervalDays: number;
  earliestSafePickup: string;
  violation: boolean;
  daysUntilSafe: number;
  treatedBy?: string;
  notes?: string;
  createdAt: number;
}

interface Pesticide {
  id: string;
  chemicalName: string;
  activeIngredient?: string;
  registrationNumber?: string;
  mrlMgPerKg?: number;
  maxDosePerHa?: string;
  preHarvestIntervalDays?: number;
  cropAuthorized?: string;
  status: string;
  notes?: string;
  createdAt: number;
  updatedAt: number;
}

interface ComplianceViolation {
  lotReference: string;
  chemical: string;
  treatmentDate: number;
  earliestSafePickup: string;
  preHarvestIntervalDays: number;
  daysShort: number;
}

interface ComplianceCheckResponse {
  violations: ComplianceViolation[];
  totalLotsChecked: number;
  compliantLots: number;
}

interface AgingEntry {
  customerPartyId: string;
  customerPartyName?: string;
  shipmentNumber: string;
  daysOutstanding: number;
  expectedFxAmount?: number;
  expectedFxCurrency?: string;
}

@Component({
  selector: 'app-export-shipments-page',
  imports: [ReactiveFormsModule, DecimalPipe],
  templateUrl: './export-shipments.page.html',
  styleUrl: './export-shipments.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExportShipmentsPage {
  readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly activeTab = signal<'SHIPMENTS' | 'COMPLIANCE' | 'PROCEEDS' | 'PESTICIDES' | 'DOCS'>('SHIPMENTS');
  readonly shipments = signal<ExportShipment[]>([]);
  readonly selectedShipment = signal<ExportShipment | null>(null);
  readonly drawerOpen = signal(false);
  readonly saving = signal(false);
  readonly treatmentLogs = signal<TreatmentLog[]>([]);
  readonly pesticides = signal<Pesticide[]>([]);
  readonly agingEntries = signal<AgingEntry[]>([]);
  readonly checkResult = signal<ComplianceCheckResponse | null>(null);
  readonly checkRunning = signal(false);

  readonly shipmentForm = new FormGroup({
    customerPartyId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    customerPartyName: new FormControl('', { nonNullable: true }),
    contractRef: new FormControl('', { nonNullable: true }),
    containerNo: new FormControl('', { nonNullable: true }),
    bookingNo: new FormControl('', { nonNullable: true }),
    acidNo: new FormControl('', { nonNullable: true }),
    portOfLoading: new FormControl('', { nonNullable: true }),
    portOfDischarge: new FormControl('', { nonNullable: true }),
    notes: new FormControl('', { nonNullable: true }),
  });

  readonly treatmentForm = new FormGroup({
    lotReference: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    chemical: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    dose: new FormControl('', { nonNullable: true }),
    treatmentDate: new FormControl(new Date().toISOString().slice(0, 10), { nonNullable: true, validators: [Validators.required] }),
    preHarvestIntervalDays: new FormControl(14, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
    treatedBy: new FormControl('', { nonNullable: true }),
    notes: new FormControl('', { nonNullable: true }),
  });

  readonly checkForm = new FormGroup({
    lotReferences: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    pickupDate: new FormControl(new Date().toISOString().slice(0, 10), { nonNullable: true, validators: [Validators.required] }),
  });

  readonly pesticideForm = new FormGroup({
    chemicalName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    activeIngredient: new FormControl('', { nonNullable: true }),
    registrationNumber: new FormControl('', { nonNullable: true }),
    mrlMgPerKg: new FormControl<number | null>(null, { nonNullable: false }),
    maxDosePerHa: new FormControl('', { nonNullable: true }),
    preHarvestIntervalDays: new FormControl<number | null>(null, { nonNullable: false }),
    cropAuthorized: new FormControl('', { nonNullable: true }),
    notes: new FormControl('', { nonNullable: true }),
  });

  readonly linesForm = signal<Array<{ itemName: string; itemCode: string; lotReference: string; quantity: number; unitOfMeasure: string; packagesCount: number }>>([]);

  readonly totalLinesQuantity = computed(() =>
    this.linesForm().reduce((sum, l) => sum + (Number(l.quantity) || 0), 0)
  );

  constructor() {
    this.loadShipments();
  }

  async loadShipments() {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.shipments.set(await firstValueFrom(this.http.get<ExportShipment[]>('/api/v1/trade/export-shipments')));
    } catch (e: unknown) {
      this.error.set(apiErrorMessage(e));
    } finally {
      this.loading.set(false);
    }
  }

  async selectShipment(shipment: ExportShipment) {
    this.selectedShipment.set(shipment);
    this.drawerOpen.set(true);
    this.shipmentForm.patchValue({
      customerPartyId: shipment.customerPartyId,
      customerPartyName: shipment.customerPartyName ?? '',
      contractRef: shipment.contractRef ?? '',
      containerNo: shipment.containerNo ?? '',
      bookingNo: shipment.bookingNo ?? '',
      acidNo: shipment.acidNo ?? '',
      portOfLoading: shipment.portOfLoading ?? '',
      portOfDischarge: shipment.portOfDischarge ?? '',
      notes: shipment.notes ?? '',
    });
    this.linesForm.set(shipment.lines.map(l => ({
      itemName: l.itemName, itemCode: l.itemCode ?? '', lotReference: l.lotReference ?? '',
      quantity: l.quantity, unitOfMeasure: l.unitOfMeasure ?? 'KG', packagesCount: l.packagesCount ?? 0,
    })));
    this.loadTreatmentLogs();
    await this.loadAging();
  }

  closeDrawer() {
    this.drawerOpen.set(false);
    this.selectedShipment.set(null);
  }

  async createShipment() {
    this.saving.set(true);
    try {
      const lines = this.linesForm().map((l, i) => ({
        lineOrder: i + 1, itemName: l.itemName, itemCode: l.itemCode || undefined,
        lotReference: l.lotReference || undefined, quantity: l.quantity,
        unitOfMeasure: l.unitOfMeasure || undefined, packagesCount: l.packagesCount || undefined,
      }));
      const payload = { ...this.shipmentForm.value, lines: lines.length > 0 ? lines : [] };
      await firstValueFrom(this.http.post('/api/v1/trade/export-shipments', payload));
      this.notification.success(this.i18n.t('export.shipment') + ' ' + this.i18n.t('common.created'));
      this.drawerOpen.set(false);
      await this.loadShipments();
    } catch (e: unknown) {
      this.notification.error(apiErrorMessage(e));
    } finally {
      this.saving.set(false);
    }
  }

  async transition(id: string, status: string) {
    try {
      await firstValueFrom(this.http.post(`/api/v1/trade/export-shipments/${id}/transition?status=${status}`, {}));
      this.notification.success(this.i18n.t('export.transition' + status.charAt(0) + status.slice(1).toLowerCase()));
      await this.loadShipments();
    } catch (e: unknown) {
      this.notification.error(apiErrorMessage(e));
    }
  }

  addLine() {
    this.linesForm.update(lines => [...lines, {
      itemName: '', itemCode: '', lotReference: '', quantity: 1, unitOfMeasure: 'KG', packagesCount: 0,
    }]);
  }

  removeLine(index: number) {
    this.linesForm.update(lines => lines.filter((_, i) => i !== index));
  }

  updateLine(index: number, field: string, value: string | number) {
    this.linesForm.update(lines => lines.map((l, i) => i === index ? { ...l, [field]: value } : l));
  }

  async loadTreatmentLogs() {
    const ship = this.selectedShipment();
    if (!ship) return;
    const lotRefs = ship.lines.map(l => l.lotReference).filter(Boolean);
    if (lotRefs.length === 0) return;
    try {
      const allLogs: TreatmentLog[] = [];
      for (const ref of lotRefs) {
        const logs = await firstValueFrom(this.http.get<TreatmentLog[]>(`/api/v1/trade/export-shipments/compliance/treatments?lotReference=${ref}`));
        allLogs.push(...logs);
      }
      this.treatmentLogs.set(allLogs);
    } catch { /* ignore */ }
  }

  async createTreatment() {
    const form = this.treatmentForm;
    if (form.invalid) return;
    const dateStr = form.value.treatmentDate ?? '';
    const dateMs = new Date(dateStr + 'T00:00:00Z').getTime();
    const payload = { ...form.value, treatmentDate: dateMs };
    try {
      await firstValueFrom(this.http.post('/api/v1/trade/export-shipments/compliance/treatments', payload));
      this.notification.success(this.i18n.t('export.createTreatment'));
      form.reset({ lotReference: '', chemical: '', dose: '', treatmentDate: new Date().toISOString().slice(0, 10), preHarvestIntervalDays: 14, treatedBy: '', notes: '' });
      await this.loadTreatmentLogs();
    } catch (e: unknown) {
      this.notification.error(apiErrorMessage(e));
    }
  }

  async checkCompliance() {
    const form = this.checkForm;
    if (form.invalid) return;
    const lots = (form.value.lotReferences ?? '').split(',').map(s => s.trim()).filter(Boolean);
    const pickupMs = new Date((form.value.pickupDate ?? '') + 'T00:00:00Z').getTime();
    this.checkRunning.set(true);
    try {
      const params = lots.map(l => `lotReferences=${encodeURIComponent(l)}`).join('&');
      this.checkResult.set(await firstValueFrom(
        this.http.get<ComplianceCheckResponse>(`/api/v1/trade/export-shipments/compliance/check?${params}&pickupDate=${pickupMs}`)
      ));
    } catch (e: unknown) {
      this.notification.error(apiErrorMessage(e));
    } finally {
      this.checkRunning.set(false);
    }
  }

  async loadAging() {
    try {
      const response = await firstValueFrom(this.http.get<{ entries: AgingEntry[] }>('/api/v1/trade/export-shipments/aging'));
      this.agingEntries.set(response.entries);
    } catch { /* ignore */ }
  }

  async loadPesticides() {
    try {
      this.pesticides.set(await firstValueFrom(this.http.get<Pesticide[]>('/api/v1/trade/export-shipments/pesticides')));
    } catch (e: unknown) {
      this.notification.error(apiErrorMessage(e));
    }
  }

  async createPesticide() {
    if (this.pesticideForm.invalid) return;
    try {
      await firstValueFrom(this.http.post('/api/v1/trade/export-shipments/pesticides', this.pesticideForm.value));
      this.notification.success(this.i18n.t('export.createPesticide'));
      this.pesticideForm.reset();
      await this.loadPesticides();
    } catch (e: unknown) {
      this.notification.error(apiErrorMessage(e));
    }
  }

  switchTab(tab: 'SHIPMENTS' | 'COMPLIANCE' | 'PROCEEDS' | 'PESTICIDES' | 'DOCS') {
    this.activeTab.set(tab);
    if (tab === 'PESTICIDES') this.loadPesticides();
    if (tab === 'PROCEEDS') this.loadAging();
  }

  async downloadDoc(type: 'coo' | 'packing-list' | 'phytosanitary') {
    const ship = this.selectedShipment();
    if (!ship) {
      this.notification.error(this.i18n.t('export.selectShipmentFirst'));
      return;
    }
    try {
      const blob = await firstValueFrom(this.http.get(
        `/api/v1/trade/export-shipments/${ship.id}/docs/${type}.xlsx`,
        { responseType: 'blob' }));
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = `${ship.shipmentNumber}-${type}.xlsx`;
      link.click();
      URL.revokeObjectURL(link.href);
    } catch (e: unknown) {
      this.notification.error(apiErrorMessage(e));
    }
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      PREPARING: this.i18n.t('export.status') + ': ' + 'PREPARING',
      BOOKED: this.i18n.t('export.status') + ': ' + 'BOOKED',
      SHIPPED: this.i18n.t('export.status') + ': ' + 'SHIPPED',
      SETTLED: this.i18n.t('export.status') + ': ' + 'SETTLED',
    };
    return map[status] ?? status;
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      PREPARING: 'status-preparing', BOOKED: 'status-booked',
      SHIPPED: 'status-shipped', SETTLED: 'status-settled',
    };
    return map[status] ?? '';
  }

  nextAction(status: string): string | null {
    const map: Record<string, string> = {
      PREPARING: 'BOOKED', BOOKED: 'SHIPPED', SHIPPED: 'SETTLED',
    };
    return map[status] ?? null;
  }

  nextActionLabel(status: string): string {
    const map: Record<string, string> = {
      PREPARING: this.i18n.t('export.transitionBooked'),
      BOOKED: this.i18n.t('export.transitionShipped'),
      SHIPPED: this.i18n.t('export.transitionSettled'),
    };
    return map[status] ?? '';
  }

  formatEpoch(ms?: number | null): string {
    if (!ms) return '—';
    return new Date(ms).toLocaleDateString();
  }
}
