import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { WorkforceService } from '../../workforce/data-access/workforce.service';
import { AdvancePolicy } from '../../workforce/models/workforce.models';

interface CategoryOption {
  id: string;
  name: string;
}

interface ExceptionDraft {
  categoryId: string;
  categoryName: string;
  mode: 'AUTO' | 'MANUAL';
  cadence: 'MONTHLY' | 'MID_MONTH_SPLIT';
  persistedVersion: number | null;
}

type Mode = 'AUTO' | 'MANUAL';
type Cadence = 'MONTHLY' | 'MID_MONTH_SPLIT';

@Component({
  selector: 'app-advances-policy-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './advances-policy-settings.component.html',
  styleUrls: ['./advances-policy-settings.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdvancesPolicySettingsComponent implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly http = inject(HttpClient);
  private readonly notification = inject(NotificationService);
  private readonly workforceService = inject(WorkforceService);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly categories = signal<CategoryOption[]>([]);

  readonly globalMode = signal<Mode>('AUTO');
  readonly globalCadence = signal<Cadence>('MONTHLY');
  readonly exceptions = signal<ExceptionDraft[]>([]);

  private baselineGlobal: { mode: Mode; cadence: Cadence } = { mode: 'AUTO', cadence: 'MONTHLY' };
  private baselineExceptions: ExceptionDraft[] = [];

  readonly dirty = computed(() => {
    if (this.globalMode() !== this.baselineGlobal.mode || this.globalCadence() !== this.baselineGlobal.cadence) {
      return true;
    }
    const current = this.exceptions();
    if (current.length !== this.baselineExceptions.length) return true;
    return current.some((item, index) => {
      const base = this.baselineExceptions[index];
      return !base || base.categoryId !== item.categoryId || base.mode !== item.mode || base.cadence !== item.cadence;
    });
  });

  readonly availableCategories = computed(() => {
    const used = new Set(this.exceptions().map((item) => item.categoryId));
    return this.categories().filter((category) => !used.has(category.id));
  });

  async ngOnInit(): Promise<void> {
    try {
      const [categories, policies] = await Promise.all([
        firstValueFrom(this.http.get<Array<{ id: string; name: string }>>('/api/v1/categories')),
        firstValueFrom(this.workforceService.loadAdvancePolicies()),
      ]);
      this.categories.set(categories.filter((category) => category.active));
      this.hydrate(policies);
    } catch {
      this.notification.error(this.i18n.t('settings.advancesPolicyLoadFailed'));
    } finally {
      this.loading.set(false);
    }
  }

  setGlobalMode(value: Mode): void {
    this.globalMode.set(value);
  }

  setGlobalCadence(value: Cadence): void {
    this.globalCadence.set(value);
  }

  addException(): void {
    const candidate = this.availableCategories()[0];
    if (!candidate) return;
    this.exceptions.update((items) => [
      ...items,
      { categoryId: candidate.id, categoryName: candidate.name, mode: 'AUTO', cadence: 'MONTHLY', persistedVersion: null },
    ]);
  }

  removeException(categoryId: string): void {
    this.exceptions.update((items) => items.filter((item) => item.categoryId !== categoryId));
  }

  updateException(categoryId: string, patch: Partial<ExceptionDraft>): void {
    this.exceptions.update((items) =>
      items.map((item) => (item.categoryId === categoryId ? { ...item, ...patch } : item)),
    );
  }

  cancel(): void {
    this.globalMode.set(this.baselineGlobal.mode);
    this.globalCadence.set(this.baselineGlobal.cadence);
    this.exceptions.set([...this.baselineExceptions.map((item) => ({ ...item }))]);
  }

  async saveAll(): Promise<void>
  {
    if (!this.dirty() || this.saving()) return;
    this.saving.set(true);
    try {
      const today = new Date().toISOString().slice(0, 10);
      const jobs: Array<Promise<unknown>> = [];
      if (
        this.globalMode() !== this.baselineGlobal.mode ||
        this.globalCadence() !== this.baselineGlobal.cadence
      ) {
        jobs.push(firstValueFrom(this.workforceService.saveAdvancePolicy(this.policyPayload(
          'GLOBAL', null, this.globalMode(), this.globalCadence(), null, today))));
      }
      for (const item of this.exceptions()) {
        const baseline = this.baselineExceptions.find((base) => base.categoryId === item.categoryId);
        const changed = !baseline || baseline.mode !== item.mode || baseline.cadence !== item.cadence;
        if (!changed) continue;
        jobs.push(firstValueFrom(this.workforceService.saveAdvancePolicy(this.policyPayload(
          'EMPLOYEE_CATEGORY', item.categoryId, item.mode, item.cadence, baseline?.persistedVersion ?? null, today))));
      }
      await Promise.all(jobs);
      const policies = await firstValueFrom(this.workforceService.loadAdvancePolicies());
      this.hydrate(policies);
      this.notification.success(this.i18n.t('settings.advancesPolicySaved'));
    } catch {
      this.notification.error(this.i18n.t('settings.advancesPolicySaveFailed'));
    } finally {
      this.saving.set(false);
    }
  }

  private policyPayload(
    scopeType: 'GLOBAL' | 'EMPLOYEE_CATEGORY',
    scopeId: string | null,
    mode: Mode,
    cadence: Cadence,
    persistedVersion: number | null,
    effectiveFrom: string,
  ): AdvancePolicy {
    return {
      scopeType,
      scopeId: scopeId ?? undefined,
      deductionMode: mode,
      deductionFrequency: cadence,
      maxDeductionPercent: 50,
      defaultInstallments: 1,
      deferralPeriods: 0,
      version: persistedVersion != null ? persistedVersion + 1 : 1,
      effectiveFrom,
      active: true,
    };
  }

  private hydrate(policies: AdvancePolicy[]): void {
    const latestByScope = new Map<string, AdvancePolicy>();
    for (const policy of policies) {
      const key = `${policy.scopeType}:${policy.scopeId ?? ''}`;
      const existing = latestByScope.get(key);
      if (!existing || policy.version >= existing.version) latestByScope.set(key, policy);
    }
    const globalPolicy = latestByScope.get('GLOBAL:');
    const modeOf = (policy: AdvancePolicy | undefined): Mode =>
      policy && policy.deductionMode === 'MANUAL' ? 'MANUAL' : 'AUTO';
    const cadenceOf = (policy: AdvancePolicy | undefined): Cadence => {
      if (policy && policy.deductionFrequency === 'MONTHLY') return 'MONTHLY';
      if (policy && policy.deductionFrequency === 'MID_MONTH_SPLIT') return 'MID_MONTH_SPLIT';
      return 'MONTHLY';
    };
    this.baselineGlobal = { mode: modeOf(globalPolicy), cadence: cadenceOf(globalPolicy) };
    this.globalMode.set(this.baselineGlobal.mode);
    this.globalCadence.set(this.baselineGlobal.cadence);

    const categoryName = (id: string): string =>
      this.categories().find((category) => category.id === id)?.name ?? id;
    const exceptionPolicies = [...latestByScope.values()]
      .filter((policy) => policy.scopeType === 'EMPLOYEE_CATEGORY' && policy.scopeId)
      .sort((left, right) => (left.scopeName ?? '').localeCompare(right.scopeName ?? ''));
    this.baselineExceptions = exceptionPolicies.map((policy) => ({
      categoryId: policy.scopeId as string,
      categoryName: policy.scopeName && policy.scopeName !== '—'
        ? policy.scopeName
        : categoryName(policy.scopeId as string),
      mode: modeOf(policy),
      cadence: cadenceOf(policy),
      persistedVersion: policy.version,
    }));
    this.exceptions.set(this.baselineExceptions.map((item) => ({ ...item })));
  }
}
