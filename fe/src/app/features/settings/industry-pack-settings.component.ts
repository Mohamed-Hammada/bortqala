import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { apiErrorMessage } from '../../core/api-error';

interface Step {
  id: string;
  key: string;
  sequence: number;
  prerequisiteKey?: string;
  optional: boolean;
  status: string;
  version: number;
}

interface RoleReadiness {
  code: string;
  required: boolean;
  available: boolean;
  assignedUsers: number;
  status: string;
}

interface TemplateDescriptor {
  key: string;
  fileName: string;
  workflow: string;
  downloadable: boolean;
  route: string;
}

interface Pack {
  code: string;
  nameKey: string;
  descriptionKey: string;
  availableVersion: number;
  installedVersion?: number;
  upgradeAvailable: boolean;
  status: string;
  requiredFeatures: string[];
  defaultRoles: string[];
  kpis: string[];
  importTemplates: string[];
  settingsJson?: string;
  customized: boolean;
  goLiveReady: boolean;
  version: number;
  steps: Step[];
  roleReadiness?: RoleReadiness[];
  templateBindings?: TemplateDescriptor[];
}

interface TypedSettings {
  dashboard?: string;
  issuePolicy?: string;
  creditControl?: boolean;
  expiryWindowsDaysStr?: string;
  terminologyEmployee?: string;
  terminologySupplier?: string;
}

@Component({
  selector: 'app-industry-pack-settings',
  imports: [FormsModule, RouterLink],
  templateUrl: './industry-pack-settings.component.html',
  styleUrl: './industry-pack-settings.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IndustryPackSettingsComponent {
  private http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  private notification = inject(NotificationService);

  readonly packs = signal<Pack[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly rawSettings = signal<Record<string, string>>({});
  readonly typedSettings = signal<Record<string, TypedSettings>>({});
  readonly advancedMode = signal<Record<string, boolean>>({});

  constructor() {
    void this.load();
  }

  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      const packs = await firstValueFrom(this.http.get<Pack[]>('/api/v1/platform/industry-packs'));
      this.packs.set(packs);
      const rawMap: Record<string, string> = {};
      const typedMap: Record<string, TypedSettings> = {};
      for (const p of packs) {
        const jsonStr = p.settingsJson ?? '{}';
        rawMap[p.code] = jsonStr;
        typedMap[p.code] = this.parseTyped(jsonStr, p.code);
      }
      this.rawSettings.set(rawMap);
      this.typedSettings.set(typedMap);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  private parseTyped(jsonStr: string, code: string): TypedSettings {
    try {
      const parsed = JSON.parse(jsonStr || '{}');
      return {
        dashboard: parsed.dashboard ?? (code === 'FOOD_DISTRIBUTION_EG' ? 'foodDistribution' : 'workforce'),
        issuePolicy: parsed.issuePolicy ?? 'FEFO',
        creditControl: parsed.creditControl ?? true,
        expiryWindowsDaysStr: Array.isArray(parsed.expiryWindowsDays) ? parsed.expiryWindowsDays.join(', ') : '7, 30, 60',
        terminologyEmployee: parsed.terminology?.employee ?? (code === 'CONTRACTOR_WORKFORCE_EG' ? 'worker' : 'employee'),
        terminologySupplier: parsed.terminology?.supplier ?? (code === 'CONTRACTOR_WORKFORCE_EG' ? 'contractor' : 'supplier'),
      };
    } catch {
      return {
        dashboard: code === 'FOOD_DISTRIBUTION_EG' ? 'foodDistribution' : 'workforce',
        issuePolicy: 'FEFO',
        creditControl: true,
        expiryWindowsDaysStr: '7, 30, 60',
      };
    }
  }

  setRawSettings(code: string, value: string) {
    this.rawSettings.update((s) => ({ ...s, [code]: value }));
  }

  updateTyped(code: string, partial: Partial<TypedSettings>) {
    this.typedSettings.update((map) => {
      const updated = { ...map[code], ...partial };
      const raw = this.serializeTyped(code, updated);
      this.rawSettings.update((s) => ({ ...s, [code]: raw }));
      return { ...map, [code]: updated };
    });
  }

  private serializeTyped(code: string, typed: TypedSettings): string {
    if (code === 'FOOD_DISTRIBUTION_EG') {
      const windows = (typed.expiryWindowsDaysStr ?? '7, 30, 60')
        .split(',')
        .map((x) => parseInt(x.trim(), 10))
        .filter((n) => !isNaN(n) && n > 0);
      return JSON.stringify(
        {
          dashboard: 'foodDistribution',
          issuePolicy: typed.issuePolicy || 'FEFO',
          creditControl: typed.creditControl ?? true,
          expiryWindowsDays: windows.length ? windows : [7, 30, 60],
        },
        null,
        2
      );
    } else {
      return JSON.stringify(
        {
          dashboard: 'workforce',
          terminology: {
            employee: typed.terminologyEmployee || 'worker',
            supplier: typed.terminologySupplier || 'contractor',
          },
        },
        null,
        2
      );
    }
  }

  toggleAdvancedMode(code: string) {
    this.advancedMode.update((m) => ({ ...m, [code]: !m[code] }));
  }

  async install(pack: Pack) {
    if (this.saving()) return;
    this.saving.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(
        this.http.post(`/api/v1/platform/industry-packs/${pack.code}/install`, {
          operationId: crypto.randomUUID(),
        })
      );
      this.notification.success(this.i18n.t('industryPack.installSuccess'));
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.saving.set(false);
    }
  }

  async upgrade(pack: Pack) {
    if (this.saving()) return;
    this.saving.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(
        this.http.post(`/api/v1/platform/industry-packs/${pack.code}/upgrade`, {
          operationId: crypto.randomUUID(),
          expectedVersion: pack.version,
        })
      );
      this.notification.success(this.i18n.t('industryPack.upgradeSuccess'));
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.saving.set(false);
    }
  }

  async reconcile(pack: Pack) {
    if (this.saving()) return;
    this.saving.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(
        this.http.post(`/api/v1/platform/industry-packs/${pack.code}/reconcile`, {
          operationId: crypto.randomUUID(),
          reason: 'Manual admin reconciliation',
        })
      );
      this.notification.success(this.i18n.t('industryPack.reconciled'));
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.saving.set(false);
    }
  }

  async complete(pack: Pack, step: Step, skip = false) {
    if (this.saving()) return;
    this.saving.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(
        this.http.post(`/api/v1/platform/industry-packs/${pack.code}/steps/${encodeURIComponent(step.key)}`, {
          skip,
          expectedVersion: step.version,
        })
      );
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.saving.set(false);
    }
  }

  async saveSettings(pack: Pack) {
    if (this.saving()) return;
    this.saving.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(
        this.http.put(`/api/v1/platform/industry-packs/${pack.code}/settings`, {
          settingsJson: this.rawSettings()[pack.code],
          expectedVersion: pack.version,
        })
      );
      this.notification.success(this.i18n.t('industryPack.settingsSaved'));
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.saving.set(false);
    }
  }
}
