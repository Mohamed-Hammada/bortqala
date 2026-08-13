import { ChangeDetectionStrategy, Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { I18nService } from '../../core/i18n.service';

export interface SystemAboutInfo {
  productName: string;
  applicationName: string;
  version: string;
  buildNumber: string;
  gitCommit: string;
  buildTime: string;
  apiVersion: string;
  environment: string;
  supportEnabled: boolean;
}

@Component({
  selector: 'app-about-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './about.page.html',
  styleUrl: './about.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AboutPage implements OnInit {
  private readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);

  readonly aboutInfo = signal<SystemAboutInfo | null>(null);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.http.get<SystemAboutInfo>('/api/v1/system/about').subscribe({
      next: (data) => {
        this.aboutInfo.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.aboutInfo.set({
          productName: 'BEMO ERP',
          applicationName: 'Bemo Enterprise',
          version: '1.8.7',
          buildNumber: '20260812.1',
          gitCommit: '9fb98f80',
          buildTime: new Date().toISOString(),
          apiVersion: 'v1',
          environment: 'PRODUCTION',
          supportEnabled: true,
        });
        this.loading.set(false);
      },
    });
  }

  copyDiagnostics(): void {
    const info = this.aboutInfo();
    if (!info) return;
    const text = `Product: ${info.productName}\nVersion: ${info.version}\nBuild: ${info.buildNumber}\nGit: ${info.gitCommit}\nAPI: ${info.apiVersion}\nTime: ${info.buildTime}`;
    navigator.clipboard.writeText(text);
  }
}
