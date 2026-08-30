import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import {
  CertifyClaimRequest,
  CreateProgressClaimRequest,
  ProjectProgressClaim,
  UpdateProgressClaimRequest,
} from '../models/claim.models';

@Injectable({
  providedIn: 'root',
})
export class ClaimService {
  private readonly http = inject(HttpClient);
  private readonly i18n = inject(I18nService);

  readonly claims = signal<ProjectProgressClaim[]>([]);
  readonly currentClaim = signal<ProjectProgressClaim | null>(null);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  loadClaims(projectId: string): Observable<ProjectProgressClaim[]> {
    this.loading.set(true);
    this.error.set(null);
    return this.http.get<ProjectProgressClaim[]>(`/api/v1/projects/${projectId}/claims`).pipe(
      tap({
        next: (res) => {
          this.claims.set(res);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(err.message || this.i18n.t('projects.loadClaimsFailed'));
          this.loading.set(false);
        },
      })
    );
  }

  getClaim(id: string): Observable<ProjectProgressClaim> {
    this.loading.set(true);
    return this.http.get<ProjectProgressClaim>(`/api/v1/project-claims/${id}`).pipe(
      tap({
        next: (res) => {
          this.currentClaim.set(res);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(err.message || this.i18n.t('projects.fetchClaimFailed'));
          this.loading.set(false);
        },
      })
    );
  }

  createClaim(projectId: string, req: CreateProgressClaimRequest): Observable<ProjectProgressClaim> {
    return this.http.post<ProjectProgressClaim>(`/api/v1/projects/${projectId}/claims`, req).pipe(
      tap((res) => {
        this.claims.update((prev) => [res, ...prev]);
        this.currentClaim.set(res);
      })
    );
  }

  updateDraftClaim(id: string, req: UpdateProgressClaimRequest): Observable<ProjectProgressClaim> {
    return this.http.put<ProjectProgressClaim>(`/api/v1/project-claims/${id}`, req).pipe(
      tap((res) => {
        this.claims.update((prev) => prev.map((c) => (c.id === id ? res : c)));
        this.currentClaim.set(res);
      })
    );
  }

  deleteDraftClaim(id: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/project-claims/${id}`).pipe(
      tap(() => {
        this.claims.update((prev) => prev.filter((c) => c.id !== id));
        if (this.currentClaim()?.id === id) {
          this.currentClaim.set(null);
        }
      })
    );
  }

  submitClaim(id: string): Observable<ProjectProgressClaim> {
    return this.http.post<ProjectProgressClaim>(`/api/v1/project-claims/${id}/submit`, {}).pipe(
      tap((res) => {
        this.claims.update((prev) => prev.map((c) => (c.id === id ? res : c)));
        this.currentClaim.set(res);
      })
    );
  }

  reviewClaim(id: string): Observable<ProjectProgressClaim> {
    return this.http.post<ProjectProgressClaim>(`/api/v1/project-claims/${id}/review`, {}).pipe(
      tap((res) => {
        this.claims.update((prev) => prev.map((c) => (c.id === id ? res : c)));
        this.currentClaim.set(res);
      })
    );
  }

  certifyClaim(id: string, req: CertifyClaimRequest): Observable<ProjectProgressClaim> {
    return this.http.post<ProjectProgressClaim>(`/api/v1/project-claims/${id}/certify`, req).pipe(
      tap((res) => {
        this.claims.update((prev) => prev.map((c) => (c.id === id ? res : c)));
        this.currentClaim.set(res);
      })
    );
  }

  postClaimToFinance(id: string): Observable<ProjectProgressClaim> {
    return this.http.post<ProjectProgressClaim>(`/api/v1/project-claims/${id}/post-finance`, {}).pipe(
      tap((res) => {
        this.claims.update((prev) => prev.map((c) => (c.id === id ? res : c)));
        this.currentClaim.set(res);
      })
    );
  }

  cancelClaim(id: string): Observable<ProjectProgressClaim> {
    return this.http.post<ProjectProgressClaim>(`/api/v1/project-claims/${id}/cancel`, {}).pipe(
      tap((res) => {
        this.claims.update((prev) => prev.map((c) => (c.id === id ? res : c)));
        this.currentClaim.set(res);
      })
    );
  }
}
