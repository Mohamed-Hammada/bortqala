import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { apiErrorMessage } from '../../core/api-error';
import { downloadBlob } from '../../core/download';
import { ModalDialogComponent } from '../../shared/ui/modal-dialog/modal-dialog.component';
import {
  CreatePacketStep,
  ManifestExport,
  PacketStatus,
  SignatureMethod,
  SignaturePacket,
  SignatureStep,
  STEPS_SORTED,
} from './esign.models';
import { ESignService } from './esign.service';

interface SignerRow {
  signerName: string;
  signerUserId: string;
  roleLabel: string;
}

@Component({
  selector: 'app-esign-page',
  imports: [ReactiveFormsModule, ModalDialogComponent],
  templateUrl: './esign.page.html',
  styleUrl: './esign.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ESignPage {
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  readonly esign = inject(ESignService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly createOpen = signal(false);
  readonly signOpen = signal(false);
  readonly signTarget = signal<SignatureStep | null>(null);
  readonly signPacketId = signal<string | null>(null);
  readonly declineOpen = signal(false);
  readonly declineTarget = signal<SignatureStep | null>(null);
  readonly manifestText = signal<string | null>(null);
  readonly statusFilter = signal<PacketStatus | ''>('');

  readonly packets = signal<SignaturePacket[]>([]);
  readonly signers = signal<SignerRow[]>([]);

  readonly packetForm = new FormGroup({
    title: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(300)] }),
    documentName: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(300)] }),
    contentHash: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(32)] }),
  });

  readonly signForm = new FormGroup({
    contentSha256: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    method: new FormControl<SignatureMethod>('DRAWN', { nonNullable: true }),
  });

  readonly declineForm = new FormGroup({
    reason: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(1000)] }),
  });

  readonly pendingForMe = computed(
    () => this.packets().filter((packet) => packet.status === 'IN_PROGRESS').length,
  );

  readonly filteredPackets = computed(() => {
    const status = this.statusFilter();
    if (!status) return this.packets();
    return this.packets().filter((packet) => packet.status === status);
  });

  constructor() {
    void this.load();
  }

  onFilterChange(status: string): void {
    this.statusFilter.set((status || '') as PacketStatus | '');
    void this.load();
  }

  onSignerInput(index: number, field: keyof SignerRow, event: Event): void {
    this.updateSigner(index, field, (event.target as HTMLInputElement).value);
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const packets = await firstValueFrom(this.esign.listPackets(this.statusFilter() || undefined));
      this.packets.set(packets ?? []);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  sortedSteps(packet: SignaturePacket): SignatureStep[] {
    return STEPS_SORTED(packet.steps);
  }

  packetStatusLabel(status: PacketStatus): string {
    return this.i18n.t(`esign.status.${status}`);
  }

  stepStatusLabel(status: SignatureStep['status']): string {
    return this.i18n.t(`esign.stepStatus.${status}`);
  }

  currentStep(packet: SignaturePacket): SignatureStep | null {
    return this.sortedSteps(packet).find((step) => step.status === 'PENDING') ?? null;
  }

  formattedDate(epoch: number | null): string {
    if (!epoch) return '—';
    return new Date(epoch).toLocaleString(this.i18n.locale() === 'ar-EG' ? 'ar-EG' : 'en-US');
  }

  // ---- Create ----

  openCreate(): void {
    this.createOpen.set(true);
    this.packetForm.reset({ title: '', documentName: '', contentHash: '' });
    this.signers.set([{ signerName: '', signerUserId: '', roleLabel: '' }]);
  }

  closeCreate(): void {
    this.createOpen.set(false);
  }

  addSigner(): void {
    this.signers.update((rows) => [...rows, { signerName: '', signerUserId: '', roleLabel: '' }]);
  }

  removeSigner(index: number): void {
    this.signers.update((rows) => rows.filter((_, i) => i !== index));
  }

  updateSigner(index: number, field: keyof SignerRow, value: string): void {
    this.signers.update((rows) => rows.map((row, i) => (i === index ? { ...row, [field]: value } : row)));
  }

  signerRequest(): CreatePacketStep[] {
    return this.signers()
      .filter((row) => row.signerName.trim().length > 0)
      .map((row, index) => ({
        stepOrder: index + 1,
        signerName: row.signerName.trim(),
        signerUserId: row.signerUserId.trim() || undefined,
        roleLabel: row.roleLabel.trim() || undefined,
      }));
  }

  async submitPacket(): Promise<void> {
    if (this.packetForm.invalid || this.submitting()) return;
    const steps = this.signerRequest();
    if (steps.length === 0) {
      this.notification.error(this.i18n.t('esign.steps'));
      return;
    }
    this.submitting.set(true);
    const value = this.packetForm.getRawValue();
    try {
      const packet = await firstValueFrom(this.esign.createPacket({
        title: value.title,
        documentName: value.documentName || undefined,
        contentHash: value.contentHash,
        steps,
      }));
      await firstValueFrom(this.esign.startRouting(packet.id));
      this.createOpen.set(false);
      this.notification.success(this.i18n.t('esign.packetCreated'));
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.submitting.set(false);
    }
  }

  // ---- Sign / Decline ----

  openSign(packet: SignaturePacket, step: SignatureStep): void {
    this.signPacketId.set(packet.id);
    this.signTarget.set(step);
    this.signForm.reset({ contentSha256: packet.contentHash, method: 'DRAWN' });
    this.signOpen.set(true);
  }

  closeSign(): void {
    this.signOpen.set(false);
  }

  async submitSign(): Promise<void> {
    const packetId = this.signPacketId();
    const step = this.signTarget();
    if (!packetId || !step || this.signForm.invalid || this.submitting()) return;
    this.submitting.set(true);
    const value = this.signForm.getRawValue();
    try {
      await firstValueFrom(this.esign.signStep(packetId, step.stepOrder, {
        contentSha256: value.contentSha256,
        method: value.method,
      }));
      this.signOpen.set(false);
      this.notification.success(this.i18n.t('esign.signedSuccess'));
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.submitting.set(false);
    }
  }

  openDecline(packet: SignaturePacket, step: SignatureStep): void {
    this.signPacketId.set(packet.id);
    this.declineTarget.set(step);
    this.declineForm.reset({ reason: '' });
    this.declineOpen.set(true);
  }

  closeDecline(): void {
    this.declineOpen.set(false);
  }

  async submitDecline(): Promise<void> {
    const packetId = this.signPacketId();
    const step = this.declineTarget();
    if (!packetId || !step || this.declineForm.invalid || this.submitting()) return;
    this.submitting.set(true);
    try {
      await firstValueFrom(this.esign.declineStep(packetId, step.stepOrder, this.declineForm.getRawValue().reason));
      this.declineOpen.set(false);
      this.notification.success(this.i18n.t('esign.declinedSuccess'));
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.submitting.set(false);
    }
  }

  // ---- Manifest ----

  async exportManifest(packet: SignaturePacket): Promise<void> {
    try {
      const manifest = await firstValueFrom(this.esign.exportManifest(packet.id));
      this.manifestText.set(JSON.stringify(manifest, null, 2));
      await this.downloadManifest(manifest);
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  private async downloadManifest(manifest: ManifestExport): Promise<void> {
    const blob = new Blob([JSON.stringify(manifest, null, 2)], { type: 'application/json' });
    downloadBlob(blob, `esign-manifest-${manifest.packetId}.json`);
  }

  closeManifest(): void {
    this.manifestText.set(null);
  }
}