import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { I18nService } from '../../core/i18n.service';
import { DeploymentService } from './deployment.service';
import { DiagnosticsResponse } from './deployment.models';

@Component({
  selector: 'app-platform-diagnostics',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './platform-diagnostics.component.html',
  styleUrl: './platform-diagnostics.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlatformDiagnosticsComponent implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly deploymentService = inject(DeploymentService);

  readonly diagnostics = signal<DiagnosticsResponse | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.loadDiagnostics();
  }

  loadDiagnostics(): void {
    this.loading.set(true);
    this.error.set(null);
    this.deploymentService.getDiagnostics().subscribe({
      next: (data) => {
        this.diagnostics.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.message || 'Failed to load diagnostics');
        this.loading.set(false);
      },
    });
  }

  refresh(): void {
    this.loading.set(true);
    this.deploymentService.evaluateDiagnostics().subscribe({
      next: (data) => {
        this.diagnostics.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.message || 'Failed to refresh diagnostics');
        this.loading.set(false);
      },
    });
  }
}
