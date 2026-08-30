import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Create3plContractPayload,
  CreateBookingPayload,
  CustomsDeclaration,
  OpenDeclarationPayload,
  RegisterStudentPayload,
  StudentEnrollment,
  ThreePlContract,
  TourismBooking,
  VerticalsSummary,
} from './verticals.models';

@Injectable({
  providedIn: 'root',
})
export class VerticalsService {
  private readonly http = inject(HttpClient);

  getSummary(): Observable<VerticalsSummary> {
    return this.http.get<VerticalsSummary>('/api/v1/verticals/summary');
  }

  // School
  listStudents(): Observable<StudentEnrollment[]> {
    return this.http.get<StudentEnrollment[]>('/api/v1/verticals/school/students');
  }

  registerStudent(payload: RegisterStudentPayload): Observable<StudentEnrollment> {
    return this.http.post<StudentEnrollment>('/api/v1/verticals/school/students', payload);
  }

  // Tourism
  listTourismBookings(): Observable<TourismBooking[]> {
    return this.http.get<TourismBooking[]>('/api/v1/verticals/tourism/bookings');
  }

  createTourismBooking(payload: CreateBookingPayload): Observable<TourismBooking> {
    return this.http.post<TourismBooking>('/api/v1/verticals/tourism/bookings', payload);
  }

  // Customs
  listCustomsDeclarations(): Observable<CustomsDeclaration[]> {
    return this.http.get<CustomsDeclaration[]>('/api/v1/verticals/customs/declarations');
  }

  openCustomsDeclaration(payload: OpenDeclarationPayload): Observable<CustomsDeclaration> {
    return this.http.post<CustomsDeclaration>('/api/v1/verticals/customs/declarations', payload);
  }

  // 3PL
  list3plContracts(): Observable<ThreePlContract[]> {
    return this.http.get<ThreePlContract[]>('/api/v1/verticals/3pl/contracts');
  }

  create3plContract(payload: Create3plContractPayload): Observable<ThreePlContract> {
    return this.http.post<ThreePlContract>('/api/v1/verticals/3pl/contracts', payload);
  }
}
