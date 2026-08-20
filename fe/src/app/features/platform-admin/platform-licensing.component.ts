import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../core/i18n.service';
import { DeploymentService } from './deployment.service';
import { LicenseStatus } from './deployment.models';

@Component({
  selector: 'app-platform-licensing',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './platform-licensing.component.html',
  styleUrl: './platform-licensing.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlatformLicensingComponent implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly deploymentService = inject(DeploymentService);

  readonly license = signal<LicenseStatus | null>(null);
  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly showInstallModal = signal(false);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);

  licenseKeyInput = '';
  payloadInput = '';
  signatureInput = '';
  fingerprintInput = '';

  ngOnInit(): void {
    this.loadLicense();
  }

  loadLicense(): void {
    this.loading.set(true);
    this.error.set(null);
    this.deploymentService.getLicenseStatus().subscribe({
      next: (status) => {
        this.license.set(status);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.message || 'Failed to load license status');
        this.loading.set(false);
      },
    });
  }

  validate(): void {
    this.loading.set(true);
    this.message.set(null);
    this.deploymentService.validateLicense().subscribe({
      next: (status) => {
        this.license.set(status);
        this.message.set(this.i18n.t('licensing.certValid'));
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.message || 'Failed to validate certificate');
        this.loading.set(false);
      },
    });
  }

  submitInstall(): void {
    if (!this.licenseKeyInput.trim()) return;

    this.submitting.set(true);
    this.error.set(null);
    this.message.set(null);

    this.deploymentService.installLicense({
      licenseKey: this.licenseKeyInput.trim(),
      certificatePayload: this.payloadInput.trim() || undefined,
      signatureEd25519: this.signatureInput.trim() || undefined,
      deviceFingerprintHash: this.fingerprintInput.trim() || undefined,
    }).subscribe({
      next: (status) => {
        this.license.set(status);
        this.message.set(this.i18n.t('licensing.certInstalled'));
        this.showInstallModal.set(false);
        this.submitting.set(false);
      },
      error: (err) => {
        this.error.set(err?.message || 'Failed to install certificate');
        this.submitting.set(false);
      },
    });
  }
}
