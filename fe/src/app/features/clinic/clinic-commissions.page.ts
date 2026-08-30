import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ClinicService } from './clinic.service';
import { DoctorCommissionStatement } from './clinic.models';
import { I18nService } from '../../core/i18n.service';

@Component({
  selector: 'app-clinic-commissions-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './clinic-commissions.page.html',
  styleUrl: './clinic-commissions.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClinicCommissionsPageComponent implements OnInit {
  private readonly clinicService = inject(ClinicService);
  private readonly fb = inject(FormBuilder);
  readonly i18n = inject(I18nService);

  readonly statement = signal<DoctorCommissionStatement | null>(null);
  readonly loading = signal(false);

  filterForm: FormGroup = this.fb.group({
    doctorId: ['doc-1', Validators.required],
    year: [new Date().getFullYear(), Validators.required],
    month: [new Date().getMonth() + 1, [Validators.required, Validators.min(1), Validators.max(12)]],
    rate: [50, [Validators.required, Validators.min(0), Validators.max(100)]],
  });

  ngOnInit(): void {
    this.generateStatement();
  }

  generateStatement(): void {
    if (this.filterForm.invalid) {
      this.filterForm.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    const val = this.filterForm.value;
    this.clinicService
      .getCommissionStatement(val.doctorId, val.year, val.month, val.rate)
      .subscribe({
        next: (res) => {
          this.statement.set(res);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
        },
      });
  }
}
