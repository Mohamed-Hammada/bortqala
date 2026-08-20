import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { I18nService } from '../../core/i18n.service';
import { DeploymentService } from './deployment.service';
import { BackupSnapshot, DrRecoveryStatus } from './deployment.models';

@Component({
  selector: 'app-platform-backups',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './platform-backups.component.html',
  styleUrl: './platform-backups.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlatformBackupsComponent implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly deploymentService = inject(DeploymentService);

  readonly backups = signal<BackupSnapshot[]>([]);
  readonly drStatus = signal<DrRecoveryStatus | null>(null);
  readonly loading = signal(false);
  readonly actionLoading = signal<string | null>(null);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.error.set(null);
    this.deploymentService.listBackups().subscribe({
      next: (list) => {
        this.backups.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.message || 'Failed to load backups');
        this.loading.set(false);
      },
    });

    this.deploymentService.getDrStatus().subscribe({
      next: (dr) => this.drStatus.set(dr),
      error: () => {},
    });
  }

  triggerBackup(): void {
    this.actionLoading.set('TRIGGER');
    this.message.set(null);
    this.deploymentService.triggerBackup({ backupType: 'FULL' }).subscribe({
      next: (created) => {
        this.backups.update((list) => [created, ...list]);
        this.message.set(this.i18n.t('backups.backupCreated'));
        this.actionLoading.set(null);
      },
      error: (err) => {
        this.error.set(err?.message || 'Failed to trigger backup');
        this.actionLoading.set(null);
      },
    });
  }

  runDrill(snapshot: BackupSnapshot): void {
    this.actionLoading.set(snapshot.id);
    this.message.set(null);
    this.deploymentService.verifyDrill(snapshot.id).subscribe({
      next: (verified) => {
        this.backups.update((list) =>
          list.map((b) => (b.id === verified.id ? verified : b))
        );
        this.message.set(this.i18n.t('backups.drillSuccess'));
        this.actionLoading.set(null);
        // Refresh DR status
        this.deploymentService.getDrStatus().subscribe({
          next: (dr) => this.drStatus.set(dr),
        });
      },
      error: (err) => {
        this.error.set(err?.message || 'Failed to run restore drill');
        this.actionLoading.set(null);
      },
    });
  }
}
