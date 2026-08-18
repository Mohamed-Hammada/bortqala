import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { apiErrorMessage } from '../../core/api-error';

interface Issue {
  code: string;
  labelKey: string;
  route: string;
  count: number;
  blocker: boolean;
}

interface Step {
  key: string;
  sequence: number;
  optional: boolean;
  status: string;
}

interface Overview {
  packCode: string;
  setupProgress: number;
  dataQualityScore: number;
  readiness: string;
  assessedAt: number;
  issues: Issue[];
  steps: Step[];
}

interface Pack {
  code: string;
  nameKey: string;
  descriptionKey: string;
  availableVersion: number;
  installedVersion?: number;
  status: string;
}

@Component({
  selector: 'app-guided-onboarding',
  imports: [RouterLink],
  templateUrl: './guided-onboarding.component.html',
  styleUrl: './guided-onboarding.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GuidedOnboardingComponent {
  private readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  private readonly notifications = inject(NotificationService);

  readonly packs = signal<Pack[]>([]);
  readonly selectedPackCode = signal<string | null>(null);
  readonly overview = signal<Overview | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  constructor() {
    void this.load();
  }

  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      const allPacks = await firstValueFrom(this.http.get<Pack[]>('/api/v1/platform/industry-packs'));
      const installedPacks = allPacks.filter((p) => p.installedVersion != null || p.status === 'INSTALLED');
      this.packs.set(installedPacks);

      if (installedPacks.length === 0) {
        this.selectedPackCode.set(null);
        this.overview.set(null);
      } else {
        const currentSelected = this.selectedPackCode();
        const codeToLoad = currentSelected && installedPacks.some((p) => p.code === currentSelected)
          ? currentSelected
          : installedPacks[0].code;
        this.selectedPackCode.set(codeToLoad);
        await this.loadOverview(codeToLoad);
      }
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async selectPack(code: string) {
    if (this.selectedPackCode() === code) return;
    this.selectedPackCode.set(code);
    await this.loadOverview(code);
  }

  private async loadOverview(code: string) {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.overview.set(await firstValueFrom(this.http.get<Overview>(`/api/v1/platform/onboarding/${code}`)));
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async assess() {
    const code = this.selectedPackCode();
    if (!code || this.saving()) return;
    this.saving.set(true);
    this.error.set(null);
    try {
      this.overview.set(
        await firstValueFrom(
          this.http.post<Overview>(`/api/v1/platform/onboarding/${code}/assess`, {
            operationId: crypto.randomUUID(),
          })
        )
      );
      this.notifications.success(this.i18n.t('onboarding.assessed'));
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.saving.set(false);
    }
  }

  statusKey(value: string) {
    return `onboarding.status.${value.toLowerCase()}`;
  }

  format(value: number) {
    return value
      ? new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(value)
      : this.i18n.t('onboarding.notAssessed');
  }
}
