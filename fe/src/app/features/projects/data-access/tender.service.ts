import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import {
  ProjectTender,
  TenderBoqItem,
  TenderBidder,
  TenderClarification,
  TenderEvaluationSummary,
  CreateTenderRequest,
  UpdateTenderRequest,
  CreateBoqItemRequest,
  InviteBidderRequest,
  SubmitBidRequest,
  RecordBidBondRequest,
  TechnicalEvaluationRequest,
  CreateClarificationRequest,
  AnswerClarificationRequest,
  AwardTenderRequest
} from '../models/tender.models';

@Injectable({
  providedIn: 'root'
})
export class TenderService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/project-tenders';

  readonly tenders = signal<ProjectTender[]>([]);
  readonly selectedTender = signal<ProjectTender | null>(null);
  readonly evaluationSummary = signal<TenderEvaluationSummary | null>(null);
  readonly loading = signal<boolean>(false);

  loadTenders(): Observable<ProjectTender[]> {
    this.loading.set(true);
    return this.http.get<ProjectTender[]>(this.baseUrl).pipe(
      tap({
        next: list => {
          this.tenders.set(list);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      })
    );
  }

  loadTender(id: string): Observable<ProjectTender> {
    this.loading.set(true);
    return this.http.get<ProjectTender>(`${this.baseUrl}/${id}`).pipe(
      tap({
        next: t => {
          this.selectedTender.set(t);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      })
    );
  }

  createTender(req: CreateTenderRequest): Observable<ProjectTender> {
    return this.http.post<ProjectTender>(this.baseUrl, req).pipe(
      tap(created => {
        this.tenders.update(prev => [created, ...prev]);
        this.selectedTender.set(created);
      })
    );
  }

  updateTender(id: string, req: UpdateTenderRequest): Observable<ProjectTender> {
    return this.http.put<ProjectTender>(`${this.baseUrl}/${id}`, req).pipe(
      tap(updated => {
        this.tenders.update(prev => prev.map(t => (t.id === id ? updated : t)));
        this.selectedTender.set(updated);
      })
    );
  }

  deleteTender(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`).pipe(
      tap(() => {
        this.tenders.update(prev => prev.filter(t => t.id !== id));
        if (this.selectedTender()?.id === id) {
          this.selectedTender.set(null);
        }
      })
    );
  }

  publishTender(id: string): Observable<ProjectTender> {
    return this.http.post<ProjectTender>(`${this.baseUrl}/${id}/publish`, {}).pipe(
      tap(pub => {
        this.tenders.update(prev => prev.map(t => (t.id === id ? pub : t)));
        this.selectedTender.set(pub);
      })
    );
  }

  cancelTender(id: string): Observable<ProjectTender> {
    return this.http.post<ProjectTender>(`${this.baseUrl}/${id}/cancel`, {}).pipe(
      tap(c => {
        this.tenders.update(prev => prev.map(t => (t.id === id ? c : t)));
        this.selectedTender.set(c);
      })
    );
  }

  addBoqItem(id: string, req: CreateBoqItemRequest): Observable<TenderBoqItem> {
    return this.http.post<TenderBoqItem>(`${this.baseUrl}/${id}/boq`, req).pipe(
      tap(() => this.loadTender(id).subscribe())
    );
  }

  updateBoqItem(id: string, itemId: string, req: CreateBoqItemRequest): Observable<TenderBoqItem> {
    return this.http.put<TenderBoqItem>(`${this.baseUrl}/${id}/boq/${itemId}`, req).pipe(
      tap(() => this.loadTender(id).subscribe())
    );
  }

  deleteBoqItem(id: string, itemId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}/boq/${itemId}`).pipe(
      tap(() => this.loadTender(id).subscribe())
    );
  }

  inviteBidder(id: string, req: InviteBidderRequest): Observable<TenderBidder> {
    return this.http.post<TenderBidder>(`${this.baseUrl}/${id}/bidders`, req).pipe(
      tap(() => this.loadTender(id).subscribe())
    );
  }

  submitBid(id: string, bidderId: string, req: SubmitBidRequest): Observable<TenderBidder> {
    return this.http.post<TenderBidder>(`${this.baseUrl}/${id}/bidders/${bidderId}/submit`, req).pipe(
      tap(() => this.loadTender(id).subscribe())
    );
  }

  recordBidBond(id: string, bidderId: string, req: RecordBidBondRequest): Observable<TenderBidder> {
    return this.http.post<TenderBidder>(`${this.baseUrl}/${id}/bidders/${bidderId}/bid-bond`, req).pipe(
      tap(() => this.loadTender(id).subscribe())
    );
  }

  evaluateBidderTechnical(id: string, bidderId: string, req: TechnicalEvaluationRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${id}/bidders/${bidderId}/technical-eval`, req).pipe(
      tap(() => this.loadTender(id).subscribe())
    );
  }

  calculateEvaluation(id: string): Observable<TenderEvaluationSummary> {
    return this.http.post<TenderEvaluationSummary>(`${this.baseUrl}/${id}/evaluate`, {}).pipe(
      tap(summary => {
        this.evaluationSummary.set(summary);
        this.loadTender(id).subscribe();
      })
    );
  }

  awardTender(id: string, req: AwardTenderRequest): Observable<ProjectTender> {
    return this.http.post<ProjectTender>(`${this.baseUrl}/${id}/award`, req).pipe(
      tap(awarded => {
        this.tenders.update(prev => prev.map(t => (t.id === id ? awarded : t)));
        this.selectedTender.set(awarded);
      })
    );
  }

  addClarification(id: string, req: CreateClarificationRequest): Observable<TenderClarification> {
    return this.http.post<TenderClarification>(`${this.baseUrl}/${id}/clarifications`, req).pipe(
      tap(() => this.loadTender(id).subscribe())
    );
  }

  answerClarification(id: string, clarifId: string, req: AnswerClarificationRequest): Observable<TenderClarification> {
    return this.http.put<TenderClarification>(`${this.baseUrl}/${id}/clarifications/${clarifId}`, req).pipe(
      tap(() => this.loadTender(id).subscribe())
    );
  }
}
