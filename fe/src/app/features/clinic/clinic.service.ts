import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AddAllergyPayload,
  AddConditionPayload,
  ClinicVisit,
  CompleteVisitPayload,
  ConsentForm,
  DoctorCommissionStatement,
  DuplicateCheckResponse,
  NationalIdParseResponse,
  Patient,
  PatientAllergy,
  PatientChart,
  PatientCondition,
  PatientDocument,
  PrescriptionLine,
  QueueVisitPayload,
  RecordVitalsPayload,
  RegisterPatientPayload,
  AppointmentMetrics,
  AvailableSlot,
  BookAppointmentPayload,
  ClinicAppointment,
  DoctorRoster,
  SaveDoctorRosterPayload,
  SignConsentPayload,
  UploadDocumentPayload,
  VisitVitals,
  BatchFefoSuggestion,
  DispensePrescriptionPayload,
  NarcoticsRegisterEntry,
  PharmacyDispenseRecord,
  PharmacyItem,
  SavePharmacyItemPayload,
  CreateLabOrderPayload,
  EnterLabResultPayload,
  LabOrder,
  LabTestItem,
  SaveLabTestItemPayload,
  SendOutLabOrderPayload,
  AttachInsurancePolicyPayload,
  CalculateInsuranceSplitPayload,
  CreateClaimBatchPayload,
  DecidePreAuthorizationPayload,
  InsuranceClaimBatch,
  InsuranceClaimLine,
  InsurancePayer,
  InsurancePlan,
  InsurancePreAuthorization,
  InsuranceSplitCalculationResult,
  PatientInsurancePolicy,
  RequestPreAuthorizationPayload,
  ResubmitClaimLinePayload,
  SaveInsurancePayerPayload,
  SaveInsurancePlanPayload,
  SettleClaimBatchPayload,
  AddNursingNotePayload,
  AddOtChargePayload,
  AdministerMarEntryPayload,
  AdmitPatientPayload,
  CompleteOtSurgeryPayload,
  CreateMarEntryPayload,
  DischargePatientPayload,
  HospitalAdmission,
  HospitalBed,
  HospitalFluidIoEntry,
  HospitalMarEntry,
  HospitalNursingNote,
  HospitalOccupancyMetrics,
  HospitalOtCharge,
  HospitalOtSchedule,
  HospitalRoom,
  HospitalWard,
  RecordFluidIoPayload,
  SaveHospitalBedPayload,
  SaveHospitalRoomPayload,
  SaveHospitalWardPayload,
  ScheduleOtPayload,
  TransferPatientBedPayload,
  AddDentalPlanItemPayload,
  CreateDentalPlanPayload,
  DentalRecord,
  DentalTreatmentPlan,
  DentalTreatmentPlanItem,
  ExamAnswer,
  ExamTemplate,
  PatientOdontogram,
  RecordToothConditionPayload,
  SaveExamTemplatePayload,
  SubmitExamAnswerPayload,
} from './clinic.models';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class ClinicService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/clinic';

  searchPatients(query = '', page = 0, size = 20): Observable<PageResponse<Patient>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (query.trim()) {
      params = params.set('query', query.trim());
    }
    return this.http.get<PageResponse<Patient>>(`${this.baseUrl}/patients`, { params });
  }

  getPatient(id: string): Observable<Patient> {
    return this.http.get<Patient>(`${this.baseUrl}/patients/${id}`);
  }

  registerPatient(payload: RegisterPatientPayload): Observable<Patient> {
    return this.http.post<Patient>(`${this.baseUrl}/patients`, payload);
  }

  updatePatient(id: string, payload: RegisterPatientPayload): Observable<Patient> {
    return this.http.put<Patient>(`${this.baseUrl}/patients/${id}`, payload);
  }

  checkDuplicates(phone?: string, nationalId?: string): Observable<DuplicateCheckResponse> {
    let params = new HttpParams();
    if (phone?.trim()) params = params.set('phone', phone.trim());
    if (nationalId?.trim()) params = params.set('nationalId', nationalId.trim());
    return this.http.get<DuplicateCheckResponse>(`${this.baseUrl}/patients/check-duplicates`, { params });
  }

  parseNationalId(nationalId: string): Observable<NationalIdParseResponse> {
    const params = new HttpParams().set('nationalId', nationalId.trim());
    return this.http.get<NationalIdParseResponse>(`${this.baseUrl}/patients/parse-national-id`, { params });
  }

  getQueue(date?: string, doctorId?: string): Observable<ClinicVisit[]> {
    let params = new HttpParams();
    if (date?.trim()) params = params.set('date', date.trim());
    if (doctorId?.trim()) params = params.set('doctorId', doctorId.trim());
    return this.http.get<ClinicVisit[]>(`${this.baseUrl}/queue`, { params });
  }

  queueVisit(payload: QueueVisitPayload): Observable<ClinicVisit> {
    return this.http.post<ClinicVisit>(`${this.baseUrl}/queue`, payload);
  }

  callVisit(id: string): Observable<ClinicVisit> {
    return this.http.post<ClinicVisit>(`${this.baseUrl}/queue/${id}/call`, {});
  }

  completeVisit(id: string, payload: CompleteVisitPayload): Observable<ClinicVisit> {
    return this.http.post<ClinicVisit>(`${this.baseUrl}/queue/${id}/complete`, payload);
  }

  cancelVisit(id: string): Observable<ClinicVisit> {
    return this.http.post<ClinicVisit>(`${this.baseUrl}/queue/${id}/cancel`, {});
  }

  getVisit(id: string): Observable<ClinicVisit> {
    return this.http.get<ClinicVisit>(`${this.baseUrl}/visits/${id}`);
  }

  savePrescriptions(visitId: string, lines: PrescriptionLine[]): Observable<PrescriptionLine[]> {
    return this.http.post<PrescriptionLine[]>(`${this.baseUrl}/visits/${visitId}/prescriptions`, lines);
  }

  getCommissionStatement(doctorId: string, year: number, month: number, rate?: number): Observable<DoctorCommissionStatement> {
    let params = new HttpParams()
      .set('doctorId', doctorId)
      .set('year', year)
      .set('month', month);
    if (rate !== undefined && rate !== null) {
      params = params.set('rate', rate);
    }
    return this.http.get<DoctorCommissionStatement>(`${this.baseUrl}/commissions/statement`, { params });
  }

  // EMR Depth API
  getPatientChart(patientId: string): Observable<PatientChart> {
    return this.http.get<PatientChart>(`${this.baseUrl}/patients/${patientId}/chart`);
  }

  addAllergy(patientId: string, payload: AddAllergyPayload): Observable<PatientAllergy> {
    return this.http.post<PatientAllergy>(`${this.baseUrl}/patients/${patientId}/allergies`, payload);
  }

  deleteAllergy(allergyId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/allergies/${allergyId}`);
  }

  addCondition(patientId: string, payload: AddConditionPayload): Observable<PatientCondition> {
    return this.http.post<PatientCondition>(`${this.baseUrl}/patients/${patientId}/conditions`, payload);
  }

  updateCondition(conditionId: string, payload: AddConditionPayload): Observable<PatientCondition> {
    return this.http.put<PatientCondition>(`${this.baseUrl}/conditions/${conditionId}`, payload);
  }

  deleteCondition(conditionId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/conditions/${conditionId}`);
  }

  recordVitals(visitId: string, payload: RecordVitalsPayload): Observable<VisitVitals> {
    return this.http.post<VisitVitals>(`${this.baseUrl}/visits/${visitId}/vitals`, payload);
  }

  uploadDocument(patientId: string, payload: UploadDocumentPayload): Observable<PatientDocument> {
    return this.http.post<PatientDocument>(`${this.baseUrl}/patients/${patientId}/documents`, payload);
  }

  deleteDocument(documentId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/documents/${documentId}`);
  }

  signConsent(patientId: string, payload: SignConsentPayload): Observable<ConsentForm> {
    return this.http.post<ConsentForm>(`${this.baseUrl}/patients/${patientId}/consents`, payload);
  }

  // WP-22 Appointments & Rosters API
  getAllRosters(): Observable<DoctorRoster[]> {
    return this.http.get<DoctorRoster[]>(`${this.baseUrl}/rosters`);
  }

  getRostersForDoctor(doctorId: string): Observable<DoctorRoster[]> {
    return this.http.get<DoctorRoster[]>(`${this.baseUrl}/doctors/${doctorId}/rosters`);
  }

  saveRoster(payload: SaveDoctorRosterPayload): Observable<DoctorRoster> {
    return this.http.post<DoctorRoster>(`${this.baseUrl}/rosters`, payload);
  }

  deleteRoster(rosterId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/rosters/${rosterId}`);
  }

  getAvailableSlots(doctorId: string, date: string): Observable<AvailableSlot[]> {
    const params = new HttpParams().set('doctorId', doctorId).set('date', date);
    return this.http.get<AvailableSlot[]>(`${this.baseUrl}/slots`, { params });
  }

  getAppointments(date: string, doctorId?: string): Observable<ClinicAppointment[]> {
    let params = new HttpParams().set('date', date);
    if (doctorId?.trim()) {
      params = params.set('doctorId', doctorId.trim());
    }
    return this.http.get<ClinicAppointment[]>(`${this.baseUrl}/appointments`, { params });
  }

  bookAppointment(payload: BookAppointmentPayload): Observable<ClinicAppointment> {
    return this.http.post<ClinicAppointment>(`${this.baseUrl}/appointments`, payload);
  }

  confirmAppointment(id: string): Observable<ClinicAppointment> {
    return this.http.post<ClinicAppointment>(`${this.baseUrl}/appointments/${id}/confirm`, {});
  }

  checkInAppointment(id: string): Observable<ClinicAppointment> {
    return this.http.post<ClinicAppointment>(`${this.baseUrl}/appointments/${id}/check-in`, {});
  }

  markNoShow(id: string): Observable<ClinicAppointment> {
    return this.http.post<ClinicAppointment>(`${this.baseUrl}/appointments/${id}/no-show`, {});
  }

  cancelAppointment(id: string): Observable<ClinicAppointment> {
    return this.http.post<ClinicAppointment>(`${this.baseUrl}/appointments/${id}/cancel`, {});
  }

  sendAppointmentReminders(date: string): Observable<number> {
    const params = new HttpParams().set('date', date);
    return this.http.post<number>(`${this.baseUrl}/appointments/reminders/send`, {}, { params });
  }

  getAppointmentMetrics(doctorId: string, period: string): Observable<AppointmentMetrics> {
    const params = new HttpParams().set('doctorId', doctorId).set('period', period);
    return this.http.get<AppointmentMetrics>(`${this.baseUrl}/appointments/metrics`, { params });
  }

  // WP-23 Pharmacy & Narcotics API
  getAllPharmacyItems(): Observable<PharmacyItem[]> {
    return this.http.get<PharmacyItem[]>(`${this.baseUrl}/pharmacy/items`);
  }

  savePharmacyItem(payload: SavePharmacyItemPayload): Observable<PharmacyItem> {
    return this.http.post<PharmacyItem>(`${this.baseUrl}/pharmacy/items`, payload);
  }

  getFefoSuggestions(id: string): Observable<BatchFefoSuggestion[]> {
    return this.http.get<BatchFefoSuggestion[]>(`${this.baseUrl}/pharmacy/items/${id}/fefo`);
  }

  dispensePrescription(prescriptionId: string, payload: DispensePrescriptionPayload): Observable<PharmacyDispenseRecord> {
    return this.http.post<PharmacyDispenseRecord>(`${this.baseUrl}/pharmacy/prescriptions/${prescriptionId}/dispense`, payload);
  }

  approveControlledDispense(dispenseId: string): Observable<PharmacyDispenseRecord> {
    return this.http.post<PharmacyDispenseRecord>(`${this.baseUrl}/pharmacy/dispense/${dispenseId}/approve`, {});
  }

  getNarcoticsRegister(from?: number, to?: number): Observable<NarcoticsRegisterEntry[]> {
    let params = new HttpParams();
    if (from != null) params = params.set('from', from.toString());
    if (to != null) params = params.set('to', to.toString());
    return this.http.get<NarcoticsRegisterEntry[]>(`${this.baseUrl}/pharmacy/narcotics`, { params });
  }

  // WP-24 Lab & Imaging API
  getAllLabTests(category?: string): Observable<LabTestItem[]> {
    let params = new HttpParams();
    if (category) params = params.set('category', category);
    return this.http.get<LabTestItem[]>(`${this.baseUrl}/lab/tests`, { params });
  }

  saveLabTest(payload: SaveLabTestItemPayload): Observable<LabTestItem> {
    return this.http.post<LabTestItem>(`${this.baseUrl}/lab/tests`, payload);
  }

  getAllLabOrders(status?: string): Observable<LabOrder[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<LabOrder[]>(`${this.baseUrl}/lab/orders`, { params });
  }

  createLabOrder(payload: CreateLabOrderPayload): Observable<LabOrder> {
    return this.http.post<LabOrder>(`${this.baseUrl}/lab/orders`, payload);
  }

  collectSample(id: string): Observable<LabOrder> {
    return this.http.post<LabOrder>(`${this.baseUrl}/lab/orders/${id}/collect`, {});
  }

  sendOutOrder(id: string, payload: SendOutLabOrderPayload): Observable<LabOrder> {
    return this.http.post<LabOrder>(`${this.baseUrl}/lab/orders/${id}/send-out`, payload);
  }

  enterLabResult(id: string, payload: EnterLabResultPayload): Observable<LabOrder> {
    return this.http.post<LabOrder>(`${this.baseUrl}/lab/orders/${id}/result`, payload);
  }

  validateLabOrder(id: string): Observable<LabOrder> {
    return this.http.post<LabOrder>(`${this.baseUrl}/lab/orders/${id}/validate`, {});
  }

  cancelLabOrder(id: string): Observable<LabOrder> {
    return this.http.post<LabOrder>(`${this.baseUrl}/lab/orders/${id}/cancel`, {});
  }

  acknowledgeCritical(id: string): Observable<LabOrder> {
    return this.http.post<LabOrder>(`${this.baseUrl}/lab/orders/${id}/ack-critical`, {});
  }

  getAgingSentOutOrders(): Observable<LabOrder[]> {
    return this.http.get<LabOrder[]>(`${this.baseUrl}/lab/orders/aging`);
  }

  getOrdersByPatient(patientId: string, validatedOnly: boolean = false): Observable<LabOrder[]> {
    const params = new HttpParams().set('validatedOnly', validatedOnly.toString());
    return this.http.get<LabOrder[]>(`${this.baseUrl}/lab/orders/patient/${patientId}`, { params });
  }

  // WP-25 Insurance & Claims API
  getAllPayers(activeOnly: boolean = false): Observable<InsurancePayer[]> {
    const params = new HttpParams().set('activeOnly', activeOnly.toString());
    return this.http.get<InsurancePayer[]>(`${this.baseUrl}/insurance/payers`, { params });
  }

  savePayer(payload: SaveInsurancePayerPayload): Observable<InsurancePayer> {
    return this.http.post<InsurancePayer>(`${this.baseUrl}/insurance/payers`, payload);
  }

  getPlansByPayer(payerId?: string): Observable<InsurancePlan[]> {
    let params = new HttpParams();
    if (payerId) params = params.set('payerId', payerId);
    return this.http.get<InsurancePlan[]>(`${this.baseUrl}/insurance/plans`, { params });
  }

  savePlan(payload: SaveInsurancePlanPayload): Observable<InsurancePlan> {
    return this.http.post<InsurancePlan>(`${this.baseUrl}/insurance/plans`, payload);
  }

  getPatientPolicies(patientId: string): Observable<PatientInsurancePolicy[]> {
    return this.http.get<PatientInsurancePolicy[]>(`${this.baseUrl}/insurance/policies/patient/${patientId}`);
  }

  attachPolicy(payload: AttachInsurancePolicyPayload): Observable<PatientInsurancePolicy> {
    return this.http.post<PatientInsurancePolicy>(`${this.baseUrl}/insurance/policies`, payload);
  }

  calculateInsuranceSplit(payload: CalculateInsuranceSplitPayload): Observable<InsuranceSplitCalculationResult> {
    return this.http.post<InsuranceSplitCalculationResult>(`${this.baseUrl}/insurance/split-calculate`, payload);
  }

  getPreAuthorizations(status?: string, patientId?: string): Observable<InsurancePreAuthorization[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    if (patientId) params = params.set('patientId', patientId);
    return this.http.get<InsurancePreAuthorization[]>(`${this.baseUrl}/insurance/pre-auth`, { params });
  }

  requestPreAuthorization(payload: RequestPreAuthorizationPayload): Observable<InsurancePreAuthorization> {
    return this.http.post<InsurancePreAuthorization>(`${this.baseUrl}/insurance/pre-auth`, payload);
  }

  decidePreAuthorization(id: string, payload: DecidePreAuthorizationPayload): Observable<InsurancePreAuthorization> {
    return this.http.post<InsurancePreAuthorization>(`${this.baseUrl}/insurance/pre-auth/${id}/decide`, payload);
  }

  getAllClaimBatches(payerId?: string): Observable<InsuranceClaimBatch[]> {
    let params = new HttpParams();
    if (payerId) params = params.set('payerId', payerId);
    return this.http.get<InsuranceClaimBatch[]>(`${this.baseUrl}/insurance/claims/batches`, { params });
  }

  createClaimBatch(payload: CreateClaimBatchPayload): Observable<InsuranceClaimBatch> {
    return this.http.post<InsuranceClaimBatch>(`${this.baseUrl}/insurance/claims/batches`, payload);
  }

  submitClaimBatch(id: string): Observable<InsuranceClaimBatch> {
    return this.http.post<InsuranceClaimBatch>(`${this.baseUrl}/insurance/claims/batches/${id}/submit`, {});
  }

  settleClaimBatch(id: string, payload: SettleClaimBatchPayload): Observable<InsuranceClaimBatch> {
    return this.http.post<InsuranceClaimBatch>(`${this.baseUrl}/insurance/claims/batches/${id}/settle`, payload);
  }

  resubmitClaimLine(payload: ResubmitClaimLinePayload): Observable<InsuranceClaimLine> {
    return this.http.post<InsuranceClaimLine>(`${this.baseUrl}/insurance/claims/lines/resubmit`, payload);
  }

  // WP-26 Hospital Ops API (ADT, OT, Nursing)
  getWards(): Observable<HospitalWard[]> {
    return this.http.get<HospitalWard[]>(`${this.baseUrl}/hospital/wards`);
  }

  saveWard(payload: SaveHospitalWardPayload): Observable<HospitalWard> {
    return this.http.post<HospitalWard>(`${this.baseUrl}/hospital/wards`, payload);
  }

  getRooms(wardId?: string): Observable<HospitalRoom[]> {
    let params = new HttpParams();
    if (wardId) params = params.set('wardId', wardId);
    return this.http.get<HospitalRoom[]>(`${this.baseUrl}/hospital/rooms`, { params });
  }

  saveRoom(payload: SaveHospitalRoomPayload): Observable<HospitalRoom> {
    return this.http.post<HospitalRoom>(`${this.baseUrl}/hospital/rooms`, payload);
  }

  getBeds(): Observable<HospitalBed[]> {
    return this.http.get<HospitalBed[]>(`${this.baseUrl}/hospital/beds`);
  }

  saveBed(payload: SaveHospitalBedPayload): Observable<HospitalBed> {
    return this.http.post<HospitalBed>(`${this.baseUrl}/hospital/beds`, payload);
  }

  admitPatient(payload: AdmitPatientPayload): Observable<HospitalAdmission> {
    return this.http.post<HospitalAdmission>(`${this.baseUrl}/hospital/admissions/admit`, payload);
  }

  transferPatient(id: string, payload: TransferPatientBedPayload): Observable<HospitalAdmission> {
    return this.http.post<HospitalAdmission>(`${this.baseUrl}/hospital/admissions/${id}/transfer`, payload);
  }

  dischargePatient(id: string, payload: DischargePatientPayload): Observable<HospitalAdmission> {
    return this.http.post<HospitalAdmission>(`${this.baseUrl}/hospital/admissions/${id}/discharge`, payload);
  }

  getAdmissions(status?: string): Observable<HospitalAdmission[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<HospitalAdmission[]>(`${this.baseUrl}/hospital/admissions`, { params });
  }

  getOccupancyMetrics(): Observable<HospitalOccupancyMetrics> {
    return this.http.get<HospitalOccupancyMetrics>(`${this.baseUrl}/hospital/metrics/occupancy`);
  }

  getOtSchedules(): Observable<HospitalOtSchedule[]> {
    return this.http.get<HospitalOtSchedule[]>(`${this.baseUrl}/hospital/ot/schedules`);
  }

  scheduleOt(payload: ScheduleOtPayload): Observable<HospitalOtSchedule> {
    return this.http.post<HospitalOtSchedule>(`${this.baseUrl}/hospital/ot/schedules`, payload);
  }

  startOt(id: string): Observable<HospitalOtSchedule> {
    return this.http.post<HospitalOtSchedule>(`${this.baseUrl}/hospital/ot/schedules/${id}/start`, {});
  }

  completeOt(id: string, payload: CompleteOtSurgeryPayload): Observable<HospitalOtSchedule> {
    return this.http.post<HospitalOtSchedule>(`${this.baseUrl}/hospital/ot/schedules/${id}/complete`, payload);
  }

  addOtCharge(id: string, payload: AddOtChargePayload): Observable<HospitalOtCharge> {
    return this.http.post<HospitalOtCharge>(`${this.baseUrl}/hospital/ot/schedules/${id}/charges`, payload);
  }

  getMarEntries(admissionId: string): Observable<HospitalMarEntry[]> {
    return this.http.get<HospitalMarEntry[]>(`${this.baseUrl}/hospital/nursing/mar/admission/${admissionId}`);
  }

  createMarEntry(payload: CreateMarEntryPayload): Observable<HospitalMarEntry> {
    return this.http.post<HospitalMarEntry>(`${this.baseUrl}/hospital/nursing/mar`, payload);
  }

  administerMarEntry(id: string, payload: AdministerMarEntryPayload): Observable<HospitalMarEntry> {
    return this.http.post<HospitalMarEntry>(`${this.baseUrl}/hospital/nursing/mar/${id}/administer`, payload);
  }

  getFluidIoEntries(admissionId: string): Observable<HospitalFluidIoEntry[]> {
    return this.http.get<HospitalFluidIoEntry[]>(`${this.baseUrl}/hospital/nursing/fluid-io/admission/${admissionId}`);
  }

  recordFluidIo(payload: RecordFluidIoPayload): Observable<HospitalFluidIoEntry> {
    return this.http.post<HospitalFluidIoEntry>(`${this.baseUrl}/hospital/nursing/fluid-io`, payload);
  }

  getNursingNotes(admissionId: string): Observable<HospitalNursingNote[]> {
    return this.http.get<HospitalNursingNote[]>(`${this.baseUrl}/hospital/nursing/notes/admission/${admissionId}`);
  }

  addNursingNote(payload: AddNursingNotePayload): Observable<HospitalNursingNote> {
    return this.http.post<HospitalNursingNote>(`${this.baseUrl}/hospital/nursing/notes`, payload);
  }

  // WP-27 Dental & Specialty Charting API
  recordToothCondition(patientId: string, payload: RecordToothConditionPayload): Observable<DentalRecord> {
    return this.http.post<DentalRecord>(`${this.baseUrl}/dental/patients/${patientId}/records`, payload);
  }

  getPatientOdontogram(patientId: string): Observable<PatientOdontogram> {
    return this.http.get<PatientOdontogram>(`${this.baseUrl}/dental/patients/${patientId}/odontogram`);
  }

  createDentalPlan(patientId: string, payload: CreateDentalPlanPayload): Observable<DentalTreatmentPlan> {
    return this.http.post<DentalTreatmentPlan>(`${this.baseUrl}/dental/patients/${patientId}/plans`, payload);
  }

  getDentalPlans(patientId: string): Observable<DentalTreatmentPlan[]> {
    return this.http.get<DentalTreatmentPlan[]>(`${this.baseUrl}/dental/patients/${patientId}/plans`);
  }

  addDentalPlanItem(planId: string, payload: AddDentalPlanItemPayload): Observable<DentalTreatmentPlanItem> {
    return this.http.post<DentalTreatmentPlanItem>(`${this.baseUrl}/dental/plans/${planId}/items`, payload);
  }

  markDentalPlanItemDone(itemId: string, visitId?: string): Observable<DentalTreatmentPlanItem> {
    let params = new HttpParams();
    if (visitId) params = params.set('visitId', visitId);
    return this.http.post<DentalTreatmentPlanItem>(`${this.baseUrl}/dental/plans/items/${itemId}/done`, {}, { params });
  }

  getExamTemplates(specialty?: string): Observable<ExamTemplate[]> {
    let params = new HttpParams();
    if (specialty) params = params.set('specialty', specialty);
    return this.http.get<ExamTemplate[]>(`${this.baseUrl}/exam-templates`, { params });
  }

  saveExamTemplate(payload: SaveExamTemplatePayload): Observable<ExamTemplate> {
    return this.http.post<ExamTemplate>(`${this.baseUrl}/exam-templates`, payload);
  }

  submitExamAnswers(payload: SubmitExamAnswerPayload): Observable<ExamAnswer> {
    return this.http.post<ExamAnswer>(`${this.baseUrl}/exam-templates/answers`, payload);
  }

  getExamAnswers(visitId: string): Observable<ExamAnswer[]> {
    return this.http.get<ExamAnswer[]>(`${this.baseUrl}/exam-templates/answers/visit/${visitId}`);
  }
}
