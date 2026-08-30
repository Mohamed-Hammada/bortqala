export interface Patient {
  id: string;
  mrn: string;
  nationalId?: string | null;
  fullName: string;
  phone: string;
  gender: 'MALE' | 'FEMALE' | 'UNKNOWN';
  birthDate?: string | null;
  bloodGroup?: string | null;
  allergiesText?: string | null;
  notes?: string | null;
  emergencyContactName?: string | null;
  emergencyContactPhone?: string | null;
  createdAt: number;
  updatedAt: number;
}

export interface RegisterPatientPayload {
  nationalId?: string | null;
  fullName: string;
  phone: string;
  gender?: string | null;
  birthDate?: string | null;
  bloodGroup?: string | null;
  allergiesText?: string | null;
  notes?: string | null;
  emergencyContactName?: string | null;
  emergencyContactPhone?: string | null;
}

export interface DuplicateCheckResponse {
  duplicateFound: boolean;
  matchingPatients: Patient[];
}

export interface NationalIdParseResponse {
  valid: boolean;
  nationalId?: string | null;
  birthDate?: string | null;
  gender?: string | null;
  governorateCode?: string | null;
  governorateName?: string | null;
  errorMessage?: string | null;
}

export interface PrescriptionLine {
  id?: string;
  drugName: string;
  dose: string;
  frequency: string;
  duration: string;
  instructions?: string | null;
  createdAt?: number;
}

export interface ClinicVisit {
  id: string;
  patientId: string;
  patientName: string;
  patientMrn: string;
  patientPhone: string;
  doctorEmployeeId: string;
  doctorName: string;
  visitDate: string;
  visitTime: number;
  token: number;
  status: 'WAITING' | 'IN_ROOM' | 'DONE' | 'CANCELLED';
  chiefComplaint?: string | null;
  diagnosisIcd?: string | null;
  diagnosisNotes?: string | null;
  feeCharged: number;
  insuranceCovered: number;
  patientShare: number;
  paymentMethod?: string | null;
  prescriptionLines: PrescriptionLine[];
  createdAt: number;
  updatedAt: number;
}

export interface QueueVisitPayload {
  patientId: string;
  doctorEmployeeId: string;
  visitDate?: string | null;
  feeCharged?: number;
  insuranceCovered?: number;
  paymentMethod?: string;
}

export interface CompleteVisitPayload {
  chiefComplaint?: string;
  diagnosisIcd?: string;
  diagnosisNotes?: string;
  feeCharged?: number;
  insuranceCovered?: number;
  paymentMethod?: string;
  prescriptionLines?: PrescriptionLine[];
}

export interface DoctorCommissionStatement {
  doctorEmployeeId: string;
  doctorName: string;
  period: string;
  completedVisitsCount: number;
  totalRevenue: number;
  commissionRatePercent: number;
  commissionAmount: number;
  visits: ClinicVisit[];
}

// WP-21 EMR Depth Models
export interface PatientAllergy {
  id: string;
  patientId: string;
  substance: string;
  severity: 'MILD' | 'MODERATE' | 'SEVERE';
  reactionNotes?: string | null;
  notedAt: number;
}

export interface AddAllergyPayload {
  substance: string;
  severity?: 'MILD' | 'MODERATE' | 'SEVERE';
  reactionNotes?: string | null;
}

export interface PatientCondition {
  id: string;
  patientId: string;
  icdCode?: string | null;
  label: string;
  chronic: boolean;
  onsetDate?: string | null;
  status: 'ACTIVE' | 'RESOLVED' | 'INACTIVE';
  notes?: string | null;
  createdAt: number;
}

export interface AddConditionPayload {
  icdCode?: string | null;
  label: string;
  chronic?: boolean;
  onsetDate?: string | null;
  status?: 'ACTIVE' | 'RESOLVED' | 'INACTIVE';
  notes?: string | null;
}

export interface VisitVitals {
  id: string;
  visitId: string;
  patientId: string;
  systolicBp?: number | null;
  diastolicBp?: number | null;
  pulse?: number | null;
  tempC?: number | null;
  spo2?: number | null;
  weightKg?: number | null;
  heightCm?: number | null;
  bmi?: number | null;
  notes?: string | null;
  recordedAt: number;
}

export interface RecordVitalsPayload {
  systolicBp?: number | null;
  diastolicBp?: number | null;
  pulse?: number | null;
  tempC?: number | null;
  spo2?: number | null;
  weightKg?: number | null;
  heightCm?: number | null;
  notes?: string | null;
}

export interface PatientDocument {
  id: string;
  patientId: string;
  visitId?: string | null;
  documentKind: 'LAB' | 'IMAGING' | 'REPORT' | 'CONSENT';
  fileName: string;
  contentType?: string | null;
  fileSize?: number | null;
  storagePath?: string | null;
  notes?: string | null;
  uploadedAt: number;
}

export interface UploadDocumentPayload {
  visitId?: string | null;
  documentKind: 'LAB' | 'IMAGING' | 'REPORT' | 'CONSENT';
  fileName: string;
  contentType?: string | null;
  fileSize?: number | null;
  storagePath?: string | null;
  notes?: string | null;
}

export interface ConsentForm {
  id: string;
  patientId: string;
  visitId?: string | null;
  templateKey: string;
  title: string;
  bodyText: string;
  signedByName: string;
  signedByRelation: 'SELF' | 'GUARDIAN' | 'NEXT_OF_KIN';
  signedAt: number;
  ipAddress?: string | null;
}

export interface SignConsentPayload {
  visitId?: string | null;
  templateKey: string;
  title: string;
  bodyText: string;
  signedByName: string;
  signedByRelation?: 'SELF' | 'GUARDIAN' | 'NEXT_OF_KIN';
  ipAddress?: string | null;
}

export interface PatientChart {
  patient: Patient;
  allergies: PatientAllergy[];
  hasSevereAllergies: boolean;
  conditions: PatientCondition[];
  vitalsHistory: VisitVitals[];
  recentVisits: ClinicVisit[];
  documents: PatientDocument[];
  consents: ConsentForm[];
}

// WP-22 Appointments & Rosters
export interface DoctorRoster {
  id: string;
  doctorEmployeeId: string;
  doctorName: string;
  weekday: number; // 0=Sunday, 1=Monday... 6=Saturday
  startTime: string;
  endTime: string;
  slotMinutes: number;
  maxPatientsPerSlot: number;
  validFrom?: string | null;
  validTo?: string | null;
  active: boolean;
}

export interface SaveDoctorRosterPayload {
  doctorEmployeeId: string;
  weekday: number;
  startTime: string;
  endTime: string;
  slotMinutes: number;
  maxPatientsPerSlot?: number;
  validFrom?: string | null;
  validTo?: string | null;
}

export interface AvailableSlot {
  startTime: string;
  startsAt: number;
  durationMinutes: number;
  available: boolean;
  bookedAppointmentId?: string | null;
}

export interface BookAppointmentPayload {
  patientId: string;
  doctorEmployeeId: string;
  visitDate: string;
  startTime: string;
  durationMinutes?: number;
  source?: 'WALKIN' | 'PHONE' | 'ONLINE' | 'WHATSAPP';
  reason?: string | null;
}

export interface ClinicAppointment {
  id: string;
  patientId: string;
  patientName: string;
  patientMrn: string;
  patientPhone: string;
  doctorEmployeeId: string;
  doctorName: string;
  visitDate: string;
  startTime: string;
  startsAt: number;
  durationMinutes: number;
  status: 'BOOKED' | 'CONFIRMED' | 'CHECKED_IN' | 'NO_SHOW' | 'CANCELLED' | 'DONE';
  source: 'WALKIN' | 'PHONE' | 'ONLINE' | 'WHATSAPP';
  reason?: string | null;
  clinicVisitId?: string | null;
  reminderSentAt?: number | null;
  createdAt: number;
  updatedAt: number;
}

export interface AppointmentMetrics {
  period: string;
  totalAppointments: number;
  bookedCount: number;
  confirmedCount: number;
  checkedInCount: number;
  completedCount: number;
  noShowCount: number;
  cancelledCount: number;
  noShowRatePercent: number;
}

// WP-23 Pharmacy & Narcotics
export interface PharmacyItem {
  id: string;
  itemId: string;
  tradeName: string;
  genericName?: string | null;
  dosageForm: 'TABLET' | 'SYRUP' | 'INJECTION' | 'CAPSULE' | 'OINTMENT' | 'DROPS' | 'INHALER';
  strengthText?: string | null;
  controlled: boolean;
  controlSchedule?: string | null;
}

export interface SavePharmacyItemPayload {
  itemId: string;
  tradeName: string;
  genericName?: string | null;
  dosageForm: string;
  strengthText?: string | null;
  controlled: boolean;
  controlSchedule?: string | null;
}

export interface BatchFefoSuggestion {
  batchNumber: string;
  expiryDate: string;
  availableQuantity: number;
  expired: boolean;
  nearExpiry: boolean;
}

export interface DispenseLinePayload {
  prescriptionLineId?: string | null;
  pharmacyItemId: string;
  batchNumber?: string | null;
  expiryDate?: string | null;
  quantity: number;
}

export interface DispensePrescriptionPayload {
  secondSignerId?: string | null;
  notes?: string | null;
  lines: DispenseLinePayload[];
}

export interface PharmacyDispenseLine {
  id: string;
  prescriptionLineId?: string | null;
  pharmacyItemId: string;
  tradeName: string;
  batchNumber?: string | null;
  expiryDate?: string | null;
  quantityDispensed: number;
  createdAt: number;
}

export interface PharmacyDispenseRecord {
  id: string;
  prescriptionId: string;
  patientId: string;
  patientName: string;
  patientMrn: string;
  prescriberDoctorId: string;
  prescriberDoctorName: string;
  dispenserUserId: string;
  dispenserUserName: string;
  secondSignerId?: string | null;
  secondSignerName?: string | null;
  status: 'PENDING_APPROVAL' | 'DISPENSED' | 'REJECTED';
  controlled: boolean;
  notes?: string | null;
  lines: PharmacyDispenseLine[];
  createdAt: number;
}

export interface NarcoticsRegisterEntry {
  id: string;
  dispenseRecordId: string;
  pharmacyItemId: string;
  tradeName: string;
  patientMrn: string;
  patientName: string;
  prescriberDoctorName: string;
  dispenserUserName: string;
  secondSignerName: string;
  batchNumber?: string | null;
  quantity: number;
  reason?: string | null;
  signedAt: number;
}

// WP-24 Lab & Imaging
export interface LabTestItem {
  id: string;
  code: string;
  category: 'LAB' | 'IMAGING';
  name: string;
  sampleType?: string | null;
  normalRangeText?: string | null;
  price: number;
}

export interface SaveLabTestItemPayload {
  code: string;
  category: string;
  name: string;
  sampleType?: string | null;
  normalRangeText?: string | null;
  price: number;
}

export interface CreateLabOrderPayload {
  patientId: string;
  visitId?: string | null;
  doctorEmployeeId: string;
  testId: string;
  externalLabPartyId?: string | null;
  externalLabName?: string | null;
}

export interface EnterLabResultPayload {
  resultValueText: string;
  resultFlag?: string | null;
  resultNotes?: string | null;
  attachmentId?: string | null;
  attachmentFilename?: string | null;
}

export interface SendOutLabOrderPayload {
  externalLabPartyId?: string | null;
  externalLabName?: string | null;
}

export interface LabOrder {
  id: string;
  patientId: string;
  patientName: string;
  patientMrn: string;
  visitId?: string | null;
  doctorEmployeeId: string;
  doctorName: string;
  testId: string;
  category: 'LAB' | 'IMAGING';
  testCode: string;
  testName: string;
  status: 'ORDERED' | 'COLLECTED' | 'SENT_OUT' | 'RESULTED' | 'VALIDATED' | 'CANCELLED';
  orderedAt: number;
  collectedAt?: number | null;
  sentOutAt?: number | null;
  resultedAt?: number | null;
  validatedAt?: number | null;
  resultValueText?: string | null;
  resultFlag?: 'NORMAL' | 'LOW' | 'HIGH' | 'CRITICAL' | null;
  resultNotes?: string | null;
  externalLabPartyId?: string | null;
  externalLabName?: string | null;
  attachmentId?: string | null;
  attachmentFilename?: string | null;
  isCriticalAcknowledged: boolean;
  criticalAcknowledgedAt?: number | null;
}

// WP-25 Insurance & Claims
export interface InsurancePayer {
  id: string;
  name: string;
  type: 'HIO' | 'PRIVATE' | 'CORPORATE';
  contactPhone?: string | null;
  contactEmail?: string | null;
  active: boolean;
}

export interface SaveInsurancePayerPayload {
  name: string;
  type: string;
  contactPhone?: string | null;
  contactEmail?: string | null;
  active?: boolean;
}

export interface InsurancePlan {
  id: string;
  payerId: string;
  name: string;
  coveragePercent: number;
  copayFlat: number;
  annualLimit?: number | null;
  exclusionsText?: string | null;
  active: boolean;
}

export interface SaveInsurancePlanPayload {
  payerId: string;
  name: string;
  coveragePercent: number;
  copayFlat: number;
  annualLimit?: number | null;
  exclusionsText?: string | null;
  active?: boolean;
}

export interface PatientInsurancePolicy {
  id: string;
  patientId: string;
  planId: string;
  planName: string;
  payerId: string;
  payerName: string;
  memberNumber: string;
  validFrom: string;
  validTo: string;
  isPrimary: boolean;
}

export interface AttachInsurancePolicyPayload {
  patientId: string;
  planId: string;
  memberNumber: string;
  validFrom: string;
  validTo: string;
  isPrimary?: boolean;
}

export interface InsurancePreAuthorization {
  id: string;
  payerId: string;
  payerName: string;
  patientId: string;
  patientMrn: string;
  patientName: string;
  visitId?: string | null;
  procedureText: string;
  approvalCode: string;
  requestedAmount: number;
  approvedAmount?: number | null;
  status: 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'EXPIRED';
  decidedAt?: number | null;
}

export interface RequestPreAuthorizationPayload {
  payerId: string;
  patientId: string;
  visitId?: string | null;
  procedureText: string;
  approvalCode: string;
  requestedAmount: number;
}

export interface DecidePreAuthorizationPayload {
  status: 'APPROVED' | 'REJECTED';
  approvedAmount?: number | null;
}

export interface InsuranceSplitCalculationResult {
  totalFee: number;
  coveragePercent: number;
  copayFlat: number;
  insurerShare: number;
  patientShare: number;
  isPolicyValid: boolean;
}

export interface CalculateInsuranceSplitPayload {
  patientId: string;
  feeCharged: number;
  visitDate?: string | null;
}

export interface InsuranceClaimLine {
  id: string;
  batchId: string;
  visitId: string;
  patientId: string;
  patientMrn: string;
  patientName: string;
  memberNumber?: string | null;
  procedureText?: string | null;
  totalFee: number;
  insurerShare: number;
  patientShare: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  rejectionReason?: string | null;
  resubmittedLineId?: string | null;
}

export interface InsuranceClaimBatch {
  id: string;
  batchNumber: string;
  payerId: string;
  payerName: string;
  period: string;
  status: 'DRAFT' | 'SUBMITTED' | 'PARTIALLY_PAID' | 'PAID' | 'REJECTED';
  totalClaimedAmount: number;
  totalApprovedAmount: number;
  totalRejectedAmount: number;
  submittedAt?: number | null;
  settledAt?: number | null;
  notes?: string | null;
  lines: InsuranceClaimLine[];
}

export interface CreateClaimBatchPayload {
  payerId: string;
  period: string;
  notes?: string | null;
}

export interface SettleClaimBatchPayload {
  lineDecisions: Array<{
    lineId: string;
    decision: 'APPROVED' | 'REJECTED';
    rejectionReason?: string | null;
  }>;
  notes?: string | null;
}

export interface ResubmitClaimLinePayload {
  originalLineId: string;
  newBatchId: string;
  adjustedInsurerShare?: number | null;
  notes?: string | null;
}

// WP-26 Hospital Ops (ADT, OT, Nursing)
export interface HospitalWard {
  id: string;
  code: string;
  name: string;
  departmentId?: string | null;
  active: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface SaveHospitalWardPayload {
  code: string;
  name: string;
  departmentId?: string | null;
  active?: boolean;
}

export interface HospitalRoom {
  id: string;
  wardId: string;
  roomNumber: string;
  roomType: 'STANDARD' | 'ICU' | 'ISOLATION' | 'VIP';
  active: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface SaveHospitalRoomPayload {
  wardId: string;
  roomNumber: string;
  roomType?: string;
  active?: boolean;
}

export interface HospitalBed {
  id: string;
  roomId: string;
  roomNumber?: string | null;
  wardId?: string | null;
  wardName?: string | null;
  bedNumber: string;
  status: 'FREE' | 'OCCUPIED' | 'MAINTENANCE' | 'ISOLATION';
  currentAdmissionId?: string | null;
  currentPatientName?: string | null;
  currentPatientMrn?: string | null;
  active: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface SaveHospitalBedPayload {
  roomId: string;
  bedNumber: string;
  status?: string;
  active?: boolean;
}

export interface HospitalBedStay {
  id: string;
  admissionId: string;
  bedId: string;
  bedNumber?: string | null;
  roomNumber?: string | null;
  wardName?: string | null;
  startedAt: number;
  endedAt?: number | null;
  transferReason?: string | null;
}

export interface HospitalAdmission {
  id: string;
  patientId: string;
  patientMrn?: string | null;
  patientName?: string | null;
  admittingDoctorId: string;
  admittingDoctorName?: string | null;
  currentBedId?: string | null;
  currentBedNumber?: string | null;
  currentRoomNumber?: string | null;
  currentWardName?: string | null;
  status: 'ADMITTED' | 'DISCHARGED';
  chiefComplaint?: string | null;
  admittedAt: number;
  dischargedAt?: number | null;
  dischargeSummary?: string | null;
  bedStays: HospitalBedStay[];
  createdAt: number;
  updatedAt: number;
}

export interface AdmitPatientPayload {
  patientId: string;
  admittingDoctorId: string;
  bedId: string;
  chiefComplaint?: string | null;
}

export interface TransferPatientBedPayload {
  targetBedId: string;
  transferReason?: string | null;
}

export interface DischargePatientPayload {
  dischargeSummary: string;
}

export interface HospitalOccupancyMetrics {
  totalBeds: number;
  occupiedBeds: number;
  occupancyRatePercent: number;
  averageLengthOfStayDays: number;
}

export interface HospitalOtCharge {
  id: string;
  otScheduleId: string;
  itemName: string;
  quantity: number;
  unitPrice: number;
  totalAmount: number;
  chargedAt: number;
}

export interface HospitalOtSchedule {
  id: string;
  theaterName: string;
  patientId: string;
  patientMrn?: string | null;
  patientName?: string | null;
  surgeonDoctorId: string;
  surgeonDoctorName?: string | null;
  surgeryType: string;
  status: 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  plannedStart: number;
  durationMinutes: number;
  actualStart?: number | null;
  actualEnd?: number | null;
  anesthesiaNotes?: string | null;
  surgicalNotes?: string | null;
  charges: HospitalOtCharge[];
  createdAt: number;
  updatedAt: number;
}

export interface ScheduleOtPayload {
  theaterName: string;
  patientId: string;
  surgeonDoctorId: string;
  surgeryType: string;
  plannedStart: number;
  durationMinutes: number;
}

export interface CompleteOtSurgeryPayload {
  anesthesiaNotes?: string | null;
  surgicalNotes?: string | null;
}

export interface AddOtChargePayload {
  itemName: string;
  quantity: number;
  unitPrice: number;
}

export interface HospitalMarEntry {
  id: string;
  admissionId: string;
  medicationName: string;
  dose: string;
  route: string;
  dueAt: number;
  status: 'DUE' | 'GIVEN' | 'REFUSED' | 'HELD';
  administeredAt?: number | null;
  nurseId?: string | null;
  nurseName?: string | null;
  notes?: string | null;
  createdAt: number;
}

export interface CreateMarEntryPayload {
  admissionId: string;
  medicationName: string;
  dose: string;
  route?: string;
  dueAt: number;
}

export interface AdministerMarEntryPayload {
  status: 'GIVEN' | 'REFUSED' | 'HELD';
  nurseId?: string | null;
  nurseName?: string | null;
  notes?: string | null;
}

export interface HospitalFluidIoEntry {
  id: string;
  admissionId: string;
  entryTime: number;
  type: 'INTAKE' | 'OUTPUT';
  routeOrFluid: string;
  amountMl: number;
  recordedBy?: string | null;
}

export interface RecordFluidIoPayload {
  admissionId: string;
  type: 'INTAKE' | 'OUTPUT';
  routeOrFluid: string;
  amountMl: number;
  recordedBy?: string | null;
}

export interface HospitalNursingNote {
  id: string;
  admissionId: string;
  recordedAt: number;
  nurseName: string;
  noteText: string;
}

export interface AddNursingNotePayload {
  admissionId: string;
  nurseName: string;
  noteText: string;
}

// WP-27 Dental & Specialty Charting
export type ToothCondition =
  | 'HEALTHY'
  | 'CARIES'
  | 'FILLED'
  | 'CROWN'
  | 'MISSING'
  | 'IMPLANT'
  | 'ROOT_CANAL'
  | 'EXTRACTION_PLANNED';

export type ToothSurface =
  | 'OCCLUSAL'
  | 'MESIAL'
  | 'DISTAL'
  | 'BUCCAL'
  | 'LINGUAL';

export interface DentalRecord {
  id: string;
  patientId: string;
  visitId?: string | null;
  toothNumber: number;
  condition: ToothCondition;
  surface?: ToothSurface | null;
  notes?: string | null;
  notedOn: number;
}

export interface RecordToothConditionPayload {
  visitId?: string | null;
  toothNumber: number;
  condition: string;
  surface?: string | null;
  notes?: string | null;
  notedOn?: number;
}

export interface ToothStatusSummary {
  toothNumber: number;
  condition: ToothCondition;
  surface?: ToothSurface | null;
  notes?: string | null;
  notedOn: number;
}

export interface PatientOdontogram {
  patientId: string;
  patientName: string;
  patientMrn: string;
  teeth: ToothStatusSummary[];
  history: DentalRecord[];
}

export interface CreateDentalPlanPayload {
  title: string;
}

export interface AddDentalPlanItemPayload {
  toothNumber: number;
  procedureText: string;
  estimatedCost: number;
}

export interface DentalTreatmentPlanItem {
  id: string;
  planId: string;
  toothNumber: number;
  procedureText: string;
  estimatedCost: number;
  status: 'PLANNED' | 'DONE' | 'CANCELLED';
  completedAt?: number | null;
  visitId?: string | null;
}

export interface DentalTreatmentPlan {
  id: string;
  patientId: string;
  title: string;
  status: 'DRAFT' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
  createdAt: number;
  updatedAt: number;
  items: DentalTreatmentPlanItem[];
}

export interface SaveExamTemplatePayload {
  specialty: string;
  name: string;
  schemaJson: string;
}

export interface ExamTemplate {
  id: string;
  specialty: string;
  name: string;
  schemaJson: string;
  active: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface SubmitExamAnswerPayload {
  visitId: string;
  templateId: string;
  answersJson: string;
  recordedBy?: string | null;
}

export interface ExamAnswer {
  id: string;
  visitId: string;
  templateId: string;
  answersJson: string;
  recordedAt: number;
  recordedBy?: string | null;
}

// Medical Vertical Depth Models
export interface PatientFamilyLink {
  id: string;
  patientId: string;
  guardianPatientId: string;
  relationshipType: 'PARENT' | 'SPOUSE' | 'SIBLING' | 'CHILD' | 'GUARDIAN';
  isPrimaryPayer: boolean;
  notes?: string | null;
  createdAt: number;
}

export interface LinkFamilyMemberPayload {
  guardianPatientId: string;
  relationshipType: string;
  isPrimaryPayer: boolean;
  notes?: string | null;
}

export interface PediatricDoseCalculationRequest {
  weightKg: number;
  doseMgPerKgPerDay: number;
  frequencyPerDay: number;
  drugConcentrationMgPerMl?: number | null;
}

export interface PediatricDoseCalculationResponse {
  weightKg: number;
  dailyDoseMg: number;
  singleDoseMg: number;
  singleDoseMl?: number | null;
  frequencyPerDay: number;
  administrationInstructions: string;
}

export interface TelemedicineSession {
  id: string;
  patientId: string;
  doctorId: string;
  doctorName: string;
  scheduledTime: number;
  meetingLink: string;
  roomToken?: string | null;
  status: 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  clinicalNotes?: string | null;
  createdAt: number;
}

export interface ScheduleTelemedicineSessionPayload {
  patientId: string;
  doctorId: string;
  doctorName: string;
  scheduledTime: number;
  roomName?: string | null;
}

export interface MedicalLicenseRecord {
  id: string;
  practitionerId: string;
  practitionerName: string;
  licenseType: string;
  licenseNumber: string;
  issuingAuthority: string;
  issueDate: number;
  expiryDate: number;
  status: 'VALID' | 'EXPIRED' | 'EXPIRING_SOON';
  createdAt: number;
}

export interface RegisterMedicalLicensePayload {
  practitionerId: string;
  practitionerName: string;
  licenseType: string;
  licenseNumber: string;
  issuingAuthority: string;
  issueDate: number;
  expiryDate: number;
}
