import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import {
  CreateLabOrderPayload,
  EnterLabResultPayload,
  LabOrder,
  LabTestItem,
  SaveLabTestItemPayload,
  SendOutLabOrderPayload,
} from './clinic.models';
import { ClinicService } from './clinic.service';

@Component({
  selector: 'app-lab-orders-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './lab-orders.page.html',
  styleUrls: ['./lab-orders.page.scss'],
})
export class LabOrdersPageComponent implements OnInit {
  private readonly clinicService = inject(ClinicService);
  private readonly notificationService = inject(NotificationService);
  readonly i18n = inject(I18nService);

  readonly activeTab = signal<'WORKLIST' | 'CATALOG' | 'AGING'>('WORKLIST');
  readonly statusFilter = signal<string>('');
  readonly loading = signal<boolean>(false);
  readonly saving = signal<boolean>(false);

  readonly categories = ['LAB', 'IMAGING'] as const;
  readonly resultFlags = ['NORMAL', 'LOW', 'HIGH', 'CRITICAL'] as const;
  readonly statusOptions = ['ORDERED', 'COLLECTED', 'SENT_OUT', 'RESULTED', 'VALIDATED', 'CANCELLED'] as const;

  // Worklist
  readonly orders = signal<LabOrder[]>([]);
  readonly agingOrders = signal<LabOrder[]>([]);

  // Catalog
  readonly labTests = signal<LabTestItem[]>([]);
  readonly showTestModal = signal<boolean>(false);
  testForm: SaveLabTestItemPayload = {
    code: '',
    category: 'LAB',
    name: '',
    sampleType: 'BLOOD',
    normalRangeText: '',
    price: 100,
  };

  // Create Order Modal
  readonly showOrderModal = signal<boolean>(false);
  orderForm: CreateLabOrderPayload = {
    patientId: '',
    visitId: '',
    doctorEmployeeId: '',
    testId: '',
    externalLabPartyId: '',
    externalLabName: '',
  };

  // Result Entry Modal
  readonly showResultModal = signal<boolean>(false);
  readonly activeResultOrderId = signal<string>('');
  resultForm: EnterLabResultPayload = {
    resultValueText: '',
    resultFlag: 'NORMAL',
    resultNotes: '',
    attachmentId: '',
    attachmentFilename: '',
  };

  ngOnInit(): void {
    this.loadOrders();
    this.loadCatalog();
  }

  setTab(tab: 'WORKLIST' | 'CATALOG' | 'AGING'): void {
    this.activeTab.set(tab);
    if (tab === 'WORKLIST') {
      this.loadOrders();
    } else if (tab === 'CATALOG') {
      this.loadCatalog();
    } else if (tab === 'AGING') {
      this.loadAging();
    }
  }

  loadOrders(): void {
    this.loading.set(true);
    this.clinicService.getAllLabOrders(this.statusFilter() || undefined).subscribe({
      next: (orders) => {
        this.orders.set(orders);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  loadCatalog(): void {
    this.clinicService.getAllLabTests().subscribe({
      next: (tests) => this.labTests.set(tests),
    });
  }

  loadAging(): void {
    this.loading.set(true);
    this.clinicService.getAgingSentOutOrders().subscribe({
      next: (orders) => {
        this.agingOrders.set(orders);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  openAddTestModal(): void {
    this.testForm = {
      code: 'TEST-' + Date.now().toString().slice(-4),
      category: 'LAB',
      name: '',
      sampleType: 'BLOOD',
      normalRangeText: '',
      price: 150,
    };
    this.showTestModal.set(true);
  }

  saveTestItem(): void {
    if (!this.testForm.name || !this.testForm.code) return;
    this.saving.set(true);
    this.clinicService.saveLabTest(this.testForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showTestModal.set(false);
        this.loadCatalog();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
      error: () => this.saving.set(false),
    });
  }

  openCreateOrderModal(): void {
    this.orderForm = {
      patientId: '',
      visitId: '',
      doctorEmployeeId: '',
      testId: this.labTests().length > 0 ? this.labTests()[0].id : '',
      externalLabPartyId: '',
      externalLabName: '',
    };
    this.showOrderModal.set(true);
  }

  submitCreateOrder(): void {
    if (!this.orderForm.patientId || !this.orderForm.testId || !this.orderForm.doctorEmployeeId) return;
    this.saving.set(true);
    this.clinicService.createLabOrder(this.orderForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showOrderModal.set(false);
        this.loadOrders();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
      error: () => this.saving.set(false),
    });
  }

  collect(order: LabOrder): void {
    this.clinicService.collectSample(order.id).subscribe({
      next: () => {
        this.loadOrders();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
    });
  }

  sendOut(order: LabOrder): void {
    const payload: SendOutLabOrderPayload = {
      externalLabPartyId: 'ext-lab-1',
      externalLabName: 'Al-Borg Lab Reference',
    };
    this.clinicService.sendOutOrder(order.id, payload).subscribe({
      next: () => {
        this.loadOrders();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
    });
  }

  openEnterResult(order: LabOrder): void {
    this.activeResultOrderId.set(order.id);
    this.resultForm = {
      resultValueText: order.resultValueText || '',
      resultFlag: order.resultFlag || 'NORMAL',
      resultNotes: order.resultNotes || '',
      attachmentId: order.attachmentId || '',
      attachmentFilename: order.attachmentFilename || '',
    };
    this.showResultModal.set(true);
  }

  saveResult(): void {
    const id = this.activeResultOrderId();
    if (!id || !this.resultForm.resultValueText) return;
    this.saving.set(true);
    this.clinicService.enterLabResult(id, this.resultForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showResultModal.set(false);
        this.loadOrders();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
      error: () => this.saving.set(false),
    });
  }

  validate(order: LabOrder): void {
    this.clinicService.validateLabOrder(order.id).subscribe({
      next: () => {
        this.loadOrders();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
    });
  }

  cancel(order: LabOrder): void {
    this.clinicService.cancelLabOrder(order.id).subscribe({
      next: () => {
        this.loadOrders();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
    });
  }

  ackCritical(order: LabOrder): void {
    this.clinicService.acknowledgeCritical(order.id).subscribe({
      next: () => {
        this.loadOrders();
        this.notificationService.success(this.i18n.t('clinic.resultSuccess'));
      },
    });
  }
}
