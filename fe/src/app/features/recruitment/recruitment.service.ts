import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ApplicationStage,
  CreateApplicationRequest,
  CreateOpeningRequest,
  DuplicateWarning,
  JobApplication,
  JobOpening,
  StageEvent,
  UpdateOpeningRequest,
} from './recruitment.models';

@Injectable({ providedIn: 'root' })
export class RecruitmentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/recruitment';

  getOpenings(): Observable<JobOpening[]> {
    return this.http.get<JobOpening[]>(`${this.baseUrl}/openings`);
  }

  createOpening(req: CreateOpeningRequest): Observable<JobOpening> {
    return this.http.post<JobOpening>(`${this.baseUrl}/openings`, req);
  }

  updateOpening(id: string, req: UpdateOpeningRequest): Observable<JobOpening> {
    return this.http.put<JobOpening>(`${this.baseUrl}/openings/${id}`, req);
  }

  closeOpening(id: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/openings/${id}/close`, {});
  }

  getApplications(openingId?: string): Observable<JobApplication[]> {
    let params = new HttpParams();
    if (openingId) params = params.set('openingId', openingId);
    return this.http.get<JobApplication[]>(`${this.baseUrl}/applications`, { params });
  }

  createApplication(req: CreateApplicationRequest): Observable<JobApplication> {
    return this.http.post<JobApplication>(`${this.baseUrl}/applications`, req);
  }

  moveStage(id: string, toStage: ApplicationStage, note?: string): Observable<JobApplication> {
    return this.http.post<JobApplication>(`${this.baseUrl}/applications/${id}/move-stage`, { toStage, note });
  }

  updateRating(id: string, rating: number): Observable<JobApplication> {
    return this.http.put<JobApplication>(`${this.baseUrl}/applications/${id}/rating`, null, {
      params: new HttpParams().set('rating', String(rating)),
    });
  }

  updateNotes(id: string, notes: string): Observable<JobApplication> {
    return this.http.put<JobApplication>(`${this.baseUrl}/applications/${id}/notes`, notes);
  }

  getStageEvents(id: string): Observable<StageEvent[]> {
    return this.http.get<StageEvent[]>(`${this.baseUrl}/applications/${id}/stage-events`);
  }

  convertToEmployee(id: string, departmentId?: string): Observable<{ employeeId: string; applicationId: string }> {
    return this.http.post<{ employeeId: string; applicationId: string }>(
      `${this.baseUrl}/applications/${id}/convert`, { departmentId });
  }

  checkDuplicates(phone?: string, email?: string): Observable<DuplicateWarning[]> {
    let params = new HttpParams();
    if (phone) params = params.set('phone', phone);
    if (email) params = params.set('email', email);
    return this.http.get<DuplicateWarning[]>(`${this.baseUrl}/applications/duplicates`, { params });
  }

  uploadCv(id: string, file: File): Observable<{ cvAttachmentId: string }> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<{ cvAttachmentId: string }>(`${this.baseUrl}/applications/${id}/cv`, form);
  }

  cvDownloadUrl(id: string): string {
    return `${this.baseUrl}/applications/${id}/cv`;
  }

  listDepartments(): Observable<Department[]> {
    return this.http.get<Department[]>('/api/v1/organization/departments');
  }
}

export interface Department {
  id: string;
  code: string;
  name: string;
  active: boolean;
}