import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { TenantSetupService } from '../../../core/tenant/tenant-setup.service';
import { BusinessVertical } from '../../../core/tenant/tenant-setup.models';

interface VerticalCardOption {
  key: BusinessVertical;
  icon: string;
  titleKey: string;
  descKey: string;
  modules: string[];
}

@Component({
  selector: 'app-business-vertical-setup',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './business-vertical-setup.component.html',
  styleUrls: ['./business-vertical-setup.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BusinessVerticalSetupComponent implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly tenantSetupService = inject(TenantSetupService);
  private readonly notification = inject(NotificationService);

  readonly loading = signal<boolean>(true);
  readonly saving = signal<boolean>(false);
  readonly currentVertical = signal<BusinessVertical>('GENERAL');
  readonly selectedVertical = signal<BusinessVertical>('GENERAL');
  readonly provisionedGroups = signal<string[]>([]);

  readonly verticalOptions: VerticalCardOption[] = [
    {
      key: 'GENERAL',
      icon: '🏛️',
      titleKey: 'vertical.general',
      descKey: 'vertical.generalDesc',
      modules: ['Finance', 'Payroll', 'Trade', 'Procurement', 'Manufacturing', 'Contracting'],
    },
    {
      key: 'MEDICAL',
      icon: '🏥',
      titleKey: 'vertical.medical',
      descKey: 'vertical.medicalDesc',
      modules: ['Finance', 'Payroll', 'Trade / Billing', 'Medical Procurement', 'Quality'],
    },
    {
      key: 'CIVIL',
      icon: '🏗️',
      titleKey: 'vertical.civil',
      descKey: 'vertical.civilDesc',
      modules: ['Finance', 'Payroll', 'Project WBS & BOQ', 'DPR Logs', 'Contractor Claims'],
    },
    {
      key: 'RETAIL',
      icon: '🛒',
      titleKey: 'vertical.retail',
      descKey: 'vertical.retailDesc',
      modules: ['Finance', 'Payroll', 'Point of Sale (POS)', 'Van Sales', 'Inventory & Barcode'],
    },
    {
      key: 'MANUFACTURING',
      icon: '🏭',
      titleKey: 'vertical.manufacturing',
      descKey: 'vertical.manufacturingDesc',
      modules: ['Finance', 'Payroll', 'Multi-Level BOM', 'Work Orders & Routing', 'Quality & OEE'],
    },
    {
      key: 'SERVICES',
      icon: '💼',
      titleKey: 'vertical.services',
      descKey: 'vertical.servicesDesc',
      modules: ['Finance', 'Payroll', 'CRM Engagements', 'Milestone Billing', 'Timesheets'],
    },
  ];

  ngOnInit(): void {
    this.loadCurrentSetup();
  }

  loadCurrentSetup(): void {
    this.loading.set(true);
    this.tenantSetupService.getVerticalSetup().subscribe({
      next: (res) => {
        this.currentVertical.set(res.vertical);
        this.selectedVertical.set(res.vertical);
        this.provisionedGroups.set(res.provisionedPolicyGroups || []);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  selectVertical(vert: BusinessVertical): void {
    this.selectedVertical.set(vert);
  }

  applyVertical(): void {
    const vert = this.selectedVertical();
    this.saving.set(true);

    this.tenantSetupService.configureVertical(vert).subscribe({
      next: (res) => {
        this.currentVertical.set(res.vertical);
        this.provisionedGroups.set(res.provisionedPolicyGroups || []);
        this.saving.set(false);
        this.notification.success(this.i18n.t('vertical.saveSuccess'));
      },
      error: (err) => {
        this.saving.set(false);
        this.notification.error(err.message || 'Failed to configure vertical');
      },
    });
  }
}
