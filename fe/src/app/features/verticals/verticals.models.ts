export interface TuitionInvoice {
  id: string;
  enrollmentId: string;
  invoiceNumber: string;
  installmentName: string;
  dueDate: number;
  amountDue: number;
  amountPaid: number;
  status: string;
  createdAt: number;
}

export interface StudentEnrollment {
  id: string;
  studentCode: string;
  studentName: string;
  gradeLevel: string;
  academicYear: string;
  guardianName: string;
  guardianPhone?: string;
  totalTuitionFee: number;
  transportFee: number;
  booksFee: number;
  totalAnnualDue: number;
  status: string;
  createdAt: number;
  installmentInvoices: TuitionInvoice[];
}

export interface RegisterStudentPayload {
  studentCode: string;
  studentName: string;
  gradeLevel: string;
  academicYear: string;
  guardianName: string;
  guardianPhone?: string;
  totalTuitionFee: number;
  transportFee?: number;
  booksFee?: number;
  installmentsCount?: number;
}

export interface TourismBooking {
  id: string;
  bookingCode: string;
  customerName: string;
  packageName: string;
  destination: string;
  travelDate: number;
  returnDate: number;
  travelersCount: number;
  sellingPrice: number;
  hotelCost: number;
  flightCost: number;
  excursionCost: number;
  totalDirectCost: number;
  grossMargin: number;
  grossMarginPercentage: number;
  status: string;
  createdAt: number;
}

export interface CreateBookingPayload {
  bookingCode: string;
  customerName: string;
  packageName: string;
  destination: string;
  travelDate: number;
  returnDate: number;
  travelersCount: number;
  sellingPrice: number;
  hotelCost?: number;
  flightCost?: number;
  excursionCost?: number;
}

export interface CustomsDeclaration {
  id: string;
  fileNumber: string;
  importerName: string;
  portOfEntry: string;
  billOfLadingNumber: string;
  customsCertificateNumber?: string;
  dutyDisbursementAmount: number;
  portHandlingAmount: number;
  clearanceServiceFee: number;
  totalInvoiceAmount: number;
  status: string;
  createdAt: number;
}

export interface OpenDeclarationPayload {
  fileNumber: string;
  importerName: string;
  portOfEntry: string;
  billOfLadingNumber: string;
  customsCertificateNumber?: string;
  dutyDisbursementAmount?: number;
  portHandlingAmount?: number;
  clearanceServiceFee: number;
}

export interface ThreePlContract {
  id: string;
  contractCode: string;
  clientName: string;
  warehouseName: string;
  palletCapacity: number;
  ratePerPalletMonthly: number;
  handlingInRatePerPallet: number;
  handlingOutRatePerPallet: number;
  estimatedMonthlyRevenue: number;
  billingFrequency: string;
  status: string;
  createdAt: number;
}

export interface Create3plContractPayload {
  contractCode: string;
  clientName: string;
  warehouseName: string;
  palletCapacity: number;
  ratePerPalletMonthly: number;
  handlingInRatePerPallet?: number;
  handlingOutRatePerPallet?: number;
  billingFrequency?: string;
}

export interface VerticalsSummary {
  totalActiveStudents: number;
  totalTuitionBilled: number;
  totalActiveBookings: number;
  totalTourismRevenue: number;
  averageTourismMarginPct: number;
  totalOpenCustomsFiles: number;
  totalDutyDisbursements: number;
  totalActive3plContracts: number;
  total3plPalletCapacity: number;
}
