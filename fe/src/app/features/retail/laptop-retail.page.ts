import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../core/i18n.service';
import { LaptopRetailService } from './laptop-retail.service';
import { RegisterDeviceRequest, RepairTicket, SellDeviceRequest, SerializedDevice } from './laptop-retail.models';

@Component({
  selector: 'app-laptop-retail-page',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe],
  template: `
    <div class="retail-page">
      <header class="page-header">
        <h1>{{ i18n.t('retail.laptopShopTitle') }}</h1>
        <div class="header-actions">
          <button class="btn btn-primary" (click)="openRegisterModal()">
            + {{ i18n.t('retail.registerDevice') }}
          </button>
          <button class="btn btn-secondary" (click)="openRepairModal()">
            🔧 {{ i18n.t('retail.createRepairTicket') }}
          </button>
        </div>
      </header>

      <div class="tabs">
        <button
          class="tab-btn"
          [class.active]="activeTab() === 'inventory'"
          (click)="activeTab.set('inventory')"
        >
          {{ i18n.t('retail.inventoryTab') }}
        </button>
        <button
          class="tab-btn"
          [class.active]="activeTab() === 'repairs'"
          (click)="activeTab.set('repairs')"
        >
          {{ i18n.t('retail.repairsTab') }}
        </button>
      </div>

      @if (activeTab() === 'inventory') {
        <div class="filter-bar">
          <div class="filter-item">
            <label>{{ i18n.t('common.status') }}</label>
            <select [(ngModel)]="statusFilter" (change)="loadDevices()">
              <option value="">{{ i18n.t('common.all') }}</option>
              <option value="IN_STOCK">{{ i18n.t('retail.statusInStock') }}</option>
              <option value="SOLD">{{ i18n.t('retail.statusSold') }}</option>
              <option value="IN_REPAIR">{{ i18n.t('retail.statusInRepair') }}</option>
            </select>
          </div>
        </div>

        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>{{ i18n.t('retail.serialNumber') }}</th>
                <th>{{ i18n.t('retail.brand') }} / {{ i18n.t('retail.model') }}</th>
                <th>{{ i18n.t('retail.specs') }}</th>
                <th>{{ i18n.t('retail.conditionGrade') }}</th>
                <th>{{ i18n.t('retail.purchasePrice') }}</th>
                <th>{{ i18n.t('retail.sellingPrice') }}</th>
                <th>{{ i18n.t('common.status') }}</th>
                <th>{{ i18n.t('retail.warranty') }}</th>
                <th>{{ i18n.t('common.actions') }}</th>
              </tr>
            </thead>
            <tbody>
              @if (loadingDevices()) {
                <tr><td colspan="9" class="text-center">{{ i18n.t('common.loading') }}</td></tr>
              } @else if (devices().length === 0) {
                <tr><td colspan="9" class="text-center">{{ i18n.t('common.noData') }}</td></tr>
              } @else {
                @for (d of devices(); track d.id) {
                  <tr>
                    <td class="font-mono font-bold">{{ d.serialNumber }}</td>
                    <td>{{ d.brand }} {{ d.model }}</td>
                    <td>{{ d.cpu }} | {{ d.ramGb }}GB RAM | {{ d.storageGb }}GB {{ d.storageType }}</td>
                    <td><span class="badge condition">{{ d.conditionGrade }}</span></td>
                    <td>{{ d.purchasePrice | number:'1.2-2' }}</td>
                    <td>{{ d.sellingPrice | number:'1.2-2' }}</td>
                    <td>
                      <span class="badge" [class.badge-success]="d.status === 'IN_STOCK'" [class.badge-info]="d.status === 'SOLD'">
                        {{ d.status }}
                      </span>
                    </td>
                    <td>
                      @if (d.status === 'SOLD') {
                        <span [class.text-success]="d.isWarrantyActive" [class.text-danger]="!d.isWarrantyActive">
                          {{ d.isWarrantyActive ? i18n.t('retail.warrantyActive') : i18n.t('retail.warrantyExpired') }}
                        </span>
                      } @else {
                        <span>—</span>
                      }
                    </td>
                    <td>
                      @if (d.status === 'IN_STOCK') {
                        <button class="btn btn-sm btn-gold" (click)="openSellModal(d)">
                          {{ i18n.t('retail.sell') }}
                        </button>
                      } @else if (d.status === 'SOLD') {
                        <button class="btn btn-sm btn-outline" (click)="returnDevice(d)">
                          {{ i18n.t('retail.return') }}
                        </button>
                      }
                    </td>
                  </tr>
                }
              }
            </tbody>
          </table>
        </div>
      } @else {
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>{{ i18n.t('retail.ticketNumber') }}</th>
                <th>{{ i18n.t('retail.serialNumber') }}</th>
                <th>{{ i18n.t('retail.customerName') }}</th>
                <th>{{ i18n.t('retail.reportedIssue') }}</th>
                <th>{{ i18n.t('retail.underWarranty') }}</th>
                <th>{{ i18n.t('common.status') }}</th>
                <th>{{ i18n.t('retail.chargedAmount') }}</th>
              </tr>
            </thead>
            <tbody>
              @if (loadingRepairs()) {
                <tr><td colspan="7" class="text-center">{{ i18n.t('common.loading') }}</td></tr>
              } @else if (repairTickets().length === 0) {
                <tr><td colspan="7" class="text-center">{{ i18n.t('common.noData') }}</td></tr>
              } @else {
                @for (t of repairTickets(); track t.id) {
                  <tr>
                    <td class="font-mono">{{ t.ticketNumber }}</td>
                    <td class="font-mono">{{ t.serialNumber }}</td>
                    <td>{{ t.customerName }} ({{ t.customerPhone }})</td>
                    <td>{{ t.reportedIssue }}</td>
                    <td>
                      <span class="badge" [class.badge-success]="t.isUnderWarranty" [class.badge-muted]="!t.isUnderWarranty">
                        {{ t.isUnderWarranty ? i18n.t('common.yes') : i18n.t('common.no') }}
                      </span>
                    </td>
                    <td><span class="badge">{{ t.status }}</span></td>
                    <td>{{ t.chargedAmount | number:'1.2-2' }}</td>
                  </tr>
                }
              }
            </tbody>
          </table>
        </div>
      }

      <!-- Register Device Modal -->
      @if (showRegisterModal()) {
        <div class="modal-overlay">
          <div class="modal-card">
            <h2>{{ i18n.t('retail.registerDevice') }}</h2>
            <form (ngSubmit)="submitRegisterDevice()">
              <div class="form-grid">
                <div class="form-group">
                  <label>{{ i18n.t('retail.serialNumber') }} *</label>
                  <input type="text" [(ngModel)]="regForm.serialNumber" name="serialNumber" required />
                </div>
                <div class="form-group">
                  <label>{{ i18n.t('retail.brand') }} *</label>
                  <input type="text" [(ngModel)]="regForm.brand" name="brand" required />
                </div>
                <div class="form-group">
                  <label>{{ i18n.t('retail.model') }} *</label>
                  <input type="text" [(ngModel)]="regForm.model" name="model" required />
                </div>
                <div class="form-group">
                  <label>{{ i18n.t('retail.cpu') }} *</label>
                  <input type="text" [(ngModel)]="regForm.cpu" name="cpu" required />
                </div>
                <div class="form-group">
                  <label>{{ i18n.t('retail.ramGb') }} *</label>
                  <input type="number" [(ngModel)]="regForm.ramGb" name="ramGb" required />
                </div>
                <div class="form-group">
                  <label>{{ i18n.t('retail.storageGb') }} *</label>
                  <input type="number" [(ngModel)]="regForm.storageGb" name="storageGb" required />
                </div>
                <div class="form-group">
                  <label>{{ i18n.t('retail.storageType') }} *</label>
                  <select [(ngModel)]="regForm.storageType" name="storageType">
                    <option value="NVMe SSD">NVMe SSD</option>
                    <option value="SATA SSD">SATA SSD</option>
                    <option value="HDD">HDD</option>
                  </select>
                </div>
                <div class="form-group">
                  <label>{{ i18n.t('retail.conditionGrade') }}</label>
                  <select [(ngModel)]="regForm.conditionGrade" name="conditionGrade">
                    <option value="NEW">NEW</option>
                    <option value="REFURBISHED">REFURBISHED</option>
                    <option value="USED_A">USED (Grade A)</option>
                    <option value="USED_B">USED (Grade B)</option>
                  </select>
                </div>
                <div class="form-group">
                  <label>{{ i18n.t('retail.purchasePrice') }} *</label>
                  <input type="number" [(ngModel)]="regForm.purchasePrice" name="purchasePrice" required />
                </div>
                <div class="form-group">
                  <label>{{ i18n.t('retail.sellingPrice') }} *</label>
                  <input type="number" [(ngModel)]="regForm.sellingPrice" name="sellingPrice" required />
                </div>
              </div>
              <div class="modal-actions">
                <button type="button" class="btn btn-secondary" (click)="showRegisterModal.set(false)">
                  {{ i18n.t('common.cancel') }}
                </button>
                <button type="submit" class="btn btn-primary">
                  {{ i18n.t('common.save') }}
                </button>
              </div>
            </form>
          </div>
        </div>
      }

      <!-- Sell Device Modal -->
      @if (showSellModal()) {
        <div class="modal-overlay">
          <div class="modal-card">
            <h2>{{ i18n.t('retail.sellDevice') }}: {{ selectedDeviceForSell()?.serialNumber }}</h2>
            <form (ngSubmit)="submitSellDevice()">
              <div class="form-group">
                <label>{{ i18n.t('retail.customerName') }} *</label>
                <input type="text" [(ngModel)]="sellForm.customerName" name="customerName" required />
              </div>
              <div class="form-group">
                <label>{{ i18n.t('retail.warrantyMonths') }}</label>
                <select [(ngModel)]="sellForm.warrantyMonths" name="warrantyMonths">
                  <option [ngValue]="0">0 {{ i18n.t('retail.months') }}</option>
                  <option [ngValue]="6">6 {{ i18n.t('retail.months') }}</option>
                  <option [ngValue]="12">12 {{ i18n.t('retail.months') }}</option>
                  <option [ngValue]="24">24 {{ i18n.t('retail.months') }}</option>
                </select>
              </div>
              <div class="form-group">
                <label>{{ i18n.t('retail.finalSellingPrice') }}</label>
                <input type="number" [(ngModel)]="sellForm.finalSellingPrice" name="finalSellingPrice" />
              </div>
              <div class="modal-actions">
                <button type="button" class="btn btn-secondary" (click)="showSellModal.set(false)">
                  {{ i18n.t('common.cancel') }}
                </button>
                <button type="submit" class="btn btn-gold">
                  {{ i18n.t('retail.confirmSale') }}
                </button>
              </div>
            </form>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .retail-page { padding: 1.5rem; }
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
    .header-actions { display: flex; gap: 0.75rem; }
    .tabs { display: flex; gap: 0.5rem; margin-bottom: 1.5rem; border-bottom: 1px solid var(--border); }
    .tab-btn { padding: 0.75rem 1.5rem; border: none; background: none; cursor: pointer; color: var(--muted); font-size: 1rem; border-bottom: 2px solid transparent; }
    .tab-btn.active { color: var(--gold); border-color: var(--gold); font-weight: 600; }
    .filter-bar { margin-bottom: 1rem; display: flex; gap: 1rem; }
    .filter-item select { padding: 0.4rem 0.8rem; border-radius: 6px; border: 1px solid var(--border); }
    .table-container { background: var(--surface-card); border-radius: 12px; border: 1px solid var(--border); overflow: auto; }
    .data-table { width: 100%; border-collapse: collapse; text-align: left; }
    .data-table th, .data-table td { padding: 0.85rem 1rem; border-bottom: 1px solid var(--border); }
    .data-table th { background: var(--surface); color: var(--muted); font-size: 0.85rem; }
    .badge { padding: 0.25rem 0.5rem; border-radius: 6px; font-size: 0.8rem; font-weight: 600; }
    .badge-success { background: var(--success-soft); color: var(--success); }
    .badge-info { background: var(--info-soft); color: var(--info); }
    .badge.condition { background: var(--surface); color: var(--ink); border: 1px solid var(--border); }
    .btn { padding: 0.6rem 1rem; border-radius: 8px; border: none; cursor: pointer; font-weight: 500; font-size: 0.9rem; }
    .btn-sm { padding: 0.35rem 0.65rem; font-size: 0.8rem; }
    .btn-primary { background: var(--gold); color: var(--ink); }
    .btn-secondary { background: var(--surface); color: var(--ink); border: 1px solid var(--border); }
    .btn-gold { background: var(--gold); color: var(--ink); }
    .btn-outline { background: transparent; border: 1px solid var(--border); color: var(--ink); }
    .modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
    .modal-card { background: var(--surface-card); padding: 2rem; border-radius: 12px; max-width: 600px; width: 100%; max-height: 90vh; overflow-y: auto; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-top: 1rem; }
    .form-group { display: flex; flex-direction: column; gap: 0.3rem; }
    .form-group input, .form-group select { padding: 0.6rem; border-radius: 6px; border: 1px solid var(--border); }
    .modal-actions { display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1.5rem; }
    .text-center { text-align: center; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LaptopRetailPage implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly retailService = inject(LaptopRetailService);

  readonly activeTab = signal<'inventory' | 'repairs'>('inventory');
  readonly devices = signal<SerializedDevice[]>([]);
  readonly repairTickets = signal<RepairTicket[]>([]);
  readonly loadingDevices = signal(true);
  readonly loadingRepairs = signal(false);

  readonly showRegisterModal = signal(false);
  readonly showSellModal = signal(false);
  readonly selectedDeviceForSell = signal<SerializedDevice | null>(null);

  statusFilter = '';

  regForm: RegisterDeviceRequest = {
    serialNumber: '',
    brand: '',
    model: '',
    cpu: '',
    ramGb: 16,
    storageGb: 512,
    storageType: 'NVMe SSD',
    conditionGrade: 'NEW',
    purchasePrice: 0,
    sellingPrice: 0,
  };

  sellForm: SellDeviceRequest = {
    customerId: 'walk-in',
    customerName: '',
    warrantyMonths: 12,
    finalSellingPrice: 0,
  };

  ngOnInit() {
    this.loadDevices();
    this.loadRepairs();
  }

  loadDevices() {
    this.loadingDevices.set(true);
    this.retailService.listDevices({ status: this.statusFilter || undefined }).subscribe({
      next: (data) => {
        this.devices.set(data);
        this.loadingDevices.set(false);
      },
      error: () => {
        this.devices.set([]);
        this.loadingDevices.set(false);
      },
    });
  }

  loadRepairs() {
    this.loadingRepairs.set(true);
    this.retailService.listRepairTickets().subscribe({
      next: (data) => {
        this.repairTickets.set(data);
        this.loadingRepairs.set(false);
      },
      error: () => {
        this.repairTickets.set([]);
        this.loadingRepairs.set(false);
      },
    });
  }

  openRegisterModal() {
    this.regForm = {
      serialNumber: '',
      brand: '',
      model: '',
      cpu: '',
      ramGb: 16,
      storageGb: 512,
      storageType: 'NVMe SSD',
      conditionGrade: 'NEW',
      purchasePrice: 0,
      sellingPrice: 0,
    };
    this.showRegisterModal.set(true);
  }

  submitRegisterDevice() {
    this.retailService.registerDevice(this.regForm).subscribe({
      next: () => {
        this.showRegisterModal.set(false);
        this.loadDevices();
      },
    });
  }

  openSellModal(device: SerializedDevice) {
    this.selectedDeviceForSell.set(device);
    this.sellForm = {
      customerId: 'walk-in',
      customerName: '',
      warrantyMonths: 12,
      finalSellingPrice: device.sellingPrice,
    };
    this.showSellModal.set(true);
  }

  submitSellDevice() {
    const dev = this.selectedDeviceForSell();
    if (!dev) return;
    this.retailService.sellDevice(dev.id, this.sellForm).subscribe({
      next: () => {
        this.showSellModal.set(false);
        this.loadDevices();
      },
    });
  }

  returnDevice(device: SerializedDevice) {
    this.retailService.returnDevice(device.id, 'CUSTOMER_RETURN').subscribe({
      next: () => this.loadDevices(),
    });
  }

  openRepairModal() {
    this.activeTab.set('repairs');
  }
}
