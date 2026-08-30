import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { ServiceOpsService } from './service-ops.service';
import {
  BookableResource,
  RentalContract,
  RentalItem,
  RentalRateUnit,
  ResourceBooking,
  ResourceKind,
  WorkOrder,
  WorkOrderPriority,
  WorkOrderStatus,
} from './service-ops.models';

@Component({
  selector: 'app-service-ops-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './service-ops.page.html',
  styleUrl: './service-ops.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ServiceOpsPageComponent implements OnInit {
  readonly i18n = inject(I18nService);
  readonly serviceOps = inject(ServiceOpsService);
  private readonly toast = inject(NotificationService);

  readonly activeTab = signal<'rentals' | 'work-orders' | 'bookings'>('rentals');

  // Modals
  readonly showNewItemModal = signal(false);
  readonly showNewContractModal = signal(false);
  readonly showCloseContractModal = signal(false);
  readonly selectedContract = signal<RentalContract | null>(null);

  readonly showNewWorkOrderModal = signal(false);
  readonly showAddLaborModal = signal(false);
  readonly showAddPartModal = signal(false);
  readonly selectedWorkOrder = signal<WorkOrder | null>(null);

  readonly showNewResourceModal = signal(false);
  readonly showNewBookingModal = signal(false);

  // Form Models
  readonly itemForm = signal({
    code: '',
    name: '',
    nameEn: '',
    category: '',
    rateDaily: 0,
    rateWeekly: 0,
    rateMonthly: 0,
    depositAmount: 0,
  });

  readonly contractForm = signal({
    contractNo: '',
    customerPartyId: '',
    startDate: '',
    expectedEndDate: '',
    rateUnit: 'DAY' as RentalRateUnit,
    rateAmount: 0,
    depositAmount: 0,
    notes: '',
    selectedItemId: '',
  });

  readonly closeContractForm = signal({
    actualEndDate: '',
    damageFee: 0,
    notes: '',
  });

  readonly workOrderForm = signal({
    ticketNo: '',
    customerPartyId: '',
    customerName: '',
    title: '',
    description: '',
    assignedEmployeeId: '',
    priority: 'NORMAL' as WorkOrderPriority,
    promisedAt: '',
  });

  readonly laborForm = signal({
    description: '',
    hours: 1,
    hourlyRate: 100,
  });

  readonly partsForm = signal({
    itemCode: '',
    itemName: '',
    quantity: 1,
    unitPrice: 50,
  });

  readonly resourceForm = signal({
    code: '',
    name: '',
    nameEn: '',
    kind: 'ROOM' as ResourceKind,
    capacity: 10,
    location: '',
  });

  readonly bookingForm = signal({
    resourceId: '',
    title: '',
    customerPartyId: '',
    customerName: '',
    startTime: '',
    endTime: '',
    notes: '',
  });

  ngOnInit(): void {
    this.loadData();
  }

  async loadData(): Promise<void> {
    if (this.activeTab() === 'rentals') {
      await this.serviceOps.loadRentals();
    } else if (this.activeTab() === 'work-orders') {
      await this.serviceOps.loadWorkOrders();
    } else if (this.activeTab() === 'bookings') {
      await this.serviceOps.loadBookings();
    }
  }

  async setTab(tab: 'rentals' | 'work-orders' | 'bookings'): Promise<void> {
    this.activeTab.set(tab);
    await this.loadData();
  }


  // --- Rentals Actions ---

  async saveRentalItem(): Promise<void> {
    const res = await this.serviceOps.createRentalItem(this.itemForm());
    if (res) {
      this.toast.success(this.i18n.t('common.save'));
      this.showNewItemModal.set(false);
    }
  }

  async saveRentalContract(): Promise<void> {
    const f = this.contractForm();
    const payload = {
      contractNo: f.contractNo,
      customerPartyId: f.customerPartyId,
      startDate: f.startDate,
      expectedEndDate: f.expectedEndDate,
      rateUnit: f.rateUnit,
      rateAmount: f.rateAmount,
      depositAmount: f.depositAmount,
      notes: f.notes,
      lines: f.selectedItemId ? [{ rentalItemId: f.selectedItemId, quantity: 1, unitRate: f.rateAmount }] : [],
    };
    const res = await this.serviceOps.createRentalContract(payload);
    if (res) {
      this.toast.success(this.i18n.t('common.save'));
      this.showNewContractModal.set(false);
    }
  }

  async activateContract(contract: RentalContract): Promise<void> {
    const ok = await this.serviceOps.activateContract(contract.id);
    if (ok) {
      this.toast.success(this.i18n.t('common.success'));
    }
  }

  openCloseContract(contract: RentalContract): void {
    this.selectedContract.set(contract);
    this.closeContractForm.set({
      actualEndDate: new Date().toISOString().split('T')[0],
      damageFee: 0,
      notes: '',
    });
    this.showCloseContractModal.set(true);
  }

  async submitCloseContract(): Promise<void> {
    const contract = this.selectedContract();
    if (!contract) return;
    const ok = await this.serviceOps.returnAndCloseContract(contract.id, this.closeContractForm());
    if (ok) {
      this.toast.success(this.i18n.t('rental.returnAndClose'));
      this.showCloseContractModal.set(false);
    }
  }

  // --- Work Orders Actions ---

  async saveWorkOrder(): Promise<void> {
    const res = await this.serviceOps.createWorkOrder(this.workOrderForm());
    if (res) {
      this.toast.success(this.i18n.t('common.save'));
      this.showNewWorkOrderModal.set(false);
    }
  }

  openLaborModal(wo: WorkOrder): void {
    this.selectedWorkOrder.set(wo);
    this.laborForm.set({ description: '', hours: 1, hourlyRate: 100 });
    this.showAddLaborModal.set(true);
  }

  async submitLabor(): Promise<void> {
    const wo = this.selectedWorkOrder();
    if (!wo) return;
    const ok = await this.serviceOps.addLaborLine(wo.id, this.laborForm());
    if (ok) {
      this.toast.success(this.i18n.t('workOrder.addLabor'));
      this.showAddLaborModal.set(false);
    }
  }

  openPartsModal(wo: WorkOrder): void {
    this.selectedWorkOrder.set(wo);
    this.partsForm.set({ itemCode: '', itemName: '', quantity: 1, unitPrice: 50 });
    this.showAddPartModal.set(true);
  }

  async submitParts(): Promise<void> {
    const wo = this.selectedWorkOrder();
    if (!wo) return;
    const ok = await this.serviceOps.addPartsLine(wo.id, this.partsForm());
    if (ok) {
      this.toast.success(this.i18n.t('workOrder.addPart'));
      this.showAddPartModal.set(false);
    }
  }

  async setWorkOrderStatus(wo: WorkOrder, status: WorkOrderStatus): Promise<void> {
    const ok = await this.serviceOps.updateWorkOrderStatus(wo.id, status);
    if (ok) {
      this.toast.success(this.i18n.t('common.success'));
    }
  }

  async deliverWorkOrder(wo: WorkOrder): Promise<void> {
    const ok = await this.serviceOps.deliverWorkOrder(wo.id);
    if (ok) {
      this.toast.success(this.i18n.t('workOrder.deliver'));
    }
  }

  // --- Bookings Actions ---

  async saveResource(): Promise<void> {
    const res = await this.serviceOps.createResource(this.resourceForm());
    if (res) {
      this.toast.success(this.i18n.t('common.save'));
      this.showNewResourceModal.set(false);
    }
  }

  async saveBooking(): Promise<void> {
    const f = this.bookingForm();
    const startMs = new Date(f.startTime).getTime();
    const endMs = new Date(f.endTime).getTime();
    const payload = {
      resourceId: f.resourceId,
      title: f.title,
      customerPartyId: f.customerPartyId,
      customerName: f.customerName,
      startTime: startMs,
      endTime: endMs,
      notes: f.notes,
    };
    const res = await this.serviceOps.createBooking(payload);
    if (res) {
      this.toast.success(this.i18n.t('booking.newBooking'));
      this.showNewBookingModal.set(false);
    }
  }

  async cancelBooking(booking: ResourceBooking): Promise<void> {
    const ok = await this.serviceOps.cancelBooking(booking.id);
    if (ok) {
      this.toast.success(this.i18n.t('common.cancel'));
    }
  }
}
