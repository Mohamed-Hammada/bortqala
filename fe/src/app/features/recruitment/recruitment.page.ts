import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { apiErrorMessage } from '../../core/api-error';
import { ModalDialogComponent } from '../../shared/ui/modal-dialog/modal-dialog.component';
import {
  APPLICATION_STAGES,
  ApplicationStage,
  JobApplication,
  JobOpening,
  StageEvent,
  STAGE_TRANSITIONS,
} from './recruitment.models';
import { Department, RecruitmentService } from './recruitment.service';

@Component({
  selector: 'app-recruitment-page',
  imports: [ReactiveFormsModule, ModalDialogComponent],
  templateUrl: './recruitment.page.html',
  styleUrl: './recruitment.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RecruitmentPage {
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  readonly recruitment = inject(RecruitmentService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly tabs = signal<'openings' | 'applications'>('openings');
  readonly drawerOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly appDrawerOpen = signal(false);
  readonly appEditingId = signal<string | null>(null);
  readonly cvFile = new FormControl<File | null>(null);
  readonly cvError = signal<string | null>(null);
  readonly stageNoteOpen = signal(false);
  readonly convertingId = signal<string | null>(null);
  readonly eventsFor = signal<JobApplication | null>(null);
  readonly events = signal<StageEvent[]>([]);
  readonly warnings = signal<string[]>([]);
  readonly stageFilter = signal<ApplicationStage | ''>('');

  readonly openings = signal<JobOpening[]>([]);
  readonly applications = signal<JobApplication[]>([]);
  readonly departments = signal<Department[]>([]);

  readonly openingForm = new FormGroup({
    titleAr: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(300)] }),
    titleEn: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(300)] }),
    departmentId: new FormControl('', { nonNullable: true }),
    headcount: new FormControl(1, { nonNullable: true, validators: [Validators.required, Validators.min(1)] }),
    description: new FormControl('', { nonNullable: true }),
    published: new FormControl(true, { nonNullable: true }),
  });

  readonly applicationForm = new FormGroup({
    openingId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    fullName: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(200)] }),
    phone: new FormControl('', { nonNullable: true }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.email] }),
    source: new FormControl('', { nonNullable: true }),
  });

  readonly convertForm = new FormGroup({
    departmentId: new FormControl('', { nonNullable: true }),
  });

  readonly filteredApplications = computed(
    () => {
      const stage = this.stageFilter();
      const list = this.applications();
      if (!stage) return list;
      return list.filter((application) => application.stage === stage);
    },
  );

  readonly stages = APPLICATION_STAGES;

  readonly activeTransitions = computed<ApplicationStage[]>(() => {
    const id = this.appEditingId();
    if (!id) return [];
    const stage = this.applications().find((application) => application.id === id)?.stage;
    return stage ? STAGE_TRANSITIONS[stage] ?? [] : [];
  });

  constructor() {
    void this.load();
  }

  departmentName(id: string | null): string {
    if (!id) return 'â€”';
    return this.departments().find((department) => department.id === id)?.name ?? id;
  }

  openingTitle(openingId: string): string {
    const opening = this.openings().find((opening) => opening.id === openingId);
    if (!opening) return openingId;
    const ar = opening.titleAr.trim();
    const en = opening.titleEn.trim();
    if (ar && this.i18n.locale() === 'ar-EG') return ar;
    return en || ar;
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const [openings, applications, departments] = await Promise.all([
        firstValueFrom(this.recruitment.getOpenings()),
        firstValueFrom(this.recruitment.getApplications()),
        firstValueFrom(this.recruitment.listDepartments()),
      ]);
      this.openings.set(openings ?? []);
      this.applications.set(applications ?? []);
      this.departments.set((departments ?? []).filter((department) => department.active));
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  switchTab(tab: 'openings' | 'applications'): void {
    this.tabs.set(tab);
  }

  // ---- Openings ----

  openNewOpening(): void {
    this.editingId.set(null);
    this.openingForm.reset({
      titleAr: '',
      titleEn: '',
      departmentId: '',
      headcount: 1,
      description: '',
      published: true,
    });
    this.drawerOpen.set(true);
  }

  openEditOpening(opening: JobOpening): void {
    if (opening.status === 'CLOSED') return;
    this.editingId.set(opening.id);
    this.openingForm.reset({
      titleAr: opening.titleAr,
      titleEn: opening.titleEn,
      departmentId: opening.departmentId ?? '',
      headcount: opening.headcount,
      description: opening.description ?? '',
      published: opening.published,
    });
    this.drawerOpen.set(true);
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
  }

  async submitOpening(): Promise<void> {
    if (this.openingForm.invalid || this.submitting()) return;
    this.submitting.set(true);
    const value = this.openingForm.getRawValue();
    try {
      const editingId = this.editingId();
      if (editingId) {
        await firstValueFrom(this.recruitment.updateOpening(editingId, value));
      } else {
        await firstValueFrom(this.recruitment.createOpening({
          titleAr: value.titleAr,
          titleEn: value.titleEn,
          departmentId: value.departmentId || undefined,
          headcount: value.headcount,
          description: value.description,
        }));
      }
      this.drawerOpen.set(false);
      this.notification.success(this.i18n.t('recruitment.saved'));
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.submitting.set(false);
    }
  }

  async closeOpening(opening: JobOpening): Promise<void> {
    if (opening.status === 'CLOSED') return;
    try {
      await firstValueFrom(this.recruitment.closeOpening(opening.id));
      this.notification.success(this.i18n.t('recruitment.closed'));
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  // ---- Applications ----

  openNewApplication(): void {
    this.appEditingId.set(null);
    this.warnings.set([]);
    this.cvFile.setValue(null);
    this.cvError.set(null);
    this.applicationForm.reset({
      openingId: this.openings().find((opening) => opening.status === 'OPEN')?.id ?? '',
      fullName: '',
      phone: '',
      email: '',
      source: '',
    });
    const phone = this.applicationForm.get('phone');
    const email = this.applicationForm.get('email');
    phone?.valueChanges.subscribe(() => void this.debouncedWarnings());
    email?.valueChanges.subscribe(() => void this.debouncedWarnings());
    this.appDrawerOpen.set(true);
  }

  private debounceHandle: ReturnType<typeof setTimeout> | null = null;

  private async debouncedWarnings(): Promise<void> {
    if (this.debounceHandle) clearTimeout(this.debounceHandle);
    this.debounceHandle = setTimeout(() => void this.checkWarnings(), 400);
  }

  async checkWarnings(): Promise<void> {
    const { phone, email } = this.applicationForm.getRawValue();
    if (!phone && !email) {
      this.warnings.set([]);
      return;
    }
    try {
      const duplicates = await firstValueFrom(this.recruitment.checkDuplicates(phone || undefined, email || undefined));
      this.warnings.set(duplicates.map((item) =>
        `${item.fullName} (${item.matchedBy === 'phone' ? this.i18n.t('recruitment.phone') : this.i18n.t('recruitment.email')})`));
    } catch {
      this.warnings.set([]);
    }
  }

  closeAppDrawer(): void {
    this.appDrawerOpen.set(false);
    this.warnings.set([]);
  }

  async submitApplication(): Promise<void> {
    if (this.applicationForm.invalid || this.submitting()) return;
    this.submitting.set(true);
const value = this.applicationForm.getRawValue();
    const cv = this.cvFile.value;
    try {
      const created = await firstValueFrom(this.recruitment.createApplication({
        openingId: value.openingId,
        fullName: value.fullName,
        phone: value.phone || undefined,
        email: value.email || undefined,
        source: value.source || undefined,
      }));
      if (cv) {
        await firstValueFrom(this.recruitment.uploadCv(created.id, cv));
      }
      this.appDrawerOpen.set(false);
      this.cvFile.setValue(null);
      this.cvError.set(null);
      this.notification.success(this.i18n.t('recruitment.applicationAdded'));
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.submitting.set(false);
    }
  }

  onCvSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    if (!file) {
      this.cvFile.setValue(null);
      this.cvError.set(null);
      return;
    }
    const allowed = [
      'application/pdf',
      'image/png',
      'image/jpeg',
      'image/webp',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      'application/msword',
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      'application/vnd.ms-excel',
      'text/csv',
    ];
    if (file.size > 5 * 1024 * 1024) {
      this.cvError.set(this.i18n.t('recruitment.cvTooLarge'));
      this.cvFile.setValue(null);
      return;
    }
    if (!allowed.includes(file.type)) {
      this.cvError.set(this.i18n.t('recruitment.cvUnsupportedType'));
      this.cvFile.setValue(null);
      return;
    }
    this.cvError.set(null);
    this.cvFile.setValue(file);
  }

  removeCv(): void {
    this.cvFile.setValue(null);
    this.cvError.set(null);
  }

  stageLabel(stage: ApplicationStage): string {
    return this.i18n.t(`recruitment.stage.${stage}`);
  }

  nextStages(stage: ApplicationStage): ApplicationStage[] {
    return STAGE_TRANSITIONS[stage] ?? [];
  }

  openMoveStage(application: JobApplication): void {
    this.appEditingId.set(application.id);
    this.stageNoteOpen.set(true);
  }

  stageOf(applicationId: string | null): ApplicationStage | null {
    if (!applicationId) return null;
    return this.applications().find((application) => application.id === applicationId)?.stage ?? null;
  }

  closeStageNote(): void {
    this.stageNoteOpen.set(false);
  }

  async moveTo(target: ApplicationStage): Promise<void> {
    const id = this.appEditingId();
    if (!id || this.submitting()) return;
    this.submitting.set(true);
    try {
      await firstValueFrom(this.recruitment.moveStage(id, target, ''));
      this.stageNoteOpen.set(false);
      this.notification.success(this.i18n.t('recruitment.stageMoved'));
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.submitting.set(false);
    }
  }

  onRatingChange(application: JobApplication, event: Event): void {
    this.setRating(application, Number((event.target as HTMLSelectElement).value));
  }

  async setRating(application: JobApplication, rating: number): Promise<void> {
    try {
      await firstValueFrom(this.recruitment.updateRating(application.id, rating));
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  openConvert(application: JobApplication): void {
    if (application.stage !== 'OFFER' && application.stage !== 'INTERVIEW') return;
    this.appEditingId.set(application.id);
    this.convertForm.reset({
      departmentId: application.openingId
        ? (this.openings().find((opening) => opening.id === application.openingId)?.departmentId ?? '')
        : '',
    });
    this.convertingId.set(application.id);
  }

  closeConvert(): void {
    this.convertingId.set(null);
  }

  async doConvert(): Promise<void> {
    const id = this.convertingId();
    if (!id || this.submitting()) return;
    this.submitting.set(true);
    try {
      const result = await firstValueFrom(this.recruitment.convertToEmployee(id, this.convertForm.getRawValue().departmentId || undefined));
      this.convertingId.set(null);
      this.notification.success(this.i18n.t('recruitment.converted', { id: result.employeeId }));
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.submitting.set(false);
    }
  }

  async showTimeline(application: JobApplication): Promise<void> {
    this.eventsFor.set(application);
    this.events.set([]);
    try {
      this.events.set((await firstValueFrom(this.recruitment.getStageEvents(application.id))) ?? []);
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  closeTimeline(): void {
    this.eventsFor.set(null);
  }

  formatDate(epoch: number): string {
    return new Date(epoch).toLocaleString(this.i18n.locale() === 'ar-EG' ? 'ar-EG' : 'en-US');
  }
}
