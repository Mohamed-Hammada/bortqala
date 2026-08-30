import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { I18nService } from '../../../core/i18n.service';
import { ProjectProgressClaim } from '../models/claim.models';

@Component({
  selector: 'app-claim-certificate-modal',
  standalone: true,
  imports: [CommonModule, DecimalPipe],
  templateUrl: './claim-certificate-modal.component.html',
  styleUrl: './claim-certificate-modal.component.scss',
})
export class ClaimCertificateModalComponent {
  readonly i18n = inject(I18nService);

  @Input() isOpen = false;
  @Input() claim: ProjectProgressClaim | null = null;
  @Input() projectName = '';

  @Output() close = new EventEmitter<void>();

  printCertificate(): void {
    window.print();
  }
}
